package com.memuo.feature.filesearch                       // 声明包名：文件检索业务模块

import androidx.lifecycle.ViewModel                        // 导入 ViewModel：UI 数据的持有者
import androidx.lifecycle.viewModelScope                  // 导入 viewModelScope：ViewModel 专属协程作用域
import com.memuo.core.search.consent.SearchConsentGate    // 导入许可闸门：搜索唯一入口（安全约束）
import com.memuo.core.search.consent.SearchSession        // 导入搜索会话
import com.memuo.core.search.index.FileIndexer            // 导入文件索引器接口
import com.memuo.core.search.index.IndexResult            // 导入索引结果
import com.memuo.core.search.index.SearchScope            // 导入检索范围
import com.memuo.core.search.index.UnauthorizedSearchException  // 导入未授权异常
import com.memuo.core.search.privilege.PrivilegeManager   // 导入提权管理器（能力等级）
import com.memuo.core.search.progress.SearchProgress      // 导入进度数据
import com.memuo.core.search.progress.SearchProgressListener  // 导入进度监听器
import com.memuo.core.search.service.FileHit              // 导入文件命中结果
import com.memuo.core.search.service.FileQuery            // 导入查询条件
import com.memuo.core.search.service.SearchService        // 导入检索服务
import com.memuo.core.storage.StorageProvider             // 导入存储提供者（应用私有目录）
import dagger.hilt.android.lifecycle.HiltViewModel        // 导入 HiltViewModel 注解
import kotlinx.coroutines.flow.MutableStateFlow           // 导入可变状态流
import kotlinx.coroutines.flow.StateFlow                  // 导入只读状态流
import kotlinx.coroutines.flow.asStateFlow                // 导入 asStateFlow
import kotlinx.coroutines.launch                          // 导入协程启动
import java.util.UUID                                     // 导入 UUID：生成唯一请求 ID
import javax.inject.Inject                                // 导入 Inject

/**
 * 文件检索页状态机 —— 用户强制约束的 UI 落实：
 *  - Idle：未开始，界面只有「开始搜索」按钮，不触发任何扫描；
 *  - Running：显式点击后进入，进度条实时绑定 SearchProgress；
 *  - 任何时刻可「停止」（SearchSession.cancel）。
 */
sealed interface FilesearchUiState {                       // 密封接口：界面状态只允许以下四种（编译期穷举）
    data object Idle : FilesearchUiState                   // 状态1：空闲（未开始，只有开始按钮）
    data class Running(val progress: SearchProgress) : FilesearchUiState  // 状态2：运行中（携带最新进度）
    data class Done(val result: IndexResult) : FilesearchUiState         // 状态3：完成（携带结果统计）
    data class Error(val message: String) : FilesearchUiState            // 状态4：出错（携带错误信息）
}

/**
 * 文件检索页 ViewModel —— M7 完整版：
 *  - 订阅提权等级（UI 实时显示三档能力）；
 *  - 开始索引按等级决定扫描范围（L0 应用私有 / L1 用户目录 / L2 全盘）；
 *  - 提供索引结果查询（AI 工具与检索页共用 SearchService）。
 */
