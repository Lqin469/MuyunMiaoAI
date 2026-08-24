package com.memuo.core.db.entity                          // 声明包名：数据库模块的实体子包

import androidx.room.ColumnInfo                            // 导入 ColumnInfo：自定义列名
import androidx.room.Entity                                // 导入 Entity：声明数据库表
import androidx.room.PrimaryKey                            // 导入 PrimaryKey：主键

/** 记忆类型枚举：FACT=事实 / PREFERENCE=偏好 / TODO=待办。 */
enum class MemoryType { FACT, PREFERENCE, TODO }          // 三种记忆类型

/**
 * 会话记忆表（kb_memory）—— AI 从对话中自动提炼的长期记忆（R6，M5 使用）。
 * source 区分来源：chat（对话）/ memo（笔记）/ import（投喂）。
 */
@Entity(tableName = "kb_memory")                           // 声明表名 kb_memory
data class KbMemory(                                       // 记忆数据类
    @PrimaryKey(autoGenerate = true) val id: Long = 0,     // 主键：自增 ID
    val type: MemoryType,                                  // 类型（FACT/PREFERENCE/TODO）
    val topic: String,                                     // 主题（聚类/检索过滤用）
    val text: String,                                      // 记忆内容
    val source: String,                                    // 来源：chat/memo/import
    val ts: Long,                                          // 时间戳
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val embedding: ByteArray? = null,  // 向量（BLOB）
)
