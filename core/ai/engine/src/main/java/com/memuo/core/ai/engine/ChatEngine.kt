package com.memuo.core.ai.engine                          // 声明包名：AI 引擎模块

import com.memuo.core.db.entity.ChatMessage                // 导入消息实体（db 层）
import com.memuo.core.db.entity.EngineType                 // 导入引擎类型枚举（LOCAL/CLOUD）
import kotlinx.coroutines.flow.Flow                        // 导入 Flow：响应式数据流

/**
 * 对话事件（ChatEvent）—— 流式对话的推送单元。
 * Delta：一段增量文本（流式 token）；Done：本轮生成结束（携带结束原因）。
 */
sealed interface ChatEvent {                              // 密封接口：对话事件只有两种
    /** 增量文本事件（流式输出的一个片段）。 */
    data class Delta(val text: String) : ChatEvent        // 携带一段文本

    /** 生成结束事件。 */
    data class Done(val reason: String) : ChatEvent       // 携带结束原因（如 "stop" / 错误信息）
}

/**
 * 对话引擎接口（ChatEngine）—— 架构不变式：所有 AI 对话能力只通过本接口。
 * 本地（MNN）与云端（OpenAI 兼容）双实现可互换（R 双轨制）。
 */
interface ChatEngine {                                    // 对话引擎接口

    /** 引擎类型（LOCAL / CLOUD）。 */
    val type: EngineType                                  // 只读类型

    /**
     * 发起一轮流式对话。
     * @param messages 历史消息（含最新一条用户消息）
     * @param system 系统提示词（可空）
     * @return 对话事件流（Delta 增量 + Done 结束）
     */
    fun streamChat(messages: List<ChatMessage>, system: String? = null): Flow<ChatEvent>  // 流式对话
}
