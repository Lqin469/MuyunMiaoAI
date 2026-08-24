package com.memuo.feature.settings                         // 声明包名：设置业务模块

import android.content.Context                            // 导入 Context：SAF 目录读取
import android.net.Uri                                    // 导入 Uri：SAF 目录标识
import androidx.lifecycle.ViewModel                       // 导入 ViewModel：UI 数据持有者
import androidx.lifecycle.viewModelScope                  // 导入 viewModelScope：ViewModel 协程作用域
import com.memuo.core.ai.engine.CloudConfig                // 导入云端配置数据类
import com.memuo.core.ai.engine.EngineRouter               // 导入引擎路由器（切换 + 模型检测）
import com.memuo.core.ai.engine.EngineSettings             // 导入引擎设置接口
import com.memuo.core.db.entity.EngineType                 // 导入引擎类型枚举
import com.memuo.core.models.ModelImporter                 // 导入模型导入器
import com.memuo.core.models.ModelDownloadManager          // 导入模型下载器
import dagger.hilt.android.lifecycle.HiltViewModel         // 导入 HiltViewModel：Hilt 提供 ViewModel
import dagger.hilt.android.qualifiers.ApplicationContext  // 导入 ApplicationContext：应用级上下文
import kotlinx.coroutines.flow.MutableStateFlow            // 导入可变状态流
import kotlinx.coroutines.flow.StateFlow                  // 导入只读状态流
import kotlinx.coroutines.flow.asStateFlow                // 导入 asStateFlow：转只读
import kotlinx.coroutines.launch                           // 导入 launch：启动协程
import java.io.File                                        // 导入 File：导入路径
import javax.inject.Inject                                 // 导入 Inject：构造函数注入

/**
 * 设置 ViewModel —— 引擎切换、模型导入（SAF/路径）、云端配置（M-010/M-011）。
 */
@HiltViewModel                                           // 注解：由 Hilt 创建并注入依赖
class SettingsViewModel @Inject constructor(             // 构造函数注入
    @ApplicationContext private val context: Context,    // 注入应用上下文（SAF 目录读取）
    private val engineSettings: EngineSettings,          // 注入引擎设置（类型状态流）
    private val router: EngineRouter,                    // 注入引擎路由器（切换 + 模型检测）
    private val importer: ModelImporter,                 // 注入模型导入器（复制模型到 app 目录）
    private val downloader: ModelDownloadManager,        // 注入模型下载器（ModelScope 下载）
    private val cloudRepo: CloudConfigRepository,        // 注入云端配置仓库
) : ViewModel() {                                        // 继承 ViewModel

    /** 当前引擎类型状态流。 */
    val engineType: StateFlow<EngineType> = engineSettings.engineType  // 暴露引擎类型

    /** 本地模型是否就绪。 */
    private val _hasLocalModel = MutableStateFlow(importer.hasLocalModel())  // 初始检测一次
    val hasLocalModel: StateFlow<Boolean> = _hasLocalModel.asStateFlow()  // 只读暴露

    /** 模型下载进度提示（下载中显示"3/8"）。 */
    private val _downloadProgress = MutableStateFlow("")  // 下载进度
    val downloadProgress: StateFlow<String> = _downloadProgress.asStateFlow()  // 只读暴露

    /** 当前云端配置（用于回显输入框）。 */
    private val _cloud = MutableStateFlow(CloudConfig(baseUrl = "", apiKey = "", model = ""))  // 初始空
    val cloud: StateFlow<CloudConfig> = _cloud.asStateFlow()  // 只读暴露

    /** 操作结果提示消息（切换/导入/保存后显示）。 */
    private val _message = MutableStateFlow("")           // 初始空消息
    val message: StateFlow<String> = _message.asStateFlow()  // 只读暴露

    init {                                                // 初始化
        loadCloudConfig()                                 // 加载云端配置
    }

    /** 加载云端配置（回显）。 */
    fun loadCloudConfig() {                               // 加载云端配置
        viewModelScope.launch {                          // 协程中读取
            _cloud.value = cloudRepo.current() ?: CloudConfig(baseUrl = "", apiKey = "", model = "")  // 无则空
        }
    }

    /** 切换对话引擎；切到本地但无模型时提示拦截。 */
    fun switchEngine(type: EngineType) {                  // 切换引擎
        viewModelScope.launch {                          // 协程中执行
            val ok = router.switchTo(type)               // 路由切换（无本地模型则 false）
            _message.value = when {                      // 按结果设置提示
                ok && type == EngineType.LOCAL -> "已切换到本地引擎"
                ok -> "已切换到云端引擎"
                else -> "切换失败：本地模型未就绪，请先导入模型"
            }
        }
    }

    /** 从设备目录绝对路径导入 MNN 模型到 app 私有目录。 */
    fun importModel(path: String) {                       // 导入模型（绝对路径）
        if (path.isBlank()) {                             // 空路径
            _message.value = "请填写模型目录路径"
            return
        }
        viewModelScope.launch {                          // 协程中复制（大文件走 IO）
            val ok = importer.importMnnToAppDir(File(path))  // 复制模型
            _hasLocalModel.value = ok                     // 更新就绪状态
            _message.value = if (ok) "模型导入成功，可切换到本地引擎" else "导入失败：目录无效（需含 config.json + llm.mnn + llm.mnn.weight）"  // 提示
        }
    }

    /** 从 SAF 文件夹选择器选中的目录导入模型（检测 + 复制）。 */
    fun importModelFromUri(uri: Uri) {                    // 导入模型（SAF 目录）
        viewModelScope.launch {                          // 协程中复制
            val ok = importer.importFromUri(context, uri)  // 检测并复制
            _hasLocalModel.value = ok                     // 更新就绪状态
            _message.value = if (ok) "模型导入成功，可切换到本地引擎" else "导入失败：所选文件夹不是有效模型（需含 config.json + llm.mnn + llm.mnn.weight）"  // 提示
        }
    }

    /** 从 ModelScope 下载 Qwen3.5-0.8B-MNN 模型（约 550M，含视觉模型）。 */
    fun downloadModel() {                                 // 下载模型
        viewModelScope.launch {                          // 协程中下载
            _downloadProgress.value = "开始下载模型…"    // 初始提示
            val err = downloader.download { done, total -> // 下载（进度回调）
                _downloadProgress.value = "下载中 $done/$total 个文件…"  // 更新进度
            }
            _hasLocalModel.value = importer.hasLocalModel()  // 更新就绪状态
            _downloadProgress.value = ""                  // 清空进度
            _message.value = if (err == null) "模型下载完成，可切换到本地引擎" else "下载失败：$err"  // 具体错误提示
        }
    }

    /** 保存云端 API 配置。 */
    fun saveCloudConfig(baseUrl: String, apiKey: String, model: String) {  // 保存云端配置
        viewModelScope.launch {                          // 协程中保存
            cloudRepo.save(baseUrl, apiKey, model)       // 保存（apiKey 加密）
            _message.value = "云端配置已保存"            // 提示
        }
    }
}
