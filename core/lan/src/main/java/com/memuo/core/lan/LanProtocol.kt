package com.memuo.core.lan                              // 声明包名：局域网传输模块

import java.security.MessageDigest                        // 导入 MessageDigest：fileId 摘要
import java.io.File                                        // 导入 File：文件路径

// ============================================================
// 协议常量与数据模型（LanProtocol）
// ============================================================

/**
 * 局域网传输协议定义（参考 LocalSend 轻量协议思想，纯文本头 + 二进制流）。
 *
 * 协议帧（UTF-8 文本头行 + 可选二进制数据）：
 *   QUERY <fileId>\n                              —— 询问服务端已接收字节数（断点续传锚点）
 *      → HAVE <bytes>\n                           —— 服务端已有 bytes 字节（.part 文件长度）
 *      → NONE\n                                   —— 服务端无此文件记录
 *   SEND {"id":..,"name":..,"size":..,"offset":..}\n + <size-offset 字节二进制流>
 *      → OK <received>\n                          —— 接收成功（received = 总字节数）
 *      → ERR <message>\n                          —— 失败（message 为原因）
 */
object LanProtocol {                                     // 协议常量对象
    const val SERVICE_TYPE = "_muyunmiao._tcp"           // NSD 服务类型（本应用专属）
    const val PORT = 21066                                // 固定传输端口（未注册自定义端口）
    const val BUFFER_SIZE = 64 * 1024                     // 传输缓冲区 64KB
    const val PROTOCOL_VERSION = "1"                      // 协议版本（TXT record 声明，防不兼容）
    const val MAX_HEADER_BYTES = 4096                     // 头行最大字节（防恶意超长头）

    /** 根据 文件名+大小+修改时间 生成稳定的 fileId（同名同大小不同内容可区分）。 */
    fun fileIdOf(file: File): String {                   // 生成 fileId
        val raw = "${file.name}|${file.length()}|${file.lastModified()}"  // 三要素拼接
        val md = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())  // SHA-256
        return md.take(8).joinToString("") { "%02x".format(it) }  // 取前 8 字节 → 16 hex 字符
    }
}

/** 局域网设备（NSD 发现结果）。 */
data class LanDevice(                                    // 设备
    val name: String,                                    // 设备名（NSD serviceName）
    val ip: String,                                      // IP 地址
    val port: Int,                                       // 传输端口
    val version: String,                                 // 协议版本（TXT record）
)

/** 接收会话状态（服务端视角，实时进度）。 */
data class ReceiveSession(                               // 接收会话
    val fileId: String,                                  // 文件 ID
    val name: String,                                    // 文件名
    val size: Long,                                      // 总大小（字节）
    val received: Long,                                  // 已接收（字节，含断点前的部分）
    val state: SessionState,                             // 会话状态
)

/** 发送会话状态（客户端视角，实时进度）。 */
data class SendSession(                                  // 发送会话
    val fileId: String,                                  // 文件 ID
    val name: String,                                    // 文件名
    val size: Long,                                      // 总大小
    val sent: Long,                                      // 已发送
    val speedBps: Long,                                  // 实时速度（字节/秒）
    val state: SessionState,                             // 会话状态
)

/** 会话状态机。 */
enum class SessionState { IDLE, RUNNING, PAUSED, SUCCESS, FAILED }  // 空闲/传输中/已暂停/成功/失败

/** 传输结果（客户端返回）。 */
data class TransferResult(                               // 传输结果
    val success: Boolean,                                // 是否成功
    val fileId: String,                                  // 文件 ID
    val totalBytes: Long,                                // 总字节
    val error: String? = null,                           // 失败原因（成功为 null）
)
