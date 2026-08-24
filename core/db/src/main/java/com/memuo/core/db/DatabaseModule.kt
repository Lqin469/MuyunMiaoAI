package com.memuo.core.db                               // 声明包名：数据库模块根包

import android.content.Context                            // 导入 Context：构建 Room 需要
import androidx.room.Room                                 // 导入 Room：数据库构建器入口
import com.memuo.core.db.dao.ChatDao                      // 导入会话 DAO
import com.memuo.core.db.dao.ConsentAuditDao              // 导入审计 DAO
import com.memuo.core.db.dao.FileLocationDao              // 导入文件位置 DAO
import com.memuo.core.db.dao.KbDao                        // 导入知识库 DAO
import com.memuo.core.db.dao.MemoryDao                    // 导入记忆 DAO
import com.memuo.core.db.dao.NoteDao                      // 导入笔记 DAO
import com.memuo.core.storage.StorageProvider             // 导入存储提供者（决定数据库文件路径）
import dagger.Module                                      // 导入 Module：Hilt 模块注解
import dagger.Provides                                    // 导入 Provides：Hilt 提供方法注解
import dagger.hilt.InstallIn                              // 导入 InstallIn：指定安装组件
import dagger.hilt.components.SingletonComponent          // 导入 SingletonComponent：应用级单例组件
import javax.inject.Singleton                             // 导入 Singleton：单例作用域

/**
 * Hilt 装配模块：提供数据库与 DAO 实例。
 * 关键点：数据库文件路径走 StorageProvider.dbDir()（R4/R5），不写死路径。
 */
@Module                                                    // 声明 Hilt 模块
@InstallIn(SingletonComponent::class)                      // 安装到应用级单例组件
object DatabaseModule {                                    // 单例对象：提供数据库依赖

    /** 提供 AppDatabase 实例（单例），数据库文件放在存储提供者指定的 db 目录。 */
    @Provides                                              // 标记为提供依赖
    @Singleton                                             // 单例（全应用共享一个数据库连接）
    fun provideDatabase(                                   // 提供方法
        context: Context,                                  // 注入应用上下文
        storage: StorageProvider,                          // 注入存储提供者（决定路径）
    ): AppDatabase {                                       // 返回数据库实例
        storage.ensureDirs()                               // 先确保目录存在（避免建库时目录缺失崩溃）
        val dbFile = storage.dbDir().resolve("muyunmiao.db") // 数据库文件 = db 目录下的 muyunmiao.db
        return Room.databaseBuilder(                       // 开始构建数据库
            context,                                       // 应用上下文
            AppDatabase::class.java,                       // 数据库类
            dbFile.absolutePath,                           // 数据库文件绝对路径（支持自定义目录）
        ).fallbackToDestructiveMigration()                 // 开发期：版本升级无 Migration 时重建（发布前应改显式 Migration）
            .build()                                       // 构建并返回实例
    }

    /** 提供 NoteDao（依赖已构建的数据库）。 */
    @Provides                                              // 标记为提供依赖
    fun provideNoteDao(db: AppDatabase): NoteDao = db.noteDao()  // 从数据库取 DAO

    /** 提供 ChatDao。 */
    @Provides                                              // 标记为提供依赖
    fun provideChatDao(db: AppDatabase): ChatDao = db.chatDao()  // 从数据库取会话 DAO

    /** 提供 KbDao。 */
    @Provides                                              // 标记为提供依赖
    fun provideKbDao(db: AppDatabase): KbDao = db.kbDao()  // 从数据库取知识库 DAO

    /** 提供 FileLocationDao。 */
    @Provides                                              // 标记为提供依赖
    fun provideFileLocationDao(db: AppDatabase): FileLocationDao = db.fileLocationDao()  // 从数据库取文件位置 DAO

    /** 提供 MemoryDao。 */
    @Provides                                              // 标记为提供依赖
    fun provideMemoryDao(db: AppDatabase): MemoryDao = db.memoryDao()  // 从数据库取记忆 DAO

    /** 提供 ConsentAuditDao。 */
    @Provides                                              // 标记为提供依赖
    fun provideConsentAuditDao(db: AppDatabase): ConsentAuditDao = db.consentAuditDao()  // 从数据库取审计 DAO
}
