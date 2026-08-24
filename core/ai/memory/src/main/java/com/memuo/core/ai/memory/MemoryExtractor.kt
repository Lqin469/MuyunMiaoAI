package com.memuo.core.ai.memory                         // 声明包名：AI 记忆模块

import com.memuo.core.ai.engine.ChatEngine                 // 导入对话引擎接口
import com.memuo.core.ai.engine.ChatEvent                  // 导入对话事件
import com.memuo.core.db.entity.ChatMessage                // 导入消息实体
import com.memuo.core.db.entity.MemoryType                 // 导入记忆类型枚举
import org.json.JSONArray                                 // 导入 JSONArray：JSON 数组
import org.json.JSONObject                                // 导入 JSONObject：JSON 对象
import javax.inject.Inject                                 // 导入 Inject：构造函数注入
import javax.inject.Singleton                              // 导入 Singleton：单例作用域

/** 待落库的记忆条目（领域模型，不含向量）。 */
data class MemoryEntry(                                   // 记忆条目
    val type: MemoryType,                                 // 类型（FACT/PREFERENCE/TODO）
    val topic: String,                                    // 主题
    val text: String,                                     // 内容
)

/**
 * 记忆提炼器（MemoryExtractor）—— 每 N 轮对话后用 LLM 提炼事实/偏好/待办（R6，M5）。
 * 通过 ChatEngine 调用（本地/云端双轨），输出结构化 JSON 再解析为 MemoryEntry。
 */
@Singleton                                               // 单例
class MemoryExtractor @Inject constructor(               // 构造函数注入
    private val engine: ChatEngine,                      // 注入对话引擎
) {
    /**
     * 从一段对话中提炼值得长期记住的信息。
     * @param messages 最近的对话消息
     * @return 提炼出的记忆条目列表（可能为空）
     */
    suspend fun extract(messages: List<ChatMessage>): List<MemoryEntry> {  // 提炼方法
        val prompt = EXTRACT_PROMPT.format(               // 拼装提炼提示词
            messages.joinToString("\n") { "${it.role}: ${it.content}" },  // 对话内容
        )
        val sb = StringBuilder()                          // 累积流式回复
        engine.streamChat(                                // 调引擎流式生成（只调一次）
            messages = listOf(ChatMessage(convId = 0, role = "user", content = prompt, ts = 0)),  // 单条提炼请求
            system = "你是记忆提炼器，只输出 JSON。",       // 系统提示
        ).collect { e ->                                  // 收集事件
            if (e is ChatEvent.Delta) sb.append(e.text)   // 累积增量文本
        }
        return parse(sb.toString())                       // 解析累积出的 JSON
    }

    /** 解析 LLM 输出的 JSON 为记忆条目。 */
    private fun parse(jsonText: String): List<MemoryEntry> {  // 解析 JSON
        return try {                                      // 捕获解析异常
            val root = JSONObject(extractJsonObject(jsonText))  // 提取并解析 JSON 对象
            val result = mutableListOf<MemoryEntry>()     // 结果列表
            parseArray(root.optJSONArray("facts"), MemoryType.FACT, result)       // 事实
            parseArray(root.optJSONArray("preferences"), MemoryType.PREFERENCE, result)  // 偏好
            parseArray(root.optJSONArray("todos"), MemoryType.TODO, result)       // 待办
            result                                        // 返回
        } catch (e: Exception) {                          // 解析失败
            emptyList()                                   // 返回空（不中断主流程）
        }
    }

    /** 从文本中截取第一个 { ... } 片段（容错 LLM 输出前后多余文字）。 */
    private fun extractJsonObject(text: String): String { // 截取 JSON 对象
        val start = text.indexOf('{')                     // 找第一个 {
        val end = text.lastIndexOf('}')                   // 找最后一个 }
        return if (start >= 0 && end > start) text.substring(start, end + 1) else "{}"  // 截取，否则空对象
    }

    /** 解析一个数组，转成指定类型的记忆条目。 */
    private fun parseArray(arr: JSONArray?, type: MemoryType, out: MutableList<MemoryEntry>) {  // 解析数组
        if (arr == null) return                            // 空数组跳过
        for (i in 0 until arr.length()) {                 // 遍历数组
            val obj = arr.optJSONObject(i) ?: continue    // 取对象，无效跳过
            val text = obj.optString("text").trim()       // 取内容
            if (text.isEmpty()) continue                  // 空内容跳过
            out += MemoryEntry(                           // 构造记忆条目
                type = type,                              // 类型
                topic = obj.optString("topic", type.name),  // 主题（缺省用类型名）
                text = text,                              // 内容
            )
        }
    }

    companion object {                                    // 伴生对象：模板常量
        /** 提炼提示词：要求输出结构化 JSON。 */
        const val EXTRACT_PROMPT = """
请从以下对话中提取值得长期记住的信息，输出 JSON（不要输出其他内容）：
{"facts":[{"text":"事实","topic":"主题"}],"preferences":[{"text":"偏好","topic":"主题"}],"todos":[{"text":"待办","topic":"主题"}]}
无对应内容则数组为空。

对话：
%s
"""
    }
}
