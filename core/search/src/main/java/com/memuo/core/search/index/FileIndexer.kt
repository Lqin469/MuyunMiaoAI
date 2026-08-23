package com.memuo.core.search.index                        // 声明包名：搜索模块的"索引"子包

import android.net.Uri                                     // 导入 Uri：Android 内容/文档统一标识（SAF 文件用）
import com.memuo.core.search.consent.SearchSession         // 导入 SearchSession：被授权的搜索会话
import com.memuo.core.search.progress.SearchProgressListener  // 导入进度监听器：实时上报进度
import java.io.File                                        // 导入 File：本地文件系统路径表示

/**
 * 允许的检索范围。
 * 注意：UserStorage / FullDisk 的构造与使用必须经 PrivilegeManager 校验能力等级（L1/L2），
 * 未经提权时只能使用 AppScoped（M7 实现 PrivilegeManager，M0 仅定义契约）。
 */
sealed interface SearchScope {                             // 密封接口：检索范围只有以下三种（编译期穷举检查）
    /** L0：应用私有目录 + 用户 SAF 授权树。 */
    data class AppScoped(                                  // 范围1：应用私有目录（无提权时的默认范围）
        val roots: List<File>,                             // 应用私有根目录列表
        val safTrees: List<Uri> = emptyList(),             // 用户通过 SAF 授权的目录（uri 列表）
    ) : SearchScope                                        // 实现密封接口

    /** L1（Shizuku-adb）：用户目录全量。 */
    data class UserStorage(                                // 范围2：用户存储全量（需要 Shizuku-adb 提权）
        val allowedTopDirs: List<String>,                  // 允许扫描的顶层目录白名单
    ) : SearchScope                                        // 实现密封接口

    /** L2（Shizuku-root）：全盘。 */
    data class FullDisk(                                   // 范围3：全盘扫描（需要 root 提权）
        val allowedTopDirs: List<String>,                  // 允许扫描的顶层目录白名单
        val includeData: Boolean = false,                  // 是否包含 /data 目录（默认不包含，风险高）
    ) : SearchScope                                        // 实现密封接口
}

/**
 * 未授权异常：任何没有 SearchSession 的搜索调用都会被拒绝。
 * 这是「绝不在后台偷偷搜索」约束的最后一道防线。
 */
class UnauthorizedSearchException(requestId: String) :     // 自定义异常类：搜索未获授权时抛出
    IllegalStateException("搜索必须由用户显式触发（requestId=$requestId）；后台自动搜索默认禁用。")  // 带中文提示的异常信息

/**
 * 文件索引器 —— 安全约束（强制）：
 *  1) 必须携带 [SearchConsentGate] 签发的 [SearchSession]，否则抛 [UnauthorizedSearchException]；
 *  2) 必须通过 [listener] 实时上报进度（契约见 SearchProgress），禁止"闷头跑完"；
 *  3) 全程响应 [SearchSession.cancelled]：每处理一批文件检查一次，用户可随时停止。
 */
interface FileIndexer {                                    // 文件索引器接口：扫描文件并写入索引（M7 提供实现）
    suspend fun index(                                     // 挂起函数：执行一次索引（协程中调用）
        session: SearchSession,                            // 必须传入被授权的会话（安全约束）
        scope: SearchScope,                                // 检索范围（决定扫描哪里）
        listener: SearchProgressListener,                  // 进度监听器（实时上报，UI 显示进度条）
    ): IndexResult                                         // 返回索引结果汇总

    /** 取消指定请求（会同步置位对应 SearchSession.cancelled）。 */
    fun cancel(requestId: String)                          // 取消方法：按请求 ID 取消正在进行的索引
}

data class IndexResult(                                    // 索引结果数据类：一次索引的统计汇总
    val requestId: String,                                 // 所属搜索会话 ID
    val scannedFiles: Long,                                // 扫描到的文件总数
    val indexedFiles: Long,                                // 成功写入索引的文件数
    val skippedFiles: Long,                                // 跳过的文件数（不支持类型/重复等）
    val durationMs: Long,                                  // 总耗时（毫秒）
    val cancelled: Boolean,                                // 是否被用户取消
)
