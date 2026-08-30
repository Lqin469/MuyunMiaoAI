package com.memuo.feature.settings                         // 声明包名：设置业务模块

import android.app.ActivityManager                      // 导入 ActivityManager（已移至 core:device，此处仅兼容引用）
import android.content.Context                           // 导入 Context：应用上下文
import android.content.Intent                             // 导入 Intent：跳转设置页
import android.net.Uri                                   // 导入 Uri：官网链接
import android.provider.Settings                          // 导入 Settings：开发者选项跳转
import androidx.compose.animation.AnimatedVisibility     // 导入 AnimatedVisibility：显隐动画
import androidx.compose.animation.core.animateFloatAsState  // 导入 animateFloatAsState：浮点动画
import androidx.compose.animation.core.tween              // 导入 tween：动画时长
import androidx.compose.foundation.background             // 导入 background：背景修饰
import androidx.compose.foundation.clickable              // 导入 clickable：点击修饰
import androidx.compose.foundation.layout.Box             // 导入 Box：盒式布局
import androidx.compose.foundation.layout.Column          // 导入 Column：纵向布局
import androidx.compose.foundation.layout.Row             // 导入 Row：横向布局
import androidx.compose.foundation.layout.fillMaxSize     // 导入 fillMaxSize：铺满
import androidx.compose.foundation.layout.fillMaxWidth    // 导入 fillMaxWidth：占满宽度
import androidx.compose.foundation.layout.navigationBarsPadding  // 导入 navigationBarsPadding：底部手势条避让
import androidx.compose.foundation.layout.padding          // 导入 padding：内边距
import androidx.compose.foundation.layout.size            // 导入 size：固定尺寸
import androidx.compose.foundation.layout.statusBarsPadding  // 导入 statusBarsPadding：状态栏避让
import androidx.compose.foundation.rememberScrollState     // 导入 rememberScrollState：滚动状态
import androidx.compose.foundation.shape.RoundedCornerShape  // 导入 RoundedCornerShape：圆角形状
import androidx.compose.foundation.verticalScroll          // 导入 verticalScroll：纵向滚动
import androidx.compose.material3.Icon                    // 导入 Icon：图标
import androidx.compose.material3.MaterialTheme           // 导入 MaterialTheme：主题
import androidx.compose.material3.Text                    // 导入 Text：文本
import androidx.compose.runtime.Composable                // 导入 Composable：可组合函数注解
import androidx.compose.runtime.collectAsState            // 导入 collectAsState：状态流→状态
import androidx.compose.runtime.getValue                  // 导入 getValue：by 委托
import androidx.compose.ui.Alignment                      // 导入 Alignment：对齐
import androidx.compose.ui.Modifier                       // 导入 Modifier：修饰
import androidx.compose.ui.draw.clip                      // 导入 clip：裁剪
import androidx.compose.ui.draw.shadow                    // 导入 shadow：投影
import androidx.compose.ui.draw.scale                     // 导入 scale：缩放
import androidx.compose.ui.graphics.Color                 // 导入 Color：颜色
import androidx.compose.ui.platform.LocalContext          // 导入 LocalContext：上下文
import androidx.compose.ui.text.font.FontWeight           // 导入 FontWeight：字重
import androidx.compose.ui.unit.dp                        // 导入 dp：尺寸单位
import androidx.compose.ui.unit.sp                        // 导入 sp：字号单位
import androidx.hilt.navigation.compose.hiltViewModel     // 导入 hiltViewModel：Hilt 提供 ViewModel
import androidx.lifecycle.ViewModel                       // 导入 ViewModel：UI 数据持有者
import androidx.lifecycle.viewModelScope                  // 导入 viewModelScope：协程作用域
import com.memuo.core.device.CapabilityChecker             // 导入能力判定器（满足度规则）
import com.memuo.core.device.CheckLevel                    // 导入检测等级枚举
import com.memuo.core.device.DeviceInfoProvider            // 导入设备信息提供者（真实检测）
import com.memuo.core.search.privilege.PrivilegeManager    // 导入提权管理器（Shizuku/libsu 真检测）
import com.memuo.core.ui.AppIcons                          // 导入应用图标集
import com.memuo.core.ui.components.BrandButton            // 导入品牌主按钮
import com.memuo.core.ui.components.LocalToast            // 导入 Toast 状态
import com.memuo.core.ui.components.MuyunModal            // 导入弹窗容器
import com.memuo.core.ui.components.ModalCloseButton      // 导入弹窗关闭按钮
import com.memuo.core.ui.components.StatusPill            // 导入状态徽章
import com.memuo.core.ui.components.StatusTone            // 导入状态色调
import com.memuo.core.ui.theme.MuyunAccentLight           // 导入浅灰底
import com.memuo.core.ui.theme.MuyunBrand                 // 导入品牌色
import com.memuo.core.ui.theme.MuyunCard                  // 导入卡片白
import com.memuo.core.ui.theme.MuyunDanger                // 导入危险红
import com.memuo.core.ui.theme.MuyunDangerBg              // 导入危险红底
import com.memuo.core.ui.theme.MuyunGreen                 // 导入成功绿
import com.memuo.core.ui.theme.MuyunGreenBg               // 导入成功绿底
import com.memuo.core.ui.theme.MuyunText                  // 导入主文字色
import com.memuo.core.ui.theme.MuyunText2                 // 导入次级文字色
import com.memuo.core.ui.theme.MuyunText3                 // 导入三级文字色
import com.memuo.core.ui.theme.MuyunTitleGradient         // 导入标题渐变
import dagger.hilt.android.lifecycle.HiltViewModel        // 导入 HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext  // 导入 ApplicationContext
import kotlinx.coroutines.delay                            // 导入 delay：延迟
import kotlinx.coroutines.flow.MutableStateFlow            // 导入可变状态流
import kotlinx.coroutines.flow.StateFlow                  // 导入只读状态流
import kotlinx.coroutines.flow.asStateFlow                // 导入 asStateFlow
import kotlinx.coroutines.launch                           // 导入 launch：协程
import javax.inject.Inject                                // 导入 Inject：构造函数注入

