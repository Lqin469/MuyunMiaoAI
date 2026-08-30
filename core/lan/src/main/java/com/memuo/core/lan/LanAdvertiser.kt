package com.memuo.core.lan                              // 声明包名：局域网传输模块

import android.content.Context                            // 导入 Context：应用上下文
import android.net.nsd.NsdManager                         // 导入 NsdManager：NSD 服务管理
import android.net.nsd.NsdServiceInfo                     // 导入 NsdServiceInfo：服务信息
import android.os.Build                                   // 导入 Build：设备型号（本机名）
import dagger.hilt.android.qualifiers.ApplicationContext  // 导入 ApplicationContext：应用级上下文
import javax.inject.Inject                                // 导入 Inject：构造函数注入
import javax.inject.Singleton                             // 导入 Singleton：单例作用域

/**
 * 局域网服务广播器（LanAdvertiser）—— 向局域网注册本机服务，供其他设备发现（需求 3）。
 *
 * 基于 Android 原生 NSD（mDNS 同族）：
 *  - 服务类型 `_muyunmiao._tcp`（仅本应用识别）；
 *  - 服务名「沐云杪-<设备型号>」，TXT record 携带协议版本；
 *  - [start] 注册 / [stop] 注销，配合 TransferServer 端口。
 */
@Singleton                                               // 单例（同一时刻只注册一次）
class LanAdvertiser @Inject constructor(                 // 构造函数注入
    @ApplicationContext private val context: Context,    // 注入应用上下文
) {

    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager  // NSD 管理器
    private var registered = false                        // 是否已注册（防重复注册）

    /** 构造本机服务信息（名称/类型/端口/TXT）。 */
    private fun buildInfo(): NsdServiceInfo =            // 构造服务信息
        NsdServiceInfo().apply {                         // 链式配置
            serviceName = "沐云杪-${Build.MODEL}"         // 服务名：应用名 + 设备型号
            serviceType = LanProtocol.SERVICE_TYPE       // 服务类型（专属）
            port = LanProtocol.PORT                       // 传输端口
            setAttribute("ver", LanProtocol.PROTOCOL_VERSION)  // TXT：协议版本
        }

    private val regListener = object : NsdManager.RegistrationListener {  // 注册监听器
        override fun onServiceRegistered(info: NsdServiceInfo) { registered = true }  // 注册成功
        override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) { registered = false }  // 失败
        override fun onServiceUnregistered(info: NsdServiceInfo) { registered = false }  // 注销
        override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) { /* 忽略：下次启动重试 */ }
    }

    /** 注册本机服务（幂等：已注册则跳过）。 */
    fun start() {                                        // 开始广播
        if (registered) return                           // 已注册跳过
        runCatching {                                    // 容错（部分 ROM NSD 异常）
            nsd.registerService(buildInfo(), NsdManager.PROTOCOL_DNS_SD, regListener)  // 注册
        }
    }

    /** 注销本机服务（页面关闭/应用退出时调用）。 */
    fun stop() {                                         // 停止广播
        if (!registered) return                          // 未注册跳过
        runCatching { nsd.unregisterService(regListener) }  // 注销
    }

    /** 本机服务名（展示用）。 */
    val localName: String get() = "沐云杪-${Build.MODEL}"  // 服务名
}
