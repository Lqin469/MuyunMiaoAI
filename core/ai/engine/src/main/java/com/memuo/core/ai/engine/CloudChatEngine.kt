package com.memuo.core.ai.engine                          // 声明包名：AI 引擎模块

import com.memuo.core.db.entity.ChatMessage                // 导入消息实体
import com.memuo.core.db.entity.EngineType                 // 导入引擎类型枚举
import kotlinx.coroutines.channels.awaitClose              // 导入 awaitClose：callbackFlow 收尾
import kotlinx.coroutines.flow.Flow                        // 导入 Flow：数据流
import kotlinx.coroutines.flow.callbackFlow                // 导入 callbackFlow：回调转 Flow
import org.json.JSONArray                                 // 导入 JSONArray：tools 声明
import javax.inject.Inject                                 // 导入 Inject：构造函数注入

/**
 * 云端对话引擎（CloudChatEngine）—— ChatEngine 的云端实现（OpenAI 兼容，M3）。
 * 把 CloudApiClient 的 SSE 回调转成 Flow<ChatEvent>，供 UI 层流式消费。
 */
class CloudChatEngine @Inject constructor(                // 构造函数注入
    private val api: CloudApiClient,                      // 注入云端 API 客户端
    private val configProvider: CloudConfigProvider,      // 注入配置提供者（feature:settings 实现）
) : ChatEngine {                                          // 实现对话引擎接口

    /** 引擎类型 = 云端。 */
    override val type: EngineType = EngineType.CLOUD      // 云端引擎

    /**
     * 流式对话：读取配置 → 调用 SSE → 逐段转发为 ChatEvent.Delta → 结束发 Done。
     */
    override fun streamChat(messages: List<ChatMessage>, system: String?): Flow<ChatEvent> =  // 流式对话
        callbackFlow {                                    // 把回调式 API 转为 Flow
            val config = configProvider.current()         // 读取云端配置（协程内）
            if (config == null) {                         // 未配置云端 API
                trySend(ChatEvent.Done("未配置云端 API，请到设置页填写 baseUrl/APIKey/模型"))  // 发结束事件（带提示）
                close()                                   // 关闭流
                return@callbackFlow                       // 提前返回
            }

            api.streamChat(                               // 发起 SSE 流式请求
                config = config,                          // 传入配置
                messages = messages,                      // 传入消息
                system = system,                          // 传入系统提示词
                onDelta = { text -> trySend(ChatEvent.Delta(text)) },  // 每段增量 → 转发 Delta 事件
                onDone = { reason ->                      // 结束回调
                    trySend(ChatEvent.Done(reason))       // 转发 Done 事件
                    close()                               // 关闭流
                },
            )
            awaitClose { }                                // 流被取消时的收尾（此处无需清理）
        }

    /**
     * 带工具的一次非流式 chat（function calling）。
     * @return 最终文本或工具调用；未配置云端 API 返回 null。
     */
    suspend fun chatWithTools(                            // 带工具的非流式 chat
        messages: List<ChatMessage>,                      // 消息列表（可含 role=tool）
        system: String?,                                  // 系统提示词
        tools: JSONArray,                                 // tools 声明
    ): ChatWithToolsResult? {                             // 结果（未配置返回 null）
        val config = configProvider.current() ?: return null  // 读配置，未配置返回 null
        return api.chatWithTools(config, messages, system, tools)  // 调用客户端
    }
}