/** 警示黄（WARN 等级专用，主题无关固定色）。 */
private val WarnAmber = Color(0xFFE6A23C)                 // 琥珀主色
private val WarnAmberBg = Color(0x33E6A23C)               // 琥珀浅底（20% 透明）

/** 自检项状态（含等级与说明：满足/受限/不满足）。 */
data class CheckItem(                                    // 自检项
    val label: String,                                   // 标签
    val value: String,                                   // 检测值
    val done: Boolean,                                   // 是否完成
    val level: CheckLevel = CheckLevel.PASS,             // 结论等级
    val hint: String = "",                               // 说明（不满足项原因）
)

/**
 * 设备自检页 —— 首次启动的真实硬件自检 + 高级权限授予（需求 1/2，M-027 真数据接入）。
 * 检测 8 项：设备型号、系统版本、64 位架构、CPU、运行内存、可用存储（core:device 真检测 +
 * 满足度判定）、Shizuku 权限、ROOT 权限（PrivilegeManager 真检测）。
 * 不满足项红徽章 + 原因说明；受限项黄徽章；权限区点击触发真实授权/引导弹窗（无线 ADB 步骤）。
 */
@Composable                                               // 可组合 UI 函数
fun DeviceCheckScreen(                                   // 设备自检页
    onNext: () -> Unit,                                  // 下一步回调（写标记 + 跳主页）
    viewModel: DeviceCheckViewModel = hiltViewModel(),   // Hilt 提供 ViewModel
) {
    val items by viewModel.items.collectAsState()        // 订阅自检项
    val done by viewModel.done.collectAsState()          // 订阅完成状态
    val permMode by viewModel.permMode.collectAsState()  // 订阅权限模式
    val permDetail by viewModel.permDetail.collectAsState()  // 订阅展开详情
    val adbGuide by viewModel.adbGuide.collectAsState()  // 订阅 ADB 引导弹窗
    val rootGuide by viewModel.rootGuide.collectAsState()  // 订阅 ROOT 引导弹窗
    val toast = LocalToast.current                       // 取全局 Toast
    val context = LocalContext.current                   // 取上下文（跳转用）

    Column(                                              // 纵向布局
        modifier = Modifier                             // 修饰
            .fillMaxSize()                              // 铺满
            .statusBarsPadding()                        // 顶部避开状态栏
            .navigationBarsPadding()                    // 底部避开手势条
            .verticalScroll(rememberScrollState())      // 可滚动
            .padding(horizontal = 24.dp),               // 左右内边距
    ) {
        Text(                                           // 大标题
            text = "设备自检",                           // 内容
            fontSize = 34.sp,                           // 34px
            fontWeight = FontWeight.Bold,               // 粗体
            style = MaterialTheme.typography.headlineLarge.copy(brush = MuyunTitleGradient),  // 渐变画刷
            modifier = Modifier.padding(top = 40.dp, bottom = 36.dp),  // 上下留白
        )
        items.forEachIndexed { index, item ->           // 遍历自检项
            val scale by animateFloatAsState(           // 完成弹跳动画
                targetValue = if (item.done) 1f else 0.6f,  // 完成放大
                animationSpec = tween(350),             // 350ms
                label = "checkPop",                     // 标签
            )
            Column(modifier = Modifier.fillMaxWidth()) {  // 每项容器（含 hint）
                Row(                                    // 检测行
                    modifier = Modifier                // 修饰
                        .fillMaxWidth()                // 占满宽度
                        .padding(vertical = 13.dp),    // 上下留白
                    verticalAlignment = Alignment.CenterVertically,  // 垂直居中
                ) {
                    Text(                               // 标签
                        text = item.label,              // 内容
                        fontSize = 16.sp,               // 16px
                        fontWeight = FontWeight.Medium, // 中粗
                        color = MuyunText,              // 主色
                        modifier = Modifier.weight(1f), // 占满
                    )
                    if (item.done) {                    // 完成
                        Text(                           // 检测值
                            text = item.value,          // 内容
                            fontSize = 13.sp,           // 13px
                            color = MuyunText3,         // 三级灰
                            modifier = Modifier.padding(end = 8.dp),  // 右留白
                        )
                        LevelBadge(scale = scale, level = item.level)  // 等级徽章（满足/受限/不满足）
                    }
                }
                if (item.done && item.level != CheckLevel.PASS && item.hint.isNotEmpty()) {  // 非满足项的说明
                    Text(                               // 原因说明
                        text = item.hint,               // 内容
                        fontSize = 12.sp,               // 12px
                        color = if (item.level == CheckLevel.FAIL) MuyunDanger else WarnAmber,  // 红/黄
                        lineHeight = 18.sp,             // 行距
                        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),  // 内边距
                    )
                }
            }
        }
        Box(                                            // 可运行提示
            modifier = Modifier                        // 修饰
                .fillMaxWidth()                        // 占满宽度
                .padding(top = 20.dp)                  // 上留白
                .clip(RoundedCornerShape(10.dp))       // 圆角 10
                .background(MuyunCard)                 // 卡片白
                .padding(horizontal = 16.dp, vertical = 14.dp),  // 内边距
        ) {
            Text(                                       // 提示文字（动态）
                text = viewModel.summaryText,           // 按检测结果动态生成
                fontSize = 14.sp,                       // 14px
                color = MuyunText2,                     // 次级灰
                lineHeight = 22.sp,                     // 行距
            )
        }
        // —— 高级权限区 ——
        Column(                                         // 权限卡片
            modifier = Modifier                        // 修饰
                .fillMaxWidth()                        // 占满宽度
                .padding(top = 26.dp)                  // 上留白
                .shadow(1.dp, RoundedCornerShape(14.dp))  // 轻投影
                .clip(RoundedCornerShape(14.dp))       // 圆角
                .background(MuyunCard)                 // 卡片白
                .padding(horizontal = 20.dp),          // 左右内边距
        ) {
            Text(                                       // 权限区标题
                text = "高级权限",                      // 内容
                fontSize = 15.sp,                       // 15px
                fontWeight = FontWeight.SemiBold,       // 半粗
                color = MuyunText,                      // 主色
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),  // 内边距
            )
            Text(                                       // 权限区副标题
                text = "可选。按需授权，随时可撤销",      // 文案
                fontSize = 12.sp,                       // 12px
                color = MuyunText3,                     // 三级灰
                modifier = Modifier.padding(bottom = 4.dp),  // 下留白
            )
            // ADB 权限行（真实 Shizuku 授权）
            PermButtonRow(                              // ADB 行
                icon = AppIcons.Code,                   // 代码图标
                name = "授予 ADB 权限",                 // 名称
                desc = "经 Shizuku 获取 ADB 能力，无需 ROOT",  // 描述
                statusText = viewModel.adbStatusText,   // 真实状态文案
                granted = viewModel.adbGranted,         // 是否已授予
                onToggle = { viewModel.grantAdb(context) },  // 真实授权/引导
            )
            AnimatedDetail(                             // 展开详情
                visible = permDetail,                   // 绑定
                text = "ADB 权限经 Shizuku 服务执行高级操作（文件检索、系统信息），无需 ROOT。授权流程：安装 Shizuku → 开发者选项开启无线调试 → Shizuku 配对并启动 → 返回本应用授权。",  // 文案
            )
            // ROOT 权限行（真实 root 检测）
            PermButtonRow(                              // ROOT 行
                icon = AppIcons.Terminal,               // 终端图标
                name = "授予 ROOT 权限",                // 名称
                desc = "最高系统权限，需设备已 ROOT（Sui）",  // 描述
                statusText = viewModel.rootStatusText,  // 真实状态文案
                granted = viewModel.rootGranted,        // 是否已授予
                onToggle = { viewModel.grantRoot() },   // 真实检测/引导
            )
            AnimatedDetail(                             // 展开详情
                visible = permDetail,                   // 绑定
                text = "ROOT 权限授予应用最高系统权限（全盘文件检索、系统级操作）。需要设备已 ROOT 并安装 Sui，在 Sui 中向本应用授权。",  // 文案
            )
        }
        // 底部按钮
        BrandButton(                                    // 下一步按钮
            text = if (done) "下一步" else "自检中…",     // 状态文字
            enabled = done,                             // 自检完可用
            onClick = onNext,                           // 下一步
            modifier = Modifier.padding(top = 26.dp, bottom = 20.dp),  // 上下留白
        )
    }

    // —— ADB 引导弹窗（未装/未启动 Shizuku 时）——
    MuyunModal(                                         // 引导弹窗
        visible = adbGuide,                             // 绑定
        onDismiss = { viewModel.closeAdbGuide() },      // 关闭
        title = "开启无线 ADB（Shizuku）",              // 标题
        headerActions = { ModalCloseButton { viewModel.closeAdbGuide() } },  // 关闭按钮
        body = {                                        // 主体：步骤清单
            GuideStep(1, "安装 Shizuku（应用商店或官网 shizuku.rikka.app）")  // 步骤 1
            GuideStep(2, "进入开发者选项 → 无线调试 → 打开")  // 步骤 2
            GuideStep(3, "打开 Shizuku → 配对 → 输入系统给出的配对码")  // 步骤 3
            GuideStep(4, "返回 Shizuku 首页 → 启动服务")  // 步骤 4
            GuideStep(5, "回到本应用 → 点击「授予 ADB 权限」")  // 步骤 5
        },
        footer = {                                      // 底部按钮
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {  // 按钮排
                androidx.compose.material3.OutlinedButton(  // 开发者选项
                    onClick = {                          // 点击跳转
                        runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) }  // 跳开发者选项
                    },
                    modifier = Modifier.weight(1f),     // 平分宽度
                ) { Text("开发者选项") }                  // 文案
                androidx.compose.material3.OutlinedButton(  // 官网
                    onClick = {                          // 点击打开官网
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/"))) }  // 浏览器打开
                    },
                    modifier = Modifier.weight(1f),     // 平分宽度
                ) { Text("打开官网") }                    // 文案
            }
        },
    )

    // —— ROOT 引导弹窗（无 root 时）——
    MuyunModal(                                         // 引导弹窗
        visible = rootGuide,                            // 绑定
        onDismiss = { viewModel.closeRootGuide() },     // 关闭
        title = "获取 ROOT 权限（Sui）",                // 标题
        headerActions = { ModalCloseButton { viewModel.closeRootGuide() } },  // 关闭按钮
        body = {                                        // 主体
            GuideStep(1, "设备需已完成 ROOT（Magisk/KernelSU 等）")  // 步骤 1
            GuideStep(2, "安装 Sui（Magisk 模块仓库可下载）")  // 步骤 2
            GuideStep(3, "打开 Sui → 向本应用授予权限")  // 步骤 3
            Text(                                       // 风险提示
                text = "ROOT 会降低系统安全性，请确认了解风险后操作。",  // 文案
                fontSize = 12.sp,                       // 12px
                color = MuyunDanger,                    // 红色警示
                modifier = Modifier.padding(top = 10.dp),  // 上留白
            )
        },
        footer = {                                      // 底部
            BrandButton(                                // 知道了
                text = "我知道了",                      // 文案
                onClick = { viewModel.closeRootGuide() },  // 关闭
            )
        },
    )
}

