package com.memuo.core.ai.engine                          // 声明包名：AI 引擎模块

import com.memuo.core.db.entity.ChatMessage                // 导入消息实体
import kotlinx.coroutines.CoroutineScope                   // 导入 CoroutineScope：重试协程作用域
import kotlinx.coroutines.Dispatchers                       // 导入 Dispatchers：IO 调度器
import kotlinx.coroutines.SupervisorJob                     // 导入 SupervisorJob：重试任务互不影响
import kotlinx.coroutines.delay                             // 导入 delay：指数退避等待
import kotlinx.coroutines.launch                            // 导入 launch：启动重试协程
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
import kotlin.math.pow                                     // 导入 pow：指数退避计算
import kotlin.random.Random                                // 导入 Random：退避抖动

/**
 * 云端 API 客户端（CloudApiClient）—— 对接 OpenAI 兼容的 /chat/completions（SSE 流式）。
 * 兼容：DeepSeek / DashScope(兼容模式) / GLM / Moonshot / 任意 OpenAI 兼容网关 / Operit 式自建。
 * 隐私红线：仅发送用户输入的对话内容，绝不发送本地文件内容。
 *
 * M-027 增强（需求 4）：
 *  - 错误分类：4xx（认证/参数/限流）不重试；5xx 与网络异常指数退避重试（最多 3 次：1s→2s→4s + 抖动）；
 *  - 流保护：SSE 已开始产出内容后连接中断不重发（防内容重复），直接报"流中断"；
 *  - 错误文案归一化：超时/认证失败/限流/服务端错误/网络不可用。
 */
