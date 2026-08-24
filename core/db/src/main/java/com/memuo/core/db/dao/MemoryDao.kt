package com.memuo.core.db.dao                            // 声明包名：数据库模块的 DAO 子包

import androidx.room.Dao                                   // 导入 Dao：Room 数据访问对象注解
import androidx.room.Insert                                // 导入 Insert：插入操作
import androidx.room.OnConflictStrategy                    // 导入冲突策略
import androidx.room.Query                                 // 导入 Query：自定义 SQL
import com.memuo.core.db.entity.KbMemory                   // 导入记忆实体
import com.memuo.core.db.entity.MemoryType                 // 导入记忆类型枚举

/**
 * 会话记忆 DAO —— 长期记忆的读写（R6，M5 使用）。
 */
@Dao                                                       // 声明这是 Room DAO
interface MemoryDao {                                      // 记忆数据访问接口

    /** 插入或更新一条记忆（冲突替换，保证幂等）。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)       // 插入，冲突替换
    suspend fun upsert(memory: KbMemory): Long             // 写入记忆，返回 ID

    /** 按关键词检索记忆（关键词兜底，语义检索走 HybridRetriever）。 */
    @Query("SELECT * FROM kb_memory WHERE text LIKE '%' || :keyword || '%' OR topic LIKE '%' || :keyword || '%' LIMIT :limit")  // SQL：模糊匹配
    suspend fun searchByKeyword(keyword: String, limit: Int = 20): List<KbMemory>  // 返回命中的记忆

    /** 取全部记忆（语义检索候选集）。 */
    @Query("SELECT * FROM kb_memory ORDER BY ts DESC LIMIT :limit")  // SQL：按时间倒序取最近 N 条
    suspend fun recent(limit: Int = 500): List<KbMemory>  // 返回最近的记忆列表

    /** 删除某条记忆（用户手动清理用）。 */
    @Query("DELETE FROM kb_memory WHERE id = :id")         // SQL：按 ID 删除
    suspend fun delete(id: Long)                           // 删除记忆
}
