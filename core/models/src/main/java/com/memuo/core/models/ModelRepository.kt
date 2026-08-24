package com.memuo.core.models                            // 声明包名：模型管理模块

import dagger.Module                                      // 导入 Module：Hilt 模块注解
import dagger.Provides                                    // 导入 Provides：Hilt 提供方法注解
import dagger.hilt.InstallIn                              // 导入 InstallIn：指定安装组件
import dagger.hilt.components.SingletonComponent          // 导入 SingletonComponent：应用级单例组件
import javax.inject.Inject                                 // 导入 Inject：构造函数注入
import javax.inject.Singleton                              // 导入 Singleton：单例作用域

/**
 * 模型仓库（ModelRepository）—— 集中管理模型目录、本地导入与硬件画像（M6）。
 * v1：返回硬编码推荐目录 + 读取 Build.MODEL / Runtime.totalMemory 的简化硬件画像；
 * M6+ 完善：按需下载、断点续传、sha256 校验、import 识别 MNN/GGUF。
 */
@Singleton                                               // 单例
class ModelRepository @Inject constructor() {             // 构造函数注入

    /** 内置推荐模型目录（M6 用 v1 规划的推荐表）。 */
    val catalog: List<ModelItem> = listOf(                // 只读列表
        ModelItem(                                         // Qwen3-0.6B（低配）
            id = "qwen3-0.6b-q4", name = "Qwen3 0.6B (Q4)", kind = ModelKind.LLM,
            quant = "Q4_0", source = ModelSource.CATALOG,
            downloadUrl = "https://www.modelscope.cn/.../qwen3-0.6b-q4.mnn",
            sha256 = null, sizeBytes = 500L * 1024 * 1024,
            minRamMb = 4096, minStorageMb = 1024, cpuNote = "ARMv8 4 核", gpuNote = "无需 GPU"
        ),
        ModelItem(                                         // Qwen3-1.7B（默认档）
            id = "qwen3-1.7b-q4", name = "Qwen3 1.7B (Q4)", kind = ModelKind.LLM,
            quant = "Q4_0", source = ModelSource.CATALOG,
            downloadUrl = "https://www.modelscope.cn/.../qwen3-1.7b-q4.mnn",
            sha256 = null, sizeBytes = 1_100L * 1024 * 1024,
            minRamMb = 6144, minStorageMb = 2048, cpuNote = "ARMv8 8 核", gpuNote = "Vulkan 可选加速"
        ),
        ModelItem(                                         // DeepSeek-R1-1.5B（推理型）
            id = "deepseek-r1-1.5b-q4", name = "DeepSeek R1 1.5B (Q4)", kind = ModelKind.LLM,
            quant = "Q4_0", source = ModelSource.CATALOG,
            downloadUrl = "https://www.modelscope.cn/.../deepseek-r1-1.5b-q4.mnn",
            sha256 = null, sizeBytes = 1_000L * 1024 * 1024,
            minRamMb = 6144, minStorageMb = 2048, cpuNote = "ARMv8 8 核", gpuNote = "Vulkan 可选加速"
        ),
        ModelItem(                                         // bge-small-zh（嵌入）
            id = "bge-small-zh", name = "bge-small-zh (fp16)", kind = ModelKind.EMBEDDING,
            quant = "fp16", source = ModelSource.CATALOG,
            downloadUrl = "https://www.modelscope.cn/.../bge-small-zh.mnn",
            sha256 = null, sizeBytes = 130L * 1024 * 1024,
            minRamMb = 2048, minStorageMb = 200, cpuNote = "ARMv8", gpuNote = "无需 GPU"
        ),
        ModelItem(                                         // PaddleOCR-VL
            id = "paddleocr-vl-mobile", name = "PaddleOCR-VL Mobile", kind = ModelKind.OCR,
            quant = "fp16", source = ModelSource.CATALOG,
            downloadUrl = "https://www.modelscope.cn/.../paddleocr-vl-mobile.mnn",
            sha256 = null, sizeBytes = 10L * 1024 * 1024,
            minRamMb = 2048, minStorageMb = 50, cpuNote = "ARMv8", gpuNote = "无需 GPU"
        ),
    )

    /** 探测当前设备硬件画像（简化版；M6 完善）。 */
    fun probeHardware(): HardwareProfile {                // 硬件探测
        val runtime = Runtime.getRuntime()                 // 取 JVM runtime
        val totalRamMb = (runtime.maxMemory() / (1024 * 1024)).toInt()  // JVM 堆上限（粗略）
        val cpuCores = runtime.availableProcessors()        // CPU 核数
        return HardwareProfile(                            // 构造画像
            totalRamMb = totalRamMb,                        // 总内存
            availRamMb = (runtime.freeMemory() / (1024 * 1024)).toInt(),  // 空闲内存
            totalStorageMb = totalRamMb * 4,                // 粗略（真实应查 Environment.getDataDirectory 等）
            cpuCores = cpuCores,                            // CPU 核数
            abi = android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",  // 主 ABI
            gpu = "未知（待 MNN-LLM 集成时探测）",         // GPU（M6 完善）
        )
    }

    /** 检查某模型项是否能在当前硬件画像下运行（红黄绿评级）。 */
    fun canRun(item: ModelItem, hw: HardwareProfile): RunStatus {  // 硬件适配评级
        val ramOk = hw.availRamMb >= item.minRamMb / 2     // 可用内存至少达最低的一半（粗略）
        val storeOk = hw.totalStorageMb >= item.minStorageMb
        val coreOk = hw.cpuCores >= 4
        return when {                                      // 按级别返回
            ramOk && storeOk && coreOk -> RunStatus.OK
            (ramOk && storeOk) || coreOk -> RunStatus.WARN
            else -> RunStatus.BLOCKED
        }
    }

    /** 模型运行状态。 */
    enum class RunStatus { OK, WARN, BLOCKED }            // 三级：可运行/警告/不可运行
}
