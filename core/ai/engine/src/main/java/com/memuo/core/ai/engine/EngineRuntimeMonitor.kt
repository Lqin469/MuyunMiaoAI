package com.memuo.core.ai.engine                          // 声明包名：AI 引擎模块

import kotlinx.coroutines.flow.MutableStateFlow            // 导入 MutableStateFlow：可变状态流
import kotlinx.coroutines.flow.StateFlow                  // 导入 StateFlow：只读状态流
import kotlinx.coroutines.flow.asStateFlow                // 导入 asStateFlow：转只读
import javax.inject.Inject                                // 导入 Inject：构造函数注入
import javax.inject.Singleton                             // 导入 Singleton：单例作用域

/** 运行时阶段：空闲/加载中/推理中/出错/已释放。 */
enum class RuntimePhase {                                 // 运行阶段
    IDLE,                                                 // 空闲（未加载）
    LOADING,                                              // 模型加载中
    RUNNING,                                              // 推理生成中
    ERROR,                                                // 出错（加载/推理失败）
}

/** 本地模型运行状态（需求 5：运行状态监控）。 */
data class EngineRuntimeState(                            // 运行状态
    val phase: RuntimePhase = RuntimePhase.IDLE,          // 当前阶段
    val modelName: String = "",                           // 模型名（目录名）
    val loadMs: Long = 0,                                 // 加载耗时（毫秒，0=未加载）
    val firstTokenMs: Long = 0,                           // 首 token 延迟（毫秒）
    val totalTokens: Int = 0,                             // 已生成 token 数（按增量回调计数）
    val error: String? = null,                            // 错误信息（ERROR 阶段）
)

/**
 * 模型运行状态监控器（EngineRuntimeMonitor）—— 本地模型生命周期状态中枢（需求 5）。
 *
 * LocalChatEngine 在 加载/推理/出错/释放 各环节更新本监控器，UI（ModelManageScreen 等）
 * 订阅 [state] 实时展示：加载耗时、首 token 延迟、token 计数、错误信息。
 */
@Singleton                                               // 单例（应用内唯一状态源）
class EngineRuntimeMonitor @Inject constructor() {       // 构造函数注入（无参）

    private val _state = MutableStateFlow(EngineRuntimeState())  // 状态（初始空闲）
    val state: StateFlow<EngineRuntimeState> = _state.asStateFlow()  // 只读暴露

    /** 进入加载阶段（modelName = 模型目录名）。 */
    fun onLoading(modelName: String) {                   // 加载开始
        _state.value = EngineRuntimeState(phase = RuntimePhase.LOADING, modelName = modelName)  // 重置为加载态
    }

    /** 加载完成（记录耗时，回空闲；保留最近统计供 UI 展示）。 */
    fun onLoaded(loadMs: Long) {                         // 加载完成
        _state.value = _state.value.copy(                // 保留模型名
            phase = RuntimePhase.IDLE,                   // 空闲
            loadMs = loadMs,                             // 加载耗时
            error = null,                                // 清错误
        )
    }

    /** 进入推理阶段（重置本轮统计）。 */
    fun onRunning() {                                    // 推理开始
        _state.value = _state.value.copy(                // 保留模型名/加载耗时
            phase = RuntimePhase.RUNNING,                // 推理中
            firstTokenMs = 0,                            // 清零首 token
            totalTokens = 0,                             // 清零 token 计数
            error = null,                                // 清错误
        )
    }

    /** 首个增量到达（记录首 token 延迟；仅首次调用生效）。 */
    fun onFirstToken(firstTokenMs: Long) {               // 首 token
        val s = _state.value                             // 当前状态
        if (s.phase == RuntimePhase.RUNNING && s.firstTokenMs == 0L) {  // 推理中且未记录
            _state.value = s.copy(firstTokenMs = firstTokenMs)  // 记录延迟
        }
    }

    /** 每个增量到达（token 计数 +1）。 */
    fun onToken() {                                      // 增量计数
        val s = _state.value                             // 当前状态
        if (s.phase == RuntimePhase.RUNNING) {           // 推理中
            _state.value = s.copy(totalTokens = s.totalTokens + 1)  // 计数 +1
        }
    }

    /** 推理结束（回空闲）。 */
    fun onDone() {                                       // 推理结束
        _state.value = _state.value.copy(phase = RuntimePhase.IDLE)  // 回空闲
    }

    /** 出错（记录错误信息）。 */
    fun onError(message: String) {                       // 出错
        _state.value = _state.value.copy(phase = RuntimePhase.ERROR, error = message)  // 错误态
    }

    /** 模型释放（统计保留，阶段回空闲）。 */
    fun onReleased() {                                   // 释放
        _state.value = EngineRuntimeState()              // 重置（释放后统计无意义）
    }
}
