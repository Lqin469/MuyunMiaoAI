package com.memuo.core.ai.engine                          // 声明包名：AI 引擎模块

import com.memuo.core.db.entity.ChatMessage                // 导入消息实体
import okhttp3.Call                                       // 导入 Call：一次 HTTP 请求
import okhttp3.Callback                                   // 导入 Callback：异步回调
import okhttp3.MediaType.Companion.toMediaType            // 导入 toMediaType：构造媒体类型
import okhttp3.OkHttpClient                               // 导入 OkHttpClient：HTTP 客户端
import okhttp3.Request                                    // 导入 Request：请求体
import okhttp3.RequestBody.Companion.toRequestBody        // 导入 toRequestBody：字符串转请求体
import okhttp3.Response                                   // 导入 Response：响应体
import org.json.JSONArray                                 // 导入 JSONArray：JSON 数组
import org.json.JSONObject                                // 导入 JSONObject：JSON 对象
import java.io.IOException                                // 导入 IOException：网络异常
import javax.inject.Inject                                // 导入 Inject：构造函数注入

/**
 * 云端 API 客户端（CloudApiClient）—— 对接 OpenAI 兼容的 /chat/completions（SSE 流式）。
 * 兼容：DeepSeek / DashScope(兼容模式) / GLM / Moonshot / 任意 OpenAI 兼容网关 / Operit 式自建。
 * 隐私红线：仅发送用户输入的对话内容，绝不发送本地文件内容。
 */
class CloudApiClient @Inject constructor(                 // 构造函数注入
    private val okHttp: OkHttpClient,                     // 注入 OkHttp 客户端
) {

    /**
     * 发起流式对话（SSE）。
     * @param config 云端配置（baseUrl/apiKey/model）
     * @param messages 消息列表（含最新用户消息）
     * @param system 系统提示词（可空）
     * @param onDelta 每收到一段增量文本时回调（主线程）
     * @param onDone 结束回调（正常结束传 "stop"，异常传 "ERR:..."）
     */
    fun streamChat(                                       // 流式对话方法
        config: CloudConfig,                              // 云端配置
        messages: List<ChatMessage>,                      // 消息列表
        system: String?,                                  // 系统提示词
        onDelta: (String) -> Unit,                        // 增量回调
        onDone: (String) -> Unit,                         // 结束回调
    ) {
        val request = Request.Builder()                   // 构造 HTTP 请求
            .url(config.baseUrl.trimEnd('/') + "/chat/completions")  // 拼接接口路径
            .header("Authorization", "Bearer ${config.apiKey}")      // 认证头：Bearer Token
            .header("Accept", "text/event-stream")        // 声明接收 SSE 流
            .post(buildBody(config, messages, system))    // 请求体（JSON）
            .build()                                      // 构建请求

        okHttp.newCall(request).enqueue(object : Callback {  // 异步执行（不阻塞线程）
            override fun onResponse(call: Call, response: Response) {  // 收到响应
                response.body?.source()?.use { src ->     // 逐行读取响应流
                    while (!src.exhausted()) {            // 循环直到流结束
                        val line = src.readUtf8Line() ?: break  // 读一行，空则结束
                        if (!line.startsWith("data:")) continue  // 跳过非 data 行（如注释/心跳）
                        val payload = line.removePrefix("data:").trim()  // 去掉 "data:" 前缀
                        if (payload == "[DONE]") break    // 收到结束标记则退出
                        parseDelta(payload)?.let(onDelta) // 解析增量文本并回调
                    }
                }
                onDone(response.message)                  // 正常结束（reason = 如 "OK"）
            }

            override fun onFailure(call: Call, e: IOException) {  // 网络失败
                onDone("ERR:${e.message}")                // 结束并携带错误信息
            }
        })
    }

    /** 构造 OpenAI 兼容请求体（JSON）。 */
    private fun buildBody(                                // 构建请求体方法
        config: CloudConfig,                              // 配置
        messages: List<ChatMessage>,                      // 消息
        system: String?,                                  // 系统提示词
    ): okhttp3.RequestBody {                             // 返回请求体
        val arr = JSONArray()                             // 消息数组
        system?.let {                                    // 如果有系统提示词
            arr.put(JSONObject().put("role", "system").put("content", it))  // 追加 system 消息
        }
        messages.forEach { m ->                           // 遍历历史消息
            arr.put(JSONObject().put("role", m.role).put("content", m.content))  // 追加 user/assistant 消息
        }
        val body = JSONObject()                           // 顶层请求体
            .put("model", config.model)                   // 模型名
            .put("stream", true)                          // 开启流式
            .put("messages", arr)                         // 消息数组
        return body.toString().toRequestBody("application/json".toMediaType())  // 转请求体
    }

    /** 解析 SSE 增量文本（choices[0].delta.content）。 */
    private fun parseDelta(payload: String): String? {    // 解析增量方法
        return try {                                      // 捕获解析异常
            val root = JSONObject(payload)                // 解析 JSON
            val choices = root.optJSONArray("choices")    // 取 choices 数组
            val delta = choices?.optJSONObject(0)?.optJSONObject("delta")  // 取第一个 delta
            val text = delta?.optString("content")        // 取增量内容
            text?.takeIf { it.isNotEmpty() }              // 空串返回 null（跳过）
        } catch (e: Exception) {                          // 解析失败
            null                                        // 返回 null（跳过这行）
        }
    }
}
