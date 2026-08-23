package com.memuo.core.ai.engine                          // 声明包名：AI 引擎模块

/**
 * 云端 API 配置（CloudConfig）—— 用户自配的 OpenAI 兼容服务（R1）。
 * @param baseUrl 服务地址（如 https://api.deepseek.com，会自动拼 /chat/completions）
 * @param apiKey 密钥（加密存储，仅运行时解密后传入）
 * @param model 模型名（如 deepseek-chat / qwen-plus）
 */
data class CloudConfig(                                   // 云端配置数据类
    val baseUrl: String,                                  // 服务地址
    val apiKey: String,                                   // 密钥
    val model: String,                                    // 模型名
)

/**
 * 云端配置提供者接口（CloudConfigProvider）—— 由 :feature:settings 实现（依赖倒置，core 不依赖 feature）。
 * 返回 null 表示用户尚未配置云端 API。
 */
interface CloudConfigProvider {                          // 云端配置提供者接口
    /** 读取当前云端配置（可空，未配置返回 null）。 */
    suspend fun current(): CloudConfig?                   // 挂起函数：读取配置
}
