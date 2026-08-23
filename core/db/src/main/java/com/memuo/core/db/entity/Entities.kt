package com.memuo.core.db.entity                          // 声明包名：数据库模块的实体子包

import androidx.room.ColumnInfo                            // 导入 ColumnInfo：自定义数据库列名/类型
import androidx.room.Entity                                // 导入 Entity：声明数据库表
import androidx.room.PrimaryKey                            // 导入 PrimaryKey：声明主键

/** 笔记类型枚举：TEXT=纯文本 / RICH=富文本(Markdown) / TODO=待办清单。 */
enum class NoteType { TEXT, RICH, TODO }                  // 三种笔记类型

/** 引擎类型枚举：LOCAL=本地 MNN / CLOUD=云端 API。 */
enum class EngineType { LOCAL, CLOUD }                    // 两种对话引擎

/**
 * 笔记表（notes）—— 常规备忘录的核心数据（M2 使用）。
 * content 统一存 Markdown 富文本；type 区分文本/富文本/待办。
 */
@Entity(tableName = "notes")                              // 声明表名 notes
data class Note(                                          // 笔记数据类（对应数据库一行）
    @PrimaryKey(autoGenerate = true) val id: Long = 0,    // 主键：自增 ID
    val title: String,                                    // 标题
    val content: String,                                  // 正文（Markdown 富文本）
    val type: NoteType,                                   // 类型（TEXT/RICH/TODO）
    val pinned: Boolean = false,                          // 是否置顶
    val createdAt: Long,                                  // 创建时间戳
    val updatedAt: Long,                                  // 更新时间戳
    val deletedAt: Long? = null,                          // 软删除时间（null=未删除，支持回收站）
)

/**
 * 待办条目表（todo_items）—— 属于某条 TODO 笔记的子项（M2 使用）。
 */
@Entity(tableName = "todo_items")                         // 声明表名 todo_items
data class TodoItem(                                       // 待办条目数据类
    @PrimaryKey(autoGenerate = true) val id: Long = 0,    // 主键：自增 ID
    val noteId: Long,                                     // 所属待办清单（Note.type = TODO 的 id）
    val text: String,                                     // 待办内容
    val done: Boolean = false,                            // 是否已完成
    val order: Int = 0,                                   // 排序序号（支持拖动排序）
)

/**
 * 会话表（conversations）—— 一次 AI 对话（M3 使用）。
 */
@Entity(tableName = "conversations")                      // 声明表名 conversations
data class Conversation(                                   // 会话数据类
    @PrimaryKey(autoGenerate = true) val id: Long = 0,    // 主键：自增 ID
    val title: String,                                    // 会话标题
    val engine: EngineType,                               // 使用的引擎（LOCAL/CLOUD）
    val kbFolderId: String? = null,                       // 绑定的知识库 ID（可空）
    val createdAt: Long,                                  // 创建时间戳
    val updatedAt: Long,                                  // 更新时间戳
)

/**
 * 消息表（messages）—— 会话中的一条消息（M3 使用）。
 * citations 存 JSON 数组（RAG 问答时带引用来源）。
 */
@Entity(tableName = "messages")                           // 声明表名 messages
data class ChatMessage(                                    // 消息数据类
    @PrimaryKey(autoGenerate = true) val id: Long = 0,    // 主键：自增 ID
    val convId: Long,                                     // 所属会话 ID
    val role: String,                                     // 角色：user（用户）/ assistant（AI）
    val content: String,                                  // 消息正文
    val citations: String? = null,                        // 引用来源（JSON，如 [{"source":"a.pdf","chunk":"..."}]）
    val ts: Long,                                         // 消息时间戳
)

/**
 * 搜索审计表（consent_audit）—— 记录每一次搜索/索引触发（M1 使用，隐私自检依据）。
 * 字段与 core:search 的 ConsentAuditEntry 一一对应（这里独立定义实体，避免模块反向依赖）。
 */
@Entity(tableName = "consent_audit")                      // 声明表名 consent_audit
data class ConsentAuditEntity(                             // 审计日志实体
    @PrimaryKey val requestId: String,                    // 主键：搜索请求 ID
    val trigger: String,                                  // 触发类型：USER_ACTION / SCHEDULED_BACKGROUND
    val scope: String,                                    // 检索范围描述
    val granted: Boolean,                                 // 是否获准执行
    val reason: String,                                   // 原因：OK / DENIED_BACKGROUND_DISABLED / DENIED_PRIVILEGE_INSUFFICIENT
    @ColumnInfo(name = "started_at") val startedAt: Long, // 触发时间戳（自定义列名 started_at）
)
