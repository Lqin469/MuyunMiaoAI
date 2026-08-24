package com.memuo.core.ai.engine                          // 声明包名：AI 引擎模块

import com.memuo.core.db.entity.ChatMessage                // 导入消息实体
import com.memuo.core.db.entity.EngineType                 // 导入引擎类型枚举
import kotlinx.coroutines.flow.Flow                        // 导入 Flow：数据流
import kotlinx.coroutines.flow.flow                        // 导入 flow：构造 Flow
import javax.inject.Inject                                 // 导入 Inject：构造函数注入
import javax.inject.Singleton                              // 导入 Singleton：单例作用域

/**
 * 本地对话引擎（LocalChatEngine）—— 基于 MNN-LLM 的本地推理实现（M6）。
 *
 * 当前状态：**桩**（stub）。真实集成需要在本地用 Android Studio 按 MNN 官方 Chat App 的
 * cmake 命令构建 MNN AAR，把产物放到 app/libs/mnn-llm.aar，并在 build.gradle.kts
 * 引入实现 + kapt 后实现真正的 streamChat（参考 core/ai/engine/CloudApiClient 的 SSE
 * 异步回调转 Flow 的模式调用 MNN JNI）。
 *
 * 桩行为：调用即返回"未集成 MNN"的提示事件，避免静默失败。
 */
@Singleton                                               // 单例
class LocalChatEngine @Inject constructor() {            // 构造函数注入（无参数）
    override val type: EngineType = EngineType.LOCAL      // 引擎类型 = 本地

    override fun streamChat(                               // 流式对话
        messages: List<ChatMessage>,                      // 消息历史
        system: String?                                    // 系统提示词
    ): Flow<ChatEvent> = flow {                           // 用 flow{} 构造 Flow
        // 桩实现：发出"未集成"的提示，让 UI 知道为什么没结果
        emit(ChatEvent.Delta("\u26a0\ufe0f MNN 本地引擎尚未集成（M6 桩）。\n"))  // ⚠️ 提示
        emit(ChatEvent.Delta("请在本地 AS 用 MNN 官方 Chat App 的 cmake 命令构建 MNN AAR 后，\n"))  // 续
        emit(ChatEvent.Delta("在 build.gradle.kts 引入 aar 并实现真正的 streamChat（JNI 桥接）。\n"))  // 续
        emit(ChatEvent.Delta("参考 core/ai/engine/CloudApiClient.kt 的异步回调转 Flow 模式。\n"))  // 续
        emit(ChatEvent.Done("LocalChatEngine 桩：MNN 集成 TODO"))  // 结束事件
    }
}
