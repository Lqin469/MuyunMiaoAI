package com.memuo.core.lan                              // 声明包名：局域网传输模块

import android.content.Context                            // 导入 Context：应用上下文
import android.net.nsd.NsdManager                         // 导入 NsdManager：NSD 服务管理
import android.net.nsd.NsdServiceInfo                     // 导入 NsdServiceInfo：服务信息
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

    /** 随机名字字符集（去易混淆 0/O/1/I）。 */
    private val charset = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"  // 24 字符

    /** 读取本机显示名：无则首次生成 5 位随机并持久化。 */
    private fun currentName(): String {                  // 读取/生成本机名
        val sp = context.getSharedPreferences("lan_device", Context.MODE_PRIVATE)  // 轻量持久化
        return sp.getString("name", null) ?: run {       // 无则生成
            val n = "沐云杪-" + buildString { repeat(5) { append(charset.random()) } }  // 5 位随机
            sp.edit().putString("name", n).apply()        // 持久化
            n                                            // 返回
        }
    }

    /** 自定义本机名（更新持久化 + 若已注册则重新注册服务，立即生效）。 */
    fun setName(name: String) {                          // 设置本机名
        val trimmed = name.trim()                        // 去空白
        if (trimmed.isBlank()) return                    // 空名忽略
        context.getSharedPreferences("lan_device", Context.MODE_PRIVATE).edit().putString("name", trimmed).apply()  // 持久化
        if (registered) {                                // 已注册则重新注册（更新 NSD 服务名）
            runCatching { nsd.unregisterService(regListener) }  // 先注销
            runCatching { nsd.registerService(buildInfo(), NsdManager.PROTOCOL_DNS_SD, regListener) }  // 再注册
        }
    }

    /** 构造本机服务信息（名称/类型/端口/TXT）。 */
    private fun buildInfo(): NsdServiceInfo =            // 构造服务信息
        NsdServiceInfo().apply {                         // 链式配置
            serviceName = currentName()                   // 服务名：5 位随机本机名
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

    /** 本机服务名（展示用，5 位随机或自定义）。 */
    val localName: String get() = currentName()           // 服务名
}
