package com.memuo.core.ai.engine                          // 声明包名：AI 引擎模块

import android.app.ActivityManager                        // 导入 ActivityManager：内存信息
import android.content.Context                            // 导入 Context：系统服务
import android.os.Build                                   // 导入 Build：设备信息/ABI
import com.memuo.core.storage.StorageProvider             // 导入存储提供者（模型目录）
import dagger.hilt.android.qualifiers.ApplicationContext  // 导入 ApplicationContext 限定符
import kotlinx.coroutines.Dispatchers                      // 导入 Dispatchers：IO 调度器
import kotlinx.coroutines.withContext                      // 导入 withContext：切线程
import java.io.File                                        // 导入 File：模型文件
import java.text.SimpleDateFormat                          // 导入 SimpleDateFormat：时间戳
import java.util.Date                                      // 导入 Date：时间
import java.util.Locale                                    // 导入 Locale：区域
import javax.inject.Inject                                 // 导入 Inject：构造函数注入
import javax.inject.Singleton                              // 导入 Singleton：单例作用域

/**
 * 本地模型加载诊断器（ModelLoadDiagnostics）—— 生成完整诊断日志，定位「模型为什么加载失败」。
 *
 * 诊断内容（M-031 新增，解决「错误被吞、看不到具体原因」的痛点）：
 *  1. 环境信息：Android 版本 / ABI / 设备型号 / 总内存 / 可用内存；
 *  2. 模型文件清单：每个必需文件的 存在性 + 字节大小；
 *  3. 文件大小校验：文本权重 / 视觉权重 / 视觉结构 是否达合理下限；
 *  4. 内存评估：模型权重所需 vs 当前可用，是否够；
 *  5. 加载测试：真实调用 [MnnLlmNative.nativeInit] 试加载，记录 耗时 / 返回 / 具体错误；
 *  6. 结论：一句话定位根因 + 建议。
 *
 * 输出为纯文本日志，既返回给 UI 展示/复制，也可由调用方写入文件。
 */
