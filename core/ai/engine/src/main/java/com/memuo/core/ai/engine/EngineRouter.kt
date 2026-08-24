package com.memuo.core.ai.engine                          // 声明包名：AI 引擎模块

import com.memuo.core.db.entity.ChatMessage                // 导入消息实体
import com.memuo.core.db.entity.EngineType                 // 导入引擎类型枚举
import com.memuo.core.storage.StorageProvider             // 导入存储提供者（模型目录检测）
import kotlinx.coroutines.flow.Flow                        // 导入 Flow：数据流
import java.io.File                                        // 导入 File：检查模型目录
import javax.inject.Inject                                 // 导入 Inject：构造函数注入
import javax.inject.Singleton                              // 导入 Singleton：单例作用域

/**
 * 引擎路由器（EngineRouter）—— 实现 ChatEngine，按用户设置动态路由到本地/云端引擎（M6）。
 *
 * 取代 M3 的"写死云端"绑定：ChatViewModel 仍只依赖 ChatEngine 接口，
 * 切换引擎只需改设置，无需改动调用方（架构不变式：双向引擎可互换）。
 * 切到本地引擎前会检查模型是否就绪（未就绪则拒绝，供 UI 拦截）。
 */
@Singleton                                               // 单例
class EngineRouter @Inject constructor(                  // 构造函数注入
    private val cloud: CloudChatEngine,                  // 注入云端引擎
    private val local: LocalChatEngine,                  // 注入本地引擎
    private val settings: EngineSettings,                // 注入引擎设置（选择结果）
    private val storage: StorageProvider,                // 注入存储提供者（检测模型目录）
) : ChatEngine {                                         // 实现 ChatEngine 接口

    /** 当前生效的引擎类型（随设置动态变化）。 */
    override val type: EngineType get() = settings.engineType.value  // 动态返回当前类型

    /** 按设置路由到对应引擎的流式对话。 */
    override fun streamChat(                              // 流式对话
        messages: List<ChatMessage>,                     // 消息历史
        system: String?,                                 // 系统提示词
    ): Flow<ChatEvent> = if (settings.engineType.value == EngineType.LOCAL) {  // 本地引擎
        local.streamChat(messages, system)               // 路由到本地
    } else {                                             // 云端引擎
        cloud.streamChat(messages, system)               // 路由到云端
    }

    /** 本地模型是否就绪（modelsDir()/llm/config.json 存在）。 */
    fun hasLocalModel(): Boolean =                        // 模型就绪检测
        File(storage.modelsDir(), "llm/config.json").exists()  // config.json 存在即视为就绪

    /** 切换引擎；切到本地但无模型时返回 false（UI 据此拦截并提示）。 */
    suspend fun switchTo(type: EngineType): Boolean {     // 切换方法
        if (type == EngineType.LOCAL && !hasLocalModel()) return false  // 本地无模型则拒绝
        settings.setEngineType(type)                     // 写入设置
        return true                                      // 切换成功
    }
}
