package com.memuo.core.ai.embed                          // 声明包名：嵌入（Embedding）模块

/**
 * MNN Embedding 的 JNI 桥（MnnEmbeddingNative）—— 封装 libmnnllm_jni.so 的 embedding native 方法。
 * 加载顺序与 MnnLlmNative 一致（System.loadLibrary 幂等，重复加载安全）。
 */
object MnnEmbeddingNative {                              // 单例对象：Embedding JNI 桥

    init {                                                // 类加载时执行
        System.loadLibrary("MNN")                         // 核心推理库
        System.loadLibrary("MNN_Express")                 // 表达式引擎
        System.loadLibrary("MNN_CL")                      // OpenCL 后端
        System.loadLibrary("llm")                         // LLM 引擎（含 Embedding 类）
        System.loadLibrary("mnnllm_jni")                  // 本 JNI 桥
    }

    /** 创建 embedding 实例，返回原生指针（0 = 失败）。 */
    external fun createEmbedding(configPath: String): Long  // 创建 embedding

    /** 文本 → 向量（返回 float 数组）。 */
    external fun encode(handle: Long, text: String): FloatArray  // 编码单条文本

    /** 向量维度。 */
    external fun dim(handle: Long): Int                   // 返回维度

    /** 释放 embedding 实例。 */
    external fun release(handle: Long)                    // 释放资源
}
