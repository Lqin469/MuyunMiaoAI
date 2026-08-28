package com.memuo.feature.settings                         // 声明包名：设置业务模块

import androidx.compose.animation.animateColorAsState    // 导入 animateColorAsState：颜色动画
import androidx.compose.foundation.background             // 导入 background：背景修饰
import androidx.compose.foundation.clickable              // 导入 clickable：点击修饰
import androidx.compose.foundation.layout.Box             // 导入 Box：盒式布局
import androidx.compose.foundation.layout.Column          // 导入 Column：纵向布局
import androidx.compose.foundation.layout.Row             // 导入 Row：横向布局
import androidx.compose.foundation.layout.fillMaxSize     // 导入 fillMaxSize：铺满
import androidx.compose.foundation.layout.fillMaxWidth    // 导入 fillMaxWidth：占满宽度
import androidx.compose.foundation.layout.padding          // 导入 padding：内边距
import androidx.compose.foundation.layout.size            // 导入 size：固定尺寸
import androidx.compose.foundation.shape.CircleShape       // 导入 CircleShape：圆形
import androidx.compose.foundation.shape.RoundedCornerShape  // 导入 RoundedCornerShape：圆角形状
import androidx.compose.material3.Icon                    // 导入 Icon：图标
import androidx.compose.material3.MaterialTheme           // 导入 MaterialTheme：主题
import androidx.compose.material3.Text                    // 导入 Text：文本
import androidx.compose.runtime.Composable                // 导入 Composable：可组合函数注解
import androidx.compose.runtime.collectAsState            // 导入 collectAsState：状态流→状态
import androidx.compose.runtime.getValue                  // 导入 getValue：by 委托
import androidx.compose.ui.Alignment                      // 导入 Alignment：对齐
import androidx.compose.ui.Modifier                       // 导入 Modifier：修饰
import androidx.compose.ui.draw.clip                      // 导入 clip：裁剪
import androidx.compose.ui.graphics.Color                 // 导入 Color：颜色
import androidx.compose.ui.text.font.FontWeight           // 导入 FontWeight：字重
import androidx.compose.ui.unit.dp                        // 导入 dp：尺寸单位
import androidx.hilt.navigation.compose.hiltViewModel     // 导入 hiltViewModel：Hilt 提供 ViewModel
import androidx.lifecycle.ViewModel                       // 导入 ViewModel：UI 数据持有者
import androidx.lifecycle.viewModelScope                  // 导入 viewModelScope：协程作用域
import com.memuo.core.ui.AppIcons                          // 导入应用图标集
import com.memuo.core.ui.components.LocalToast            // 导入 Toast 状态
import com.memuo.core.ui.components.SectionCard            // 导入分组卡片
import com.memuo.core.ui.components.SectionCardTitle       // 导入分组标题
import com.memuo.core.ui.components.SubBody               // 导入子页内容容器
import com.memuo.core.ui.components.SubHeader             // 导入子页顶栏
import com.memuo.core.ui.theme.MuyunAccentLight           // 导入浅灰底
import com.memuo.core.ui.theme.MuyunGreen                 // 导入成功绿
import com.memuo.core.ui.theme.MuyunText                  // 导入主文字色
import com.memuo.core.ui.theme.MuyunText2                 // 导入次级文字色
import com.memuo.core.ui.theme.MuyunText3                 // 导入三级文字色
import dagger.hilt.android.lifecycle.HiltViewModel        // 导入 HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow            // 导入可变状态流
import kotlinx.coroutines.flow.StateFlow                  // 导入只读状态流
import kotlinx.coroutines.flow.asStateFlow                // 导入 asStateFlow
import kotlinx.coroutines.launch                           // 导入 launch：协程
import javax.inject.Inject                                // 导入 Inject：构造函数注入

/**
 * 权限管理页 —— 三档权限模式单选（HTML 权限管理页迁移）。
 * 对应 HTML：基础/ADB/ROOT 三个单选卡片（图标 + 名称 + 描述 + 圆形勾选），选中即保存。
 * 选择结果持久化到 DataStore，设备自检页的权限状态与之联动（HTML applyPermToDeviceCheck）。
 */
