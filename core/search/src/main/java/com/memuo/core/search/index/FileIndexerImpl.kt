package com.memuo.core.search.index                        // 声明包名：搜索模块的"索引"子包

import android.content.Context                             // 导入 Context：应用上下文
import androidx.documentfile.provider.DocumentFile         // 导入 DocumentFile：SAF 授权树遍历
import com.memuo.core.db.dao.FileLocationDao               // 导入文件位置 DAO
import com.memuo.core.db.entity.FileLocation               // 导入文件位置实体
import com.memuo.core.search.consent.SearchSession         // 导入搜索会话（安全约束）
import com.memuo.core.search.progress.SearchPhase          // 导入搜索阶段
import com.memuo.core.search.progress.SearchProgress       // 导入进度数据
import com.memuo.core.search.progress.SearchProgressListener  // 导入进度监听器
import com.memuo.core.search.privilege.PrivilegeManager    // 导入提权管理器
import dagger.hilt.android.qualifiers.ApplicationContext   // 导入 ApplicationContext 限定符
import kotlinx.coroutines.Dispatchers                      // 导入 Dispatchers：IO 线程
import kotlinx.coroutines.withContext                      // 导入 withContext：切换线程
import java.io.File                                        // 导入 File：本地文件
import java.util.concurrent.ConcurrentHashMap              // 导入 ConcurrentHashMap：活跃会话表
import javax.inject.Inject                                 // 导入 Inject
import javax.inject.Singleton                              // 导入 Singleton

/**
 * 文件索引器实现（FileIndexerImpl）—— M7 真实实现。
 *
 * 安全约束（强制遵守）：
 *  1) 范围与提权等级严格校验：UserStorage 需 L1+、FullDisk 需 L2，未达标抛 [UnauthorizedSearchException]；
 *  2) 每扫描 200 个文件上报一次进度（含当前路径）+ 检查取消标记；
 *  3) 只存元数据（路径/名称/扩展名/大小/时间），绝不读取或存储文件内容。
 */
