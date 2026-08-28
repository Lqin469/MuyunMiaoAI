package com.memuo.core.models                            // 声明包名：模型管理模块

import android.app.ActivityManager                        // 导入 ActivityManager：真实物理内存
import android.content.Context                            // 导入 Context：系统服务
import android.os.Environment                             // 导入 Environment：数据分区
import android.os.StatFs                                  // 导入 StatFs：存储空间
import dagger.Module                                      // 导入 Module：Hilt 模块注解
import dagger.Provides                                    // 导入 Provides：Hilt 提供方法注解
import dagger.hilt.InstallIn                              // 导入 InstallIn：指定安装组件
import dagger.hilt.android.qualifiers.ApplicationContext  // 导入 ApplicationContext：应用级上下文
import dagger.hilt.components.SingletonComponent          // 导入 SingletonComponent：应用级单例组件
import javax.inject.Inject                                 // 导入 Inject：构造函数注入
import javax.inject.Singleton                              // 导入 Singleton：单例作用域

/**
 * 模型仓库（ModelRepository）—— 集中管理模型目录、本地导入与硬件画像（M6）。
 * M-027 修复：硬件画像改用真实物理内存（ActivityManager）与真实存储（StatFs），
 * 取代原先用 `Runtime.maxMemory()`（JVM 堆上限，常仅数百 MB 甚至接近 0）导致的
 * 「模型体积超过可用内存（约 0GB）」误判。
 */
@Singleton                                               // 单例
class ModelRepository @Inject constructor(               // 构造函数注入
    @ApplicationContext private val context: Context,    // 注入应用上下文（读系统服务）
) {

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

    /** 探测当前设备硬件画像（真实物理内存/存储/CPU/ABI，M-027 修复）。 */
    fun probeHardware(): HardwareProfile {                // 硬件探测
        // ① 真实物理内存（ActivityManager.MemoryInfo，而非 JVM 堆上限）
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager  // 内存服务
        val mem = ActivityManager.MemoryInfo()            // 内存信息容器
        am.getMemoryInfo(mem)                             // 读取
        val totalRamMb = (mem.totalMem / (1024 * 1024)).toInt()  // 物理总内存（MB）
        val availRamMb = (mem.availMem / (1024 * 1024)).toInt()  // 当前可用内存（MB）
        // ② 真实存储（数据分区 StatFs）
        val stat = StatFs(Environment.getDataDirectory().path)  // 数据分区
        val totalStorageMb = (stat.totalBytes / (1024 * 1024)).toInt()  // 总存储（MB）
        val cpuCores = Runtime.getRuntime().availableProcessors()  // CPU 核数
        return HardwareProfile(                            // 构造画像
            totalRamMb = totalRamMb,                       // 物理总内存
            availRamMb = availRamMb,                       // 可用内存
            totalStorageMb = totalStorageMb,               // 总存储
            cpuCores = cpuCores,                           // CPU 核数
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
