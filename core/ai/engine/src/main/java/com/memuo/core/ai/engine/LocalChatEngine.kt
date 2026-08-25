package com.memuo.core.ai.engine                          // 声明包名：AI 引擎模块

import android.app.ActivityManager                        // 导入 ActivityManager：查询可用内存
import android.content.Context                            // 导入 Context：系统服务
import com.memuo.core.db.entity.ChatMessage                // 导入消息实体
import com.memuo.core.db.entity.EngineType                 // 导入引擎类型枚举
import com.memuo.core.storage.StorageProvider             // 导入存储提供者（模型目录）
import dagger.hilt.android.qualifiers.ApplicationContext   // 导入 ApplicationContext 限定符
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
 * 加载前做内存预检（2B 模型权重约 1.1G，需约 2.5G 可用内存），不足则提前拦截并给出具体数值。
 */
@Singleton                                               // 单例
class LocalChatEngine @Inject constructor(               // 构造函数注入
    @ApplicationContext private val context: Context,    // 注入应用上下文（内存检测）
    private val storage: StorageProvider,                // 注入存储提供者（决定模型目录）
) : ChatEngine {                                         // 实现 ChatEngine 接口
    override val type: EngineType = EngineType.LOCAL      // 引擎类型 = 本地

    private val mutex = Mutex()                          // 互斥锁：防止并发重复加载
    private var nativePtr: Long = 0L                     // 原生 Llm 指针（0 = 未加载）
    private var lastLoadError: String? = null            // 最近一次加载失败的具体原因（供诊断提示）

    /** 本地 LLM 模型目录（约定：modelsDir()/llm/）。 */
    private fun modelDir(): File = File(storage.modelsDir(), "llm")  // 模型目录

    /** 查询系统当前可用内存（MB）。 */
    private fun availableMemMb(): Long {                 // 可用内存查询
        val am = context.getSystemService(ActivityManager::class.java)  // 取 ActivityManager
        val info = ActivityManager.MemoryInfo()          // 内存信息容器
        am.getMemoryInfo(info)                           // 读取
        return info.availMem / (1024 * 1024)             // 字节 → MB
    }

    /** 懒加载模型，返回原生指针（0 = 未就绪）。 */
    private suspend fun ensureLoaded(): Long = mutex.withLock {  // 加锁加载
        if (nativePtr != 0L) return@withLock nativePtr  // 已加载直接返回
        val dir = modelDir()                            // 模型目录
        val config = File(dir, "config.json")           // 配置文件
        if (!config.exists()) {                          // 无模型
            lastLoadError = null                         // 清空错误（交给 streamChat 用文件检测提示）
            return@withLock 0L                          // 返回 0
        }
        // 内存预检：按权重大小估算所需内存（MNN memory:low 用 mmap 映射权重，实际峰值 ≈ 权重×1.5 + KV cache/开销）
        val weight = File(dir, "llm.mnn.weight")         // 权重文件
        if (weight.exists()) {                           // 有权重才做预检
            val weightMb = weight.length() / (1024 * 1024)  // 权重大小（MB）
            val requiredMb = weightMb * 3 / 2 + 256      // 估算所需（权重×1.5 + 256MB KV/开销）
            val availMb = availableMemMb()               // 当前可用
            if (availMb in 1..(requiredMb - 1)) {        // 可用内存不足
                lastLoadError = "内存不足：该模型权重约 ${weightMb}MB，运行需约 ${requiredMb}MB 可用内存，当前仅 ${availMb}MB。\n建议关闭后台应用后重试，或改用更小模型（如 Qwen3.5-0.8B）。"
                return@withLock 0L                       // 拦截，返回 0
            }
        }
        nativePtr = withContext(Dispatchers.IO) {        // IO 线程加载（阻塞 JNI）
            runCatching { MnnLlmNative.nativeInit(dir.absolutePath) }.getOrDefault(0L)  // 初始化
        }
        if (nativePtr == 0L) {
            lastLoadError = "模型加载失败：文件齐全但 MNN 初始化出错，可能是 config.json 与权重不匹配，或模型文件损坏。请使用 MNN 官方导出的 Qwen 模型。"  // 记录失败原因
        } else {
            lastLoadError = null                         // 加载成功，清空错误
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
            // 优先用加载阶段记录的具体原因（内存不足 / 初始化失败）
            val loadErr = lastLoadError                 // 取记录的原因
            // 必需文件清单（Qwen3.5 多模态 is_visual:true 需视觉文件 visual.mnn/visual.mnn.weight）
            val required = listOf("config.json", "llm.mnn", "llm.mnn.weight", "tokenizer.txt", "llm.mnn.json", "visual.mnn", "visual.mnn.weight")
            val missing = required.filterNot { File(dir, it).exists() }  // 缺失的文件
            val weight = File(dir, "llm.mnn.weight")    // 权重文件
            val msg = when {                            // 按状态给具体提示
                loadErr != null -> "⚠️ $loadErr\n"      // 内存不足 / 加载失败（具体原因）
                missing.isNotEmpty() -> "⚠️ 模型文件缺失：${missing.joinToString("、")}。\n请重新导入完整模型目录（含上述全部文件）。\n"
                weight.length() < 100L * 1024 * 1024 -> "⚠️ 权重文件不完整（仅 ${weight.length() / 1024 / 1024}MB）。请重新导入完整模型。\n"
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
