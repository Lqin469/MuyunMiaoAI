package com.memuo.core.lan                              // 声明包名：局域网传输模块

import android.content.Context                            // 导入 Context：应用上下文
import android.net.nsd.NsdManager                         // 导入 NsdManager：NSD 服务管理
import android.net.nsd.NsdServiceInfo                     // 导入 NsdServiceInfo：服务信息
import android.net.wifi.WifiManager                        // 导入 WifiManager：多播锁（NSD 发现前提）
import dagger.hilt.android.qualifiers.ApplicationContext  // 导入 ApplicationContext：应用级上下文
import kotlinx.coroutines.flow.MutableStateFlow           // 导入 MutableStateFlow：可变状态流
import kotlinx.coroutines.flow.StateFlow                  // 导入 StateFlow：只读状态流
import kotlinx.coroutines.flow.asStateFlow                // 导入 asStateFlow：转只读
import javax.inject.Inject                                // 导入 Inject：构造函数注入
import javax.inject.Singleton                             // 导入 Singleton：单例作用域

/**
 * 局域网设备扫描器（LanScanner）—— 发现同网段运行本应用的设备（需求 3）。
 *
 * 流程：发现服务 → 逐台解析（resolveService 取 IP/端口/TXT）→ 更新设备列表状态流。
 * 注意：部分设备需持有多播锁（WifiManager.MulticastLock）才能收到 mDNS 广播。
 */
@Singleton                                               // 单例
class LanScanner @Inject constructor(                    // 构造函数注入
    @ApplicationContext private val context: Context,    // 注入应用上下文
) {

    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager  // NSD 管理器
    private val wifi = context.getSystemService(Context.WIFI_SERVICE) as WifiManager  // Wi-Fi 管理器

    private val _devices = MutableStateFlow<List<LanDevice>>(emptyList())  // 已发现设备
    val devices: StateFlow<List<LanDevice>> = _devices.asStateFlow()  // 只读暴露
    private val _scanning = MutableStateFlow(false)      // 是否扫描中
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()  // 只读暴露

    private var multicastLock: WifiManager.MulticastLock? = null  // 多播锁（部分设备必需）
    private val pending = mutableMapOf<String, NsdServiceInfo>()  // 待解析服务（serviceName → info，防重复）

    private val discoveryListener = object : NsdManager.DiscoveryListener {  // 发现监听器
        override fun onDiscoveryStarted(serviceType: String) { _scanning.value = true }  // 开始扫描
        override fun onServiceFound(info: NsdServiceInfo) {  // 发现服务 → 发起解析
            if (info.serviceName !in pending) {          // 未解析过
                pending[info.serviceName] = info         // 记录
                runCatching { nsd.resolveService(info, resolveListener) }  // 解析 IP/端口（容错）
            }
        }
        override fun onServiceLost(info: NsdServiceInfo) {  // 服务下线 → 移除设备
            pending.remove(info.serviceName)             // 移除待解析记录
            _devices.value = _devices.value.filterNot { it.name == info.serviceName }  // 从列表移除
        }
        override fun onDiscoveryStopped(serviceType: String) { _scanning.value = false }  // 停止扫描
        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) { _scanning.value = false }  // 启动失败
        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) { /* 忽略 */ }
    }

    private val resolveListener = object : NsdManager.ResolveListener {  // 解析监听器
        override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) { pending.remove(info.serviceName) }  // 解析失败移除
        override fun onServiceResolved(info: NsdServiceInfo) {  // 解析成功 → 加入列表
            val ip = info.host?.hostAddress ?: run { pending.remove(info.serviceName); return }  // 无 IP 放弃
            val device = LanDevice(                      // 组装设备
                name = info.serviceName,                 // 设备名
                ip = ip,                                 // IP
                port = info.port,                        // 端口
                version = info.attributes?.get("ver")?.toString().orEmpty(),  // 协议版本
            )
            _devices.value = (_devices.value.filterNot { it.name == device.name } + device).sortedBy { it.name }  // 去重更新
        }
    }

    /** 开始扫描（持多播锁，60 秒后自动停止释放）。 */
    fun start() {                                        // 开始扫描
        if (_scanning.value) return                      // 已在扫描
        multicastLock = wifi.createMulticastLock("muyunmiao-nsd").apply {  // 创建多播锁
            setReferenceCounted(false)                   // 非引用计数（手动控制）
            acquire()                                    // 立即获取
        }
        runCatching {                                    // 容错
            nsd.discoverServices(LanProtocol.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)  // 发现服务
        }
    }

    /** 停止扫描（释放多播锁）。 */
    fun stop() {                                         // 停止扫描
        runCatching { nsd.stopServiceDiscovery(discoveryListener) }  // 停止发现
        pending.clear()                                  // 清待解析
        _devices.value = emptyList()                     // 清列表
        _scanning.value = false                          // 标记停止
        multicastLock?.takeIf { it.isHeld }?.release()   // 释放多播锁
        multicastLock = null                             // 置空
    }
}
