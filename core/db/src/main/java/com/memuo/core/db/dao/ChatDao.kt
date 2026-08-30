package com.memuo.core.db.dao                            // 声明包名：数据库模块的 DAO 子包

import androidx.room.Dao                                   // 导入 Dao：Room 数据访问对象注解
import androidx.room.Insert                                // 导入 Insert：插入操作
import androidx.room.OnConflictStrategy                    // 导入冲突策略
import androidx.room.Query                                 // 导入 Query：自定义 SQL
import com.memuo.core.db.entity.ChatMessage                // 导入消息实体
import com.memuo.core.db.entity.Conversation               // 导入会话实体
import kotlinx.coroutines.flow.Flow                        // 导入 Flow：响应式数据流

/**
 * 会话/消息 DAO —— AI 对话的数据访问（M3 使用）。
 */
@Dao                                                       // 声明这是 Room DAO
interface ChatDao {                                        // 会话数据访问接口

    /** 观察所有会话（按更新时间倒序）。 */
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")  // SQL：会话倒序
    fun observeConversations(): Flow<List<Conversation>>   // 返回会话流

    /** 插入或更新一个会话。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)       // 插入，冲突替换
    suspend fun upsertConversation(conv: Conversation): Long  // 返回会话 ID

    /** 观察某个会话的全部消息（按时间正序）。 */
    @Query("SELECT * FROM messages WHERE convId = :convId ORDER BY ts ASC")  // SQL：按会话 + 时间正序
    fun observeMessages(convId: Long): Flow<List<ChatMessage>>  // 返回消息流

    /** 插入一条消息。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)       // 插入，冲突替换
    suspend fun insertMessage(msg: ChatMessage): Long      // 返回消息 ID

    /** 更新会话标题（首条消息后自动生成标题用）。 */
    @Query("UPDATE conversations SET title = :title, updatedAt = :ts WHERE id = :id")  // SQL：更新标题+时间
    suspend fun updateTitle(id: Long, title: String, ts: Long)  // 按 ID 更新标题

    /** 更新会话的最后活跃时间。 */
    @Query("UPDATE conversations SET updatedAt = :ts WHERE id = :id")  // SQL：更新活跃时间
    suspend fun touch(id: Long, ts: Long)                 // 按 ID 更新活跃时间

    /** 删除一个会话。 */
    @Query("DELETE FROM conversations WHERE id = :id")    // SQL：按 ID 删除会话
    suspend fun deleteConversation(id: Long)              // 删除会话

    /** 删除某会话的全部消息。 */
    @Query("DELETE FROM messages WHERE convId = :convId") // SQL：按会话删除消息
    suspend fun deleteMessagesByConv(convId: Long)        // 删除会话消息

    /** 删除单条消息（重新生成 AI 回复时移除旧回复用，原型迁移新增）。 */
    @Query("DELETE FROM messages WHERE id = :id")         // SQL：按 ID 删除消息
    suspend fun deleteMessage(id: Long)                   // 删除单条消息
}
