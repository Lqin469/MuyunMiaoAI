package com.memuo.feature.settings                         // 声明包名：设置业务模块

import androidx.compose.foundation.layout.Column          // 导入 Column：纵向布局
import androidx.compose.foundation.layout.fillMaxSize     // 导入 fillMaxSize：铺满
import androidx.compose.foundation.layout.padding          // 导入 padding：内边距
import androidx.compose.material3.MaterialTheme           // 导入 MaterialTheme：主题
import androidx.compose.material3.Text                    // 导入 Text：文本
import androidx.compose.runtime.Composable                // 导入 Composable：可组合函数注解
import androidx.compose.runtime.collectAsState            // 导入 collectAsState：状态流→状态
import androidx.compose.runtime.getValue                  // 导入 getValue：by 委托
import androidx.compose.ui.Modifier                       // 导入 Modifier：修饰
import androidx.compose.ui.unit.dp                        // 导入 dp：尺寸单位
import androidx.hilt.navigation.compose.hiltViewModel     // 导入 hiltViewModel：Hilt 提供 ViewModel
import androidx.lifecycle.ViewModel                       // 导入 ViewModel：UI 数据持有者
import androidx.lifecycle.viewModelScope                  // 导入 viewModelScope：协程作用域
import com.memuo.core.models.ModelImporter                 // 导入模型导入器（模型就绪检测）
import com.memuo.core.storage.WallpaperPrefs               // 导入壁纸偏好
import com.memuo.core.storage.WallpaperSource              // 导入壁纸来源
import com.memuo.core.ui.AppIcons                          // 导入应用图标集
import com.memuo.core.ui.components.SectionCard            // 导入分组卡片
import com.memuo.core.ui.components.SectionCardTitle       // 导入分组标题
import com.memuo.core.ui.components.SettingsMenuRow        // 导入菜单行
import com.memuo.core.ui.components.SubBody                // 导入子页内容容器
import com.memuo.core.ui.components.SubHeader              // 导入子页顶栏
import com.memuo.core.ui.theme.MuyunText3                  // 导入三级文字色
import dagger.hilt.android.lifecycle.HiltViewModel        // 导入 HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow            // 导入可变状态流
import kotlinx.coroutines.flow.StateFlow                  // 导入只读状态流
import kotlinx.coroutines.flow.asStateFlow                // 导入 asStateFlow
import kotlinx.coroutines.launch                           // 导入 launch：协程
import org.json.JSONArray                                  // 导入 JSONArray：解析 JSON
import javax.inject.Inject                                // 导入 Inject：构造函数注入

/**
 * 设置主页 —— 分组卡片 + 菜单项（HTML 设置页迁移）。
 * 分组：通用设置（自定义壁纸/数据迁移）、服务（云端API管理）、系统（权限管理）、模型（模型管理）。
 */
@Composable                                               // 可组合 UI 函数
fun SettingsHomeScreen(                                   // 设置主页
    onBack: () -> Unit,                                   // 返回回调
    onWallpaper: () -> Unit,                              // 自定义壁纸
    onMigrate: () -> Unit,                                // 数据迁移
    onApi: () -> Unit,                                    // 云端API管理
    onPermission: () -> Unit,                             // 权限管理
    onModel: () -> Unit,                                  // 模型管理
    viewModel: SettingsHomeViewModel = hiltViewModel(),   // Hilt 提供 ViewModel
) {
    val wallHint by viewModel.wallHint.collectAsState()   // 壁纸提示
    val apiHint by viewModel.apiHint.collectAsState()     // API 提示
    val permHint by viewModel.permHint.collectAsState()   // 权限提示
    val modelHint by viewModel.modelHint.collectAsState() // 模型提示

    Column(modifier = Modifier.fillMaxSize()) {           // 纵向布局
        SubHeader(title = "设置", onBack = onBack)         // 顶栏
        SubBody(modifier = Modifier.fillMaxSize()) {       // 内容容器
            Column(modifier = Modifier.fillMaxSize()) {    // 纵向内容
                SectionCard {                              // 通用设置分组
                    SectionCardTitle("通用设置")            // 分组标题
                    SettingsMenuRow(                       // 主题设置行
                        icon = AppIcons.Palette,           // 调色盘图标
                        name = "主题",                     // 名称（原「自定义壁纸」重构为主题系统）
                        desc = wallHint,                   // 描述（默认壁纸/已自定义）
                        onClick = onWallpaper,             // 跳转
                    )
                    SettingsMenuRow(                       // 数据迁移行
                        icon = AppIcons.Upload,            // 上传图标
                        name = "数据迁移",                  // 名称
                        desc = "局域网传输",                // 描述（HTML migrate-count-hint）
                        onClick = onMigrate,               // 跳转
                    )
                }
                Column(modifier = Modifier.padding(top = 14.dp)) {  // 卡片间距（HTML .set-card margin-bottom 14）
                    SectionCard {                          // 服务分组
                        SectionCardTitle("服务")            // 分组标题
                        SettingsMenuRow(                   // 云端API管理行
                            icon = AppIcons.Cloud,         // 云朵图标
                            name = "云端API管理",           // 名称
                            desc = apiHint,                // 描述（使用中：X / 未配置）
                            onClick = onApi,               // 跳转
                        )
                    }
                }
                Column(modifier = Modifier.padding(top = 14.dp)) {  // 卡片间距
                    SectionCard {                          // 系统分组
                        SectionCardTitle("系统")            // 分组标题
                        SettingsMenuRow(                   // 权限管理行
                            icon = AppIcons.Shield,        // 盾牌图标
                            name = "权限管理",              // 名称
                            desc = permHint,               // 描述（当前：基础/ADB/ROOT）
                            onClick = onPermission,        // 跳转
                        )
                    }
                }
                Column(modifier = Modifier.padding(top = 14.dp)) {  // 卡片间距
                    SectionCard {                          // 模型分组
                        SectionCardTitle("模型")            // 分组标题
                        SettingsMenuRow(                   // 模型管理行
                            icon = AppIcons.Model,         // 勾圆图标
                            name = "模型管理",              // 名称
                            desc = modelHint,              // 描述（未导入/已导入 N 个）
                            onClick = onModel,             // 跳转
                        )
                    }
                }
                Text(                                     // 底部说明（HTML .set-hint）
                    text = "管理用于「云端」模式对话的 API 服务、端侧模型与设备高级权限，选择结果即时保存并生效。",  // 文案
                    style = MaterialTheme.typography.labelSmall,  // 小字（HTML 12px）
                    color = MuyunText3,                   // 三级灰
                    lineHeight = MaterialTheme.typography.labelSmall.lineHeight * 1.5f,  // 行距（HTML 1.7）
                    modifier = Modifier.padding(vertical = 14.dp, horizontal = 4.dp),  // 内边距
                )
            }
        }
    }
}

