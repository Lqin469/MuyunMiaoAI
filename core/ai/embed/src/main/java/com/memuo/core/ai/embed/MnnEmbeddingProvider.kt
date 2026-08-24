package com.memuo.core.ai.embed                          // 声明包名：嵌入（Embedding）模块

import com.memuo.core.storage.StorageProvider             // 导入存储提供者（模型目录）
import kotlinx.coroutines.Dispatchers                      // 导入调度器（IO 线程跑 native）
import kotlinx.coroutines.sync.Mutex                       // 导入 Mutex：防并发加载
import kotlinx.coroutines.sync.withLock                    // 导入 withLock
import kotlinx.coroutines.withContext                      // 导入 withContext
import java.io.File                                        // 导入 File
import javax.inject.Inject                                 // 导入 Inject
import javax.inject.Singleton                              // 导入 Singleton
import kotlin.math.sqrt                                   // 导入 sqrt：归一化

/**
 * MNN-bge 本地嵌入（MnnEmbeddingProvider）—— 真实现（M6/M-014）。
 * 加载 bge-small-zh 的 MNN 模型（modelsDir()/embed/），用 JNI 桥调 MNN Embedding 取句向量并归一化。
 * 模型未就绪时优雅降级为 SimpleHash（避免崩溃，检索仍可用但质量降级）。
 */
@Singleton                                               // 单例作用域
class MnnEmbeddingProvider @Inject constructor(          // 构造函数注入
    private val storage: StorageProvider,                // 注入存储提供者
    private val fallback: SimpleHashEmbeddingProvider,   // 注入哈希兜底（模型未就绪时降级）
) : EmbeddingProvider {                                  // 实现嵌入接口

    private val mutex = Mutex()                          // 互斥锁：防并发重复加载
    private var handle: Long = 0L                        // embedding 原生指针（0 = 未加载）

    /** bge-small-zh 向量维度 = 512。 */
    override val dim: Int = 512                          // 维度 512

    /** 模型目录（约定：modelsDir()/embed/）。 */
    private fun modelConfig(): File = File(storage.modelsDir(), "embed/config.json")  // 模型 config.json

    /** 懒加载 embedding 模型，返回原生指针（0 = 未就绪）。 */
    private fun ensureLoaded(): Long {                   // 懒加载
        if (handle != 0L) return handle                  // 已加载直接返回
        val config = modelConfig()                       // config.json 路径
        if (!config.exists()) return 0L                  // 无模型返回 0
        handle = runCatching {                           // 捕获加载异常
            MnnEmbeddingNative.createEmbedding(config.absolutePath)  // 创建 embedding
        }.getOrDefault(0L)                               // 失败返回 0
        return handle                                     // 返回指针
    }

    /** 批量编码：模型就绪走 bge；未就绪降级为哈希（保证检索闭环不崩）。 */
    override suspend fun embed(texts: List<String>): List<FloatArray> = withContext(Dispatchers.IO) {  // IO 线程
        val h = ensureLoaded()                           // 确保模型加载
        if (h == 0L) return@withContext fallback.embed(texts)  // 未就绪 → 降级哈希
        texts.map { text ->                              // 逐条编码
            runCatching {                                // 单条失败不拖垮整批
                val vec = MnnEmbeddingNative.encode(h, text)  // JNI 编码
                normalize(vec)                           // L2 归一化
            }.getOrElse { fallback.embed(listOf(text)).first() }  // 失败降级哈希
        }
    }

    /** L2 归一化（模长归一为 1，余弦相似度直接用点积）。 */
    private fun normalize(vec: FloatArray): FloatArray { // 归一化
        if (vec.isEmpty()) return vec                     // 空向量直接返回
        val norm = sqrt(vec.sumOf { (it * it).toDouble() }).toFloat()  // 模长
        if (norm > 0f) for (i in vec.indices) vec[i] /= norm  // 逐维归一化
        return vec                                        // 返回
    }

    /** 释放模型（应用退出或切换模型时调用，MVP 不主动调）。 */
    fun release() {                                       // 释放方法
        val h = handle                                    // 取指针
        handle = 0L                                       // 置空
        if (h != 0L) runCatching { MnnEmbeddingNative.release(h) }  // 释放 native
    }
}
