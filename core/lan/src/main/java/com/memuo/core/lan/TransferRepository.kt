package com.memuo.core.lan                              // 声明包名：局域网传输模块

import kotlinx.coroutines.CoroutineScope                  // 导入 CoroutineScope：协程作用域
import kotlinx.coroutines.Job                             // 导入 Job：协程任务（暂停/恢复）
import kotlinx.coroutines.SupervisorJob                    // 导入 SupervisorJob：子协程互不影响
import kotlinx.coroutines.cancel                           // 导入 cancel：取消作用域
import kotlinx.coroutines.delay                            // 导入 delay：轮询间隔
import kotlinx.coroutines.flow.MutableStateFlow            // 导入 MutableStateFlow：可变状态流
import kotlinx.coroutines.flow.StateFlow                  // 导入 StateFlow：只读状态流
import kotlinx.coroutines.flow.asStateFlow                // 导入 asStateFlow：转只读
import kotlinx.coroutines.launch                           // 导入 launch：启动协程
import java.io.File                                        // 导入 File：待发送文件
import javax.inject.Inject                                // 导入 Inject：构造函数注入
import javax.inject.Singleton                             // 导入 Singleton：单例作用域

/**
 * 传输编排器（TransferRepository）—— 局域网传输的总入口（需求 3）。
 *
 * 职责：组合 [LanScanner]（发现）/ [LanAdvertiser]（本机可见）/ [TransferServer]（接收）/
 * [TransferClient]（发送），对外暴露统一状态流，UI 只与本类交互：
 *  - [devices]：在线设备列表（NSD 实时刷新）；
 *  - [sendSession]：发送会话（进度/速度/状态）；
 *  - [receiveSessions]：接收会话列表；
 *  - 暂停/恢复：取消发送协程（服务端 .part 保留）→ 恢复时重新 QUERY 断点续传。
 */
@Singleton                                               // 单例
class TransferRepository @Inject constructor(            // 构造函数注入
    private val scanner: LanScanner,                     // 注入扫描器
    private val advertiser: LanAdvertiser,               // 注入广播器
    private val server: TransferServer,                  // 注入接收服务
    private val client: TransferClient,                  // 注入发送客户端
) {

    /** 设备列表（透传扫描器）。 */
    val devices: StateFlow<List<LanDevice>> = scanner.devices  // 设备列表
    /** 是否扫描中。 */
    val scanning: StateFlow<Boolean> = scanner.scanning  // 扫描状态
    /** 接收会话列表（透传服务端）。 */
    val receiveSessions: StateFlow<List<ReceiveSession>> = server.sessions  // 接收会话

    private val _sendSession = MutableStateFlow<SendSession?>(null)  // 当前发送会话（null = 空闲）
    val sendSession: StateFlow<SendSession?> = _sendSession.asStateFlow()  // 只读暴露

    private val scope = CoroutineScope(SupervisorJob())  // 编排作用域
    private var sendJob: Job? = null                     // 发送任务（暂停 = cancel）
    private var lastFile: File? = null                   // 最近发送文件（恢复用）
    private var lastDevice: LanDevice? = null            // 最近目标设备（恢复用）
    private var lastSent = 0L                            // 最近已发送字节（断点记忆）

    /** 开启本机接收 + 广播 + 扫描（页面打开时调用）。 */
    fun startAll() {                                     // 开启全部
        server.start()                                   // 启动接收服务
        advertiser.start()                               // 注册本机服务
        scanner.start()                                  // 开始发现设备
    }

    /** 停止一切（页面关闭时调用）。 */
    fun stopAll() {                                      // 停止全部
        scanner.stop()                                   // 停止扫描
        advertiser.stop()                                // 注销服务
        server.stop()                                    // 停止接收
        pause()                                          // 暂停发送
    }

    /** 发送文件到目标设备（自动断点续传：服务端 .part 已有部分直接跳过）。 */
    fun send(device: LanDevice, file: File) {            // 发送文件
        if (sendJob?.isActive == true) return            // 已有发送任务
        lastDevice = device                              // 记录设备
        lastFile = file                                  // 记录文件
        lastSent = 0L                                    // 重置断点
        startSendJob()                                   // 启动发送
    }

    /** 暂停发送（协程取消，服务端 .part 保留可续传）。 */
    fun pause() {                                        // 暂停
        sendJob?.cancel()                                // 取消发送协程
        sendJob = null                                   // 置空
        _sendSession.value = _sendSession.value?.copy(state = SessionState.PAUSED)  // 状态置暂停
    }

    /** 恢复发送（从上次断点继续）。 */
    fun resume() {                                       // 恢复
        if (sendJob?.isActive == true) return            // 已在传输
        startSendJob()                                   // 重新启动（内部从 lastSent 续传）
    }

    /** 启动发送协程（新任务或恢复共用）。 */
    private fun startSendJob() {                         // 启动发送
        val device = lastDevice ?: return                // 无设备
        val file = lastFile ?: return                    // 无文件
        val fileId = LanProtocol.fileIdOf(file)          // fileId
        _sendSession.value = SendSession(fileId, file.name, file.length(), lastSent, 0, SessionState.RUNNING)  // 初始状态
        sendJob = scope.launch {                         // 发送协程
            try {                                        // 容错
                val result = client.send(device, file, lastSent) { sent, total, speed ->  // 执行发送
                    lastSent = sent                      // 记录断点
                    _sendSession.value = _sendSession.value?.copy(sent = sent, speedBps = speed, state = SessionState.RUNNING)  // 更新进度
                }
                _sendSession.value = _sendSession.value?.copy(  // 结果状态
                    sent = result.totalBytes,            // 已发送
                    state = if (result.success) SessionState.SUCCESS else SessionState.FAILED,  // 成功/失败
                )
            } catch (e: kotlinx.coroutines.CancellationException) {  // 主动取消（暂停）
                _sendSession.value = _sendSession.value?.copy(state = SessionState.PAUSED)  // 暂停状态
            }
        }
    }

    /** 清理发送会话（完成/失败后 UI 关闭）。 */
    fun clearSend() {                                    // 清理会话
        sendJob?.cancel()                                // 取消任务
        sendJob = null                                   // 置空
        lastFile = null; lastDevice = null; lastSent = 0L  // 清断点
        _sendSession.value = null                        // 清会话
    }
}
