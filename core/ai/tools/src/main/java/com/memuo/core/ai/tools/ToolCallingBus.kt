package com.memuo.core.ai.tools                            // 声明包名：AI 工具调用总线模块

import com.memuo.core.search.service.FileQuery              // 导入文件查询条件
import com.memuo.core.search.service.SearchService          // 导入检索服务（AI 查文件用）
import org.json.JSONArray                                  // 导入 JSONArray：构造结果 JSON
import org.json.JSONObject                                 // 导入 JSONObject：解析参数/构造结果
import javax.inject.Inject                                 // 导入 Inject
import javax.inject.Singleton                              // 导入 Singleton

/**
 * 工具定义（ToolDef）—— 一个可被 AI 调用的工具。
 * @param name 工具名（LLM 声明用）
 * @param description 工具说明（写进系统提示，告诉 LLM 何时调用）
 * @param parameters 参数的 JSON Schema（function calling 的 tools 声明用）
 * @param executor 执行器：入参 JSON 字符串 → 出参 JSON 字符串
 */
data class ToolDef(                                       // 工具定义数据类
    val name: String,                                     // 工具名
    val description: String,                              // 工具说明
    val parameters: JSONObject,                           // 参数 JSON Schema（function calling）
    val executor: suspend (String) -> String,             // 执行器（挂起函数）
)

/**
 * AI 工具调用总线（ToolCallingBus）—— 统一注册与分发 AI 工具（M7）。
 * 内置工具：
 *  - search_file：按文件名关键词检索设备文件，返回真实路径；
 *  - tell_location：返回指定文件的位置说明。
 * 架构不变式：所有 AI 工具必须经本总线分发，禁止引擎直接调用具体工具。
 */
@Singleton                                               // 单例
class ToolCallingBus @Inject constructor(                // 构造函数注入
    private val searchService: SearchService,            // 注入检索服务
) {
    private val registry = LinkedHashMap<String, ToolDef>()  // 工具注册表（保序）

    init {                                                // 初始化：注册内置工具
        registerSearchFile()                              // 注册文件搜索工具
        registerTellLocation()                            // 注册位置说明工具
    }

    /** 注册一个工具（同名覆盖）。 */
    fun register(tool: ToolDef) { registry[tool.name] = tool }  // 写入注册表

    /** 全部已注册工具（供 AI 系统提示声明）。 */
    fun tools(): List<ToolDef> = registry.values.toList()  // 返回工具列表

    /** 生成给 LLM 看的工具声明（约定调用格式：[[tool_name:argsJson]]）。 */
    fun describeForLlm(): String = buildString {          // 构建声明文本
        append("你可用的设备工具（需要时在回复中输出 [[工具名:JSON参数]] 调用，我会执行并把结果告诉你）：\n")
        registry.values.forEach { tool ->                 // 遍历工具
            append("- ${tool.name}：${tool.description}\n")  // 每个工具一行
        }
    }

    /** 生成 OpenAI 兼容的 tools 声明（function calling 协议）。 */
    fun describeAsTools(): JSONArray = JSONArray().apply {  // 构建 tools JSON 数组
        registry.values.forEach { tool ->                 // 遍历工具
            put(JSONObject().apply {                      // 每个工具一项
                put("type", "function")                   // 类型：function
                put("function", JSONObject().apply {      // 函数定义
                    put("name", tool.name)                // 名称
                    put("description", tool.description)  // 说明
                    put("parameters", tool.parameters)    // 参数 JSON Schema
                })
            })
        }
    }

    /** 分发工具调用：按名称查注册表并执行；未知工具返回错误 JSON。 */
    suspend fun dispatch(name: String, argsJson: String): String =  // 分发执行
        registry[name]?.executor?.invoke(argsJson)        // 执行器
            ?: """{"error":"unknown tool: $name"}"""       // 未知工具

    /** 从回复文本中提取所有 [[tool:args]] 调用标记（chat 集成用）。 */
    fun extractCalls(text: String): List<Pair<String, String>> {  // 提取工具调用
        val regex = Regex("\\[\\[([a-z_]+):(.*?)]]")      // 匹配 [[name:args]]（非贪婪）
        return regex.findAll(text).map {                  // 遍历所有匹配
            it.groupValues[1] to it.groupValues[2].trim()  // (工具名, 参数)
        }.toList()
    }

    // ---------- 内置工具实现 ----------

    /** 注册 search_file：按关键词查文件真实路径。 */
    private fun registerSearchFile() {                    // 注册文件搜索
        register(                                         // 写入注册表
            ToolDef(
                name = "search_file",                     // 工具名
                description = "搜索设备上的文件（按文件名关键词模糊匹配），返回文件真实路径。",  // 说明
                parameters = JSONObject().apply {         // 参数 Schema
                    put("type", "object")                 // 对象类型
                    put("properties", JSONObject().apply {  // 属性
                        put("keyword", JSONObject().put("type", "string").put("description", "文件名关键词"))  // 关键词
                        put("extension", JSONObject().put("type", "string").put("description", "可选扩展名"))  // 扩展名
                    })
                    put("required", JSONArray().put("keyword"))  // 必填：关键词
                },
                executor = { argsJson ->                  // 执行器
                    val obj = runCatching { JSONObject(argsJson) }.getOrNull()  // 解析参数
                    val keyword = obj?.optString("keyword")?.trim().orEmpty()  // 关键词
                    if (keyword.isEmpty()) {              // 无关键词
                        return@ToolDef """{"error":"keyword 不能为空"}"""  // 返回错误
                    }
                    val ext = obj?.optString("extension")?.takeIf { it.isNotBlank() }  // 可选扩展名
                    val hits = searchService.search(      // 执行检索
                        FileQuery(keyword = keyword, extension = ext, limit = 10)  // 最多 10 条
                    )
                    val arr = JSONArray()                 // 结果数组
                    hits.forEach { h ->                   // 组装结果
                        arr.put(                          // 每条命中
                            JSONObject()
                                .put("path", h.path)      // 真实路径
                                .put("name", h.name)      // 文件名
                                .put("size", h.sizeBytes) // 大小
                        )
                    }
                    if (hits.isEmpty()) {                 // 无命中
                        """{"hits":[],"message":"未找到与 '$keyword' 相关的文件（可能尚未建立索引，请先在文件检索页搜索一次）"}"""  // 提示
                    } else {                              // 有命中
                        JSONObject().put("hits", arr).toString()  // 返回 JSON
                    }
                }
            )
        )
    }

    /** 注册 tell_location：返回指定文件位置说明。 */
    private fun registerTellLocation() {                  // 注册位置说明
        register(                                         // 写入注册表
            ToolDef(
                name = "tell_location",                   // 工具名
                description = "返回指定文件的位置说明（用户问'文件在哪'时用）。",  // 说明
                parameters = JSONObject().apply {         // 参数 Schema
                    put("type", "object")                 // 对象类型
                    put("properties", JSONObject().apply {  // 属性
                        put("path", JSONObject().put("type", "string").put("description", "文件路径"))  // 路径
                    })
                    put("required", JSONArray().put("path"))  // 必填：路径
                },
                executor = { argsJson ->                  // 执行器
                    val path = runCatching { JSONObject(argsJson).optString("path") }.getOrDefault("")  // 解析路径
                    if (path.isBlank()) {                 // 无路径
                        return@ToolDef """{"error":"path 不能为空"}"""  // 返回错误
                    }
                    """{"path":"$path","message":"文件位置：$path"}"""  // 返回位置说明
                }
            )
        )
    }
}
