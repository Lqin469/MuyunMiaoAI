package com.memuo.core.ai.engine                          // 声明包名：AI 引擎模块

/**
 * MNN LLM 的 JNI 桥（MnnLlmNative）—— 封装 libmnnllm_jni.so 的 native 方法。
 *
 * 加载顺序（依赖顺序，不可乱）：MNN（核心）→ MNN_Express（表达式）→ MNN_CL（OpenCL）→ llm（LLM）→ mnnllm_jni（桥）。
 * 这些 .so 已随应用打包在 app/src/main/jniLibs/arm64-v8a/ 下。
 */
object MnnLlmNative {                                     // 单例对象：JNI 桥

    init {                                                // 类加载时执行
        System.loadLibrary("MNN")                         // 核心推理库（无依赖）
        System.loadLibrary("MNN_Express")                 // 表达式引擎（依赖 MNN）
        System.loadLibrary("MNN_CL")                      // OpenCL 后端（依赖 MNN）
        System.loadLibrary("llm")                         // LLM 引擎（依赖以上）
        System.loadLibrary("mnnllm_jni")                  // 本 JNI 桥
    }

    /** 加载模型，返回原生指针（0 = 失败）。 */
    external fun nativeInit(modelDir: String): Long       // 初始化 native 模型

    /** 流式生成回复（同步阻塞，务必在后台线程调用）。 */
    external fun nativeResponse(ptr: Long, prompt: String, callback: DeltaCallback): Boolean  // 流式生成

    /** 释放模型实例。 */
    external fun nativeRelease(ptr: Long)                 // 释放 native 资源

    /**
     * 增量回调（DeltaCallback）—— native 层逐段调用 onDelta 推送生成文本。
     * 用普通类（非 interface）便于 JNI 的 GetMethodID 稳定查找到 onDelta。
     */
    open class DeltaCallback {                            // 增量回调基类
        /** 收到一段增量文本（调用方覆写）。 */
        open fun onDelta(text: String) {}                 // 默认空实现
    }
}
