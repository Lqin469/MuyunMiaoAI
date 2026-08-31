package com.memuo.core.db                               // 声明包名：数据库模块根包

import androidx.room.migration.Migration                 // 导入 Migration：Room 迁移基类
import androidx.sqlite.db.SupportSQLiteDatabase          // 导入 SupportSQLiteDatabase：迁移执行用数据库句柄

/**
 * 数据库显式迁移定义（Migrations）—— 替代开发期的 fallbackToDestructiveMigration（破坏性清库）。
 *
 * 版本演进历史（见 git 提交 / docs/04-database.md）：
 *   v1 (M1)  notes / todo_items / conversations / messages / consent_audit（5 表）
 *   v2 (M4)  + kb_documents / kb_chunks（知识库）
 *   v3 (M4)  + file_locations（文件位置记录）
 *   v4 (M5)  + kb_memory（会话记忆）
 *
 * 说明：v1→v4 全程只「新增表」，无「改已有表结构」，故每个迁移仅需 CREATE TABLE。
 * 建表 SQL 与 Room 生成的期望结构完全一致（含反引号、AUTOINCREMENT、PRIMARY KEY 约束），
 * 否则 Room 运行时 schema 校验会报 IllegalStateException。
 */
object Migrations {                                      // 迁移集合对象（单例，集中管理）

    /** v1 → v2（M4）：新增知识库文档表 + 分块表。 */
    val MIGRATION_1_2 = object : Migration(1, 2) {       // 匿名迁移对象：从版本 1 升到 2
        override fun migrate(db: SupportSQLiteDatabase) {  // 迁移回调：执行建表 SQL
            // 知识库文档表：docId(内容 hash) 为主键，fileHash 用于增量去重
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `kb_documents` (`docId` TEXT NOT NULL, " +
                    "`folderId` TEXT NOT NULL, `fileName` TEXT NOT NULL, `fileUri` TEXT NOT NULL, " +
                    "`fileHash` TEXT NOT NULL, `status` TEXT NOT NULL, `chunkCount` INTEGER NOT NULL, " +
                    "`indexedAt` INTEGER, PRIMARY KEY(`docId`))"
            )
            // 知识库分块表：embedding 存向量序列化字节（BLOB）
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `kb_chunks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`docId` TEXT NOT NULL, `folderId` TEXT NOT NULL, `seq` INTEGER NOT NULL, " +
                    "`text` TEXT NOT NULL, `embedding` BLOB)"
            )
        }
    }

    /** v2 → v3（M4 收尾）：新增文件位置记录表（R10，记录不可解析文件的位置）。 */
    val MIGRATION_2_3 = object : Migration(2, 3) {       // 匿名迁移对象：从版本 2 升到 3
        override fun migrate(db: SupportSQLiteDatabase) {  // 迁移回调：执行建表 SQL
            // 文件位置表：path 为主键；size_bytes / indexed_at 为自定义列名
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `file_locations` (`path` TEXT NOT NULL, " +
                    "`archivePath` TEXT, `name` TEXT NOT NULL, `ext` TEXT NOT NULL, " +
                    "`size_bytes` INTEGER NOT NULL, `mtime` INTEGER NOT NULL, " +
                    "`indexed_at` INTEGER NOT NULL, PRIMARY KEY(`path`))"
            )
        }
    }

    /** v3 → v4（M5）：新增会话记忆表（R6，AI 提炼的长期记忆）。 */
    val MIGRATION_3_4 = object : Migration(3, 4) {       // 匿名迁移对象：从版本 3 升到 4
        override fun migrate(db: SupportSQLiteDatabase) {  // 迁移回调：执行建表 SQL
            // 会话记忆表：type(FACT/PREFERENCE/TODO) 存枚举名；embedding 为向量 BLOB
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `kb_memory` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`type` TEXT NOT NULL, `topic` TEXT NOT NULL, `text` TEXT NOT NULL, " +
                    "`source` TEXT NOT NULL, `ts` INTEGER NOT NULL, `embedding` BLOB)"
            )
        }
    }

    /** 全部迁移，按版本升序排列，供 Room.databaseBuilder.addMigrations 展开使用。 */
    val ALL: Array<Migration> = arrayOf(                 // 迁移数组（供 addMigrations 展开）
        MIGRATION_1_2,                                   // 1→2
        MIGRATION_2_3,                                   // 2→3
        MIGRATION_3_4,                                   // 3→4
    )
}