@Singleton                                               // 单例
class FileIndexerImpl @Inject constructor(               // 构造函数注入
    @ApplicationContext private val context: Context,    // 应用上下文（SAF 树读取）
    private val privilege: PrivilegeManager,             // 提权管理器（等级校验）
    private val dao: FileLocationDao,                    // 文件位置 DAO（索引落库）
) : FileIndexer {                                        // 实现 FileIndexer 接口

    /** 活跃会话表：requestId → 会话（cancel 时置位取消标记）。 */
    private val sessions = ConcurrentHashMap<String, SearchSession>()  // 线程安全的活跃会话映射

    override suspend fun index(                            // 执行一次索引
        session: SearchSession,                           // 被授权的会话（必经许可闸门）
        scope: SearchScope,                               // 检索范围
        listener: SearchProgressListener,                 // 进度监听器（实时上报）
    ): IndexResult = withContext(Dispatchers.IO) {        // 切 IO 线程执行
        sessions[session.requestId] = session             // 登记活跃会话（供 cancel）
        val startedAt = System.currentTimeMillis()        // 记录开始时间
        var scanned = 0L                                  // 已扫描文件数
        var indexed = 0L                                  // 已入库文件数
        var skipped = 0L                                  // 跳过文件数
        var cancelled = false                             // 是否被取消
        try {
            report(listener, session, SearchPhase.INITIALIZING, scanned, 0, null, startedAt)  // 阶段1：准备中
            val roots = resolveRoots(scope)               // 校验提权等级并解析扫描根目录（未达标抛异常）
            val batch = ArrayList<FileLocation>(200)      // 批量缓冲（200 条一批落库）

            for (root in roots) {                         // 遍历每个根目录
                report(listener, session, SearchPhase.SCANNING_DIRS, scanned, 0, root.absolutePath, startedAt)  // 阶段2：扫描
                val it = root.walkTopDown().iterator()    // 深度优先遍历文件树
                while (it.hasNext()) {                    // 逐文件处理
                    if (session.cancelled) { cancelled = true; break }  // 用户点了停止 → 立即退出
                    val f = it.next()                     // 下一个文件
                    if (!f.isFile) continue               // 只索引文件（跳过目录）
                    if (f.length() <= 0L) { skipped++; continue }  // 空文件跳过
                    scanned++                             // 计数
                    batch.add(                            // 收集元数据（绝不读内容）
                        FileLocation(
                            path = f.absolutePath,        // 绝对路径
                            name = f.name,                // 文件名
                            ext = f.extension?.lowercase() ?: "",  // 扩展名（小写）
                            sizeBytes = f.length(),       // 大小
                            mtime = f.lastModified(),     // 修改时间
                            indexedAt = startedAt,        // 本次索引时间
                        )
                    )
                    if (batch.size >= 200) {              // 满一批
                        dao.upsertAll(batch)              // 批量落库
                        indexed += batch.size             // 计数
                        batch.clear()                     // 清空缓冲
                        report(listener, session, SearchPhase.SCANNING_DIRS, scanned, 0, f.absolutePath, startedAt)  // 上报进度
                    }
                }
                if (cancelled) break                      // 取消则停止后续根目录
            }
            if (batch.isNotEmpty() && !cancelled) {       // 剩余不足一批的尾巴
                dao.upsertAll(batch)                      // 落库
                indexed += batch.size                     // 计数
            }
            report(listener, session, SearchPhase.INDEXING_DB, scanned, 0, null, startedAt)  // 阶段3：写入完成
            if (cancelled) {                              // 用户取消
                report(listener, session, SearchPhase.CANCELLED, scanned, 0, null, startedAt)  // 上报取消
            } else {                                      // 正常完成
                report(listener, session, SearchPhase.DONE, scanned, 0, null, startedAt)  // 上报完成
            }
        } catch (e: Exception) {                          // 任何异常
            report(listener, session, SearchPhase.FAILED, scanned, 0, e.message ?: "未知错误", startedAt)  // 上报失败
        } finally {
            sessions.remove(session.requestId)            // 移除活跃会话
        }
        IndexResult(                                      // 汇总结果
            requestId = session.requestId,
            scannedFiles = scanned,
            indexedFiles = indexed,
            skippedFiles = skipped,
            durationMs = System.currentTimeMillis() - startedAt,
            cancelled = cancelled,
        )
    }

    override fun cancel(requestId: String) {              // 取消指定请求
        sessions[requestId]?.cancel()                     // 置位对应会话的取消标记
    }

    /** 校验提权等级并解析扫描根目录（能力与范围不匹配时抛未授权异常）。 */
    private fun resolveRoots(scope: SearchScope): List<File> = when (scope) {  // 按范围解析
        is SearchScope.AppScoped -> scope.roots           // L0：应用私有目录（无需提权）
        is SearchScope.UserStorage -> {                   // L1：用户目录全量
            if (privilege.currentLevel() == PrivilegeManager.Level.NONE) {  // 未提权
                throw UnauthorizedSearchException("${scope.allowedTopDirs}")  // 拒绝（无 requestId 场景用目录标识）
            }
            scope.allowedTopDirs.map { File(it) }         // 白名单顶层目录
        }
        is SearchScope.FullDisk -> {                      // L2：全盘
            if (privilege.currentLevel() != PrivilegeManager.Level.SHIZUKU_ROOT) {  // 非 root
                throw UnauthorizedSearchException("${scope.allowedTopDirs}")  // 拒绝
            }
            scope.allowedTopDirs.map { File(it) }         // 白名单顶层目录
        }
    }

    /** 上报一次进度（构造 SearchProgress 并回调）。 */
    private fun report(                                   // 进度上报
        listener: SearchProgressListener,                 // 监听器
        session: SearchSession,                           // 会话
        phase: SearchPhase,                               // 阶段
        scanned: Long,                                    // 已扫描数
        total: Long,                                      // 总数（未知传 0）
        path: String?,                                    // 当前路径
        startedAt: Long,                                  // 开始时间
    ) {
        listener.onProgress(                              // 回调进度
            SearchProgress(
                requestId = session.requestId,
                phase = phase,
                scannedItems = scanned,
                totalItems = total,
                currentPath = path,
                startedAt = startedAt,
            )
        )
    }

    /** SAF 授权树遍历（AppScoped.safTrees）：把授权目录内的文件也收集进批处理。 */
    @Suppress("unused")                                   // 保留给后续接入（契约完整）
    private fun collectDocumentTree(doc: DocumentFile, batch: ArrayList<FileLocation>, startedAt: Long) {  // SAF 递归
        for (child in doc.listFiles()) {                  // 遍历子项
            when {
                child.isDirectory -> collectDocumentTree(child, batch, startedAt)  // 目录递归
                child.isFile -> batch.add(                // 文件收集元数据
                    FileLocation(
                        path = child.uri.toString(),      // SAF 文件用 uri 字符串表示路径
                        name = child.name ?: "unnamed",   // 文件名
                        ext = child.name?.substringAfterLast('.', "")?.lowercase() ?: "",  // 扩展名
                        sizeBytes = child.length(),       // 大小
                        mtime = child.lastModified(),     // 修改时间
                        indexedAt = startedAt,            // 索引时间
                    )
                )
            }
        }
    }
}
