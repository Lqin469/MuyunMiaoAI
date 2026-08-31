package com.memuo.feature.chat                           // 声明包名：对话业务模块

import androidx.lifecycle.ViewModel                       // 导入 ViewModel：UI 数据持有者
import androidx.lifecycle.viewModelScope                  // 导入 viewModelScope：ViewModel 协程作用域
import com.memuo.core.ai.engine.ChatEngine                 // 导入对话引擎接口
import com.memuo.core.ai.engine.ChatEvent                  // 导入对话事件
import com.memuo.core.ai.engine.CloudChatEngine             // 导入云端引擎（function calling 用）
import com.memuo.core.ai.engine.EngineRouter               // 导入引擎路由器（本地/云端切换，原型迁移新增）
import com.memuo.core.ai.engine.EngineSettings             // 导入引擎设置（切换后状态流）
import com.memuo.core.ai.memory.MemoryStore                // 导入记忆仓库（M5 每 N 轮提炼）
import com.memuo.core.ai.tools.ToolCallingBus              // 导入 AI 工具总线（M7 search_file）
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
 * 对话 ViewModel —— AI 对话的核心逻辑（M3 + M7 工具调用 + 原型交互迁移）。
 * 发消息 → 写库（用户消息）→ 调引擎流式接收 → 累积增量 → 结束落库（AI 回复）
 * → 解析回复中的工具调用标记 [[tool:args]] → 经 ToolCallingBus 执行 → 结果追加为消息。
 * 新增（HTML 原型迁移）：云端/本地切换、重新生成、附件暂存（图片/文件随消息展示）。
 */