/** 等级徽章：满足绿 / 受限黄 / 不满足红。 */
@Composable                                               // 可组合函数
private fun LevelBadge(                                   // 等级徽章
    scale: Float,                                        // 弹跳缩放
    level: CheckLevel,                                   // 等级
) {
    val (bg, fg, text) = when (level) {                  // 按等级配色
        CheckLevel.PASS -> Triple(MuyunGreenBg, MuyunGreen, "满足")    // 满足绿
        CheckLevel.WARN -> Triple(WarnAmberBg, WarnAmber, "受限")      // 受限黄
        CheckLevel.FAIL -> Triple(MuyunDangerBg, MuyunDanger, "不满足")  // 不满足红
    }
    Box(                                                 // 徽章容器
        modifier = Modifier                             // 修饰
            .scale(scale)                               // 弹跳缩放
            .clip(RoundedCornerShape(20.dp))            // 胶囊
            .background(bg)                             // 浅底
            .padding(horizontal = 10.dp, vertical = 3.dp),  // 内边距
    ) {
        Text(                                            // 徽章文字
            text = text,                                 // 内容
            fontSize = 12.sp,                            // 12px
            fontWeight = FontWeight.SemiBold,            // 半粗
            color = fg,                                  // 前景色
        )
    }
}

/** 引导步骤行（编号 + 文案）。 */
@Composable                                               // 可组合函数
private fun GuideStep(                                    // 引导步骤
    no: Int,                                             // 步骤编号
    text: String,                                        // 步骤文案
) {
    Row(                                                 // 横向布局
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),  // 内边距
        verticalAlignment = Alignment.CenterVertically,  // 垂直居中
    ) {
        Box(                                             // 编号圆
            modifier = Modifier                         // 修饰
                .size(24.dp)                            // 24dp
                .clip(RoundedCornerShape(12.dp))        // 圆形
                .background(MuyunAccentLight)           // 品牌浅底
            ,
            contentAlignment = Alignment.Center,         // 居中
        ) {
            Text(                                        // 编号
                text = "$no",                            // 内容
                fontSize = 12.sp,                        // 12px
                fontWeight = FontWeight.Bold,            // 粗体
                color = MuyunBrand,                      // 品牌色
            )
        }
        Text(                                            // 步骤文案
            text = text,                                 // 内容
            fontSize = 13.sp,                            // 13px
            color = MuyunText2,                          // 次级灰
            lineHeight = 19.sp,                          // 行距
            modifier = Modifier.padding(start = 10.dp),  // 左留白
        )
    }
}

