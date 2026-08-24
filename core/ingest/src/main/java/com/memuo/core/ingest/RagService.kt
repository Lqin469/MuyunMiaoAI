package com.memuo.core.ingest                            // 声明包名：内容入库模块（含检索增强问答）

import com.memuo.core.ai.embed.HybridRetriever            // 导入混合检索器
import com.memuo.core.ai.engine.ChatEngine                 // 导入对话引擎接口
import com.memuo.core.ai.engine.ChatEvent                  // 导入对话事件
import com.memuo.core.ai.memory.MemoryStore                // 导入记忆仓库（M5 并入长期记忆）
import com.memuo.core.db.entity.ChatMessage                // 导入消息实体
import kotlinx.coroutines.flow.Flow                        // 导入 Flow：数据流
import javax.inject.Inject                                 // 导入 Inject：构造函数注入
import javax.inject.Singleton                              // 导入 Singleton：单例作用域

/**
 * 检索增强问答服务（RagService）—— 知识库 + 长期记忆问答的核心（M4/M5）。
 * 流程：问题 → 混合检索命中块 + 检索长期记忆 → 拼装带引用 Prompt → 交给引擎流式生成。
 */
@Singleton                                               // 单例
class RagService @Inject constructor(                    // 构造函数注入
    private val retriever: HybridRetriever,              // 注入混合检索器
    private val memoryStore: MemoryStore,                // 注入记忆仓库（M5）
    private val engine: ChatEngine,                      // 注入对话引擎（本地/云端）
) {
    /**
     * 基于知识库 + 长期记忆回答一个问题（流式返回）。
     * @param folderId 知识库 ID
     * @param question 用户问题
     * @return 对话事件流（Delta 增量 + Done 结束）
     */
    suspend fun ask(folderId: String, question: String): Flow<ChatEvent> {  // 知识库问答
        val hits = retriever.retrieve(folderId, question)  // 检索知识库命中块
        val memories = memoryStore.search(question, limit = 5)  // 检索长期记忆（关键词兜底）

        val memorySection = if (memories.isNotEmpty()) {  // 若有相关记忆，拼装记忆段
            "【你的长期记忆】\n" + memories.joinToString("\n") { "- ${it.text}" } + "\n"
        } else ""                                         // 无记忆则空

        if (hits.isEmpty() && memories.isEmpty()) {       // 知识库与记忆都为空
            return engine.streamChat(                     // 直接让引擎提示
                messages = listOf(ChatMessage(convId = 0, role = "user", content = question, ts = 0)),  // 原问题
                system = "知识库与记忆均为空，请提示用户先投喂内容。",  // 系统提示
            )
        }

        val context = hits.withIndex().joinToString("\n\n") { (i, h) ->  // 拼装知识库上下文
            "[${i + 1}] ${h.chunk.text}"                  // 编号 + 文本
        }
        val prompt = RAG_TEMPLATE.format(memorySection, context, question)  // 套用模板（记忆 + 知识库 + 问题）
        return engine.streamChat(                         // 交给引擎流式生成
            messages = listOf(ChatMessage(convId = 0, role = "user", content = prompt, ts = 0)),  // 拼装后的 Prompt
            system = null,                                // 无额外系统提示（模板里已有）
        )
    }

    companion object {                                    // 伴生对象：模板常量
        /** 带引用 + 记忆的 RAG 提示词模板。 */
        const val RAG_TEMPLATE = """
你是用户的私人 AI 助手，回答时优先结合【长期记忆】，并严格基于【知识库】作答。

规则：
1. 【长期记忆】里与问题相关的内容可直接采纳；【知识库】内容须标注 [n] 角标引用。
2. 找不到答案时明确说"知识库中没有相关信息"。
3. 用中文、Markdown 结构化输出。

%s
【知识库】
%s

【用户问题】
%s
"""
    }
}
