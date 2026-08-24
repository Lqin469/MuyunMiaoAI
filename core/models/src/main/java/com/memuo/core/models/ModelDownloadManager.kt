package com.memuo.core.models                            // 声明包名：模型管理模块

import com.memuo.core.storage.StorageProvider             // 导入存储提供者（模型目标目录）
import kotlinx.coroutines.Dispatchers                      // 导入调度器（IO 线程下载）
import kotlinx.coroutines.withContext                      // 导入 withContext
import okhttp3.OkHttpClient                               // 导入 OkHttp 客户端
import okhttp3.Request                                    // 导入 Request
import java.io.File                                        // 导入 File
import javax.inject.Inject                                 // 导入 Inject
import javax.inject.Named                                  // 导入 Named：限定符
import javax.inject.Singleton                              // 导入 Singleton

/**
 * 模型下载管理器（ModelDownloadManager）—— 从 ModelScope 下载 Qwen3.5-0.8B-MNN 到 modelsDir()/llm/（R2）。
 * MVP：顺序下载必需文件 + 文件级进度；断点续传与 sha256 校验后续增强。
 */
@Singleton                                               // 单例
class ModelDownloadManager @Inject constructor(          // 构造函数注入
    @Named("modelDownload") private val okHttp: OkHttpClient,  // 注入下载专用 OkHttp
    private val storage: StorageProvider,                // 注入存储提供者
) {
    companion object {                                    // 常量
        private const val BASE = "https://modelscope.cn/models/MNN/Qwen3.5-0.8B-MNN/resolve/master"  // 下载基地址

        /** 必需下载文件（与导入必需清单一致，含视觉模型）。 */
        val FILES = listOf(                               // 文件清单
            "config.json", "llm.mnn", "llm.mnn.json", "llm.mnn.weight",
            "tokenizer.txt", "visual.mnn", "visual.mnn.weight", "llm_config.json",
        )
    }

    /**
     * 下载模型到 modelsDir()/llm/。
     * @param onProgress 进度回调（已下载文件数, 总文件数）
     * @return 全部下载成功返回 true
     */
    suspend fun download(onProgress: (Int, Int) -> Unit): Boolean = withContext(Dispatchers.IO) {  // IO 线程
        val target = File(storage.modelsDir(), "llm").apply { mkdirs() }  // 目标目录
        FILES.forEachIndexed { i, name ->                 // 逐个下载
            onProgress(i, FILES.size)                     // 报告进度
            if (!downloadFile("$BASE/$name", File(target, name))) return@withContext false  // 失败则中止
        }
        onProgress(FILES.size, FILES.size)                // 完成
        true                                              // 成功
    }

    /** 下载单个文件到目标路径。 */
    private fun downloadFile(url: String, target: File): Boolean {  // 单文件下载
        return runCatching {                              // 捕获异常
            val req = Request.Builder().url(url).build()  // 构造请求
            okHttp.newCall(req).execute().use { resp ->   // 同步执行
                if (!resp.isSuccessful) return@runCatching false  // HTTP 错误
                resp.body?.byteStream()?.use { input ->   // 读响应流
                    target.outputStream().use { output -> input.copyTo(output) }  // 写本地
                }
            }
            target.exists() && target.length() > 0        // 校验非空
        }.getOrDefault(false)                             // 异常返回 false
    }
}