/** 权限行：图标 + 名称/描述 + 状态徽章。 */
@Composable                                               // 可组合函数
private fun PermButtonRow(                                // 权限行
    icon: androidx.compose.ui.graphics.vector.ImageVector,  // 图标
    name: String,                                         // 名称
    desc: String,                                         // 描述
    statusText: String,                                   // 状态文案（真实状态）
    granted: Boolean,                                     // 是否已授予
    onToggle: () -> Unit,                                 // 切换回调
) {
    Row(                                                  // 横向布局
        modifier = Modifier                              // 修饰
            .fillMaxWidth()                              // 占满宽度
            .clickable { onToggle() }                    // 点击切换
            .padding(vertical = 14.dp),                  // 上下 14
        verticalAlignment = Alignment.CenterVertically,   // 垂直居中
    ) {
        Box(                                              // 图标底
            modifier = Modifier                          // 修饰
                .size(34.dp)                             // 34dp
                .clip(RoundedCornerShape(10.dp))         // 圆角 10
                .background(if (granted) MuyunGreenBg else MuyunAccentLight),  // 已授予绿底/浅灰
            contentAlignment = Alignment.Center,          // 居中
        ) {
            Icon(                                         // 图标
                imageVector = icon,                       // 矢量
                contentDescription = name,                // 描述
                tint = if (granted) MuyunGreen else MuyunText2,  // 绿/灰
                modifier = Modifier.size(14.dp),          // 14dp
            )
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {  // 信息区
            Text(                                         // 名称
                text = name,                              // 内容
                fontSize = 14.sp,                         // 14px
                fontWeight = FontWeight.Medium,           // 中粗
                color = MuyunText,                        // 主色
            )
            Text(                                         // 描述
                text = desc,                              // 内容
                fontSize = 11.sp,                         // 11px
                color = MuyunText3,                       // 三级灰
                modifier = Modifier.padding(top = 3.dp),  // 上留白
            )
        }
        StatusPill(                                       // 状态徽章
            text = statusText,                            // 真实状态文案
            tone = if (granted) StatusTone.SUCCESS else StatusTone.NEUTRAL,  // 色调
        )
    }
}

/** 展开详情。 */
@Composable                                               // 可组合函数
private fun AnimatedDetail(                               // 展开详情
    visible: Boolean,                                     // 是否展开
    text: String,                                         // 内容
) {
    AnimatedVisibility(visible = visible) {               // 显隐动画
        Box(                                              // 详情容器
            modifier = Modifier                          // 修饰
                .fillMaxWidth()                          // 占满宽度
                .padding(vertical = 8.dp)                // 上下留白
                .clip(RoundedCornerShape(8.dp))          // 圆角 8
                .background(MuyunAccentLight)            // 浅灰底
                .padding(12.dp),                          // 内边距
        ) {
            Text(                                         // 详情文字
                text = text,                              // 内容
                fontSize = 12.sp,                         // 12px
                color = MuyunText3,                       // 三级灰
                lineHeight = 20.sp,                       // 行距
            )
        }
    }
}

/**
 * 设备自检 ViewModel —— 真实硬件检测（core:device）+ 真实权限检测（PrivilegeManager）。
 */
@HiltViewModel                                           // 注解：由 Hilt 创建
class DeviceCheckViewModel @Inject constructor(          // 构造函数注入
    @ApplicationContext private val context: Context,    // 注入应用上下文
    private val prefs: ExtPrefs,                         // 注入扩展偏好（权限模式）
    private val deviceInfo: DeviceInfoProvider,          // 注入设备信息提供者（真检测）
    private val capability: CapabilityChecker,           // 注入能力判定器（满足度）
    private val privilege: PrivilegeManager,             // 注入提权管理器（Shizuku/libsu）
) : ViewModel() {                                        // 继承 ViewModel

    private val _items = MutableStateFlow(               // 自检项（初始未完成，8 项）
        listOf(
            CheckItem("设备型号", "--", false),          // 型号
            CheckItem("系统版本", "--", false),          // 系统
            CheckItem("64 位架构", "--", false),         // 架构
            CheckItem("CPU 核心", "--", false),          // CPU
            CheckItem("运行内存", "--", false),          // 内存
            CheckItem("可用存储", "--", false),          // 存储
            CheckItem("Shizuku 权限", "--", false),      // ADB
            CheckItem("ROOT 权限", "--", false),         // ROOT
        )
    )
    val items: StateFlow<List<CheckItem>> = _items.asStateFlow()  // 只读暴露
    private val _done = MutableStateFlow(false)          // 是否全部完成
    val done: StateFlow<Boolean> = _done.asStateFlow()   // 只读暴露
    private val _permMode = MutableStateFlow("basic")    // 权限模式
    val permMode: StateFlow<String> = _permMode.asStateFlow()  // 只读暴露
    private val _permDetail = MutableStateFlow(false)    // 详情展开
    val permDetail: StateFlow<Boolean> = _permDetail.asStateFlow()  // 只读暴露
    private val _adbGuide = MutableStateFlow(false)      // ADB 引导弹窗
    val adbGuide: StateFlow<Boolean> = _adbGuide.asStateFlow()  // 只读暴露
    private val _rootGuide = MutableStateFlow(false)     // ROOT 引导弹窗
    val rootGuide: StateFlow<Boolean> = _rootGuide.asStateFlow()  // 只读暴露

    private var snapshotValues = emptyList<CheckResultHolder>()  // 硬件检测结果缓存（权限项刷新时重建清单）

    /** 单项目检测结果持有（内部缓存用）。 */
    private data class CheckResultHolder(                // 缓存结构
        val label: String,                               // 标签
        val value: String,                               // 实测值
        val level: CheckLevel,                           // 等级
        val hint: String,                                // 说明
    )

    /** ADB 状态文案（真实：服务未运行/未授权/已授权）。 */
    val adbStatusText: String get() = when {             // 按状态生成
        privilege.currentLevel() == PrivilegeManager.Level.SHIZUKU_ROOT -> "ROOT 模式"  // root 已含 ADB 能力
        privilege.currentLevel() == PrivilegeManager.Level.SHIZUKU_ADB -> if (privilege.hasPermission()) "已授予" else "未授权"  // adb 在线
        else -> "未运行"                                  // 服务未启动
    }

    /** ADB 是否已授予（含 root 模式）。 */
    val adbGranted: Boolean get() =                       // 已授予判定
        privilege.currentLevel() != PrivilegeManager.Level.NONE && privilege.hasPermission()  // 服务在线且已授权

    /** ROOT 状态文案。 */
    val rootStatusText: String get() =                    // 状态文案
        if (privilege.currentLevel() == PrivilegeManager.Level.SHIZUKU_ROOT) "已授予" else "未授予"  // root 判定

    /** ROOT 是否已授予。 */
    val rootGranted: Boolean get() =                      // 已授予判定
        privilege.currentLevel() == PrivilegeManager.Level.SHIZUKU_ROOT  // SHIZUKU_ROOT 等级

    /** 动态总结文案（按检测结果生成）。 */
    val summaryText: String get() {                       // 总结文案
        val results = _items.value.filter { it.done }    // 已完成项
        return when {                                    // 按最坏情况
            results.any { it.level == CheckLevel.FAIL } -> "存在不满足项：相关能力不可用（详见红色标注）。云端 AI 与常规备忘功能不受影响。"  // 有不满足
            results.any { it.level == CheckLevel.WARN } -> "本机可用，部分功能受限（详见黄色标注），建议按提示调整。"  // 有受限
            else -> "本机可运行端侧模型，可启用本地私人管家"  // 全满足
        }
    }

    init {                                                // 初始化
        viewModelScope.launch { prefs.permMode.collect { _permMode.value = it } }  // 加载权限模式
        // 订阅提权状态（服务上下线/授权变化 → 刷新权限两项）
        viewModelScope.launch { privilege.level.collect { refreshPrivilegeItems() } }  // 等级变化
        viewModelScope.launch { privilege.authorized.collect { refreshPrivilegeItems() } }  // 授权变化
        runCheck()                                        // 开始自检
    }

    /** 逐项自检（硬件 6 项真检测，权限 2 项实时）。 */
    private fun runCheck() {                              // 自检
        viewModelScope.launch {                           // 协程
            val snapshot = deviceInfo.snapshot()          // ① 真实设备快照
            val results = capability.checkAll(snapshot)   // ② 满足度判定（5 项）
            // 组装硬件 6 项（型号/系统为信息项 + 判定 5 项）
            val holders = mutableListOf(                  // 结果缓存
                CheckResultHolder("设备型号", snapshot.displayModel, CheckLevel.PASS, ""),  // 型号
                CheckResultHolder("系统版本", "Android ${snapshot.androidVersion} (API ${snapshot.sdkInt})", CheckLevel.PASS, ""),  // 系统
            )
            results.forEach { r -> holders += CheckResultHolder(r.label, r.value, r.level, r.hint) }  // 判定项
            snapshotValues = holders                      // 缓存
            publishItems()                                // 发布 8 项（含权限）
        }
    }

    /** 按缓存 + 实时权限状态重建 8 项清单。 */
    private fun refreshPrivilegeItems() {                 // 刷新权限项
        if (snapshotValues.isEmpty()) return             // 硬件未检完
        publishItems()                                    // 重建发布
    }

    /** 发布完整清单（硬件缓存 + 权限实时项）。 */
    private fun publishItems() {                          // 发布清单
        val perm = privilege.currentLevel()               // 当前提权等级
        val list = snapshotValues.map { h ->              // 硬件项
            CheckItem(h.label, h.value, true, h.level, h.hint)  // 已完成
        } + listOf(                                       // 权限 2 项
            CheckItem("Shizuku 权限", adbStatusText, true, if (adbGranted) CheckLevel.PASS else CheckLevel.WARN, if (adbGranted) "" else "未开启：文件检索等高级能力不可用，点击下方「授予 ADB 权限」开启"),  // ADB
            CheckItem("ROOT 权限", rootStatusText, true, if (rootGranted) CheckLevel.PASS else CheckLevel.WARN, if (rootGranted) "" else "未授予：全盘检索等最高能力不可用（可选）"),  // ROOT
        )
        _items.value = list                               // 发布
        _done.value = true                                // 完成（检测不阻塞进入）
    }

    /** 授予/撤销 ADB：真实请求 Shizuku 授权；服务未运行则弹引导。 */
    fun grantAdb(context: Context) {                      // ADB 授权
        if (adbGranted) {                                 // 已授予
            _permDetail.value = true                      // 展开详情（含撤销路径说明）
            return
        }
        if (privilege.currentLevel() == PrivilegeManager.Level.NONE) {  // 服务未运行
            _adbGuide.value = true                        // 弹无线 ADB 引导
            return
        }
        privilege.requestAdbPermission { ok ->            // 发起真实授权请求
            if (!ok) _adbGuide.value = true               // 失败弹引导
        }
        _permDetail.value = true                          // 展开详情
    }

    /** 授予/撤销 ROOT：真实检测；无 root 则弹 Sui 引导。 */
    fun grantRoot() {                                     // ROOT 授权
        if (rootGranted) {                                // 已授予
            _permDetail.value = true                      // 展开详情
            return
        }
        _rootGuide.value = true                           // 弹 ROOT 引导
    }

    /** 关闭 ADB 引导弹窗。 */
    fun closeAdbGuide() { _adbGuide.value = false }       // 关闭

    /** 关闭 ROOT 引导弹窗。 */
    fun closeRootGuide() { _rootGuide.value = false }     // 关闭
}
