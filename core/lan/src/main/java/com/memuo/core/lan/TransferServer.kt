package com.memuo.core.lan                              // 声明包名：局域网传输模块

import com.memuo.core.storage.StorageProvider             // 导入存储提供者（接收目录）
import kotlinx.coroutines.CoroutineScope                  // 导入 CoroutineScope：协程作用域
import kotlinx.coroutines.Dispatchers                      // 导入 Dispatchers：IO 调度器
import kotlinx.coroutines.SupervisorJob                    // 导入 SupervisorJob：子协程互不影响
import kotlinx.coroutines.cancel                           // 导入 cancel：取消作用域
import kotlinx.coroutines.flow.MutableStateFlow            // 导入 MutableStateFlow：可变状态流
import kotlinx.coroutines.flow.StateFlow                  // 导入 StateFlow：只读状态流
import kotlinx.coroutines.flow.asStateFlow                // 导入 asStateFlow：转只读
import kotlinx.coroutines.launch                           // 导入 launch：启动协程
import org.json.JSONObject                                 // 导入 JSONObject：解析 SEND 头
import java.io.BufferedInputStream                         // 导入 BufferedInputStream：缓冲读
import java.io.File                                        // 导入 File：接收文件
import java.io.RandomAccessFile                            // 导入 RandomAccessFile：断点续写
import java.net.ServerSocket                              // 导入 ServerSocket：TCP 监听
import java.net.Socket                                    // 导入 Socket：客户端连接
import javax.inject.Inject                                // 导入 Inject：构造函数注入
import javax.inject.Singleton                             // 导入 Singleton：单例作用域

/**
 * 传输接收服务（TransferServer）—— 监听固定端口，接收局域网设备发来的文件（需求 3）。
 *
 * 关键设计：
 *  - 断点续传锚点：接收数据写入 `<文件>.part`，QUERY 时返回 .part 长度，
 *    发送端从该偏移续传，中断后数据不丢失；
 *  - 完整性校验：接收字节数达到声明 size 后，校验 .part 长度并重命名为正式文件名；
 *  - 实时进度：每会话状态写入 [sessions] 状态流，UI 订阅展示。
 */
