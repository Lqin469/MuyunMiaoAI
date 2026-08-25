package com.memuo.core.db.dao                            // 声明包名：数据库模块的 DAO 子包

import androidx.room.Dao                                   // 导入 Dao：Room 数据访问对象注解
import androidx.room.Insert                                // 导入 Insert：插入操作
import androidx.room.OnConflictStrategy                    // 导入冲突策略
import androidx.room.Query                                 // 导入 Query：自定义 SQL
import com.memuo.core.db.entity.FileLocation               // 导入文件位置实体

/**
 * 文件位置 DAO —— 不可解析文件的位置记录读写（R10）。
 */
@Dao                                                       // 声明这是 Room DAO
interface FileLocationDao {                                // 文件位置数据访问接口

    /** 插入或更新一条位置记录（冲突替换，保证幂等）。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)       // 插入，冲突替换
    suspend fun upsert(loc: FileLocation)                  // 写入位置记录

    /** 批量插入或更新位置记录（全盘索引批量落库用）。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)       // 批量插入，冲突替换
    suspend fun upsertAll(locs: List<FileLocation>)        // 批量写入

    /** 按文件名/路径关键词检索位置记录（AI 问答"文件在哪"用）。 */
    @Query("SELECT * FROM file_locations WHERE name LIKE '%' || :keyword || '%' OR path LIKE '%' || :keyword || '%' LIMIT :limit")  // SQL：模糊匹配
    suspend fun search(keyword: String, limit: Int = 20): List<FileLocation>  // 返回命中的位置记录
}
