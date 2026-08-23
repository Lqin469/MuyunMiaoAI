package com.memuo.core.search.audit                        // 声明包名：搜索模块的"审计"子包

/**
 * 搜索/索引审计日志条目（隐私自检依据）。
 * 每次触发（无论放行与否）都记录一条，落库表 `consent_audit`（:core:db 实现）。
 */
data class ConsentAuditEntry(                              // 审计日志数据类：每次搜索触发留痕
    val requestId: String,                                 // 本次搜索请求的 ID（关联会话）
    val trigger: String,                                   // 触发类型：USER_ACTION（用户点击）/ SCHEDULED_BACKGROUND（后台计划）
    val scope: String,                                     // 检索范围描述（如 AppScoped / UserStorage）
    val granted: Boolean,                                  // 是否被允许执行（放行 or 拒绝）
    val reason: String,                                    // 拒绝原因：DENIED_BACKGROUND_DISABLED（后台开关关闭）/
                                                           //          DENIED_PRIVILEGE_INSUFFICIENT（权限不足）/ OK（放行）
    val startedAt: Long,                                   // 触发时间戳（审计查询用）
)
