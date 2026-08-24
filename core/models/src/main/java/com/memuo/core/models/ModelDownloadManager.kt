package com.memuo.core.models                            // 声明包名：模型管理模块

import com.memuo.core.storage.StorageProvider             // 导入存储提供者（模型目标目录）
import kotlinx.coroutines.Dispatchers                      // 导入调度器（IO 线程下载）
import kotlinx.coroutines.withContext                      // 导入 withContext
import okhttp3.OkHttpClient                               // 导入 OkHttp 客户端
import okhttp3.Request                                    // 导入 Request
import java.io.File                                        // 导入 File
import java.io.FileOutputStream                            // 导入 FileOutputStream：支持追加写（断点续传）
import javax.inject.Inject                                 // 导入 Inject
import javax.inject.Named                                  // 导入 Named：限定符
import javax.inject.Singleton                              // 导入 Singleton

/**
 * 模型下载管理器（ModelDownloadManager）—— 从 ModelScope 下载 Qwen3.5-0.8B-MNN 到 modelsDir()/llm/（R2）。
 * ModelScope resolve URL 会 302 重定向到 CDN（需 UA/Referer）；逐文件下载 + 文件级进度 + 具体错误返回。
 */
@Singleton                                               // 单例
class ModelDownloadManager @Inject constructor(          // 构造函数注入
    @Named("modelDownload") private val okHttp: OkHttpClient,  // 注入下载专用 OkHttp
    private val storage: StorageProvider,                // 注入存储提供者
) {
    companion object {                                    // 常量
        private const val BASE = "https://modelscope.cn/models/MNN/Qwen3.5-0.8B-MNN/resolve/master"  // 下载基地址
        private const val UA = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"  // 浏览器 UA

        /** 必需下载文件（与导入必需清单一致，含视觉模型）。 */
        val FILES = listOf(                               // 文件清单
            "config.json", "llm.mnn", "llm.mnn.json", "llm.mnn.weight",
            "tokenizer.txt", "visual.mnn", "visual.mnn.weight", "llm_config.json",
        )
    }

    /**
     * 下载模型到 modelsDir()/llm/。
     * @param onProgress 进度回调（已下载文件数, 总文件数）
     * @return null 表示全部成功；非 null 为具体错误信息
     */
    suspend fun download(onProgress: (Int, Int) -> Unit): String? = withContext(Dispatchers.IO) {  // IO 线程
        val target = File(storage.modelsDir(), "llm").apply { mkdirs() }  // 目标目录
        FILES.forEachIndexed { i, name ->                 // 逐个下载
            onProgress(i, FILES.size)                     // 报告进度
            val err = downloadFile("$BASE/$name", File(target, name))  // 下载单文件
            if (err != null) return@withContext "下载 $name 失败：$err"  // 有错误则中止并返回
        }
        onProgress(FILES.size, FILES.size)                // 完成
        null                                              // 成功
    }

    /** 下载单个文件（带断点续传 + 重试）；返回 null 表示成功，非 null 为错误描述。 */
    private fun downloadFile(url: String, target: File): String? {  // 单文件下载（带重试）
        var lastErr = "未知错误"                          // 最后一次错误
        repeat(3) {                                       // 最多重试 3 次
            val err = downloadOnce(url, target)           // 单次下载
            if (err == null) return null                  // 成功
            lastErr = err                                 // 记录错误
        }
        return lastErr                                    // 重试耗尽返回错误
    }

    /** 单次下载：先 HEAD 判断是否已完整（避免对完整文件发 Range 导致 404），未完整则断点续传。 */
    private fun downloadOnce(url: String, target: File): String? {  // 单次下载
        return runCatching {                              // 捕获异常
            // 1. HEAD 请求获取文件完整大小（用于判断是否已下载完整）
            val total = runCatching {                     // HEAD 可能失败，容错
                okHttp.newCall(                            // HEAD 请求
                    Request.Builder().url(url)
                        .header("User-Agent", UA)
                        .header("Referer", "https://modelscope.cn/")
                        .head().build()
                ).execute().use { resp ->                 // 执行 HEAD
                    if (resp.isSuccessful) resp.header("Content-Length")?.toLongOrNull() ?: -1L else -1L
                }
            }.getOrDefault(-1L)                           // 失败返回 -1（未知大小）

            // 2. 已下载完整 → 直接跳过（避免 Range 导致 404/416）
            if (total > 0 && target.exists() && target.length() >= total) return@runCatching null

            // 3. 断点续传（仅对部分下载的文件发 Range）
            val existing = if (target.exists()) target.length() else 0L  // 已下载字节
            val req = Request.Builder()                   // 构造请求
                .url(url)                                 // URL
                .header("User-Agent", UA)                 // 浏览器 UA（CDN 校验）
                .header("Referer", "https://modelscope.cn/")  // 来源（CDN 校验）
                .apply {                                  // 断点续传
                    if (existing > 0 && (total < 0 || existing < total)) header("Range", "bytes=$existing-")  // 部分文件才续传
                }
                .build()                                  // 构建
            okHttp.newCall(req).execute().use { resp ->   // 同步执行
                when {                                    // 按状态码处理
                    resp.code == 416 -> return@runCatching null  // 416 = 已完整（Range 越界），视为成功
                    resp.code == 206 || resp.isSuccessful -> {  // 206 续传 / 200 完整
                        val resume = resp.code == 206     // 是否续传（追加写）
                        resp.body?.byteStream()?.use { input ->  // 读响应流
                            FileOutputStream(target, resume).use { output -> input.copyTo(output) }  // 追加/覆盖
                        }
                    }
                    else -> return@runCatching "HTTP ${resp.code}"  // 其他错误
                }
            }
            if (target.exists() && target.length() > 0) null else "文件为空"  // 校验非空
        }.getOrElse { it.message ?: "网络错误" }           // 异常信息
    }
}