@Singleton                                               // 单例
class ModelLoadDiagnostics @Inject constructor(          // 构造函数注入
    @ApplicationContext private val context: Context,    // 注入应用上下文
    private val storage: StorageProvider,                // 注入存储提供者（模型目录）
    private val localEngine: LocalChatEngine,            // 注入本地引擎（判断是否已加载，M-034 防重复加载闪退）
) {

    /** 必需文件清单（Qwen3.5 多模态：文本 + 视觉）。 */
    private val REQUIRED_FILES = listOf(                  // 必需文件
        "config.json", "llm.mnn", "llm.mnn.weight", "tokenizer.txt",  // 文本 LLM 核心
        "llm.mnn.json",                                   // 模型结构 JSON（新版 MNN 必需）
        "visual.mnn", "visual.mnn.weight",                // 多模态视觉模型
    )

    /**
     * 执行完整诊断，返回日志文本。
     * 注意：会真实调用 nativeInit 试加载（2B 模型约 10~30 秒），务必在 IO 线程调用。
     */
    suspend fun runDiagnostics(): String = withContext(Dispatchers.IO) {  // IO 线程
        val sb = StringBuilder()                         // 日志缓冲
        val sep = "=".repeat(52)                         // 分隔线
        sb.appendLine(sep)                               // 顶部分隔
        sb.appendLine("沐云杪 AI · 本地模型加载诊断日志")  // 标题
        sb.appendLine("生成时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")  // 时间戳
        sb.appendLine(sep)                                // 分隔
        sb.appendLine()

        // ① 环境信息
        sb.appendLine("【一、环境信息】")
        sb.appendLine("  · Android 版本: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        sb.appendLine("  · 设备型号: ${Build.MANUFACTURER} ${Build.MODEL}")
        sb.appendLine("  · 主 ABI: ${Build.SUPPORTED_ABIS.firstOrNull() ?: "未知"}")
        sb.appendLine("  · 支持 ABI: ${Build.SUPPORTED_ABIS.joinToString(", ")}")
        val mem = readMemory()                            // 内存
        sb.appendLine("  · 总内存: ${fmtGb(mem.first)} GB")
        sb.appendLine("  · 可用内存: ${fmtGb(mem.second)} GB")
        sb.appendLine()

        // ② 模型目录
        val modelDir = File(storage.modelsDir(), "llm")   // 模型目录
        sb.appendLine("【二、模型目录】")
        sb.appendLine("  · 路径: ${modelDir.absolutePath}")
        sb.appendLine("  · 目录存在: ${if (modelDir.exists()) "是" else "否 ← 未导入模型！"}")
        sb.appendLine()

        // ③ 文件清单
        sb.appendLine("【三、模型文件清单】")
        val missing = mutableListOf<String>()             // 缺失文件
        if (modelDir.exists()) {
            // 列出目录下所有文件（含非必需，帮助发现异常）
            modelDir.listFiles()?.sortedBy { it.name }?.forEach { f ->  // 遍历
                val marker = if (f.name in REQUIRED_FILES) "" else "（非必需）"  // 标记
                sb.appendLine("  · ${f.name}: ${if (f.isFile) fmtBytes(f.length()) else "[目录]"} $marker")
            }
            REQUIRED_FILES.forEach { name ->              // 检查必需文件
                if (!File(modelDir, name).exists()) missing += name  // 缺失记录
            }
        }
        sb.appendLine()

        // ④ 文件大小校验
        sb.appendLine("【四、文件大小校验】")
        val sizeIssues = mutableListOf<String>()          // 大小异常
        fun checkFile(name: String, minBytes: Long, desc: String) {  // 校验辅助
            val f = File(modelDir, name)                  // 文件
            val ok = f.exists() && f.length() >= minBytes  // 存在且达标
            sb.appendLine("  · $name: ${if (f.exists()) fmtBytes(f.length()) else "缺失"}，要求 ≥ ${fmtBytes(minBytes)} ${if (ok) "✓" else "✗ ← $desc"}")
            if (!ok) sizeIssues += name                   // 记录异常
        }
        checkFile("llm.mnn.weight", 100L * 1024 * 1024, "文本权重不完整")
        checkFile("visual.mnn.weight", 30L * 1024 * 1024, "视觉权重不完整")
        checkFile("llm.mnn", 100L * 1024, "模型结构文件损坏")
        checkFile("visual.mnn", 100L * 1024, "视觉结构文件损坏")
        checkFile("tokenizer.txt", 1024, "词表文件损坏")
        sb.appendLine()

        // ⑤ 内存评估
        sb.appendLine("【五、内存评估】")
        val weightFile = File(modelDir, "llm.mnn.weight") // 权重文件
        val visualWeightFile = File(modelDir, "visual.mnn.weight")  // 视觉权重
        if (weightFile.exists()) {
            val weightMb = weightFile.length() / (1024 * 1024)  // 权重 MB
            val visualMb = visualWeightFile.length() / (1024 * 1024)  // 视觉 MB
            val totalMb = weightMb + visualMb             // 总权重
            val requiredMb = totalMb * 3 / 2 + 256        // 估算所需
            val availMb = mem.second                      // 可用内存 MB
            val enough = availMb >= requiredMb            // 是否够
            sb.appendLine("  · 文本权重 ${weightMb}MB + 视觉权重 ${visualMb}MB = 共 ${totalMb}MB")
            sb.appendLine("  · 估算运行需约 ${requiredMb}MB 可用内存")
            sb.appendLine("  · 当前可用 ${availMb}MB → ${if (enough) "足够 ✓" else "不足 ✗"}")
        }
        sb.appendLine()

        // ⑥ 加载测试（M-034：模型已加载则跳过真实加载，避免重复 nativeInit 内存翻倍导致闪退）
        sb.appendLine("【六、加载测试】")
        var loadOk = false                                // 加载结果
        var loadError = ""                                // 加载错误
        if (localEngine.isModelLoaded()) {                // 模型已加载（运行中）
            sb.appendLine("  · 模型当前已加载（正在运行），跳过重复加载测试")
            sb.appendLine("  · 结果: 已加载 ✓（避免重复加载导致内存翻倍崩溃）")
            loadOk = true                                 // 视为加载成功
        } else {                                          // 模型未加载 → 真实试加载
            sb.appendLine("  · 正在调用 nativeInit 试加载模型（2B 模型约需 10~30 秒，请稍候）...")
            val start = System.currentTimeMillis()        // 计时
            var ptr = 0L                                  // native 指针
            try {                                         // 容错
                ptr = MnnLlmNative.nativeInit(modelDir.absolutePath)  // 真实加载
                loadOk = ptr != 0L                        // 非 0 即成功
                if (!loadOk) {                            // 失败
                    loadError = MnnLlmNative.nativeGetLoadError()  // 拿具体错误
                }
            } catch (e: Throwable) {                      // 异常（native 崩溃等）
                loadError = "nativeInit 抛出异常: ${e.javaClass.simpleName}: ${e.message}"  // 记录异常
            } finally {                                   // 清理
                val cost = System.currentTimeMillis() - start  // 耗时
                sb.appendLine("  · 耗时: ${cost}ms")
                sb.appendLine("  · 结果: ${if (loadOk) "加载成功 ✓" else "加载失败 ✗"}")
                if (loadError.isNotBlank()) sb.appendLine("  · 具体错误: $loadError")
                else if (!loadOk) sb.appendLine("  · 具体错误: （空，MNN 未返回任何错误日志）")
                if (ptr != 0L) MnnLlmNative.nativeRelease(ptr)  // 试加载成功则释放
            }
        }
        sb.appendLine()

        // ⑦ 结论
        sb.appendLine("【七、结论】")
        sb.appendLine(when {
            missing.isNotEmpty() -> "模型文件缺失: ${missing.joinToString("、")}。请重新导入完整模型目录。"
            sizeIssues.isNotEmpty() -> "文件不完整: ${sizeIssues.joinToString("、")}。可能是导入时复制中断，请删除后重新导入。"
            loadOk -> "模型加载成功，本地推理可用。若对话仍异常，请检查是否误选了云端引擎。"
            loadError.isNotBlank() -> "加载失败原因: $loadError"
            else -> "加载失败，但 MNN 未返回具体错误。请检查模型是否为 MNN 官方导出的 Qwen 模型。"
        })
        sb.appendLine()
        sb.appendLine(sep)                                // 底部分隔
        sb.toString()                                     // 返回日志文本
    }

    /** 读内存（返回 Pair<总MB, 可用MB>）。 */
    private fun readMemory(): Pair<Long, Long> {          // 读内存
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager  // 内存服务
        val info = ActivityManager.MemoryInfo()           // 内存容器
        am.getMemoryInfo(info)                            // 读取
        return (info.totalMem / 1024 / 1024) to (info.availMem / 1024 / 1024)  // 字节→MB
    }

    /** MB → GB 格式化（保留 1 位小数）。 */
    private fun fmtGb(mb: Long): String = "%.1f".format(Locale.getDefault(), mb / 1024.0)  // 转 GB

    /** 字节格式化。 */
    private fun fmtBytes(b: Long): String = when {        // 格式化
        b >= 1L shl 30 -> "%.2f GB".format(Locale.getDefault(), b / 1024.0 / 1024 / 1024)
        b >= 1L shl 20 -> "%.1f MB".format(Locale.getDefault(), b / 1024.0 / 1024)
        b >= 1L shl 10 -> "%.0f KB".format(Locale.getDefault(), b / 1024.0)
        else -> "$b B"
    }
}
