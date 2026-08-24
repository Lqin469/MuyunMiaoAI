package com.memuo.core.ai.memory                         // 声明包名：AI 记忆模块

import com.memuo.core.ai.embed.EmbeddingProvider          // 导入嵌入提供者
import com.memuo.core.ai.embed.toBytes                    // 导入向量序列化扩展
import com.memuo.core.db.dao.MemoryDao                    // 导入记忆 DAO
import com.memuo.core.db.entity.ChatMessage                // 导入消息实体
import com.memuo.core.db.entity.KbMemory                  // 导入记忆实体
import javax.inject.Inject                                 // 导入 Inject：构造函数注入
import javax.inject.Singleton                              // 导入 Singleton：单例作用域

/**
 * 记忆仓库（MemoryStore）—— 会话记忆的落库与检索（R6，M5）。
 * 提供一站式 remember()：提炼 → 嵌入 → 落库；以及按关键词检索。
 */
@Singleton                                               // 单例
class MemoryStore @Inject constructor(                   // 构造函数注入
    private val memoryDao: MemoryDao,                    // 注入记忆 DAO
    private val embedder: EmbeddingProvider,             // 注入嵌入提供者
    private val extractor: MemoryExtractor,              // 注入提炼器
) {
    /**
     * 从一段对话提炼并保存长期记忆。
     * @param messages 最近的对话消息
     * @return 实际保存的记忆条数
     */
    suspend fun remember(messages: List<ChatMessage>): Int {  // 提炼并保存
        val entries = extractor.extract(messages)        // 第一步：LLM 提炼
        if (entries.isEmpty()) return 0                   // 无记忆则返回 0
        val vectors = embedder.embed(entries.map { it.text })  // 第二步：批量嵌入
        val now = System.currentTimeMillis()             // 时间戳
        entries.forEachIndexed { i, e ->                 // 第三步：逐条落库
            memoryDao.upsert(                             // 写入记忆
                KbMemory(                                 // 构造记忆实体
                    type = e.type,                        // 类型
                    topic = e.topic,                      // 主题
                    text = e.text,                        // 内容
                    source = "chat",                      // 来源：对话
                    ts = now,                             // 时间
                    embedding = vectors[i].toBytes(),     // 向量（序列化）
                ),
            )
        }
        return entries.size                              // 返回条数
    }

    /** 按关键词检索记忆（关键词兜底；语义检索后续并入 HybridRetriever）。 */
    suspend fun search(keyword: String, limit: Int = 20): List<KbMemory> =  // 关键词检索
        memoryDao.searchByKeyword(keyword, limit)        // 调 DAO
}
