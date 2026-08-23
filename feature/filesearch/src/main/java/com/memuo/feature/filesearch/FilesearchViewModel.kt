package com.memuo.feature.filesearch                       // 声明包名：文件检索业务模块

import androidx.lifecycle.ViewModel                        // 导入 ViewModel：UI 数据的持有者（旋转屏幕不丢失）
import androidx.lifecycle.viewModelScope                  // 导入 viewModelScope：ViewModel 专属协程作用域（自动取消）
import com.memuo.core.search.consent.SearchConsentGate    // 导入许可闸门：搜索唯一入口（安全约束）
import com.memuo.core.search.consent.SearchSession        // 导入搜索会话：被授权的搜索凭证
import com.memuo.core.search.index.FileIndexer            // 导入文件索引器接口
import com.memuo.core.search.index.IndexResult            // 导入索引结果
import com.memuo.core.search.index.SearchScope            // 导入检索范围
import com.memuo.core.search.index.UnauthorizedSearchException  // 导入未授权异常
import com.memuo.core.search.progress.SearchProgress      // 导入进度数据
import com.memuo.core.search.progress.SearchProgressListener  // 导入进度监听器
import kotlinx.coroutines.flow.MutableStateFlow           // 导入可变状态流（可写入的 UI 状态）
import kotlinx.coroutines.flow.StateFlow                  // 导入只读状态流（UI 订阅用）
import kotlinx.coroutines.launch                          // 导入协程启动函数
import java.util.UUID                                      // 导入 UUID：生成唯一请求 ID

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

class FilesearchViewModel(                                 // 文件检索页的 ViewModel（界面数据与逻辑）
    private val consentGate: SearchConsentGate,            // 注入许可闸门（搜索前必须过闸）
    private val indexer: FileIndexer,                      // 注入文件索引器（实际执行扫描）
) : ViewModel() {                                          // 继承 ViewModel 基类

    private val _ui = MutableStateFlow<FilesearchUiState>(FilesearchUiState.Idle)  // 私有可变状态：初始为空闲
    val ui: StateFlow<FilesearchUiState> = _ui             // 对外只读状态：UI 订阅它来刷新界面

    private var session: SearchSession? = null             // 记录当前会话（用于取消）

    /** 唯一入口：用户点击「开始搜索」调用；绝不从后台/生命周期回调自动调用。 */
    fun startIndex() {                                     // 开始搜索（仅由用户点击按钮触发）
        if (_ui.value is FilesearchUiState.Running) return // 防重入：已经在运行就直接返回
        viewModelScope.launch {                            // 在协程中执行（后台线程跑，不卡界面）
            val s = consentGate.beginUserInitiated(UUID.randomUUID().toString())  // 第一步：过许可闸门，取得用户授权会话
            session = s                                    // 保存会话（供取消用）
            try {                                          // 捕获异常
                // TODO(M7): scope 由 PrivilegeManager 能力等级决定（L0=AppScoped / L1=UserStorage / L2=FullDisk）
                val result = indexer.index(                // 调用索引器执行扫描（携带授权会话）
                    session = s,                           // 传入会话（无会话会抛异常，安全兜底）
                    scope = SearchScope.AppScoped(roots = emptyList()),  // 扫描范围：当前用应用私有目录（M7 按提权等级扩展）
                    listener = SearchProgressListener { p -> _ui.value = FilesearchUiState.Running(p) },  // 每个进度回调都刷新 UI 状态
                )
                _ui.value = FilesearchUiState.Done(result) // 完成后切换状态：显示结果统计
            } catch (e: UnauthorizedSearchException) {     // 捕获"未获授权"异常（理论上不会发生，兜底）
                _ui.value = FilesearchUiState.Error(e.message ?: "未获授权")  // 显示错误信息
            } catch (e: Exception) {                       // 捕获其他异常
                _ui.value = FilesearchUiState.Error(e.message ?: "搜索失败")  // 显示错误信息
            }
        }
    }

    /** 用户点击「停止」；indexer 会尽快停止并上报 CANCELLED 进度。 */
    fun cancel() {                                         // 停止搜索（用户点击"停止"按钮）
        session?.cancel()                                  // 置位会话取消标记（索引器每批检查）
        session?.let { indexer.cancel(it.requestId) }      // 通知索引器立即取消（双重保险）
    }
}
