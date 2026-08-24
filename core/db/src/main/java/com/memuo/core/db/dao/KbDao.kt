package com.memuo.core.db.dao                            // 声明包名：数据库模块的 DAO 子包

import androidx.room.Dao                                   // 导入 Dao：Room 数据访问对象注解
import androidx.room.Insert                                // 导入 Insert：插入操作
import androidx.room.OnConflictStrategy                    // 导入冲突策略
import androidx.room.Query                                 // 导入 Query：自定义 SQL
import com.memuo.core.db.entity.IngestStatus               // 导入入库状态枚举
import com.memuo.core.db.entity.KbChunk                    // 导入分块实体
import com.memuo.core.db.entity.KbDocument                 // 导入文档实体
import kotlinx.coroutines.flow.Flow                        // 导入 Flow：响应式数据流

/**
 * 知识库 DAO —— 投喂文档与分块的读写（M4 使用）。
 */
@Dao                                                       // 声明这是 Room DAO
interface KbDao {                                          // 知识库数据访问接口

    /** 插入或更新一份文档（冲突替换，保证幂等）。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)       // 插入，冲突替换
    suspend fun upsertDocument(doc: KbDocument)            // 写入文档

    /** 观察某知识库的全部文档（按入库时间倒序）。 */
    @Query("SELECT * FROM kb_documents WHERE folderId = :folderId ORDER BY indexedAt DESC")  // SQL：按知识库 + 时间
    fun observeDocuments(folderId: String): Flow<List<KbDocument>>  // 返回文档流

    /** 按 ID 取一份文档。 */
    @Query("SELECT * FROM kb_documents WHERE docId = :docId")  // SQL：按 ID 查
    suspend fun getDocument(docId: String): KbDocument?    // 返回可空文档

    /** 更新文档状态（解析中/已入库/失败）。 */
    @Query("UPDATE kb_documents SET status = :status WHERE docId = :docId")  // SQL：更新状态
    suspend fun updateStatus(docId: String, status: IngestStatus)  // 按 ID 更新状态

    /** 删除某文档的全部分块（重新入库前先清空，保证幂等）。 */
    @Query("DELETE FROM kb_chunks WHERE docId = :docId")   // SQL：按文档删除分块
    suspend fun deleteChunksByDoc(docId: String)           // 清空旧分块

    /** 批量插入分块。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)       // 插入，冲突替换
    suspend fun insertChunks(chunks: List<KbChunk>)        // 批量写入分块

    /** 取某知识库的全部分块（向量检索的候选集）。 */
    @Query("SELECT * FROM kb_chunks WHERE folderId = :folderId")  // SQL：按知识库取全部分块
    suspend fun chunksByFolder(folderId: String): List<KbChunk>  // 返回分块列表

    /** 关键词检索：按知识库在分块文本中模糊匹配（LIKE，近似 FTS）。 */
    @Query("SELECT * FROM kb_chunks WHERE folderId = :folderId AND text LIKE '%' || :keyword || '%' LIMIT :limit")  // SQL：模糊匹配
    suspend fun searchByKeyword(folderId: String, keyword: String, limit: Int): List<KbChunk>  // 返回命中的分块
}
