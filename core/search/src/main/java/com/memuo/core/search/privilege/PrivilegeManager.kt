package com.memuo.core.search.privilege               // 声明包名：搜索模块的"提权"子包

import android.content.Context                           // 导入 Context：应用上下文
import android.content.pm.PackageManager                 // 导入 PackageManager：权限结果常量
import com.topjohnwu.superuser.Shell                     // 导入 Shell：libsu root shell
import dagger.hilt.android.qualifiers.ApplicationContext // 导入 ApplicationContext：应用级上下文限定符
import kotlinx.coroutines.flow.MutableStateFlow          // 导入 MutableStateFlow：可变状态流
import kotlinx.coroutines.flow.StateFlow                 // 导入 StateFlow：只读状态流
import kotlinx.coroutines.flow.asStateFlow               // 导入 asStateFlow：转只读
import rikka.shizuku.Shizuku                             // 导入 Shizuku：提权服务 API
import javax.inject.Inject                               // 导入 Inject：构造函数注入
import javax.inject.Singleton                            // 导入 Singleton：单例作用域

/**
 * 提权管理器（PrivilegeManager）—— 基于 Shizuku + libsu 的三档能力（M7，参考 Operit R11 方案）。
 *
 * | 等级 | 获得方式 | 能力 |
 * |---|---|---|
 * | NONE | 默认 | 仅检索应用私有目录 + SAF 授权目录 |
 * | SHIZUKU_ADB | adb/无线调试启动 Shizuku | 检索 /storage/emulated/0 全用户目录 |
 * | SHIZUKU_ROOT | Shizuku + root(SUI) | 检索全盘 + libsu shell 执行 |
 *
 * 通过 [level] 状态流实时暴露当前等级（服务上下线自动刷新），UI 全程明示。
 */
@Singleton                                               // 单例（应用生命周期内唯一实例）
class PrivilegeManager @Inject constructor(              // 构造函数注入
    @ApplicationContext private val ctx: Context,        // 注入应用上下文
) {
    /** 能力等级枚举：无权限 / Shizuku-adb / Shizuku-root。 */
    enum class Level { NONE, SHIZUKU_ADB, SHIZUKU_ROOT }  // 三档能力

    companion object {                                    // 常量
        private const val REQUEST_CODE = 1001             // Shizuku 授权请求码（自定义）
    }

    private val _level = MutableStateFlow(currentLevel()) // 当前等级（初始检测一次）
    val level: StateFlow<Level> = _level.asStateFlow()    // 只读暴露（UI 实时订阅）

    private val _authorized = MutableStateFlow(hasPermission())  // 授权状态（初始检测一次）
    val authorized: StateFlow<Boolean> = _authorized.asStateFlow()  // 只读暴露

    // Shizuku 服务上线/下线监听器（刷新等级状态）
    private val binderReceived = Shizuku.OnBinderReceivedListener { refresh() }  // 服务上线 → 刷新
    private val binderDead = Shizuku.OnBinderDeadListener { refresh() }         // 服务下线 → 刷新

    init {                                                // 初始化：注册服务状态监听
        Shizuku.addBinderReceivedListener(binderReceived) // 监听服务上线
        Shizuku.addBinderDeadListener(binderDead)         // 监听服务下线
    }

    /** 实时检测当前能力等级：Shizuku 在线 + root 已授权 → ROOT；服务在线 → ADB；否则 NONE。 */
    fun currentLevel(): Level = when {                    // 按条件判断
        Shizuku.pingBinder() && (Shell.isAppGrantedRoot() == true) -> Level.SHIZUKU_ROOT  // Shizuku 在线且本应用有 root 授权（libsu 检测，13.x 无 isSuiAvailable）
        Shizuku.pingBinder() -> Level.SHIZUKU_ADB         // 服务在线但非 root
        else -> Level.NONE                                // 服务未启动
    }

    /** 刷新等级与授权状态流（服务状态变化时调用）。 */
    private fun refresh() {                               // 刷新状态
        _level.value = currentLevel()                     // 更新等级
        _authorized.value = hasPermission()               // 更新授权状态
    }

    /** 本应用是否已获得 Shizuku 授权（v11+ 才有 checkSelfPermission）。 */
    fun hasPermission(): Boolean =                        // 授权状态检测
        Shizuku.pingBinder() &&                           // 服务在线
            !Shizuku.isPreV11() &&                        // v11+（否则无 checkSelfPermission）
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED  // 已授权

    /**
     * 请求 Shizuku adb 授权（应用会弹出 Shizuku 授权页）。
     * @param onResult 结果回调（true = 已授权）
     */
    fun requestAdbPermission(onResult: (Boolean) -> Unit) {  // 请求授权
        if (Shizuku.isPreV11() || !Shizuku.pingBinder()) {  // 服务不可用
            onResult(false)                               // 直接失败
            return
        }
        if (hasPermission()) {                            // 已授权
            onResult(true)                                // 直接成功
            return
        }
        val listener = object : Shizuku.OnRequestPermissionResultListener {  // 结果监听器（object 便于移除）
            override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {  // 授权结果回调
                Shizuku.removeRequestPermissionResultListener(this)  // 用完即移除
                refresh()                                 // 刷新等级
                onResult(grantResult == PackageManager.PERMISSION_GRANTED)  // 回调结果
            }
        }
        Shizuku.addRequestPermissionResultListener(listener)  // 注册监听
        Shizuku.requestPermission(REQUEST_CODE)           // 发起授权请求（弹系统授权页）
    }

    /** 通过 libsu 执行 root shell 命令（仅 SHIZUKU_ROOT 等级可用，其余返回空串）。 */
    fun shell(command: String): String {                  // root shell 执行
        if (currentLevel() != Level.SHIZUKU_ROOT) return ""  // 非 root 等级拒绝
        return runCatching {                              // 容错执行
            Shell.cmd(command).exec().out.joinToString("\n")  // 同步执行并拼接输出
        }.getOrDefault("")                                // 失败返回空串
    }
}