@HiltViewModel                                           // 注解：由 Hilt 创建并注入依赖
class ChatViewModel @Inject constructor(                 // 构造函数注入
    private val chatDao: ChatDao,                        // 注入会话 DAO
    private val engine: ChatEngine,                      // 注入对话引擎（当前为云端实现）
    private val cloudEngine: CloudChatEngine,            // 注入云端引擎（function calling 工具循环）
    private val memoryStore: MemoryStore,                // 注入记忆仓库（M5 提炼）
    private val toolBus: ToolCallingBus,                 // 注入工具总线（M7 文件检索工具）
    private val engineSettings: EngineSettings,          // 注入引擎设置（云端/本地状态流）
    private val engineRouter: EngineRouter,              // 注入引擎路由器（切换校验）
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

    /** 当前引擎类型（云端/本地，聊天页头部胶囊显示用）。 */
    val engineType: StateFlow<EngineType> = engineSettings.engineType  // 直接暴露设置流

    /** 切换结果提示（云端/本地切换后由 UI 弹 Toast）。 */
    private val _engineMessage = MutableStateFlow<String?>(null)  // 提示消息
    val engineMessage: StateFlow<String?> = _engineMessage.asStateFlow()  // 只读暴露

    /** 消息附件（图片/文件暂存，key = 消息 ID；仅内存态，重启后丢失，原型级能力）。 */
    private val _attachments = MutableStateFlow<Map<Long, List<Attachment>>>(emptyMap())  // 附件映射
    val attachments: StateFlow<Map<Long, List<Attachment>>> = _attachments.asStateFlow()  // 只读暴露

    private var convId: Long = 0L                         // 当前会话 ID
    private var convCollectJob: kotlinx.coroutines.Job? = null  // 会话列表收集任务（防重复收集）
    private var msgCollectJob: kotlinx.coroutines.Job? = null   // 消息列表收集任务（防重复收集）

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

    /** 切换云端/本地引擎（聊天页头部胶囊，对应 HTML toggleCloudLocal）。 */
    fun toggleCloudLocal() {                              // 切换引擎
        val target = if (engineSettings.engineType.value == EngineType.CLOUD) EngineType.LOCAL else EngineType.CLOUD  // 取相反类型
        viewModelScope.launch {                          // 协程中切换
            val ok = engineRouter.switchTo(target)        // 路由切换（无本地模型则 false）
            _engineMessage.value = when {                 // 按结果提示（对应 HTML showToast）
                ok && target == EngineType.CLOUD -> "已切换到云端模式"  // 云端
                ok -> "已切换到本地模式"                  // 本地
                else -> "本地模型未就绪，请先导入模型"     // 拦截
            }
        }
    }

    /** 消费切换提示（UI 弹完置空，避免重复弹）。 */
    fun consumeEngineMessage() {                          // 消费提示
        _engineMessage.value = null                       // 置空
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

    /** 删除一个会话（连同其消息）。 */
    fun deleteConversation(id: Long) {                   // 删除会话
        viewModelScope.launch {                          // 协程中执行
            chatDao.deleteMessagesByConv(id)             // 先删消息
            chatDao.deleteConversation(id)               // 再删会话
        }
    }

    /** 发送一条用户消息并流式接收 AI 回复（附件随消息展示，原型 v20 行为）。 */
    fun send(text: String, attachments: List<Attachment> = emptyList()) {  // 发送消息
        if ((text.isBlank() && attachments.isEmpty()) || convId == 0L) return  // 空文本且无附件则忽略
        viewModelScope.launch {                          // 协程中执行
            val now = System.currentTimeMillis()         // 时间戳
            // 附件描述拼进正文（Room 只存文本；缩略图由 _attachments 内存映射渲染）
            val attachDesc = attachments.joinToString("") {  // 拼附件描述
                if (it.kind == AttachmentKind.IMAGE) "[图片:${it.name}]" else "[文件:${it.name} ${it.sizeText}]"  // 图片/文件
            }
            val msgId = chatDao.insertMessage(           // 写用户消息（insertMessage 返回行 ID）
                ChatMessage(convId = convId, role = "user", content = text + attachDesc, ts = now)  // 内容 = 文字 + 附件描述
            )
            if (attachments.isNotEmpty()) {              // 有附件
                _attachments.value = _attachments.value + (msgId to attachments)  // 记入内存映射
            }
            chatDao.touch(convId, now)                   // 更新会话活跃时间

            runStream(history = chatDao.observeMessages(convId).first(), promptText = text)  // 流式接收（复用公共逻辑）
        }
    }

    /** 重新生成：删除最后一条 AI 回复，用上一条用户消息重发（对应 HTML regenerate）。 */
    fun regenerate() {                                    // 重新生成
        if (_streaming.value) return                      // 流式中锁定（HTML regenerating 防抖）
        val last = _messages.value.lastOrNull() ?: return // 无消息则返回
        if (last.role != "assistant") return              // 最后一条不是 AI 则返回
        // 找到 AI 消息之前的最后一条用户消息作为提示词
        val userMsg = _messages.value.filter { it.role == "user" }.lastOrNull() ?: return  // 无用户消息则返回
        viewModelScope.launch {                          // 协程中执行
            chatDao.deleteMessage(last.id)               // 移除最后一条 AI 回复（HTML splice）
            runStream(history = chatDao.observeMessages(convId).first(), promptText = userMsg.content)  // 重新流式生成
        }
    }

    /** 流式接收引擎回复并落库（send/regenerate 共用的内部流程）。 */
    private suspend fun runStream(history: List<ChatMessage>, promptText: String) {  // 流式对话
        _streaming.value = true                          // 进入流式状态
        _streamText.value = ""                           // 清空流式文本
        if (engineSettings.engineType.first() == EngineType.CLOUD) {  // 云端引擎
            runCloudWithTools(history)                   // function calling 工具循环
            return                                      // 结束
        }
        val sb = StringBuilder()                         // 累积 AI 回复（本地流式）
        engine.streamChat(history, system = toolBus.describeForLlm()).collect { event ->  // 流式收集（注入工具声明）
            when (event) {                              // 分发事件
                is ChatEvent.Delta -> {                  // 增量文本
                    sb.append(event.text)                // 累积
                    _streamText.value = sb.toString()    // 实时刷新流式文本
                }
                is ChatEvent.Done -> {                   // 结束
                    // 有增量文本 → 用累积内容；无内容但有原因（错误/提示）→ 显示原因，避免"发送无反应"
                    val rawText = sb.toString()          // 原始回复（可能含工具标记）
                    val finalText = rawText.ifBlank {
                        event.reason.takeIf { it.isNotBlank() && it != "stop" && it != "empty" }.orEmpty()
                    }
                    if (finalText.isNotBlank()) {         // 有最终文本则落库
                        // 把工具调用标记（[[name:args]]）从展示文本中移除（结果以独立消息追加）
                        val cleaned = finalText.replace(Regex("\\[\\[[a-z_]+:.*?]]"), "（已执行设备检索）")  // 标记替换为占位
                        chatDao.insertMessage(ChatMessage(convId = convId, role = "assistant", content = cleaned, ts = System.currentTimeMillis()))  // 写 AI 回复
                    }
                    _streaming.value = false             // 退出流式状态
                    _streamText.value = ""               // 清空
                    handleToolCalls(finalText)           // 执行回复中的工具调用（M7）
                    maybeRemember(history + ChatMessage(convId = convId, role = "user", content = promptText, ts = 0L))  // 每 N 轮触发记忆提炼（M5）
                }
            }
        }
    }

    /** 解析 AI 回复中的工具调用标记并执行，结果追加为一条消息（M7 文件检索工具）。 */
    private suspend fun handleToolCalls(text: String) {   // 工具调用处理
        val calls = toolBus.extractCalls(text)            // 提取 [[name:args]] 标记
        if (calls.isEmpty()) return                       // 无标记则返回
        val results = calls.mapNotNull { (name, args) ->  // 逐个执行
            val result = toolBus.dispatch(name, args)     // 分发执行
            if (result.contains("\"error\"")) null else "工具「$name」结果：$result"  // 失败丢弃、成功格式化
        }
        if (results.isNotEmpty()) {                       // 有结果
            chatDao.insertMessage(                        // 结果追加为消息
                ChatMessage(
                    convId = convId,
                    role = "assistant",
                    content = "🔍 已执行设备检索：\n" + results.joinToString("\n"),  // 结果内容
                    ts = System.currentTimeMillis(),
                )
            )
        }
    }

    /** 云端 function calling 工具循环：chatWithTools → 工具调用 → 执行 → 回传，直到最终文本。 */
    private suspend fun runCloudWithTools(history: List<ChatMessage>) {  // function calling 循环
        val messages = history.toMutableList()            // 消息列表（追加 tool 结果）
        val tools = toolBus.describeAsTools()             // OpenAI tools 声明
        var finalText = ""                                // 最终回复
        var rounds = 0                                    // 工具轮数
        try {
            while (rounds < 4) {                          // 最多 4 轮工具调用
                val result = cloudEngine.chatWithTools(messages, system = null, tools = tools)  // 一次 chat
                if (result == null) {                     // 未配置云端 API
                    finalText = "未配置云端 API，请到设置页填写 baseUrl/APIKey/模型"
                    break
                }
                if (result.toolCalls.isEmpty()) {         // 无工具调用 → 最终文本
                    finalText = result.text.orEmpty()
                    break
                }
                val executed = result.toolCalls.mapNotNull { tc ->  // 执行每个工具
                    val r = toolBus.dispatch(tc.name, tc.arguments)  // 分发执行
                    if (r.contains("\"error\"")) null else "工具「${tc.name}」结果：$r"  // 失败丢弃、成功格式化
                }
                if (executed.isEmpty()) {                 // 工具全部失败
                    finalText = result.text.orEmpty()     // 用已有文本
                    break
                }
                messages.add(                             // 工具结果回传（role=tool）
                    ChatMessage(convId = convId, role = "tool", content = executed.joinToString("\n"), ts = System.currentTimeMillis())
                )
                rounds++                                  // 轮数 +1
            }
        } catch (e: Exception) {                          // 网络/解析异常
            finalText = "对话出错：${e.message ?: "未知错误"}"
        }
        if (finalText.isNotBlank()) {                     // 有最终文本则落库
            chatDao.insertMessage(                        // 写 AI 回复
                ChatMessage(convId = convId, role = "assistant", content = finalText, ts = System.currentTimeMillis())
            )
        }
        _streaming.value = false                          // 退出流式状态
        _streamText.value = ""                            // 清空
    }

    /** 每 4 轮对话触发一次记忆提炼（M5）：把最近 8 条消息交给 MemoryStore 提炼并落库。 */
    private suspend fun maybeRemember(history: List<ChatMessage>) {  // 记忆提炼触发
        val userCount = history.count { it.role == "user" }  // 统计用户消息数（轮数）
        if (userCount == 0 || userCount % 4 != 0) return     // 不是第 4 的倍数轮则不提炼
        memoryStore.remember(history.takeLast(8))           // 取最近 8 条消息提炼并落库
    }
}

/** 附件类型枚举：图片 / 文件。 */
enum class AttachmentKind { IMAGE, FILE }                 // 两种附件

/**
 * 附件（Attachment）—— 聊天消息携带的图片/文件（原型 v18/v20 迁移）。
 * uri 指向相册/SAF 位置；图片由 UI 层经 rememberBitmap 解码显示。
 */
data class Attachment(                                    // 附件数据类
    val kind: AttachmentKind,                             // 类型
    val name: String,                                     // 文件名
    val uri: String,                                      // 内容 Uri
    val sizeText: String,                                 // 大小文案（如 1.2 MB）
)
