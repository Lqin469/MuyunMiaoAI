package com.memuo.core.lan                              // 声明包名：局域网传输模块

import kotlinx.coroutines.Dispatchers                      // 导入 Dispatchers：IO 调度器
import kotlinx.coroutines.currentCoroutineContext          // 导入 currentCoroutineContext：协程上下文
import kotlinx.coroutines.delay                            // 导入 delay：速度采样间隔
import kotlinx.coroutines.isActive                          // 导入 isActive：协程存活判断（支持取消=暂停）
import kotlinx.coroutines.withContext                      // 导入 withContext：切换调度器
import org.json.JSONObject                                 // 导入 JSONObject：构造 SEND 头
import java.io.BufferedInputStream                         // 导入 BufferedInputStream：缓冲读响应
import java.io.BufferedOutputStream                        // 导入 BufferedOutputStream：缓冲写数据
import java.io.File                                        // 导入 File：待发送文件
import java.net.Socket                                    // 导入 Socket：TCP 客户端
import javax.inject.Inject                                // 导入 Inject：构造函数注入
import javax.inject.Singleton                             // 导入 Singleton：单例作用域

/**
 * 传输发送客户端（TransferClient）—— 向局域网设备发送文件，支持断点续传（需求 3）。
 *
 * 流程：连接目标 → QUERY 已有字节（断点锚点）→ SEND 头 + 从偏移读文件流式发送 → 读回执。
 * 取消 = 暂停：协程取消后服务端 .part 保留，下次从断点继续；速度按 500ms 窗口采样。
 */
@Singleton                                               // 单例（无状态，可并发发送）
class TransferClient @Inject constructor() {             // 构造函数注入（无参）

    /**
     * 发送文件到指定设备。
     * @param device 目标设备（含 IP/端口）
     * @param file 本地文件
     * @param fromOffset 起始偏移（默认 0；续传时传上次已发送字节数）
     * @param onProgress 进度回调（已发送/总大小/实时速度）
     * @return 传输结果
     */
    suspend fun send(                                    // 发送方法
        device: LanDevice,                               // 目标设备
        file: File,                                      // 文件
        fromOffset: Long = 0,                            // 续传偏移
        onProgress: (sent: Long, total: Long, speedBps: Long) -> Unit = { _, _, _ -> },  // 进度回调
    ): TransferResult = withContext(Dispatchers.IO) {    // IO 线程执行
        val fileId = LanProtocol.fileIdOf(file)          // 生成 fileId
        runCatching {                                    // 容错（连接/IO 异常）
            Socket(device.ip, device.port).use { socket ->  // 建立 TCP 连接
                socket.soTimeout = 30_000                // 读超时 30s（防挂死）
                val output = BufferedOutputStream(socket.getOutputStream())  // 输出流
                val input = BufferedInputStream(socket.getInputStream())     // 输入流

                // ① 断点查询：服务端已有多少字节
                output.write("QUERY $fileId\n".toByteArray()); output.flush()  // 发 QUERY
                val offset = readLine(input)?.let { line ->  // 读响应
                    if (line.startsWith("HAVE ")) line.removePrefix("HAVE ").trim().toLongOrNull() ?: 0L else 0L  // 解析已有字节
                } ?: 0L                                  // 无响应按 0
                val start = maxOf(offset, fromOffset)    // 取更大偏移（本机断点与服务端断点取大，防重复）
                if (start >= file.length()) return@use TransferResult(true, fileId, file.length())  // 已传完

                // ② SEND 头：JSON（文件元信息 + 偏移）
                val meta = JSONObject()                  // 头 JSON
                    .put("id", fileId)                   // 文件 ID
                    .put("name", file.name)              // 文件名
                    .put("size", file.length())          // 总大小
                    .put("offset", start)                // 续传偏移
                output.write("SEND $meta\n".toByteArray()); output.flush()  // 发 SEND 头

                // ③ 流式发送文件内容（从 start 偏移读）
                file.inputStream().use { fis ->          // 打开文件
                    if (start > 0) fis.skip(start)       // 跳到续传偏移
                    val buffer = ByteArray(LanProtocol.BUFFER_SIZE)  // 发送缓冲
                    var sent = start                     // 已发送计数
                    var windowSent = 0L                  // 速度采样窗口字节
                    var windowStart = System.currentTimeMillis()  // 窗口起点
                    var speed = 0L                       // 当前速度
                    while (sent < file.length()) {       // 发送循环
                        if (!currentCoroutineContext().isActive) throw kotlinx.coroutines.CancellationException("已暂停")  // 取消=暂停
                        val n = fis.read(buffer)         // 读一块
                        if (n < 0) break                 // 文件读完
                        output.write(buffer, 0, n)       // 写 socket
                        sent += n                        // 累计
                        windowSent += n                  // 窗口累计
                        val now = System.currentTimeMillis()  // 当前时间
                        if (now - windowStart >= 500) {  // 每 500ms 采样一次速度
                            speed = windowSent * 1000 / maxOf(1, now - windowStart)  // 字节/秒
                            windowSent = 0               // 重置窗口
                            windowStart = now            // 重置起点
                            onProgress(sent, file.length(), speed)  // 进度回调
                        }
                    }
                    output.flush()                       // 冲刷剩余数据
                    onProgress(sent, file.length(), speed)  // 最终进度
                }

                // ④ 读回执
                val reply = readLine(input) ?: "ERR 无回执"  // 响应行
                if (reply.startsWith("OK")) {            // 成功
                    TransferResult(true, fileId, file.length())  // 成功结果
                } else {                                 // 失败
                    TransferResult(false, fileId, file.length(), reply.removePrefix("ERR "))  // 失败结果
                }
            }
        }.getOrElse { e ->                               // 异常兜底
            if (e is kotlinx.coroutines.CancellationException) throw e  // 取消继续抛（暂停语义）
            TransferResult(false, fileId, file.length(), e.message ?: "连接失败")  // 失败结果
        }
    }

    /** 读一行响应（限长 4KB）。 */
    private fun readLine(input: BufferedInputStream): String? {  // 读响应行
        val buf = java.io.ByteArrayOutputStream()        // 行缓冲
        var total = 0                                    // 已读
        while (total < LanProtocol.MAX_HEADER_BYTES) {   // 限长
            val b = input.read()                         // 读一字节
            if (b < 0) return if (total == 0) null else buf.toString("UTF-8")  // EOF
            if (b == '\n'.code) return buf.toString("UTF-8")  // 行结束
            buf.write(b); total++                        // 累积
        }
        return null                                      // 超长
    }
}
