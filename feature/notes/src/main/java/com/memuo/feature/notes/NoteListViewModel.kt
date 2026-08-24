package com.memuo.feature.notes                         // 声明包名：笔记业务模块

import androidx.lifecycle.ViewModel                       // 导入 ViewModel：UI 数据持有者
import androidx.lifecycle.viewModelScope                  // 导入 viewModelScope：ViewModel 协程作用域
import com.memuo.core.db.dao.NoteDao                       // 导入笔记 DAO
import com.memuo.core.db.entity.Note                       // 导入笔记实体
import com.memuo.core.db.entity.NoteType                   // 导入笔记类型枚举
import com.memuo.core.ingest.NoteBridge                    // 导入笔记事件总线（core 层）
import com.memuo.core.ingest.NoteChanged                   // 导入笔记变更事件
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
) : ViewModel() {                                        // 继承 ViewModel

    /** 所有未删除笔记的状态流（置顶优先、按更新时间倒序）。 */
    val notes: StateFlow<List<Note>> = noteDao.observeActive()  // 观察活跃笔记（响应式）
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())  // 转为状态流，订阅后 5 秒内保活

    /** 观察单条笔记（编辑页实时加载用）。 */
    fun observeNote(id: Long): Flow<Note?> = noteDao.observeById(id)  // 返回单条笔记的响应式流

    /** 新建一条空白笔记，返回新笔记 ID。 */
    fun createNote(): Long {                             // 新建笔记方法
        val now = System.currentTimeMillis()             // 取当前时间戳
        val note = Note(                                 // 构造新笔记（空白）
            title = "",                                  // 空标题
            content = "",                                // 空内容
            type = NoteType.TEXT,                        // 默认纯文本
            createdAt = now,                             // 创建时间
            updatedAt = now,                             // 更新时间
        )
        var id = 0L                                      // 保存插入后的 ID
        viewModelScope.launch {                          // 协程中执行（IO 操作）
            id = noteDao.upsert(note)                    // 写入数据库，拿到新 ID
            bridge.emitChanged(id, NoteChanged.Action.CREATED)  // 发布"已创建"事件（供知识库订阅）
        }
        return id                                        // 返回 ID（注意：异步写入，ID 在协程里才赋值，调用方应仅用于导航）
    }

    /** 软删除一条笔记。 */
    fun deleteNote(id: Long) {                           // 删除笔记方法
        viewModelScope.launch {                          // 协程中执行
            noteDao.softDelete(id, System.currentTimeMillis())  // 写入软删除时间
            bridge.emitChanged(id, NoteChanged.Action.DELETED)   // 发布"已删除"事件
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
}
