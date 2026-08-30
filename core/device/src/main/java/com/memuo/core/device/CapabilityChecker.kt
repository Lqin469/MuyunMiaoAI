package com.memuo.core.device                            // 声明包名：设备检测模块

import javax.inject.Inject                                 // 导入 Inject：构造函数注入
import javax.inject.Singleton                              // 导入 Singleton：单例作用域

// ============================================================
// 满足度判定模型（CheckLevel / CheckResult）
// ============================================================

/** 检测结论等级：满足 / 受限可用 / 不满足。 */
enum class CheckLevel {                                    // 检测等级
    PASS,                                                  // 满足：完全支持
    WARN,                                                  // 受限：可用但功能受限（如内存偏小仅云端）
    FAIL,                                                  // 不满足：核心能力不可用（如 32 位设备无 MNN 库）
}

/** 单项目检测结果（实测值 + 等级 + 说明）。 */
data class CheckResult(                                    // 检测结果
    val id: String,                                        // 项目 ID（如 "arch"）
    val label: String,                                     // 显示标签（如 "64 位架构"）
    val value: String,                                     // 实测值（如 "arm64-v8a"）
    val level: CheckLevel,                                 // 结论等级
    val hint: String,                                      // 说明文字（不满足项的原因/建议）
)

// ============================================================
// 能力判定器（CapabilityChecker）—— 运行条件满足度规则
// ============================================================

/**
 * 能力判定器（CapabilityChecker）—— 把设备快照翻译成「满足/受限/不满足」结论（需求 1）。
 *
 * 判定规则（阈值集中在此，方便统一调整）：
 *  - 64 位架构：FAIL。MNN 推理库仅提供 arm64-v8a so，32 位设备端侧模型不可用；
 *  - 系统版本：PASS。minSdk=26 是安装门槛，能装上即满足；
 *  - CPU 核心：< 4 核 WARN（推理并发受限）；
 *  - 运行内存：< 3GB WARN（端侧模型不可用，仅云端 AI）；≥ 3GB PASS；
 *  - 可用存储：< 512MB FAIL（应用数据与索引无法落地）；< 1.5GB WARN（模型导入需 1GB+）。
 */
@Singleton                                               // 单例（无状态）
class CapabilityChecker @Inject constructor() {          // 构造函数注入（无参，规则类）

    companion object {                                   // 阈值常量（集中管理）
        private const val MIN_CORES = 4                  // 建议最低核心数
        private const val MIN_RAM_GB = 3.0               // 端侧模型最低内存（GB）
        private const val MIN_STORAGE_GB = 0.5           // 应用最低可用存储（GB）
        private const val WARN_STORAGE_GB = 1.5          // 模型导入建议可用存储（GB）
    }

    /** 对设备快照执行全部判定，返回自检项结果清单。 */
    fun checkAll(info: DeviceSnapshot): List<CheckResult> =  // 全项判定
        listOf(                                          // 固定顺序的清单
            checkArch(info),                             // ① 64 位架构
            checkSystem(info),                           // ② 系统版本
            checkCpu(info),                              // ③ CPU 核心
            checkRam(info),                              // ④ 运行内存
            checkStorage(info),                          // ⑤ 可用存储
        )

    /** ① 架构判定：MNN 仅 arm64-v8a，32 位直接不满足。 */
    private fun checkArch(info: DeviceSnapshot): CheckResult {  // 架构判定
        val abi64 = info.cpu.abis.firstOrNull { it.contains("64") }  // 取 64 位 ABI
        return if (abi64 != null) {                      // 支持 64 位
            CheckResult("arch", "64 位架构", abi64, CheckLevel.PASS, "端侧推理库（MNN）可用")  // 满足
        } else {                                         // 仅 32 位
            CheckResult("arch", "64 位架构", info.cpu.abis.firstOrNull().orEmpty(), CheckLevel.FAIL, "本机为 32 位设备，端侧模型不可用（MNN 仅提供 arm64-v8a）")  // 不满足
        }
    }

    /** ② 系统版本判定：能安装即满足（minSdk=26），仅信息展示。 */
    private fun checkSystem(info: DeviceSnapshot): CheckResult =  // 系统判定
        CheckResult(                                     // 组装结果
            id = "system",                               // ID
            label = "系统版本",                          // 标签
            value = "Android ${info.androidVersion} (API ${info.sdkInt})",  // 实测值
            level = CheckLevel.PASS,                     // 满足（minSdk 安装门槛已过）
            hint = "满足最低系统要求（Android 8.0+）",    // 说明
        )

    /** ③ CPU 核心判定：< 4 核受限。 */
    private fun checkCpu(info: DeviceSnapshot): CheckResult {  // CPU 判定
        val freqText = if (info.cpu.maxFreqMhz > 0) " / ${info.cpu.maxFreqMhz} MHz" else ""  // 主频展示（读不到则省略）
        val value = "${info.cpu.cores} 核$freqText"      // 实测值文案
        return if (info.cpu.cores >= MIN_CORES) {        // 核心充足
            CheckResult("cpu", "CPU 核心", value, CheckLevel.PASS, "核心数满足端侧推理需求")  // 满足
        } else {                                         // 核心偏少
            CheckResult("cpu", "CPU 核心", value, CheckLevel.WARN, "核心数偏少，本地推理速度受限，建议使用云端引擎")  // 受限
        }
    }

    /** ④ 内存判定：< 3GB 端侧不可用（仅云端），≥ 3GB 可用。 */
    private fun checkRam(info: DeviceSnapshot): CheckResult {  // 内存判定
        val value = "%.1f GB".format(java.util.Locale.getDefault(), info.memory.totalGb)  // 总内存文案
        return if (info.memory.totalGb >= MIN_RAM_GB) {  // 内存充足
            CheckResult("ram", "运行内存", value, CheckLevel.PASS, "可运行端侧大模型（建议 0.8B 量化模型）")  // 满足
        } else {                                         // 内存不足
            CheckResult("ram", "运行内存", value, CheckLevel.WARN, "内存低于 3GB，端侧模型不可用，将自动使用云端 AI")  // 受限
        }
    }

    /** ⑤ 存储判定：< 512MB 不满足；< 1.5GB 受限（模型导入需 1GB+）。 */
    private fun checkStorage(info: DeviceSnapshot): CheckResult {  // 存储判定
        val value = "%.1f GB".format(java.util.Locale.getDefault(), info.storage.freeGb)  // 可用空间文案
        return when {                                    // 按阈值分档
            info.storage.freeGb < MIN_STORAGE_GB -> CheckResult("storage", "可用存储", value, CheckLevel.FAIL, "可用空间低于 512MB，应用数据与索引无法正常保存，请清理存储")  // 不满足
            info.storage.freeGb < WARN_STORAGE_GB -> CheckResult("storage", "可用存储", value, CheckLevel.WARN, "空间低于 1.5GB，导入端侧模型（约 1GB）可能失败")  // 受限
            else -> CheckResult("storage", "可用存储", value, CheckLevel.PASS, "空间充足，可导入端侧模型")  // 满足
        }
    }
}
