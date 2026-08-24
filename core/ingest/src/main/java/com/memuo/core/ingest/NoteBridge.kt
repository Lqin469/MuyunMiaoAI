package com.memuo.core.ingest                            // 声明包名：内容入库模块（NoteBridge 上移到 core，供各层订阅）

import kotlinx.coroutines.flow.MutableSharedFlow          // 导入 MutableSharedFlow：可发射事件的热流
import kotlinx.coroutines.flow.SharedFlow                 // 导入 SharedFlow：只读事件流
import kotlinx.coroutines.flow.asSharedFlow               // 导入 asSharedFlow：把可变流转为只读
import javax.inject.Inject                                 // 导入 Inject：构造函数注入
import javax.inject.Singleton                              // 导入 Singleton：单例作用域

/**
 * 笔记变更事件（NoteBridge 发布，供 R7「笔记自动进知识库」订阅）。
 * @param noteId 发生变更的笔记 ID
 * @param action 变更类型
 */
data class NoteChanged(                                   // 笔记变更事件数据类
    val noteId: Long,                                     // 笔记 ID
    val action: Action,                                   // 变更类型
) {
    /** 变更动作枚举：CREATED=新建 / UPDATED=更新 / DELETED=删除。 */
    enum class Action { CREATED, UPDATED, DELETED }       // 三种变更动作
}

/**
 * 笔记事件总线（NoteBridge）—— 领域事件桥（core 层，供 feature:notes 发布、core:ingest 订阅）。
 * 笔记的增删改在这里发事件，知识库模块订阅它实现「备忘录内容自动被 AI 读取」（R7）。
 */
@Singleton                                               // 单例：全应用共享一个事件总线
class NoteBridge @Inject constructor() {                 // 构造函数注入（无参数）

    /** 内部可变事件流（带缓冲，订阅前的事件可保留给慢消费者）。 */
    private val _changes = MutableSharedFlow<NoteChanged>(extraBufferCapacity = 64)  // 缓冲 64 条事件

    /** 对外只读事件流。 */
    val changes: SharedFlow<NoteChanged> = _changes.asSharedFlow()  // 只读视图，供订阅者收集

    /** 发布一条笔记变更事件（挂起，供协程调用）。 */
    suspend fun emitChanged(noteId: Long, action: NoteChanged.Action) {  // 发布事件方法
        _changes.emit(NoteChanged(noteId, action))       // 向事件流发射一条变更事件
    }
}
