package com.memuo.core.db.dao                            // 声明包名：数据库模块的 DAO 子包

import androidx.room.Dao                                   // 导入 Dao：Room 的数据访问对象注解
import androidx.room.Insert                                // 导入 Insert：插入操作
import androidx.room.OnConflictStrategy                    // 导入冲突策略（重复插入时的处理）
import androidx.room.Query                                 // 导入 Query：自定义 SQL 查询
import com.memuo.core.db.entity.ConsentAuditEntity         // 导入审计实体
import com.memuo.core.db.entity.Note                       // 导入笔记实体
import com.memuo.core.db.entity.TodoItem                   // 导入待办实体
import kotlinx.coroutines.flow.Flow                        // 导入 Flow：可观察的响应式数据流

/**
 * 笔记 DAO —— 常规备忘录的增删改查（M2 使用）。
 */
@Dao                                                       // 声明这是一个 Room DAO
interface NoteDao {                                        // 笔记数据访问接口

    /** 观察所有未删除的笔记（置顶优先、按更新时间倒序）。 */
    @Query("SELECT * FROM notes WHERE deletedAt IS NULL ORDER BY pinned DESC, updatedAt DESC")  // SQL：过滤软删除 + 排序
    fun observeActive(): Flow<List<Note>>                  // 返回 Flow：数据变化时自动推送（响应式）

    /** 观察回收站中的笔记（软删除的，按删除时间倒序，回收站页用）。 */
    @Query("SELECT * FROM notes WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")  // SQL：只看软删除 + 排序
    fun observeTrashed(): Flow<List<Note>>                 // 返回 Flow：回收站列表

    /** 观察单条笔记（编辑页实时加载用）。 */
    @Query("SELECT * FROM notes WHERE id = :id")           // SQL：按 ID 查单条
    fun observeById(id: Long): Flow<Note?>                 // 返回可空 Flow（删除后自动变 null）

    /** 按 ID 取单条笔记（一次性读取）。 */
    @Query("SELECT * FROM notes WHERE id = :id")           // SQL：按 ID 查单条
    suspend fun getById(id: Long): Note?                   // 挂起函数：一次性返回

    /** 插入或更新一条笔记（冲突时替换）。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)       // 插入，主键冲突则替换
    suspend fun upsert(note: Note): Long                   // 挂起函数：返回插入后的行 ID

    /** 软删除一条笔记（写入删除时间，不物理删除，可回收）。 */
    @Query("UPDATE notes SET deletedAt = :ts WHERE id = :id")  // SQL：设置软删除时间
    suspend fun softDelete(id: Long, ts: Long)             // 按 ID 软删除

    /** 从回收站恢复一条笔记（清空软删除时间）。 */
    @Query("UPDATE notes SET deletedAt = NULL WHERE id = :id")  // SQL：清空软删除时间
    suspend fun restore(id: Long)                          // 按 ID 恢复

    /** 彻底删除一条笔记（回收站「彻底删除」用，物理删除）。 */
    @Query("DELETE FROM notes WHERE id = :id")             // SQL：物理删除
    suspend fun purge(id: Long)                            // 按 ID 彻底删除

    /** 清空回收站（物理删除全部软删除笔记）。 */
    @Query("DELETE FROM notes WHERE deletedAt IS NOT NULL")  // SQL：批量物理删除
    suspend fun purgeTrashed()                             // 清空回收站

    /** 观察某条笔记的全部待办条目（按排序号升序）。 */
    @Query("SELECT * FROM todo_items WHERE noteId = :noteId ORDER BY `order` ASC")  // SQL：按所属笔记 + 排序
    fun observeTodos(noteId: Long): Flow<List<TodoItem>>   // 返回待办流

    /** 插入或更新一条待办。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)       // 插入，冲突替换
    suspend fun upsertTodo(item: TodoItem): Long           // 返回行 ID

    /** 更新待办的完成状态。 */
    @Query("UPDATE todo_items SET done = :done WHERE id = :id")  // SQL：更新完成状态
    suspend fun updateTodoDone(id: Long, done: Boolean)    // 按 ID 更新

    /** 删除一条待办。 */
    @Query("DELETE FROM todo_items WHERE id = :id")        // SQL：按 ID 删除
    suspend fun deleteTodo(id: Long)                       // 删除待办
}

/**
 * 搜索审计 DAO —— 记录搜索/索引触发（隐私自检依据，ADR-001）。
 */
@Dao                                                       // 声明这是一个 Room DAO
interface ConsentAuditDao {                                // 审计日志数据访问接口

    /** 插入一条审计日志（冲突时替换，保证同一请求只留一条）。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)       // 插入，冲突替换
    suspend fun insert(entry: ConsentAuditEntity)          // 挂起函数：写入审计记录

    /** 按时间倒序查询最近的审计记录（设置页"搜索审计"展示用）。 */
    @Query("SELECT * FROM consent_audit ORDER BY started_at DESC LIMIT :limit")  // SQL：倒序取最近 N 条
    suspend fun recent(limit: Int = 100): List<ConsentAuditEntity>  // 返回最近的审计记录列表
}
