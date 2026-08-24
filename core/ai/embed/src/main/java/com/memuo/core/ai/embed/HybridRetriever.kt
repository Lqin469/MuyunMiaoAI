package com.memuo.core.ai.embed                          // 声明包名：嵌入（Embedding）模块

import com.memuo.core.db.dao.KbDao                        // 导入知识库 DAO
import com.memuo.core.db.entity.KbChunk                   // 导入分块实体
import java.nio.ByteBuffer                                // 导入 ByteBuffer：字节/浮点互转
import java.nio.ByteOrder                                 // 导入 ByteOrder：字节序
import javax.inject.Inject                                // 导入 Inject：构造函数注入

/** 检索结果：分块 + 语义相似度 + 融合分。 */
data class RetrievedChunk(                                // 检索命中结果
    val chunk: KbChunk,                                   // 分块内容
    val score: Float,                                     // 最终得分（RRF 融合分）
)

/**
 * 混合检索器（HybridRetriever）—— 语义检索（余弦相似度）+ 关键词检索（LIKE）RRF 融合。
 * 中文场景下关键词检索能兜住专名/编号，语义检索兜住同义表述。
 */
class HybridRetriever @Inject constructor(               // 构造函数注入
    private val kbDao: KbDao,                             // 注入知识库 DAO
    private val embedder: EmbeddingProvider,              // 注入嵌入提供者
) {
    /**
     * 检索 topK 个最相关的分块。
     * @param folderId 知识库 ID
     * @param query 用户问题
     * @param topK 返回条数
     */
    suspend fun retrieve(folderId: String, query: String, topK: Int = 8): List<RetrievedChunk> {  // 检索方法
        val qVec = embedder.embed(listOf(query)).first()  // 把问题编码为向量
        val candidates = kbDao.chunksByFolder(folderId)   // 取该知识库全部分块（候选集）

        val semantic = candidates                          // 语义检索
            .map { it to cosine(it.embedding.toFloats(), qVec) }  // 计算每条分块与问题的余弦相似度
            .sortedByDescending { it.second }              // 相似度降序
            .take(topK * 2)                                // 取前 2*topK 作为语义候选

        val keyword = kbDao.searchByKeyword(folderId, query, topK * 2)  // 关键词检索（LIKE 模糊匹配）

        return rrfMerge(semantic, keyword, topK)           // RRF 融合后取 topK
    }

    /** 倒数排名融合（RRF）：合并语义与关键词两组候选，按排名倒数求和。 */
    private fun rrfMerge(                                 // RRF 融合方法
        semantic: List<Pair<KbChunk, Float>>,             // 语义候选（含相似度）
        keyword: List<KbChunk>,                           // 关键词候选
        topK: Int,                                        // 最终条数
        k: Int = 60,                                      // RRF 平滑常数
    ): List<RetrievedChunk> {                            // 返回融合结果
        val score = HashMap<Long, Double>()                // chunkId → 融合分
        semantic.forEachIndexed { i, p ->                 // 遍历语义候选
            score[p.first.id] = (score[p.first.id] ?: 0.0) + 1.0 / (k + i + 1)  // 累加语义排名分
        }
        keyword.forEachIndexed { i, c ->                  // 遍历关键词候选
            score[c.id] = (score[c.id] ?: 0.0) + 1.0 / (k + i + 1)  // 累加关键词排名分
        }
        return score.entries                              // 按融合分排序
            .sortedByDescending { it.value }              // 降序
            .take(topK)                                   // 取 topK
            .mapNotNull { e ->                            // 转成结果对象
                val c = semantic.firstOrNull { it.first.id == e.key }?.first
                    ?: keyword.firstOrNull { it.id == e.key } ?: return@mapNotNull null
                RetrievedChunk(c, e.value.toFloat())      // 构造结果
            }
    }

    /** 余弦相似度（点积 / 模长积）；除以模长保证对任意向量都正确（不依赖归一化）。 */
    private fun cosine(a: FloatArray, b: FloatArray): Float {  // 余弦相似度
        var dot = 0f                                      // 点积
        var na = 0f                                       // a 模长平方
        var nb = 0f                                       // b 模长平方
        for (i in a.indices) {                            // 逐维累加
            dot += a[i] * b[i]                            // 点积累加
            na += a[i] * a[i]                             // a 模长平方累加
            nb += b[i] * b[i]                             // b 模长平方累加
        }
        val denom = kotlin.math.sqrt(na) * kotlin.math.sqrt(nb)  // 分母 = |a| * |b|
        return if (denom > 0f) dot / denom else 0f        // 除模长（防止除零）
    }

    /** ByteArray → FloatArray（BLOB 反序列化）。 */
    private fun ByteArray?.toFloats(): FloatArray {       // 反序列化向量
        if (this == null) return FloatArray(0)            // 空则返回空数组
        val buf = ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN)  // 包装为字节缓冲（小端）
        return FloatArray(size / 4) { buf.float }         // 每 4 字节读一个 float
    }
}

/** FloatArray → ByteArray（BLOB 序列化，入库用）。 */
fun FloatArray.toBytes(): ByteArray {                     // 序列化向量
    val buf = ByteBuffer.allocate(size * 4).order(ByteOrder.LITTLE_ENDIAN)  // 分配缓冲（小端）
    forEach { buf.putFloat(it) }                          // 逐个写入 float
    return buf.array()                                    // 返回字节数组
}