/** 设置主页 ViewModel —— 汇总各模块状态为菜单行描述。 */
@HiltViewModel                                           // 注解：由 Hilt 创建
class SettingsHomeViewModel @Inject constructor(         // 构造函数注入
    private val prefs: ExtPrefs,                         // 注入扩展偏好
    private val wallPrefs: WallpaperPrefs,               // 注入壁纸偏好
    private val importer: ModelImporter,                 // 注入模型导入器
) : ViewModel() {                                        // 继承 ViewModel

    private val _wallHint = MutableStateFlow("默认壁纸")  // 壁纸提示
    val wallHint: StateFlow<String> = _wallHint.asStateFlow()  // 只读暴露
    private val _apiHint = MutableStateFlow("未配置")     // API 提示
    val apiHint: StateFlow<String> = _apiHint.asStateFlow()  // 只读暴露
    private val _permHint = MutableStateFlow("当前：基础")  // 权限提示
    val permHint: StateFlow<String> = _permHint.asStateFlow()  // 只读暴露
    private val _modelHint = MutableStateFlow("未导入")    // 模型提示
    val modelHint: StateFlow<String> = _modelHint.asStateFlow()  // 只读暴露

    private var apiSnapshot = JSONArray()                 // API 列表快照
    private var apiCount = 0                              // API 数量
    private var currentApiId = -1L                        // 当前 API id

    init {                                                // 初始化
        viewModelScope.launch {                          // 协程中收集
            wallPrefs.config.collect { cfg ->             // 壁纸配置变化
                _wallHint.value = when (cfg.source) {     // 按来源提示（HTML updateWallHint）
                    WallpaperSource.DEFAULT -> "默认主题"  // 默认
                    WallpaperSource.UPLOAD -> "自定义背景" // 上传
                    WallpaperSource.PRESET -> "已选主题"   // 主题
                }
            }
        }
        viewModelScope.launch {                          // 协程中收集
            prefs.apiListJson.collect { json ->           // API 列表变化
                apiSnapshot = runCatching { JSONArray(json) }.getOrDefault(JSONArray())  // 解析快照
                apiCount = apiSnapshot.length()           // 数量
                updateApiHint()                           // 刷新提示
            }
        }
        viewModelScope.launch {                          // 协程中收集
            prefs.currentApiId.collect { id ->            // 当前 API 变化
                currentApiId = id                         // 记录
                updateApiHint()                           // 刷新提示
            }
        }
        viewModelScope.launch {                          // 协程中收集
            prefs.permMode.collect { mode ->              // 权限模式变化
                _permHint.value = "当前：" + when (mode) {  // 映射中文（HTML updatePermHint）
                    "adb" -> "ADB"                        // ADB
                    "root" -> "ROOT"                      // ROOT
                    else -> "基础"                        // 基础
                }
            }
        }
        viewModelScope.launch {                          // 协程中收集
            prefs.modelListJson.collect { json ->         // 模型列表变化
                val count = if (json.isBlank()) 0 else runCatching { JSONArray(json).length() }.getOrDefault(0)  // 数量
                _modelHint.value = if (count > 0) "已导入 $count 个" else "未导入"  // 提示
            }
        }
    }

    /** 按快照刷新 API 菜单行描述（HTML updateApiCountHint）。 */
    private fun updateApiHint() {                         // 刷新 API 提示
        _apiHint.value = if (apiCount == 0) {             // 无配置
            "未配置"                                       // 未配置
        } else {                                          // 有配置
            val cur = (0 until apiCount).mapNotNull { i -> runCatching { apiSnapshot.getJSONObject(i) }.getOrNull() }  // 对象列表
                .firstOrNull { it.optLong("id") == currentApiId }  // 找当前使用
            cur?.let { "使用中：" + it.optString("name") } ?: "已配置 $apiCount 个"  // 使用中/已配置
        }
    }
}
