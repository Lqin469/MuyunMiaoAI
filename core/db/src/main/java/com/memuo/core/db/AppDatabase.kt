package com.memuo.core.db                               // 声明包名：数据库模块根包

import androidx.room.Database                             // 导入 Database：Room 数据库注解
import androidx.room.RoomDatabase                         // 导入 RoomDatabase：数据库基类
import com.memuo.core.db.dao.ChatDao                      // 导入会话 DAO
import com.memuo.core.db.dao.ConsentAuditDao              // 导入审计 DAO
import com.memuo.core.db.dao.FileLocationDao              // 导入文件位置 DAO
import com.memuo.core.db.dao.KbDao                        // 导入知识库 DAO
import com.memuo.core.db.dao.MemoryDao                    // 导入记忆 DAO
import com.memuo.core.db.dao.NoteDao                      // 导入笔记 DAO
import com.memuo.core.db.entity.ChatMessage               // 导入消息实体
import com.memuo.core.db.entity.ConsentAuditEntity        // 导入审计实体
import com.memuo.core.db.entity.Conversation              // 导入会话实体
import com.memuo.core.db.entity.FileLocation              // 导入文件位置实体
import com.memuo.core.db.entity.KbChunk                   // 导入分块实体
import com.memuo.core.db.entity.KbDocument                // 导入文档实体
import com.memuo.core.db.entity.KbMemory                  // 导入记忆实体
import com.memuo.core.db.entity.Note                      // 导入笔记实体
import com.memuo.core.db.entity.TodoItem                  // 导入待办实体

/**
 * 应用数据库（AppDatabase）—— 全应用唯一的 Room 数据库实例。
 * version 从 1 开始；以后每次改表结构都要 +1 并提供 Migration（迁移策略）。
 * 数据库文件路径由 StorageProvider.dbDir() 决定（R4/R5 自定义目录）。
 */
@Database(                                                // 声明这是 Room 数据库
    entities = [                                          // 注册全部表对应的实体类
        Note::class,                                      // 笔记表
        TodoItem::class,                                  // 待办表
        Conversation::class,                              // 会话表
        ChatMessage::class,                               // 消息表
        KbDocument::class,                                // 知识库文档表
        KbChunk::class,                                   // 知识库分块表
        KbMemory::class,                                  // 会话记忆表
        FileLocation::class,                              // 文件位置记录表
        ConsentAuditEntity::class,                        // 搜索审计表
    ],
    version = 4,                                          // 数据库版本号（M5 新增 kb_memory 表，升到 4）
    exportSchema = true,                                  // 导出 schema 历史（配合显式 Migration 做编译期校验）
)
abstract class AppDatabase : RoomDatabase() {             // 抽象数据库类（Room 生成实现）

    /** 提供笔记 DAO。 */
    abstract fun noteDao(): NoteDao                       // 笔记数据访问对象

    /** 提供会话/消息 DAO。 */
    abstract fun chatDao(): ChatDao                       // 会话数据访问对象

    /** 提供知识库 DAO。 */
    abstract fun kbDao(): KbDao                           // 知识库数据访问对象

    /** 提供记忆 DAO。 */
    abstract fun memoryDao(): MemoryDao                   // 记忆数据访问对象

    /** 提供文件位置 DAO。 */
    abstract fun fileLocationDao(): FileLocationDao       // 文件位置数据访问对象

    /** 提供审计 DAO。 */
    abstract fun consentAuditDao(): ConsentAuditDao       // 审计数据访问对象
}