class CloudApiClient @Inject constructor(                 // 构造函数注入
    private val okHttp: OkHttpClient,                     // 注入 OkHttp 客户端（含超时配置）
) {

    companion object {                                    // 重试常量
        private const val MAX_ATTEMPTS = 3                // 最多尝试次数（含首次）
        private const val BASE_BACKOFF_MS = 1000L         // 退避基数 1s（指数：1s/2s/4s）
        private const val JITTER_MS = 500L                // 抖动上限 500ms
    }

    /** 重试协程作用域（仅用于退避等待，随进程存活）。 */
    private val retryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)  // 重试作用域

    /**
     * 发起流式对话（SSE）。
     * @param config 云端配置（baseUrl/apiKey/model）
     * @param messages 消息列表（含最新用户消息）
     * @param system 系统提示词（可空）
     * @param onDelta 每收到一段增量文本时回调（OkHttp 线程池；实现方用 trySend 保证线程安全）
     * @param onDone 结束回调（正常结束传 "stop"，异常传 "ERR:..."）
     */
    fun streamChat(                                       // 流式对话方法
        config: CloudConfig,                              // 云端配置
        messages: List<ChatMessage>,                      // 消息列表
        system: String?,                                  // 系统提示词
        onDelta: (String) -> Unit,                        // 增量回调
        onDone: (String) -> Unit,                         // 结束回调
    ) {
        attempt(config, messages, system, onDelta, onDone, attempt = 1)  // 第 1 次尝试
    }

    /** 单次请求尝试（带重试判定）。 */
    private fun attempt(                                  // 单次尝试
        config: CloudConfig,                              // 配置
        messages: List<ChatMessage>,                      // 消息
        system: String?,                                  // 系统提示词
        onDelta: (String) -> Unit,                        // 增量回调
        onDone: (String) -> Unit,                         // 结束回调
        attempt: Int,                                     // 当前第几次
    ) {
        val request = Request.Builder()                   // 构造 HTTP 请求
            .url(config.baseUrl.trimEnd('/') + "/chat/completions")  // 拼接接口路径
            .header("Authorization", "Bearer ${config.apiKey}")      // 认证头
            .header("Accept", "text/event-stream")        // 声明接收 SSE 流
            .post(buildBody(config, messages, system))    // 请求体（JSON）
            .build()                                      // 构建请求

        okHttp.newCall(request).enqueue(object : Callback {  // 异步执行
            override fun onResponse(call: Call, response: Response) {  // 收到响应
                if (!response.isSuccessful) {            // HTTP 非 2xx
                    val code = response.code             // 状态码
                    val retryable = code >= 500 || code == 429  // 5xx/限流可重试
                    if (retryable && attempt < MAX_ATTEMPTS) {  // 可重试且未达上限
                        scheduleRetry(config, messages, system, onDelta, onDone, attempt)  // 退避重试
                    } else {                             // 不可重试或已达上限
                        onDone("ERR:${classifyHttpError(code)}")  // 分类错误文案
                    }
                    response.close()                     // 释放连接
                    return
                }
                var streamed = false                     // 是否已产出内容（流保护标志）
                try {                                    // 读流（异常统一处理）
                    response.body?.source()?.use { src ->  // 逐行读取响应流
                        while (!src.exhausted()) {        // 循环直到流结束
                            val line = src.readUtf8Line() ?: break  // 读一行
                            if (!line.startsWith("data:")) continue  // 跳过非 data 行
                            val payload = line.removePrefix("data:").trim()  // 去掉前缀
                            if (payload == "[DONE]") break  // 结束标记
                            parseDelta(payload)?.let { d ->  // 解析增量
                                streamed = true          // 标记已产出
                                onDelta(d)               // 回调增量
                            }
                        }
                    }
                    onDone(if (streamed) "stop" else "ERR:空响应")  // 正常结束（空响应报错）
                } catch (e: IOException) {               // 流读取中断
                    // 已产出内容 → 不重发（防重复）；未产出 → 网络错误重试
                    if (!streamed && attempt < MAX_ATTEMPTS) {  // 可重试
                        scheduleRetry(config, messages, system, onDelta, onDone, attempt)  // 退避重试
                    } else {                             // 不重试
                        onDone("ERR:${if (streamed) "流中断" else "网络不可用"}")  // 归一化文案
                    }
                } finally { response.close() }           // 释放连接
            }

            override fun onFailure(call: Call, e: IOException) {  // 连接失败（超时/不可达）
                if (attempt < MAX_ATTEMPTS) {            // 未达上限
                    scheduleRetry(config, messages, system, onDelta, onDone, attempt)  // 退避重试
                } else {                                 // 已达上限
                    onDone("ERR:${classifyIoError(e)}")  // 归一化文案
                }
            }
        })
    }

    /** 指数退避后重试（1s→2s→4s + 随机抖动 0~500ms）。 */
    private fun scheduleRetry(                            // 退避重试
        config: CloudConfig,                              // 配置
        messages: List<ChatMessage>,                      // 消息
        system: String?,                                  // 系统提示词
        onDelta: (String) -> Unit,                        // 增量回调
        onDone: (String) -> Unit,                         // 结束回调
        attempt: Int,                                     // 已尝试次数
    ) {
        val backoff = BASE_BACKOFF_MS * 2.0.pow(attempt - 1).toLong() + Random.nextLong(JITTER_MS)  // 指数 + 抖动
        retryScope.launch {                              // 协程等待
            delay(backoff)                               // 退避等待
            attempt(config, messages, system, onDelta, onDone, attempt + 1)  // 重试
        }
    }

    /** HTTP 错误分类 → 归一化文案。 */
    private fun classifyHttpError(code: Int): String = when (code) {  // 分类
        401, 403 -> "认证失败，请检查 API Key"             // 认证
        404 -> "接口不存在，请检查 API 地址"               // 404
        429 -> "请求过于频繁，请稍后再试"                  // 限流
        in 500..599 -> "服务端错误，请稍后再试"            // 5xx
        else -> "请求失败（HTTP $code）"                  // 兜底
    }

    /** IO 异常分类 → 归一化文案。 */
    private fun classifyIoError(e: IOException): String =  // 分类
        if (e is java.net.SocketTimeoutException) "请求超时，请检查网络"  // 超时
        else if (e is java.net.UnknownHostException) "网络不可用，请检查网络连接"  // DNS 失败
        else if (e is java.net.ConnectException) "无法连接服务器"  // 连接拒绝
        else "网络异常"                                   // 兜底

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
