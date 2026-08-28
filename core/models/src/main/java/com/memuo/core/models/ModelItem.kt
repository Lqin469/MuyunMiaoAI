package com.memuo.core.models                            // 声明包名：模型管理模块

/**
 * 模型类型枚举（M6）。
 */
enum class ModelKind {                                     // 四种模型类型
    LLM,          // 对话大语言模型（Qwen3 / DeepSeek-R1 等）
    EMBEDDING,    // 嵌入模型（bge-small-zh / bge-m3 等）
    OCR,          // 文字识别（PaddleOCR-VL 等）
    VISION_LLM,   // 视觉语言模型（Qwen2.5-VL 等，M6 后续）
}

/**
 * 模型来源枚举：CATALOG=目录下载 / LOCAL_IMPORT=本地导入。
 */
enum class ModelSource { CATALOG, LOCAL_IMPORT }

/**
 * 模型元数据（ModelItem）—— 一份可下载/可导入的模型。
 */
data class ModelItem(                                      // 模型项数据类
    val id: String,                                        // 唯一 ID
    val name: String,                                      // 显示名
    val kind: ModelKind,                                   // 类型
    val quant: String,                                     // 量化方案（如 Q4_0）
    val source: ModelSource,                               // 来源
    val downloadUrl: String? = null,                       // 下载地址（目录下载用，本地导入为 null）
    val sha256: String? = null,                            // 校验和
    val sizeBytes: Long,                                   // 体积（字节）
    val minRamMb: Int,                                     // 最低内存要求（MB）
    val minStorageMb: Int,                                 // 最低存储要求（MB）
    val cpuNote: String,                                   // CPU 要求说明
    val gpuNote: String,                                   // GPU 要求说明
)

/**
 * 硬件画像（HardwareProfile）—— 当前设备的硬件探测结果（用于推荐模型）。
 * 运行期用 ActivityManager.memoryClass + MemInfo 探测。
 */
data class HardwareProfile(                                 // 硬件画像
    val totalRamMb: Int,                                   // 设备总内存（MB）
    val availRamMb: Int,                                   // 可用内存（MB）
    val totalStorageMb: Int,                               // 设备总存储（MB）
    val cpuCores: Int,                                     // CPU 核心数
    val abi: String,                                       // CPU 架构（arm64-v8a 等）
    val gpu: String,                                       // GPU 信息（Vulkan 版本等）
)

/**
 * 本地模型信息（LocalModelInfo）—— 本地模型选择页用（M-035）。
 * 由 ModelImporter.listLocalModels() 扫描 modelsDir() 生成。
 */
data class LocalModelInfo(                                 // 本地模型信息
    val id: String,                                        // 唯一 ID（如 "mnn-llm" / "gguf-xxx"）
    val name: String,                                      // 显示名
    val format: String,                                    // 格式（MNN / GGUF）
    val sizeBytes: Long,                                   // 体积（字节）
    val runnable: Boolean,                                 // 当前是否可运行（MNN 可，GGUF 需 llama.cpp 运行时）
)
