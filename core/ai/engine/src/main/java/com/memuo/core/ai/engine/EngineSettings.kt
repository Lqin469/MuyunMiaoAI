package com.memuo.core.ai.engine                          // 声明包名：AI 引擎模块

import com.memuo.core.db.entity.EngineType                 // 导入引擎类型枚举（LOCAL/CLOUD）
import kotlinx.coroutines.flow.StateFlow                   // 导入 StateFlow：只读状态流

/**
 * 引擎设置接口（EngineSettings）—— 持久化用户选择的对话引擎（云端 / 本地）。
 * core 定义接口，feature:settings 实现（依赖倒置：core 不依赖 feature）。
 * M6 起 ChatEngine 绑定从"写死云端"改为经 EngineRouter 按此设置动态路由。
 */
interface EngineSettings {                                // 引擎设置接口（契约）
    /** 当前引擎类型状态流（默认 CLOUD）。 */
    val engineType: StateFlow<EngineType>                 // 只读引擎类型

    /** 设置引擎类型（用户在设置页切换时调用）。 */
    suspend fun setEngineType(type: EngineType)           // 挂起：写入设置
}
