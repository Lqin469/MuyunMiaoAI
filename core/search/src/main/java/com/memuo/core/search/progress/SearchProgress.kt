package com.memuo.core.search.progress                     // 声明包名：搜索模块的"进度"子包

import kotlin.math.min                                     // 导入 min：取两个数的最小值（进度上限 100%）

/**
 * 搜索阶段。UI 按此展示当前状态文案。
 */
enum class SearchPhase(val label: String) {                // 枚举：搜索所处的阶段（每个阶段带中文文案）
    INITIALIZING("准备中"),                                // 阶段1：初始化（统计文件总数）
    SCANNING_DIRS("扫描目录"),                             // 阶段2：遍历目录（核心耗时阶段）
    HASHING("校验文件"),                                   // 阶段3：校验文件（增量判断用 hash）
    INDEXING_DB("写入索引"),                               // 阶段4：把结果写入数据库索引
    QUERYING("检索中"),                                    // 阶段5：执行检索查询
    DONE("完成"),                                          // 结束：成功完成
    CANCELLED("已取消"),                                   // 结束：用户取消
    FAILED("失败"),                                        // 结束：出错失败
}

/**
 * 搜索/索引进度实时上报契约（用户强制约束，ADR-002）。
 *
 * 上报规则（FileIndexer 必须遵守）：
 *  1. 阶段切换必须上报一次；
 *  2. 扫描/索引期间每处理 200 个文件或每前进 1%，至少上报一次（含 currentPath）；
 *  3. 结束（DONE / CANCELLED / FAILED）必须上报，percent 分别为 1f / 停在当前值 / 0f。
 *
 * UI 必须将本对象渲染为可见进度条（:feature:filesearch 的 SearchProgressBar）。
 */
data class SearchProgress(                                // 进度数据类：一次搜索的完整进度快照（不可变）
    val requestId: String,                                // 所属搜索会话的 ID
    val phase: SearchPhase,                               // 当前阶段
    val scannedItems: Long = 0L,                          // 已处理的文件数（实时累加）
    val totalItems: Long = 0L,                            // 文件总数（初始化阶段统计得出）
    val currentPath: String? = null,                      // 当前正在处理的文件路径（UI 展示用）
    val message: String = "",                             // 附加消息（如错误信息）
    val startedAt: Long,                                  // 搜索开始时间戳
    val updatedAt: Long = startedAt,                      // 本次进度上报的时间戳
) {
    /** 0f..1f；totalItems 未知时（如初始化）为 0f，UI 可显示不确定进度条。 */
    val percent: Float                                    // 计算属性：进度百分比
        get() = if (totalItems > 0L) min(1f, scannedItems.toFloat() / totalItems) else 0f  // 已处理÷总数，封顶 1.0
}

/** 进度回调；ViewModel 收到后立刻刷新 UI（Flow 桥在 UI 层封装）。 */
fun interface SearchProgressListener {                    // 函数式接口：进度监听器（只有回调被调用）
    fun onProgress(progress: SearchProgress)              // 回调方法：收到最新进度后刷新界面
}
