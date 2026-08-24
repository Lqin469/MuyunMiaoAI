package com.memuo.feature.chat                           // 声明包名：对话业务模块

import androidx.lifecycle.ViewModel                       // 导入 ViewModel：UI 数据持有者
import androidx.lifecycle.viewModelScope                  // 导入 viewModelScope：ViewModel 协程作用域
import com.memuo.core.ai.engine.ChatEngine                 // 导入对话引擎接口
import com.memuo.core.ai.engine.ChatEvent                  // 导入对话事件
import com.memuo.core.ai.memory.MemoryStore                // 导入记忆仓库（M5 每 N 轮提炼）
import com.memuo.core.db.dao.ChatDao                       // 导入会话 DAO
import com.memuo.core.db.entity.ChatMessage                // 导入消息实体
import com.memuo.core.db.entity.Conversation               // 导入会话实体
import com.memuo.core.db.entity.EngineType                 // 导入引擎类型
import dagger.hilt.android.lifecycle.HiltViewModel         // 导入 HiltViewModel：Hilt 提供 ViewModel
import kotlinx.coroutines.flow.MutableStateFlow            // 导入可变状态流
import kotlinx.coroutines.flow.StateFlow                  // 导入只读状态流
import kotlinx.coroutines.flow.asStateFlow                // 导入 asStateFlow：转只读
import kotlinx.coroutines.flow.first                       // 导入 first：取一次值
import kotlinx.coroutines.launch                           // 导入 launch：启动协程
import javax.inject.Inject                                 // 导入 Inject：构造函数注入

/**
 * 对话 ViewModel —— AI 对话的核心逻辑（M3）。
 * 发消息 → 写库（用户消息）→ 调引擎流式接收 → 累积增量 → 结束落库（AI 回复）。
 */
@HiltViewModel                                           // 注解：由 Hilt 创建并注入依赖
class ChatViewModel @Inject constructor(                 // 构造函数注入
    private val chatDao: ChatDao,                        // 注入会话 DAO
    private val engine: ChatEngine,                      // 注入对话引擎（当前为云端实现）
    private val memoryStore: MemoryStore,                // 注入记忆仓库（M5 提炼）
) : ViewModel() {                                        // 继承 ViewModel

    /** 会话列表状态流（用于会话侧栏/列表页）。 */
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())  // 可变会话列表
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow() // 只读会话列表

    /** 当前会话的消息列表。 */
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())  // 可变消息列表
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()      // 只读消息列表

    /** 是否正在流式生成。 */
    private val _streaming = MutableStateFlow(false)      // 流式状态
    val streaming: StateFlow<Boolean> = _streaming.asStateFlow()  // 只读流式状态

    /** 正在流式生成的文本（实时显示用）。 */
    private val _streamText = MutableStateFlow("")        // 流式文本
    val streamText: StateFlow<String> = _streamText.asStateFlow()  // 只读流式文本

    private var convId: Long = 0L                         // 当前会话 ID

    /** 加载会话列表（进入对话页时调用）。 */
    fun loadConversations() {                             // 加载会话列表
        viewModelScope.launch {                          // 协程中收集
            chatDao.observeConversations().collect { _conversations.value = it }  // 收集并更新
        }
    }

    /** 打开某个会话（加载其消息）。 */
    fun openConversation(id: Long) {                     // 打开会话
        convId = id                                      // 记录当前会话 ID
        viewModelScope.launch {                          // 协程中收集消息
            chatDao.observeMessages(id).collect { _messages.value = it }  // 收集消息并更新
        }
    }

    /** 新建一个会话，创建完成后通过 [onCreated] 回调返回会话 ID。 */
    fun newConversation(onCreated: (Long) -> Unit) {     // 新建会话（回调式，避免异步 ID 竞态）
        viewModelScope.launch {                          // 协程中写入
            val now = System.currentTimeMillis()         // 时间戳
            val conv = Conversation(                     // 构造新会话
                title = "新对话",                        // 默认标题
                engine = engine.type,                    // 用当前生效引擎类型（EngineRouter 动态返回）
                createdAt = now,                         // 创建时间
                updatedAt = now,                         // 更新时间
            )
            val id = chatDao.upsertConversation(conv)    // 落库拿 ID
            onCreated(id)                                // 回调返回真实 ID
        }
    }

    /** 确保存在一个可用会话：有则返回最新，无则新建（供对话 tab 进入时调用，避免异步竞态）。 */
    fun ensureConversation(onReady: (Long) -> Unit) {    // 确保会话存在
        viewModelScope.launch {                          // 协程中执行
            val convs = chatDao.observeConversations().first()  // 取一次真实会话列表（阻塞等首个值）
            if (convs.isEmpty()) {                       // 无会话
                newConversation(onReady)                 // 新建并回调 ID
            } else {                                     // 有会话
                onReady(convs.first().id)                // 用最新会话 ID 回调
            }
        }
    }

    /** 发送一条用户消息并流式接收 AI 回复。 */
    fun send(text: String) {                             // 发送消息
        if (text.isBlank() || convId == 0L) return       // 空文本或无会话则忽略
        viewModelScope.launch {                          // 协程中执行
            val now = System.currentTimeMillis()         // 时间戳
            chatDao.insertMessage(ChatMessage(convId = convId, role = "user", content = text, ts = now))  // 写用户消息
            chatDao.touch(convId, now)                   // 更新会话活跃时间

            val history = chatDao.observeMessages(convId).first()  // 取当前会话全部历史（一次性）
            _streaming.value = true                      // 进入流式状态
            _streamText.value = ""                       // 清空流式文本
            val sb = StringBuilder()                     // 累积 AI 回复

            engine.streamChat(history, system = null).collect { event ->  // 流式收集事件
                when (event) {                          // 分发事件
                    is ChatEvent.Delta -> {              // 增量文本
                        sb.append(event.text)            // 累积
                        _streamText.value = sb.toString() // 实时刷新流式文本
                    }
                    is ChatEvent.Done -> {               // 结束
                        // 有增量文本 → 用累积内容；无内容但有原因（错误/提示）→ 显示原因，避免"发送无反应"
                        val finalText = sb.toString().ifBlank {
                            event.reason.takeIf { it.isNotBlank() && it != "stop" && it != "empty" }.orEmpty()
                        }
                        if (finalText.isNotBlank()) {     // 有最终文本则落库
                            chatDao.insertMessage(ChatMessage(convId = convId, role = "assistant", content = finalText, ts = System.currentTimeMillis()))  // 写 AI 回复（或错误提示）
                        }
                        _streaming.value = false         // 退出流式状态
                        _streamText.value = ""           // 清空
                        maybeRemember(history)           // 每 N 轮触发记忆提炼（M5）
                    }
                }
            }
        }
    }

    /** 每 4 轮对话触发一次记忆提炼（M5）：把最近 8 条消息交给 MemoryStore 提炼并落库。 */
    private suspend fun maybeRemember(history: List<ChatMessage>) {  // 记忆提炼触发
        val userCount = history.count { it.role == "user" }  // 统计用户消息数（轮数）
        if (userCount == 0 || userCount % 4 != 0) return     // 不是第 4 的倍数轮则不提炼
        memoryStore.remember(history.takeLast(8))           // 取最近 8 条消息提炼并落库
    }
}