@Composable                                               // 可组合 UI 函数
fun PermissionScreen(                                    // 权限管理页
    onBack: () -> Unit,                                  // 返回回调
    viewModel: PermissionViewModel = hiltViewModel(),    // Hilt 提供 ViewModel
) {
    val mode by viewModel.mode.collectAsState()          // 订阅权限模式
    val toast = LocalToast.current                       // 取全局 Toast

    Column(modifier = Modifier.fillMaxSize()) {           // 纵向布局
        SubHeader(title = "权限管理", onBack = onBack)     // 顶栏
        SubBody(modifier = Modifier.fillMaxSize()) {       // 内容容器
            Column(modifier = Modifier.fillMaxSize()) {    // 纵向内容
                SectionCard {                              // 权限模式分组（HTML .set-card）
                    SectionCardTitle("权限模式")            // 分组标题
                    listOf(                               // 三档选项（HTML .perm-choice）
                        Triple("basic", AppIcons.Info, "基础权限" to "默认。不授予 ADB 或 ROOT，仅使用普通功能"),  // 基础
                        Triple("adb", AppIcons.Code, "ADB 权限" to "USB 调试增强，无需 ROOT"),  // ADB
                        Triple("root", AppIcons.Terminal, "ROOT 权限" to "最高系统权限，需设备已 ROOT"),  // ROOT
                    ).forEach { (m, icon, text) ->        // 遍历选项
                        PermChoice(                       // 单选卡片
                            selected = mode == m,         // 是否选中
                            icon = icon,                  // 图标
                            name = text.first,            // 名称
                            desc = text.second,           // 描述
                            onClick = {                   // 点击选中
                                viewModel.setMode(m)      // 保存
                                toast.show(               // Toast（HTML selectPermMode 同款文案）
                                    when (m) {            // 按模式
                                        "basic" -> "已切换为基础权限"   // 基础
                                        "adb" -> "已启用 ADB 权限"     // ADB
                                        else -> "已启用 ROOT 权限"     // ROOT
                                    }
                                )
                            },
                        )
                    }
                }
                Text(                                     // 底部说明（HTML .set-hint）
                    text = "选择后立即保存，并应用于设备自检等后续操作；两者均不选时默认使用基础权限。",  // 文案
                    style = MaterialTheme.typography.labelSmall,  // 小字（HTML 12px）
                    color = MuyunText3,                   // 三级灰
                    lineHeight = MaterialTheme.typography.labelSmall.lineHeight * 1.5f,  // 行距
                    modifier = Modifier.padding(vertical = 14.dp, horizontal = 4.dp),  // 内边距
                )
            }
        }
    }
}

/** 权限单选卡片：图标 + 名称/描述 + 圆形勾选（对应 HTML .perm-choice）。 */
@Composable                                               // 可组合函数
private fun PermChoice(                                  // 单选卡片
    selected: Boolean,                                   // 是否选中
    icon: androidx.compose.ui.graphics.vector.ImageVector,  // 图标
    name: String,                                        // 名称
    desc: String,                                        // 描述
    onClick: () -> Unit,                                 // 点击回调
) {
    val iconBg by animateColorAsState(                   // 图标底色动画
        targetValue = if (selected) MuyunGreen else MuyunAccentLight,  // 选中绿/常态灰
        label = "permIconBg",                            // 标签
    )
    val iconTint by animateColorAsState(                 // 图标色动画
        targetValue = if (selected) MuyunGreen else MuyunText2,  // 选中绿/常态灰
        label = "permIconTint",                          // 标签
    )
    Row(                                                 // 横向布局
        modifier = Modifier                              // 修饰
            .fillMaxWidth()                              // 占满宽度
            .clickable { onClick() }                     // 点击
            .padding(vertical = 14.dp),                  // 上下 14（HTML padding 14px 0）
        verticalAlignment = Alignment.CenterVertically,  // 垂直居中
    ) {
        Box(                                             // 图标底
            modifier = Modifier                         // 修饰
                .size(36.dp)                            // 36dp（HTML .perm-choice-icon）
                .clip(RoundedCornerShape(10.dp))        // 圆角 10
                .background(iconBg),                    // 底色（动画）
            contentAlignment = Alignment.Center,         // 居中
        ) {
            Icon(                                        // 图标
                imageVector = icon,                      // 矢量
                contentDescription = name,               // 描述
                tint = iconTint,                         // 色（动画）
                modifier = Modifier.size(16.dp),         // 16dp
            )
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {  // 信息区
            Text(                                        // 名称
                text = name,                             // 内容
                style = MaterialTheme.typography.titleSmall,  // 字号（HTML 14px）
                fontWeight = FontWeight.SemiBold,        // 半粗
                color = MuyunText,                       // 主文字色
            )
            Text(                                        // 描述
                text = desc,                             // 内容
                style = MaterialTheme.typography.labelSmall,  // 小字（HTML 11px）
                color = MuyunText3,                      // 三级灰
                modifier = Modifier.padding(top = 3.dp),  // 上留白
            )
        }
        Box(                                             // 圆形勾选（HTML .perm-choice-check）
            modifier = Modifier                         // 修饰
                .size(22.dp)                            // 22dp
                .clip(CircleShape)                      // 圆形
                .background(if (selected) MuyunGreen else Color.Transparent)  // 选中绿底
                .padding(0.dp),                         // 无内边距
            contentAlignment = Alignment.Center,         // 居中
        ) {
            if (selected) {                              // 选中显示对勾
                Icon(                                    // 对勾
                    imageVector = AppIcons.Check,        // 图标
                    contentDescription = null,           // 装饰
                    tint = Color.White,                  // 白
                    modifier = Modifier.size(12.dp),     // 12dp
                )
            }
        }
    }
}

/** 权限管理 ViewModel —— 模式读写（选中即持久化）。 */
@HiltViewModel                                           // 注解：由 Hilt 创建
class PermissionViewModel @Inject constructor(           // 构造函数注入
    private val prefs: ExtPrefs,                         // 注入扩展偏好
) : ViewModel() {                                        // 继承 ViewModel

    private val _mode = MutableStateFlow("basic")        // 当前模式（默认基础）
    val mode: StateFlow<String> = _mode.asStateFlow()    // 只读暴露

    init {                                                // 初始化
        viewModelScope.launch {                          // 协程中收集
            prefs.permMode.collect { _mode.value = it }  // 同步模式
        }
    }

    /** 切换模式（HTML selectPermMode：选中即保存）。 */
    fun setMode(m: String) {                              // 切换模式
        _mode.value = m                                  // 立即更新 UI
        viewModelScope.launch { prefs.setPermMode(m) }   // 持久化
    }
}