package com.memuo.core.ai.engine                          // 声明包名：AI 引擎模块

import com.memuo.core.db.entity.ChatMessage                // 导入消息实体
import com.memuo.core.db.entity.EngineType                 // 导入引擎类型枚举
import com.memuo.core.storage.StorageProvider             // 导入存储提供者（模型目录）
import kotlinx.coroutines.Dispatchers                      // 导入调度器（IO 线程）
import kotlinx.coroutines.flow.Flow                        // 导入 Flow：数据流
import kotlinx.coroutines.flow.callbackFlow                // 导入 callbackFlow：回调转流
import kotlinx.coroutines.sync.Mutex                       // 导入 Mutex：互斥锁（防并发加载）
import kotlinx.coroutines.sync.withLock                    // 导入 withLock：锁内执行
import kotlinx.coroutines.withContext                      // 导入 withContext：切换调度器
import java.io.File                                        // 导入 File：模型目录
import javax.inject.Inject                                 // 导入 Inject：构造函数注入
import javax.inject.Singleton                              // 导入 Singleton：单例作用域

/**
 * 本地对话引擎（LocalChatEngine）—— 基于 MNN-LLM 的本地推理实现（M6 真实现）。
 *
 * 流程：懒加载模型（modelsDir()/llm/ 目录，含 config.json）→ 经 MnnLlmNative JNI 桥
 * 调用 MNN 的 Llm::response 流式生成，把增量文本经 callbackFlow 转成 ChatEvent 流。
 *
 * 模型未就绪时：返回明确提示（不崩溃），引导用户导入模型。
 */
@Singleton                                               // 单例
class LocalChatEngine @Inject constructor(               // 构造函数注入
    private val storage: StorageProvider,                // 注入存储提供者（决定模型目录）
) : ChatEngine {                                         // 实现 ChatEngine 接口
    override val type: EngineType = EngineType.LOCAL      // 引擎类型 = 本地

    private val mutex = Mutex()                          // 互斥锁：防止并发重复加载
    private var nativePtr: Long = 0L                     // 原生 Llm 指针（0 = 未加载）

    /** 本地 LLM 模型目录（约定：modelsDir()/llm/）。 */
    private fun modelDir(): File = File(storage.modelsDir(), "llm")  // 模型目录

    /** 懒加载模型，返回原生指针（0 = 未就绪）。 */
    private suspend fun ensureLoaded(): Long = mutex.withLock {  // 加锁加载
        if (nativePtr != 0L) return@withLock nativePtr  // 已加载直接返回
        val dir = modelDir()                            // 模型目录
        val config = File(dir, "config.json")           // 配置文件
        if (!config.exists()) return@withLock 0L        // 无模型返回 0
        nativePtr = withContext(Dispatchers.IO) {       // IO 线程加载（阻塞 JNI）
            runCatching { MnnLlmNative.nativeInit(dir.absolutePath) }.getOrDefault(0L)  // 初始化
        }
        nativePtr                                       // 返回指针
    }

    override fun streamChat(                             // 流式对话
        messages: List<ChatMessage>,                    // 消息历史
        system: String?                                  // 系统提示词（MVP 暂忽略）
    ): Flow<ChatEvent> = callbackFlow {                  // 用 callbackFlow 桥接 JNI 回调
        val ptr = ensureLoaded()                        // 确保模型加载
        if (ptr == 0L) {                                // 模型未就绪/加载失败
            val dir = modelDir()                        // 模型目录
            // 必需文件清单（Qwen3.5 多模态 is_visual:true 需视觉文件 visual.mnn/visual.mnn.weight）
            val required = listOf("config.json", "llm.mnn", "llm.mnn.weight", "tokenizer.txt", "llm.mnn.json", "visual.mnn", "visual.mnn.weight")
            val missing = required.filterNot { File(dir, it).exists() }  // 缺失的文件
            val weight = File(dir, "llm.mnn.weight")    // 权重文件
            val msg = when {                            // 按状态给具体提示
                missing.isNotEmpty() -> "⚠️ 模型文件缺失：${missing.joinToString("、")}。\n请重新导入完整模型目录（含上述全部文件）。\n"
                weight.length() < 100L * 1024 * 1024 -> "⚠️ 权重文件不完整（${weight.length() / 1024 / 1024}MB），应为约 470MB。请重新导入。\n"
                else -> "⚠️ 模型加载失败：文件齐全但 MNN 加载出错，可能是 config.json 与权重不匹配。请用 MNN 官方导出的 Qwen 模型。\n"
            }
            trySend(ChatEvent.Delta(msg))               // 提示
            trySend(ChatEvent.Done("no_model"))         // 结束
            close()                                     // 关闭流
            return@callbackFlow                          // 返回
        }

        val userMsg = messages.lastOrNull { it.role == "user" }?.content.orEmpty()  // 取最后一条用户消息
        if (userMsg.isBlank()) {                        // 空输入
            trySend(ChatEvent.Done("empty"))            // 结束
            close()                                     // 关闭流
            return@callbackFlow                          // 返回
        }

        val callback = object : MnnLlmNative.DeltaCallback() {  // 构造增量回调
            override fun onDelta(text: String) {        // native 逐段回调
                trySend(ChatEvent.Delta(text))          // 转成 Delta 事件
            }
        }

        val ok = withContext(Dispatchers.IO) {          // IO 线程阻塞生成
            runCatching { MnnLlmNative.nativeResponse(ptr, userMsg, callback) }.getOrDefault(false)  // 生成
        }
        trySend(ChatEvent.Done(if (ok) "stop" else "error"))  // 结束事件（原因）
        close()                                         // 关闭流
    }

    /** 释放模型（设置页"卸载模型"时调用）。 */
    fun release() {                                     // 释放方法
        val p = nativePtr                               // 取指针
        nativePtr = 0L                                  // 置空
        if (p != 0L) MnnLlmNative.nativeRelease(p)      // 释放 native 资源
    }
}
