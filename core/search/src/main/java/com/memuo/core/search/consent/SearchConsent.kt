package com.memuo.core.search.consent                     // 声明包名：搜索模块的"许可"子包（核心安全约束所在）

import kotlinx.coroutines.flow.StateFlow                  // 导入 StateFlow：可观察的状态流（UI 可订阅变化）

/**
 * 隐私红线（用户强制约束，ADR-001）：
 * 任何「全盘搜索 / 文件搜索 / 文件索引」都必须在用户于当前界面显式发起后才执行；
 * 禁止任何代码路径在后台未经许可自动发起搜索。
 *
 * 约束落地方式：
 *  - 一切搜索/索引必须通过 [SearchConsentGate] 获取 [SearchSession] 才能调用 FileIndexer；
 *  - [SearchTrigger.SCHEDULED_BACKGROUND]（计划/空闲自动索引）默认被 [SearchSettings.backgroundIndexingEnabled]
 *    拒绝（默认 false），即使开启也必须伴随可见进度与可取消能力（ADR-002）；
 *  - 每次触发写审计日志（ConsentAudit），供隐私自检。
 */
enum class SearchTrigger {                                // 枚举：定义"搜索是由什么触发的"（两种来源）
    /** 用户在界面上显式点击「开始搜索 / 开始索引」——始终放行。 */
    USER_ACTION,                                          // 用户主动点击（唯一默认允许的触发方式）

    /** 计划任务 / 空闲自动索引——仅当用户显式开启 backgroundIndexingEnabled 才允许。 */
    SCHEDULED_BACKGROUND,                                 // 后台/计划触发（默认禁止，需用户显式打开开关）
}

/** 一次获准的搜索会话；只有持有它才能调用 FileIndexer.index()。 */
class SearchSession private constructor(                  // 搜索会话：代表"一次被用户授权的搜索"（私有构造，只能经工厂创建）
    val requestId: String,                                // 会话唯一标识（建议 UUID，用于取消与审计）
    val trigger: SearchTrigger,                           // 本次触发的来源（用户点击 or 后台计划）
    val startedAt: Long,                                  // 会话开始时间戳（审计用）
) {
    @Volatile                                            // 注解：多线程下可见性保证（UI 线程改，索引线程读）
    var cancelled: Boolean = false                        // 取消标记：默认未取消
        private set                                       // 外部只能读，只能通过 cancel() 修改

    /** 供 FileIndexer 每处理一批文件时轮询；UI 的「停止」按钮调用它。 */
    fun cancel() {                                        // 取消方法：用户点"停止"时调用
        cancelled = true                                  // 置位取消标记，索引器下一批文件处理时会检测到并退出
    }

    companion object {                                    // 伴生对象：静态工厂方法区
        fun new(requestId: String, trigger: SearchTrigger, startedAt: Long): SearchSession =  // 工厂方法：创建会话
            SearchSession(requestId, trigger, startedAt)  // 调用私有构造函数生成实例
    }
}

/**
 * 搜索许可闸门：搜索类功能的统一出入口。
 * 调用方（ViewModel / Worker）必须先过此闸，任何绕过该闸的搜索实现都不被允许合并。
 */
class SearchConsentGate(                                  // 许可闸门类：所有搜索的唯一入口（PR 评审强制检查点）
    private val settings: SearchSettings,                 // 搜索设置（读取后台索引开关）
    private val now: () -> Long = System::currentTimeMillis,  // 时间函数（可注入以便测试）
) {
    /**
     * 用户显式触发的搜索：始终放行，返回有效会话。
     * @param requestId 建议 UUID；同时写入审计日志。
     */
    fun beginUserInitiated(requestId: String): SearchSession =   // 用户点击触发的入口：无条件放行
        SearchSession.new(requestId, SearchTrigger.USER_ACTION, now())  // 创建"用户主动"类型的会话并返回

    /**
     * 后台/计划搜索：仅当用户显式开启 [SearchSettings.backgroundIndexingEnabled] 时放行；
     * 否则返回 null，调用方必须放弃执行并把原因写入审计日志（DENIED_BACKGROUND_DISABLED）。
     */
    fun beginScheduled(requestId: String): SearchSession? =       // 后台触发的入口：默认拒绝
        if (settings.backgroundIndexingEnabled.value) {           // 判断用户是否显式开启了后台索引开关
            SearchSession.new(requestId, SearchTrigger.SCHEDULED_BACKGROUND, now())  // 已开启：创建后台会话
        } else {
            null                                                  // 未开启：返回 null（调用方必须放弃执行）
        }

    /** 判断某类触发当前是否被允许（供 UI 预判 / 审计）。 */
    fun isAllowed(trigger: SearchTrigger): Boolean =              // 查询某触发类型当前是否被允许
        trigger == SearchTrigger.USER_ACTION || settings.backgroundIndexingEnabled.value  // 用户点击永远允许；后台需开关开启
}

/**
 * 搜索设置契约（M0 仅定义接口；由 :feature:settings 用 DataStore 实现并持久化）。
 * 注意：默认值必须为 false，且设置页必须显式说明开启后的行为。
 */
interface SearchSettings {                                // 搜索设置接口：实现方为设置模块
    /** 后台自动索引总开关，默认 false。 */
    val backgroundIndexingEnabled: StateFlow<Boolean>     // 状态流：UI 可实时订阅开关变化（默认 false）
}