@Singleton                                               // 单例（应用内唯一监听）
class TransferServer @Inject constructor(                // 构造函数注入
    private val storage: StorageProvider,                // 注入存储提供者（接收目录）
) {

    /** 接收目录：storage 根下 transfer/in。 */
    private val inboxDir: File get() = File(storage.root, "transfer/in").apply { mkdirs() }  // 惰性创建

    private var serverSocket: ServerSocket? = null       // 监听套接字
    private var scope: CoroutineScope? = null            // 服务协程作用域
    private var listening = false                         // 是否监听中

    private val _sessions = MutableStateFlow<List<ReceiveSession>>(emptyList())  // 接收会话列表
    val sessions: StateFlow<List<ReceiveSession>> = _sessions.asStateFlow()  // 只读暴露
    private val _running = MutableStateFlow(false)       // 是否运行中
    val running: StateFlow<Boolean> = _running.asStateFlow()  // 只读暴露

    /** 启动监听（幂等）。 */
    fun start() {                                        // 启动服务
        if (listening) return                            // 已监听
        listening = true                                 // 标记
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)  // 服务作用域（IO）
        scope!!.launch {                                 // 监听协程
            runCatching {                                // 容错（端口占用等）
                val socket = ServerSocket(LanProtocol.PORT)  // 绑定端口
                serverSocket = socket                    // 记录
                _running.value = true                    // 标记运行
                while (listening) {                      // 接受循环
                    val client = socket.accept()         // 阻塞等待连接
                    launch { handle(client) }            // 每连接一个协程
                }
            }.onFailure { _running.value = false }       // 失败标记停止
        }
    }

    /** 停止监听。 */
    fun stop() {                                         // 停止服务
        listening = false                                // 标记停止
        runCatching { serverSocket?.close() }            // 关闭监听（解除 accept 阻塞）
        serverSocket = null                              // 置空
        scope?.cancel()                                  // 取消所有会话协程
        scope = null                                     // 置空
        _running.value = false                           // 标记停止
    }

    /** 处理单个连接：读头行 → QUERY/SEND 分发。 */
    private fun handle(socket: Socket) {                 // 连接处理
        socket.use { s ->                                // 用后即关
            val input = BufferedInputStream(s.getInputStream())  // 输入流
            val header = readLine(input) ?: return       // 读头行（空 = 直接断开）
            val parts = header.split(" ")                // 按空格拆分
            when (parts.getOrNull(0)) {                  // 按指令分发
                "QUERY" -> handleQuery(s, parts.getOrNull(1))  // 断点查询
                "SEND" -> handleSend(s, input, header)   // 文件接收
                else -> Unit                             // 未知指令忽略
            }
        }
    }

    /** 处理 QUERY：返回 .part 已有字节数（断点续传锚点）。 */
    private fun handleQuery(socket: Socket, fileId: String?) {  // 断点查询
        val reply = if (fileId == null) {                // 参数缺失
            "NONE\n"                                     // 无记录
        } else {                                         // 正常查询
            val part = partFile(fileId)                  // 对应 .part 文件
            if (part.exists()) "HAVE ${part.length()}\n" else "NONE\n"  // 已有/无
        }
        socket.getOutputStream().use { it.write(reply.toByteArray()) }  // 写响应
    }

    /** 处理 SEND：解析 JSON 头，从 offset 续写 .part，完成校验后改名。 */
    private fun handleSend(socket: Socket, input: BufferedInputStream, header: String) {  // 文件接收
        val jsonText = header.removePrefix("SEND ").trim()  // 取 JSON 部分
        val meta = runCatching { JSONObject(jsonText) }.getOrNull() ?: run {  // 解析头（失败即拒）
            socket.getOutputStream().use { it.write("ERR 头解析失败\n".toByteArray()) }
            return
        }
        val fileId = meta.optString("id")                // 文件 ID
        val name = meta.optString("name").ifBlank { "unnamed" }  // 文件名（防空）
        val size = meta.optLong("size")                  // 总大小
        val offset = meta.optLong("offset")              // 续传偏移
        if (fileId.isEmpty() || size <= 0) {             // 参数非法
            socket.getOutputStream().use { it.write("ERR 参数非法\n".toByteArray()) }
            return
        }
        upsertSession(fileId, name, size, offset, SessionState.RUNNING)  // 建立/更新会话状态

        val part = partFile(fileId)                      // .part 文件
        val raf = RandomAccessFile(part, "rw")           // 可读写
        raf.seek(offset)                                 // 从偏移续写
        var received = offset                            // 已接收字节数
        val buffer = ByteArray(LanProtocol.BUFFER_SIZE)  // 传输缓冲
        var fail: String? = null                         // 失败原因
        try {                                            // 读数据循环
            while (received < size) {                    // 未收满
                val remain = (size - received).coerceAtMost(buffer.size.toLong()).toInt()  // 本次可读
                val n = input.read(buffer, 0, remain)    // 读一块
                if (n < 0) { fail = "连接中断"; break }   // 对端断开
                raf.write(buffer, 0, n)                  // 写 .part
                received += n                            // 累计
                if (received - offset >= 256L * 1024 || received == size) {  // 进度节流（256KB 或完成）
                    upsertSession(fileId, name, size, received, SessionState.RUNNING)  // 更新进度
                }
            }
        } catch (e: Exception) {                         // IO 异常
            fail = e.message ?: "IO 错误"                 // 记录原因
        } finally { raf.close() }                        // 关文件

        val ok = fail == null && received == size && part.length() == size  // 完整性校验
        if (ok) {                                        // 校验通过
            val finalFile = File(inboxDir, name)         // 正式文件
            if (finalFile.exists()) finalFile.delete()   // 同名覆盖
            part.renameTo(finalFile)                     // .part → 正式名
            upsertSession(fileId, name, size, received, SessionState.SUCCESS)  // 成功状态
            socket.getOutputStream().use { it.write("OK $received\n".toByteArray()) }  // 回执
        } else {                                         // 失败（.part 保留供续传）
            upsertSession(fileId, name, size, received, SessionState.PAUSED)  // 暂停状态（可续传）
            socket.getOutputStream().use { it.write("ERR ${fail ?: "校验失败"}\n".toByteArray()) }  // 回执
        }
    }

    /** .part 断点文件路径（fileId 命名，与正式文件名解耦）。 */
    private fun partFile(fileId: String): File = File(inboxDir, "$fileId.part")  // .part 路径

    /** 更新会话列表（按 fileId 去重）。 */
    private fun upsertSession(fileId: String, name: String, size: Long, received: Long, state: SessionState) {  // 更新会话
        val session = ReceiveSession(fileId, name, size, received, state)  // 组装
        _sessions.value = _sessions.value.filterNot { it.fileId == fileId } + session  // 去重替换
    }

    /** 读一行（限长 4KB，防恶意超长头）。 */
    private fun readLine(input: BufferedInputStream): String? {  // 读头行
        val buf = java.io.ByteArrayOutputStream()        // 行缓冲
        var total = 0                                    // 已读字节
        while (total < LanProtocol.MAX_HEADER_BYTES) {   // 限长循环
            val b = input.read()                         // 读一字节
            if (b < 0) return if (total == 0) null else buf.toString("UTF-8")  // EOF
            if (b == '\n'.code) return buf.toString("UTF-8")  // 行结束
            buf.write(b)                                 // 累积
            total++                                      // 计数
        }
        return null                                      // 超长拒绝
    }
}
