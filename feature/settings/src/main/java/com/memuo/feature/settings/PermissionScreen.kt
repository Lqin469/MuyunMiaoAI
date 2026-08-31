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
import androidx.compose.runtime.LaunchedEffect            // 导入 LaunchedEffect：副作用（提示→Toast）
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
import com.memuo.core.search.privilege.PrivilegeManager  // 导入提权管理器：真实权限验证/授权
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
    val level by viewModel.level.collectAsState()        // 订阅真实能力等级
    val message by viewModel.message.collectAsState()    // 订阅切换结果提示
    val toast = LocalToast.current                       // 取全局 Toast

    LaunchedEffect(message) {                            // 切换结果 → Toast
        message?.let { toast.show(it); viewModel.consumeMessage() }  // 弹提示并消费
    }

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
                            onClick = { viewModel.setMode(m) },  // 点击切换（真实验证权限，结果经 message 提示）
                        )
                    }
                }
                Text(                                     // 当前真实权限等级（实测，非用户选择值）
                    text = "当前真实权限：${levelLabel(level)}",  // 实测等级
                    style = MaterialTheme.typography.labelSmall,  // 小字
                    color = if (level == PrivilegeManager.Level.NONE) MuyunText2 else MuyunGreen,  // 无权限灰/有权限绿
                    modifier = Modifier.padding(top = 12.dp, start = 4.dp, end = 4.dp),  // 内边距
                )
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

/** 能力等级 → 中文展示（页面「当前真实权限」显示用）。 */
private fun levelLabel(level: PrivilegeManager.Level): String = when (level) {  // 等级转中文
    PrivilegeManager.Level.NONE -> "无（仅基础权限）"        // 无提权
    PrivilegeManager.Level.SHIZUKU_ADB -> "Shizuku ADB"    // adb 提权
    PrivilegeManager.Level.SHIZUKU_ROOT -> "Shizuku ROOT"  // root 提权
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

/** 权限管理 ViewModel —— 模式读写 + 真实权限验证（切换时校验是否真有该权限）。 */
@HiltViewModel                                           // 注解：由 Hilt 创建
class PermissionViewModel @Inject constructor(           // 构造函数注入
    private val prefs: ExtPrefs,                         // 注入扩展偏好
    private val privilege: PrivilegeManager,             // 注入提权管理器（真实权限检测/授权）
) : ViewModel() {                                        // 继承 ViewModel

    private val _mode = MutableStateFlow("basic")        // 当前模式（默认基础）
    val mode: StateFlow<String> = _mode.asStateFlow()    // 只读暴露

    /** 真实能力等级（Shizuku-adb / Shizuku-root / NONE），页面实时展示。 */
    val level: StateFlow<PrivilegeManager.Level> = privilege.level  // 真实等级状态流

    /** 切换结果提示（验证成功/失败原因）。 */
    private val _message = MutableStateFlow<String?>(null)  // 提示消息（null=无）
    val message: StateFlow<String?> = _message.asStateFlow()  // 只读暴露

    init {                                                // 初始化
        viewModelScope.launch {                          // 协程中收集
            prefs.permMode.collect { _mode.value = it }  // 同步已保存模式
        }
    }

    /** 消费提示消息（Toast 展示后置空，避免重复弹）。 */
    fun consumeMessage() { _message.value = null }        // 清空消息

    /** 切换模式：真实验证权限，验证通过才切换并持久化，否则提示失败原因。 */
    fun setMode(m: String) {                              // 切换模式
        when (m) {                                       // 按目标模式分支
            "basic" -> applyMode(m, "已切换为基础权限")   // 基础：无需权限，直接切
            "adb" -> requestAdb()                        // ADB：经 Shizuku 授权
            "root" -> verifyRoot()                       // ROOT：验证 root 权限
        }
    }

    /** 请求 ADB 权限（Shizuku 授权）；已授权则直接切，否则弹 Shizuku 授权页。 */
    private fun requestAdb() {                            // ADB 授权
        if (privilege.hasPermission()) {                 // 已获 Shizuku 授权
            applyMode("adb", "已启用 ADB 权限")           // 直接切换
        } else {                                         // 未授权
            privilege.requestAdbPermission { granted ->  // 弹 Shizuku 授权页
                if (granted) applyMode("adb", "已启用 ADB 权限（Shizuku 授权成功）")  // 授权成功
                else _message.value = "ADB 权限启用失败：Shizuku 未授权或服务未启动"  // 授权失败
            }
        }
    }

    /** 验证 ROOT 权限：需设备已 root 且 Shizuku 在线（SHIZUKU_ROOT 等级）。 */
    private fun verifyRoot() {                            // ROOT 验证
        if (privilege.currentLevel() == PrivilegeManager.Level.SHIZUKU_ROOT) {  // 已达 root 等级
            applyMode("root", "已启用 ROOT 权限")          // 切换
        } else {                                         // 无 root
            _message.value = "ROOT 权限启用失败：设备未 ROOT 或未授予 root 权限（请先确认设备已 ROOT 并在 Shizuku 中授权）"  // 失败原因
        }
    }

    /** 应用模式：更新 UI + 持久化 + 发提示。 */
    private fun applyMode(m: String, msg: String) {       // 应用模式
        _mode.value = m                                  // 更新 UI
        viewModelScope.launch { prefs.setPermMode(m) }   // 持久化
        _message.value = msg                             // 发提示
    }
}