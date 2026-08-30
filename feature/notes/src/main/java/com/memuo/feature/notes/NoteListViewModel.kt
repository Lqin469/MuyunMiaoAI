package com.memuo.feature.notes                         // 声明包名：笔记业务模块

import androidx.lifecycle.ViewModel                       // 导入 ViewModel：UI 数据持有者
import androidx.lifecycle.viewModelScope                  // 导入 viewModelScope：ViewModel 协程作用域
import com.memuo.core.db.dao.NoteDao                       // 导入笔记 DAO
import com.memuo.core.db.entity.Note                       // 导入笔记实体
import com.memuo.core.db.entity.NoteType                   // 导入笔记类型枚举
import com.memuo.core.ingest.KnowledgeRepository           // 导入知识库仓库（手动存入知识库）
import com.memuo.core.ingest.NoteBridge                    // 导入笔记事件总线（core 层）
import com.memuo.core.ingest.NoteChanged                   // 导入笔记变更事件
import com.memuo.core.storage.NotePrefs                    // 导入笔记偏好（自动入库/回收站天数）
import dagger.hilt.android.lifecycle.HiltViewModel         // 导入 HiltViewModel：Hilt 提供 ViewModel
import kotlinx.coroutines.flow.Flow                       // 导入 Flow：响应式数据流
import kotlinx.coroutines.flow.SharingStarted              // 导入 SharingStarted：状态流启动策略
import kotlinx.coroutines.flow.StateFlow                   // 导入 StateFlow：只读状态流
import kotlinx.coroutines.flow.stateIn                     // 导入 stateIn：冷流转热状态流
import kotlinx.coroutines.launch                           // 导入 launch：启动协程
import javax.inject.Inject                                 // 导入 Inject：构造函数注入

/**
 * 笔记列表 ViewModel —— 常规备忘录的增删改查逻辑（M2）。
 * 数据来自 Room（NoteDao），并通过 NoteBridge 发布变更事件（供 R7 知识库订阅）。
 */
@HiltViewModel                                           // 注解：由 Hilt 创建并注入依赖
class NoteListViewModel @Inject constructor(             // 构造函数注入
    private val noteDao: NoteDao,                        // 注入笔记 DAO
    private val bridge: NoteBridge,                      // 注入事件总线
    private val repo: KnowledgeRepository,               // 注入知识库仓库（手动存入知识库）
    private val notePrefs: NotePrefs,                    // 注入笔记偏好（自动入库/回收站天数）
) : ViewModel() {                                        // 继承 ViewModel

    /** 所有未删除笔记的状态流（置顶优先、按更新时间倒序）。 */
    val notes: StateFlow<List<Note>> = noteDao.observeActive()  // 观察活跃笔记（响应式）
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())  // 转为状态流，订阅后 5 秒内保活

    /** 回收站笔记的状态流（软删除，按删除时间倒序，回收站页用）。 */
    val trashed: StateFlow<List<Note>> = noteDao.observeTrashed()  // 观察软删除笔记
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())  // 转为状态流

    /** 自动存入知识库开关（默认开，设置面板用）。 */
    val autoIngest: StateFlow<Boolean> = notePrefs.autoIngest  // 订阅开关
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)  // 转状态流

    /** 回收站保留天数（默认 30 天，设置面板/回收站页用）。 */
    val trashDays: StateFlow<Int> = notePrefs.trashDays   // 订阅天数
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 30)  // 转状态流

    /** 观察单条笔记（编辑页实时加载用）。 */
    fun observeNote(id: Long): Flow<Note?> = noteDao.observeById(id)  // 返回单条笔记的响应式流

    /** 新建一条空白笔记，创建完成后通过 [onCreated] 回调返回新笔记 ID。 */
    fun createNote(onCreated: (Long) -> Unit) {          // 新建笔记（回调式，避免异步 ID 竞态）
        viewModelScope.launch {                          // 协程中执行（IO 操作）
            val now = System.currentTimeMillis()         // 取当前时间戳
            val note = Note(                             // 构造新笔记（空白）
                title = "",                              // 空标题
                content = "",                            // 空内容
                type = NoteType.TEXT,                    // 默认纯文本
                createdAt = now,                         // 创建时间
                updatedAt = now,                         // 更新时间
            )
            val id = noteDao.upsert(note)                // 写入数据库，拿到新 ID
            bridge.emitChanged(id, NoteChanged.Action.CREATED)  // 发布"已创建"事件（供知识库订阅）
            onCreated(id)                                // 回调返回新 ID（此时才真正拿到）
        }
    }

    /** 软删除一条笔记（移入回收站）。 */
    fun deleteNote(id: Long) {                           // 删除笔记方法
        viewModelScope.launch {                          // 协程中执行
            noteDao.softDelete(id, System.currentTimeMillis())  // 写入软删除时间
            bridge.emitChanged(id, NoteChanged.Action.DELETED)   // 发布"已删除"事件
        }
    }

    /** 从回收站恢复一条笔记。 */
    fun restoreNote(id: Long) {                          // 恢复笔记
        viewModelScope.launch {                          // 协程中执行
            noteDao.restore(id)                          // 清空软删除时间
        }
    }

    /** 彻底删除一条笔记（回收站物理删除）。 */
    fun purgeNote(id: Long) {                            // 彻底删除
        viewModelScope.launch {                          // 协程中执行
            noteDao.purge(id)                            // 物理删除
        }
    }

    /** 清空回收站。 */
    fun emptyTrash() {                                   // 清空回收站
        viewModelScope.launch {                          // 协程中执行
            noteDao.purgeTrashed()                       // 批量物理删除
        }
    }

    /** 更新笔记正文（保存时调用）。 */
    fun updateContent(id: Long, title: String, content: String) {  // 更新内容方法
        viewModelScope.launch {                          // 协程中执行
            val current = noteDao.getById(id) ?: return@launch  // 读取现有笔记，不存在则直接返回
            val updated = current.copy(                  // 复制并更新字段
                title = title,                           // 更新标题
                content = content,                       // 更新正文
                updatedAt = System.currentTimeMillis(),  // 更新修改时间
            )
            noteDao.upsert(updated)                      // 写回数据库
            bridge.emitChanged(id, NoteChanged.Action.UPDATED)   // 发布"已更新"事件（触发知识库增量同步）
        }
    }

    /** 保存最新内容并手动存入知识库（先写库保证入库的是最新内容，忽略自动入库开关）。 */
    fun saveAndIngest(id: Long, title: String, content: String, onDone: () -> Unit) {  // 保存+手动入库
        viewModelScope.launch {                          // 协程中执行
            val current = noteDao.getById(id)            // 读现有笔记
            if (current != null) {                       // 存在则先保存最新内容
                val updated = current.copy(              // 复制更新
                    title = title,                       // 标题
                    content = content,                   // 正文
                    updatedAt = System.currentTimeMillis(),  // 更新时间
                )
                noteDao.upsert(updated)                  // 写库
            }
            repo.ingestNote(id)                          // 入库（此时读的是最新内容）
            onDone()                                     // 完成回调
        }
    }

    /** 保存自动入库开关。 */
    fun setAutoIngest(on: Boolean) {                     // 写开关
        viewModelScope.launch { notePrefs.setAutoIngest(on) }  // 持久化
    }

    /** 保存回收站保留天数。 */
    fun setTrashDays(days: Int) {                        // 写天数
        viewModelScope.launch { notePrefs.setTrashDays(days) }  // 持久化
    }
}
