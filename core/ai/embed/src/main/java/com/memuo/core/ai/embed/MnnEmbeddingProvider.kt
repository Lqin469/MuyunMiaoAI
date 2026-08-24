package com.memuo.core.ai.embed                          // 声明包名：嵌入（Embedding）模块

import javax.inject.Inject                                 // 导入 Inject：构造函数注入
import javax.inject.Singleton                              // 导入 Singleton：单例作用域

/**
 * MNN-bge 本地嵌入（MnnEmbeddingProvider）—— M6 目标实现（bge-small-zh，512 维）。
 *
 * 当前状态：**桩**（stub）。真实集成步骤：
 *  1) 本地用 Android Studio 拉 MNN 源码，构建含 BERT 能力的 AAR；
 *  2) 把 bge-small-zh-v1.5 转换后的 .mnn 模型放到 StorageProvider.modelsDir()/embed/；
 *  3) 在本类 embed() 中加载模型、跑 bert tokenizer、调 JNI 取句向量并归一化。
 *
 * 桩行为：embed() 抛 IllegalStateException（快速失败），
 * 避免静默产出错误向量污染检索质量。
 * 本类未注册进 EmbedModule（默认绑定仍是 SimpleHashEmbeddingProvider），
 * MNN 落地后只需改 EmbedModule 的 @Binds 指向本类即可无缝切换。
 */
@Singleton                                               // 单例作用域
class MnnEmbeddingProvider @Inject constructor() : EmbeddingProvider {  // 构造函数注入，实现嵌入接口

    /** bge-small-zh 向量维度 = 512（替换 SimpleHash 后需重建分块向量，维度约定见 EmbedModule）。 */
    override val dim: Int = 512                          // 维度 512

    /** 批量编码：桩实现直接抛异常（MNN AAR 未集成）。 */
    override suspend fun embed(texts: List<String>): List<FloatArray> {  // 批量编码
        throw IllegalStateException(                      // 快速失败，防止静默污染检索质量
            "MNN-bge 嵌入尚未集成（M6 桩）：请在本地 AS 构建 MNN AAR，并把 bge 模型放到 modelsDir/embed/ 后实现 embed()。"
        )
    }
}