@HiltViewModel                                           // Hilt 提供
class FilesearchViewModel @Inject constructor(           // 构造函数注入
    private val consentGate: SearchConsentGate,          // 许可闸门（搜索必须过闸）
    private val indexer: FileIndexer,                    // 文件索引器
    private val privilege: PrivilegeManager,             // 提权管理器（能力等级 + 授权）
    private val searchService: SearchService,            // 检索服务（查询已建索引）
    private val storage: StorageProvider,                // 存储提供者（应用私有目录）
) : ViewModel() {                                         // 继承 ViewModel

    /** L1/L2 允许扫描的用户目录白名单（顶层目录，跳过 Android/data 等无意义目录）。 */
    private companion object {                            // 常量
        val USER_TOP_DIRS = listOf(                       // 白名单
            "/storage/emulated/0/Download",               // 下载
            "/storage/emulated/0/Documents",              // 文档
            "/storage/emulated/0/Pictures",               // 图片
            "/storage/emulated/0/DCIM",                   // 相机
            "/storage/emulated/0/Music",                  // 音乐
            "/storage/emulated/0/Movies",                 // 视频
            "/storage/emulated/0/tencent",                // 腾讯系应用目录
            "/storage/emulated/0/Android/media",          // 媒体共享目录
        )
    }

    /** 当前提权能力等级（UI 实时订阅显示）。 */
    val level: StateFlow<PrivilegeManager.Level> = privilege.level  // 直接暴露

    /** Shizuku 授权状态（未授权时显示「授权」按钮）。 */
    val authorized: StateFlow<Boolean> = privilege.authorized  // 直接暴露

    /** 索引状态机。 */
    private val _ui = MutableStateFlow<FilesearchUiState>(FilesearchUiState.Idle)  // 初始空闲
    val ui: StateFlow<FilesearchUiState> = _ui.asStateFlow()  // 只读暴露

    /** 查询结果列表（检索页输入关键词查询）。 */
    private val _results = MutableStateFlow<List<FileHit>>(emptyList())  // 初始空
    val results: StateFlow<List<FileHit>> = _results.asStateFlow()  // 只读暴露

    private var session: SearchSession? = null            // 当前会话（取消用）

    /** 请求 Shizuku 授权（用户点「授权」按钮）。 */
    fun requestPermission() {                             // 请求授权
        privilege.requestAdbPermission { granted ->       // 回调结果
            if (granted) _ui.value = FilesearchUiState.Idle  // 授权成功回空闲（用户重新点开始）
        }
    }

    /** 按当前提权等级解析扫描范围（等级与范围严格匹配）。 */
    private fun scopeForLevel(): SearchScope = when (privilege.currentLevel()) {  // 按等级
        PrivilegeManager.Level.NONE -> SearchScope.AppScoped(roots = listOf(storage.root))  // L0：应用私有
        PrivilegeManager.Level.SHIZUKU_ADB -> SearchScope.UserStorage(allowedTopDirs = USER_TOP_DIRS)  // L1：用户目录
        PrivilegeManager.Level.SHIZUKU_ROOT -> SearchScope.FullDisk(allowedTopDirs = USER_TOP_DIRS)  // L2：全盘（同目录，root 可含 /data）
    }

    /** 唯一索引入口：用户点击「开始搜索」调用；绝不后台自动调用。 */
    fun startIndex() {                                    // 开始索引
        if (_ui.value is FilesearchUiState.Running) return  // 防重入
        viewModelScope.launch {                           // 协程执行
            val s = consentGate.beginUserInitiated(UUID.randomUUID().toString())  // 过许可闸门
            session = s                                   // 保存会话
            try {
                val result = indexer.index(               // 执行索引
                    session = s,
                    scope = scopeForLevel(),              // 按提权等级决定范围
                    listener = SearchProgressListener { p -> _ui.value = FilesearchUiState.Running(p) },  // 进度刷新
                )
                _ui.value = FilesearchUiState.Done(result)  // 完成
            } catch (e: UnauthorizedSearchException) {    // 未授权异常
                _ui.value = FilesearchUiState.Error(e.message ?: "未获授权")  // 显示错误
            } catch (e: Exception) {                      // 其他异常
                _ui.value = FilesearchUiState.Error(e.message ?: "搜索失败")  // 显示错误
            }
        }
    }

    /** 取消进行中的索引。 */
    fun cancel() {                                        // 取消
        session?.cancel()                                 // 置位取消标记
        session?.let { indexer.cancel(it.requestId) }     // 通知索引器
    }

    /** 按关键词查询已建索引（检索页搜索框 + AI 工具同源）。 */
    fun query(keyword: String) {                          // 查询索引
        if (keyword.isBlank()) {                          // 空关键词
            _results.value = emptyList()                  // 清空结果
            return
        }
        viewModelScope.launch {                           // 协程查询
            _results.value = searchService.search(FileQuery(keyword = keyword, limit = 50))  // 查询并更新
        }
    }
}
