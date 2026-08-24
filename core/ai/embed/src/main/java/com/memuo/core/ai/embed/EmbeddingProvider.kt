package com.memuo.core.ai.embed                          // 声明包名：嵌入（Embedding）模块

import javax.inject.Inject                                 // 导入 Inject：构造函数注入（让 Hilt 可实例化）
import kotlin.math.sqrt                                  // 导入 sqrt：开方（归一化用）

/**
 * 嵌入提供者接口（EmbeddingProvider）—— 架构不变式：文本向量化只通过本接口，双轨可互换。
 * 本地（bge，M6 用 MNN）与云端（embedding API）实现同一接口。
 */
interface EmbeddingProvider {                            // 嵌入提供者接口
    /** 向量维度（存储与检索必须一致）。 */
    val dim: Int                                          // 向量维度

    /** 把一批文本编码为向量（返回与输入同序）。 */
    suspend fun embed(texts: List<String>): List<FloatArray>  // 批量编码
}

/**
 * 简易本地嵌入（SimpleHashEmbeddingProvider）—— 字符 n-gram 哈希向量（M4 占位实现）。
 * 作用：让「分块→嵌入→检索→作答」闭环在 M4 即可运行；M6 用 bge（MNN 本地）替换本实现，
 * EmbeddingProvider 接口不变，检索层无感升级。
 */
class SimpleHashEmbeddingProvider @Inject constructor() : EmbeddingProvider {  // 简易哈希嵌入（占位）；@Inject 让 Hilt 可实例化

    /** 向量维度 = 256。 */
    override val dim: Int = 256                          // 维度 256

    /** 批量编码：逐文本哈希成向量。 */
    override suspend fun embed(texts: List<String>): List<FloatArray> =  // 批量编码
        texts.map { hashEmbed(it) }                       // 逐条哈希

    /** 把单条文本哈希成 256 维向量（字符 1~2 gram 累加 + 归一化）。 */
    private fun hashEmbed(text: String): FloatArray {    // 单条编码
        val vec = FloatArray(dim)                         // 初始化全零向量
        for (i in text.indices) {                         // 遍历每个字符
            addGram(vec, text[i].toString())              // 单字符 gram
            if (i + 1 < text.length) addGram(vec, text.substring(i, i + 2))  // 双字符 gram
        }
        val norm = sqrt(vec.sumOf { (it * it).toDouble() }).toFloat()  // 计算向量模长
        if (norm > 0f) for (i in vec.indices) vec[i] /= norm  // 归一化（模长为 1）
        return vec                                        // 返回向量
    }

    /** 把某个 gram 累加到向量对应维度。 */
    private fun addGram(vec: FloatArray, gram: String) { // 累加 gram
        val idx = (gram.hashCode() and 0x7fffffff) % dim  // 哈希到 [0, dim) 区间
        vec[idx] += 1f                                    // 该维度权重 +1
    }
}
