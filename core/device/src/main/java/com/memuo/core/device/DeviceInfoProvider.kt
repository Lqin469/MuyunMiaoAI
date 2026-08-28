package com.memuo.core.device                            // 声明包名：设备检测模块

import android.app.ActivityManager                       // 导入 ActivityManager：内存信息
import android.content.Context                            // 导入 Context：应用上下文
import android.os.Build                                   // 导入 Build：设备型号/系统版本/ABI
import android.os.Environment                             // 导入 Environment：数据分区路径
import android.os.StatFs                                  // 导入 StatFs：存储空间统计
import dagger.hilt.android.qualifiers.ApplicationContext  // 导入 ApplicationContext：应用级上下文限定符
import kotlinx.coroutines.Dispatchers                      // 导入 Dispatchers：IO 调度器
import kotlinx.coroutines.withContext                      // 导入 withContext：切换调度器
import java.io.File                                        // 导入 File：读取 CPU 频率文件
import javax.inject.Inject                                 // 导入 Inject：构造函数注入
import javax.inject.Singleton                              // 导入 Singleton：单例作用域

// ============================================================
// 设备信息数据模型（DeviceSnapshot 聚合体）
// ============================================================

/** CPU 信息（核心数/主频/ABI/位宽）。 */
data class CpuInfo(                                        // CPU 信息
    val cores: Int,                                        // 逻辑核心数（Runtime.availableProcessors）
    val maxFreqMhz: Int,                                   // 最高主频（MHz，读 /sys/devices/system/cpu）
    val abis: List<String>,                                // 支持的 ABI 列表（Build.SUPPORTED_ABIS）
    val is64Bit: Boolean,                                  // 是否支持 64 位（MNN 库仅 arm64-v8a）
)

/** 内存信息（总容量/可用容量，GB）。 */
data class MemoryInfo(                                     // 内存信息
    val totalGb: Double,                                   // 总内存（GB）
    val availGb: Double,                                   // 当前可用内存（GB）
)

/** 存储信息（总量/可用，GB，数据分区）。 */
data class StorageInfo(                                    // 存储信息
    val totalGb: Double,                                   // 总容量（GB）
    val freeGb: Double,                                    // 可用空间（GB）
)

/** 设备快照（自检所需全部硬件/系统信息）。 */
data class DeviceSnapshot(                                 // 设备快照
    val manufacturer: String,                              // 厂商（如 Xiaomi）
    val model: String,                                     // 型号（如 23013RK75C）
    val androidVersion: String,                            // 系统版本（如 14）
    val sdkInt: Int,                                       // API 级别（如 34）
    val cpu: CpuInfo,                                      // CPU 信息
    val memory: MemoryInfo,                                // 内存信息
    val storage: StorageInfo,                              // 存储信息
) {
    /** 展示用设备型号（厂商 + 型号拼接，如 "Xiaomi 23013RK75C"）。 */
    val displayModel: String get() = "$manufacturer $model".trim()  // 拼接展示名
}

// ============================================================
// 设备信息提供者（DeviceInfoProvider）—— 真实硬件/系统检测
// ============================================================

/**
 * 设备信息提供者（DeviceInfoProvider）—— 采集设备型号、系统版本、CPU、内存、存储
 * 五类真实数据（需求 1：设备自检的数据来源）。
 *
 * 实现全部基于 Android Framework 原生 API，零第三方依赖：
 *  - 型号/系统：[Build] 系列常量；
 *  - 内存：[ActivityManager.MemoryInfo]；
 *  - 存储：[StatFs]（数据分区）；
 *  - CPU 核心：[Runtime.availableProcessors]；
 *  - CPU 主频：读内核暴露的 /sys/devices/system/cpu/ 下各核心 cpufreq/cpuinfo_max_freq。
 */
@Singleton                                               // 单例（无状态，进程内一份）
class DeviceInfoProvider @Inject constructor(            // 构造函数注入
    @ApplicationContext private val context: Context,    // 注入应用上下文
) {

    /** 采集设备快照（IO 线程执行，避免阻塞主线程）。 */
    suspend fun snapshot(): DeviceSnapshot = withContext(Dispatchers.IO) {  // 切 IO 线程
        DeviceSnapshot(                                  // 组装快照
            manufacturer = Build.MANUFACTURER,           // 厂商
            model = Build.MODEL,                         // 型号
            androidVersion = Build.VERSION.RELEASE,      // 系统版本
            sdkInt = Build.VERSION.SDK_INT,              // API 级别
            cpu = readCpu(),                             // CPU 信息
            memory = readMemory(),                       // 内存信息
            storage = readStorage(),                     // 存储信息
        )
    }

    /** 读取 CPU 信息：核心数 + 最高主频 + ABI + 位宽。 */
    private fun readCpu(): CpuInfo {                     // 读取 CPU
        val cores = Runtime.getRuntime().availableProcessors()  // 逻辑核心数
        val freq = readMaxFreqMhz()                      // 最高主频（读 sysfs）
        val abis = Build.SUPPORTED_ABIS.toList()         // ABI 列表（首个为主 ABI）
        return CpuInfo(                                  // 组装
            cores = cores,                               // 核心数
            maxFreqMhz = freq,                           // 主频
            abis = abis,                                 // ABI
            is64Bit = abis.any { it.contains("64") },    // 含 64 位 ABI 即支持 64 位
        )
    }

    /** 读 CPU 最高主频：遍历每个核心的 cpuinfo_max_freq（单位 kHz），取最大值。 */
    private fun readMaxFreqMhz(): Int {                  // 读主频
        var maxKhz = 0                                   // 最大频率（kHz）
        val cpuDir = File("/sys/devices/system/cpu")     // CPU sysfs 目录
        cpuDir.listFiles()?.forEach { core ->            // 遍历 cpuN 目录
            if (!core.name.matches(Regex("cpu\\d+"))) return@forEach  // 跳过非核心目录（如 cpufreq 汇总）
            val freqFile = File(core, "cpufreq/cpuinfo_max_freq")  // 该核心最大频率文件
            runCatching {                                // 容错（部分设备/权限下读不到）
                freqFile.readText().trim().toIntOrNull()?.let { khz ->  // 解析 kHz
                    if (khz > maxKhz) maxKhz = khz       // 取最大值
                }
            }
        }
        return maxKhz / 1000                             // kHz → MHz（读不到返回 0，界面显示"未知"）
    }

    /** 读取内存信息（ActivityManager）。 */
    private fun readMemory(): MemoryInfo {               // 读内存
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager  // 内存服务
        val mem = ActivityManager.MemoryInfo()           // 内存信息容器
        am.getMemoryInfo(mem)                             // 读取
        return MemoryInfo(                               // 组装
            totalGb = mem.totalMem / 1024.0 / 1024 / 1024,  // 总内存 GB
            availGb = mem.availMem / 1024.0 / 1024 / 1024,  // 可用内存 GB
        )
    }

    /** 读取存储信息（数据分区 StatFs）。 */
    private fun readStorage(): StorageInfo {             // 读存储
        val stat = StatFs(Environment.getDataDirectory().path)  // 数据分区
        return StorageInfo(                              // 组装
            totalGb = stat.totalBytes / 1024.0 / 1024 / 1024,  // 总量 GB
            freeGb = stat.availableBytes / 1024.0 / 1024 / 1024,  // 可用 GB
        )
    }
}
