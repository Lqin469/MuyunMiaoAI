package com.memuo.feature.settings                         // 声明包名：设置业务模块

import androidx.activity.compose.rememberLauncherForActivityResult  // 导入 rememberLauncherForActivityResult：SAF 选择器
import androidx.activity.result.contract.ActivityResultContracts  // 导入 ActivityResultContracts：文件选择契约
import androidx.compose.foundation.background             // 导入 background：背景修饰
import androidx.compose.foundation.clickable              // 导入 clickable：点击修饰
import androidx.compose.foundation.layout.Box             // 导入 Box：盒式布局
import androidx.compose.foundation.layout.Column          // 导入 Column：纵向布局
import androidx.compose.foundation.layout.Row             // 导入 Row：横向布局
import androidx.compose.foundation.layout.fillMaxWidth    // 导入 fillMaxWidth：占满宽度
import androidx.compose.foundation.layout.height          // 导入 height：固定高度
import androidx.compose.foundation.layout.padding          // 导入 padding：内边距
import androidx.compose.foundation.layout.size            // 导入 size：固定尺寸
import androidx.compose.foundation.shape.CircleShape       // 导入 CircleShape：圆形
import androidx.compose.foundation.shape.RoundedCornerShape  // 导入 RoundedCornerShape：圆角形状
import androidx.compose.material3.Icon                    // 导入 Icon：图标
import androidx.compose.material3.LinearProgressIndicator   // 导入 LinearProgressIndicator：进度条
import androidx.compose.material3.MaterialTheme           // 导入 MaterialTheme：主题
import androidx.compose.material3.Text                    // 导入 Text：文本
import androidx.compose.runtime.Composable                // 导入 Composable：可组合函数注解
import androidx.compose.runtime.LaunchedEffect            // 导入 LaunchedEffect：副作用
import androidx.compose.runtime.collectAsState            // 导入 collectAsState：状态流→状态
import androidx.compose.runtime.getValue                  // 导入 getValue：by 委托
import androidx.compose.runtime.mutableStateOf            // 导入 mutableStateOf：可变状态
import androidx.compose.runtime.remember                  // 导入 remember：记住状态
import androidx.compose.runtime.setValue                  // 导入 setValue：by 委托写
import androidx.compose.ui.Alignment                      // 导入 Alignment：对齐
import androidx.compose.ui.Modifier                       // 导入 Modifier：修饰
import androidx.compose.ui.draw.clip                      // 导入 clip：裁剪
import androidx.compose.ui.platform.LocalContext          // 导入 LocalContext：上下文
import androidx.compose.ui.text.font.FontWeight           // 导入 FontWeight：字重
import androidx.compose.ui.text.style.TextAlign           // 导入 TextAlign：文字对齐
import androidx.compose.ui.text.style.TextOverflow        // 导入 TextOverflow：溢出省略
import androidx.compose.ui.unit.dp                        // 导入 dp：尺寸单位
import androidx.hilt.navigation.compose.hiltViewModel     // 导入 hiltViewModel：Hilt 提供 ViewModel
import androidx.lifecycle.ViewModel                       // 导入 ViewModel：UI 数据持有者
import androidx.lifecycle.viewModelScope                  // 导入 viewModelScope：协程作用域
import com.memuo.core.lan.LanDevice                        // 导入局域网设备模型
import com.memuo.core.lan.SessionState                     // 导入会话状态枚举
import com.memuo.core.lan.TransferRepository               // 导入传输编排器（真传输）
import com.memuo.core.ui.AppIcons                          // 导入应用图标集
import com.memuo.core.ui.components.BrandButton            // 导入品牌主按钮
import com.memuo.core.ui.components.LocalToast            // 导入 Toast 状态
import com.memuo.core.ui.components.ModalCloseButton      // 导入弹窗关闭按钮
import com.memuo.core.ui.components.MuyunModal            // 导入弹窗容器
import com.memuo.core.ui.components.MuyunSegmented        // 导入分段胶囊
import com.memuo.core.ui.components.StatusBar             // 导入状态条
import com.memuo.core.ui.components.StatusTone            // 导入状态色调
import com.memuo.core.ui.theme.MuyunAccentLight           // 导入浅灰底
import com.memuo.core.ui.theme.MuyunBrand                 // 导入品牌色
import com.memuo.core.ui.theme.MuyunCard                  // 导入卡片白
import com.memuo.core.ui.theme.MuyunGreen                 // 导入成功绿
import com.memuo.core.ui.theme.MuyunText                  // 导入主文字色
import com.memuo.core.ui.theme.MuyunText2                 // 导入次级文字色
import com.memuo.core.ui.theme.MuyunText3                 // 导入三级文字色
import dagger.hilt.android.lifecycle.HiltViewModel        // 导入 HiltViewModel
import kotlinx.coroutines.Dispatchers                      // 导入 Dispatchers：IO 调度器
import kotlinx.coroutines.flow.MutableStateFlow            // 导入可变状态流
import kotlinx.coroutines.flow.StateFlow                  // 导入只读状态流
import kotlinx.coroutines.flow.asStateFlow                // 导入 asStateFlow
import kotlinx.coroutines.launch                           // 导入 launch：协程
import kotlinx.coroutines.withContext                      // 导入 withContext：切线程
import java.io.File                                        // 导入 File：文件操作
import javax.inject.Inject                                // 导入 Inject：构造函数注入

/** 局域网传输类型枚举。 */
enum class LanType { TEXT, FILE, IMAGE }                  // 三种类型

/**
 * 局域网传输弹窗 —— 真实设备发现（NSD）+ 真实文件传输（TCP 断点续传）（需求 3，M-027 真实现）。
 * 原 HTML 版为模拟（随机设备/假进度），本版接入 core:lan：
 *  - 设备列表：NSD 实时发现（本机 + 局域网内其他沐云杪设备）；
 *  - 文件选择：系统 SAF 文件选择器（文本/文件/图片）；
 *  - 传输：真实 TCP 流式发送，实时进度/速度，支持暂停（断点保留）与恢复；
 *  - 接收：本机 TransferServer 自动接收（transfer/in 目录）。
 */
@Composable                                               // 可组合 UI 函数
fun LanTransferDialog(                                   // 局域网传输弹窗
    visible: Boolean,                                    // 是否显示
    onDismiss: () -> Unit,                               // 关闭回调
    viewModel: LanViewModel = hiltViewModel(),           // Hilt 提供 ViewModel
) {
    val type by viewModel.type.collectAsState()          // 类型
    val devices by viewModel.devices.collectAsState()    // 设备（真实 NSD）
    val sendDevice by viewModel.sendDevice.collectAsState()  // 发送设备
    val refreshing by viewModel.scanning.collectAsState()  // 扫描中（透传 NSD）
    val status by viewModel.status.collectAsState()      // 状态
    val session by viewModel.session.collectAsState()    // 传输会话（进度/速度）
    val settingsVisible by viewModel.settingsVisible.collectAsState()  // 设置弹窗
    val receiveMode by viewModel.receiveMode.collectAsState()  // 接收方式
    val savePath by viewModel.savePath.collectAsState()  // 保存路径
    val toast = LocalToast.current                       // 取全局 Toast
    val context = LocalContext.current                   // 取上下文
    var text by remember { mutableStateOf("") }          // 文本内容
    var picked by remember { mutableStateOf<File?>(null) }  // 已选文件（真实路径）

    // SAF 文件选择器（文本/文件/图片共用，按类型给 MIME 提示）
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->  // 选择器
        uri?.let { u ->                                  // 选中
            picked = viewModel.cachePicked(context, u)   // 缓存到应用目录（避免 Uri 权限过期）
        }
    }

    LaunchedEffect(visible) {                            // 弹窗打开时
        if (visible) viewModel.open() else viewModel.close()  // 开启/停止传输服务
    }

    MuyunModal(                                          // 主弹窗
        visible = visible,                               // 绑定
        onDismiss = onDismiss,                           // 关闭
        title = "局域网传输",                            // 标题
        headerActions = {                                // 标题栏右侧（设置 + 关闭）
            Box(                                         // 设置按钮
                modifier = Modifier                     // 修饰
                    .size(32.dp)                        // 32dp
                    .clip(CircleShape)                  // 圆形
                    .clickable { viewModel.openSettings() },  // 打开设置
                contentAlignment = Alignment.Center,     // 居中
            ) {
                Icon(                                    // 齿轮图标
                    imageVector = AppIcons.Gear,         // 图标
                    contentDescription = "传输设置",       // 描述
                    tint = MuyunText3,                   // 三级灰
                    modifier = Modifier.size(17.dp),     // 17dp
                )
            }
            ModalCloseButton { onDismiss() }             // 关闭按钮
        },
        body = {                                        // 弹窗主体
            MuyunSegmented(                              // 类型 tabs
                labels = listOf("文本传输", "文件传输", "图片传输"),  // 三段
                selectedIndex = type.ordinal,            // 当前
                onSelect = { viewModel.setType(LanType.entries[it]); picked = null },  // 切换（清选择）
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),  // 下留白
            )
            if (type == LanType.TEXT) {                  // 文本 → textarea
                Box(                                     // 文本输入框
                    modifier = Modifier                 // 修饰
                        .fillMaxWidth()                 // 占满宽度
                        .height(72.dp)                  // 高 72
                        .clip(RoundedCornerShape(10.dp))  // 圆角
                        .background(MuyunCard)          // 白底
                        .padding(horizontal = 14.dp, vertical = 12.dp),  // 内边距
                ) {
                    androidx.compose.foundation.text.BasicTextField(  // 无边框输入
                        value = text,                     // 绑定
                        onValueChange = { text = it },    // 更新
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MuyunText),  // 字体
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(MuyunBrand),  // 光标
                        modifier = Modifier.fillMaxWidth(),  // 占满
                        decorationBox = { inner ->        // 占位
                            if (text.isEmpty()) {         // 空
                                Text("输入要发送的文本内容…", color = MuyunText3, style = MaterialTheme.typography.bodyMedium)  // 占位
                            }
                            inner()                       // 输入区
                        },
                    )
                }
            } else {                                      // 文件/图片 → 选择区
                Box(                                      // 选择区
                    modifier = Modifier                 // 修饰
                        .fillMaxWidth()                 // 占满宽度
                        .padding(vertical = 18.dp, horizontal = 14.dp)  // 内边距
                        .clip(RoundedCornerShape(10.dp))  // 圆角
                        .background(MuyunCard)          // 白底
                        .clickable {                      // 点击打开系统选择器
                            picker.launch(if (type == LanType.FILE) "*/*" else "image/*")  // 按类型过滤
                        },
                    contentAlignment = Alignment.Center,  // 居中
                ) {
                    Text(                                // 选择提示
                        text = picked?.let { "已选择：${it.name}（${viewModel.fmtSize(it.length())}），点击重新选择" } ?: "点击选择要传输的${if (type == LanType.FILE) "文件" else "图片"}",  // 文案
                        style = MaterialTheme.typography.bodySmall,  // 字号
                        color = MuyunText3,              // 三级灰
                        textAlign = TextAlign.Center,    // 居中
                    )
                }
            }
            // 接收设备区（本机固定，TransferServer 自动接收）
            Row(                                          // 区头
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 8.dp),  // 内边距
            ) {
                Text(                                     // 标题
                    text = "接收设备",                    // 内容
                    style = MaterialTheme.typography.bodySmall,  // 字号
                    fontWeight = FontWeight.SemiBold,    // 半粗
                    color = MuyunText2,                  // 次级灰
                    modifier = Modifier.weight(1f),      // 占满
                )
                Box(                                      // 默认本机标签
                    modifier = Modifier                 // 修饰
                        .clip(RoundedCornerShape(14.dp))  // 胶囊
                        .background(MuyunAccentLight)   // 浅灰底
                        .padding(horizontal = 12.dp, vertical = 4.dp),  // 内边距
                ) {
                    Text(                                 // 文字
                        text = "默认本机",                // 内容
                        style = MaterialTheme.typography.labelSmall,  // 小字
                        fontWeight = FontWeight.Medium,  // 中粗
                        color = MuyunText3,              // 三级灰
                    )
                }
            }
            LanDeviceRow(                                 // 本机行
                device = LanDevice("本机（接收中）", "", 0, "1"),  // 本机
                selected = true,                          // 固定选中
                statusText = "自动接收",                  // 状态文字
                onClick = {},                             // 不可点击
            )
            // 发送设备区（真实 NSD 设备）
            Row(                                          // 区头
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 8.dp),  // 内边距
                verticalAlignment = Alignment.CenterVertically,  // 垂直居中
            ) {
                Text(                                     // 标题
                    text = "发送设备",                    // 内容
                    style = MaterialTheme.typography.bodySmall,  // 字号
                    fontWeight = FontWeight.SemiBold,    // 半粗
                    color = MuyunText2,                  // 次级灰
                    modifier = Modifier.weight(1f),      // 占满
                )
                Box(                                      // 刷新按钮（重新扫描）
                    modifier = Modifier                 // 修饰
                        .clip(RoundedCornerShape(14.dp))  // 胶囊
                        .background(MuyunAccentLight)   // 浅灰底
                        .clickable(enabled = !refreshing) { viewModel.refresh() }  // 点击重扫
                        .padding(horizontal = 12.dp, vertical = 4.dp),  // 内边距
                ) {
                    Text(                                 // 文字
                        text = if (refreshing) "扫描中…" else "刷新",  // 状态文字
                        style = MaterialTheme.typography.labelSmall,  // 小字
                        fontWeight = FontWeight.Medium,  // 中粗
                        color = MuyunBrand,              // 品牌色
                    )
                }
            }
            if (devices.isEmpty()) {                      // 无设备
                Text(                                     // 空提示
                    text = "未发现设备。请确保两台设备在同一 Wi-Fi 且都打开本应用",  // 文案
                    style = MaterialTheme.typography.labelSmall,  // 小字
                    color = MuyunText3,                   // 三级灰
                    textAlign = TextAlign.Center,         // 居中
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),  // 内边距
                )
            }
            devices.forEach { d ->                        // 遍历真实设备
                LanDeviceRow(                             // 设备行
                    device = d,                           // 数据
                    selected = sendDevice?.ip == d.ip,    // 是否选中
                    statusText = "在线",                  // 状态（NSD 发现即在线）
                    onClick = { viewModel.selectDevice(d) },  // 选择
                )
            }
            // 状态条
            StatusBar(                                    // 状态
                text = status.first,                      // 文案
                tone = status.second,                     // 色调
                modifier = Modifier.padding(top = 8.dp),  // 上留白
            )
            // 进度条（真实会话进度）
            val s = session                              // 会话快照
            if (s != null && s.size > 0 && s.state != SessionState.SUCCESS) {  // 传输中/暂停
                LinearProgressIndicator(                  // 进度条
                    progress = { (s.sent.toFloat() / s.size).coerceIn(0f, 1f) },  // 进度
                    modifier = Modifier                  // 修饰
                        .fillMaxWidth()                  // 占满宽度
                        .height(6.dp)                    // 高 6
                        .padding(top = 10.dp)            // 上留白
                        .clip(RoundedCornerShape(3.dp)), // 圆角 3
                    color = MuyunBrand,                   // 品牌色
                    trackColor = MuyunAccentLight,        // 轨道浅灰
                )
                Text(                                     // 进度文字（字节 + 速度）
                    text = "${viewModel.fmtSize(s.sent)} / ${viewModel.fmtSize(s.size)} · ${viewModel.fmtSize(s.speedBps)}/s",  // 文案
                    style = MaterialTheme.typography.labelSmall,  // 小字
                    color = MuyunText3,                   // 三级灰
                    modifier = Modifier.padding(top = 4.dp),  // 上留白
                )
            }
        },
        footer = {                                       // 底部按钮（按会话状态切换 开始/暂停/继续）
            if (session?.state == SessionState.RUNNING) {  // 传输中 → 暂停
                BrandButton(                             // 暂停按钮
                    text = "暂停传输",                    // 文案
                    onClick = { viewModel.pause() },      // 暂停（断点保留）
                )
            } else if (session?.state == SessionState.PAUSED) {  // 已暂停 → 继续
                BrandButton(                             // 继续按钮
                    text = "继续传输",                    // 文案
                    onClick = { viewModel.resume() },     // 从断点继续
                )
            } else {                                     // 空闲 → 开始
                BrandButton(                             // 开始传输
                    text = "开始传输",                    // 文案
                    onClick = {                          // 点击传输
                        when (type) {                    // 校验
                            LanType.TEXT -> if (text.isBlank()) { toast.show("请输入要传输的文本"); return@BrandButton }  // 文本校验
                            else -> if (picked == null) { toast.show(if (type == LanType.FILE) "请先选择文件" else "请先选择图片"); return@BrandButton }  // 文件校验
                        }
                        viewModel.startTransfer(type, text, picked)  // 真实发送
                    },
                )
            }
        },
    )

    // —— 传输设置弹窗 ——
    MuyunModal(                                          // 设置弹窗
        visible = settingsVisible,                       // 绑定
        onDismiss = { viewModel.closeSettings() },       // 关闭
        title = "传输设置",                              // 标题
        headerActions = { ModalCloseButton { viewModel.closeSettings() } },  // 关闭按钮
        body = {                                        // 主体
            Text(                                        // 接收方式标题
                text = "接收方式",                       // 内容
                style = MaterialTheme.typography.bodySmall,  // 字号
                fontWeight = FontWeight.SemiBold,        // 半粗
                color = MuyunText2,                      // 次级灰
                modifier = Modifier.padding(bottom = 8.dp),  // 下留白
            )
            MuyunSegmented(                              // 接收方式分段
                labels = listOf("手动接收", "自动保存"),  // 两段
                selectedIndex = if (receiveMode == "auto") 1 else 0,  // 当前
                onSelect = { viewModel.setReceiveMode(if (it == 1) "auto" else "manual") },  // 切换
                modifier = Modifier.fillMaxWidth(),      // 占满
            )
            Text(                                        // 方式说明
                text = if (receiveMode == "manual") "收到文件后弹窗询问是否保存（下个版本提供）" else "收到文件后自动保存到应用传输目录",  // 文案
                style = MaterialTheme.typography.labelSmall,  // 小字
                color = MuyunText3,                      // 三级灰
                lineHeight = MaterialTheme.typography.labelSmall.lineHeight * 1.4f,  // 行距
                modifier = Modifier.padding(top = 8.dp, bottom = 14.dp),  // 内边距
            )
            Text(                                        // 保存路径标题
                text = "接收目录",                       // 内容
                style = MaterialTheme.typography.bodySmall,  // 字号
                fontWeight = FontWeight.SemiBold,        // 半粗
                color = MuyunText2,                      // 次级灰
                modifier = Modifier.padding(bottom = 8.dp),  // 下留白
            )
            Box(                                          // 路径展示（只读：统一应用传输目录）
                modifier = Modifier                       // 修饰
                    .fillMaxWidth()                       // 占满
                    .clip(RoundedCornerShape(10.dp))      // 圆角
                    .background(MuyunCard)                // 白底
                    .padding(horizontal = 14.dp, vertical = 11.dp),  // 内边距
            ) {
                Text(                                     // 路径文字
                    text = savePath,                      // 内容
                    style = MaterialTheme.typography.labelMedium,  // 小字
                    color = MuyunText,                    // 主色
                    maxLines = 1,                         // 单行
                    overflow = TextOverflow.Ellipsis,     // 省略
                )
            }
        },
        footer = {                                       // 底部
            BrandButton(                                 // 保存
                text = "保存",                           // 文字
                onClick = { viewModel.closeSettings(); toast.show("设置已保存") },  // 保存 + Toast
            )
        },
    )
}

/** 设备行（在线点 + 名称 + 状态）。 */
@Composable                                               // 可组合函数
private fun LanDeviceRow(                                 // 设备行
    device: LanDevice,                                    // 设备
    selected: Boolean,                                    // 是否选中
    statusText: String,                                   // 状态文字
    onClick: () -> Unit,                                  // 点击
    enabled: Boolean = true,                              // 可用
) {
    Row(                                                  // 横向布局
        modifier = Modifier                              // 修饰
            .fillMaxWidth()                              // 占满宽度
            .padding(bottom = 8.dp)                      // 下留白
            .clip(RoundedCornerShape(10.dp))             // 圆角
            .background(if (selected) MuyunAccentLight else MuyunCard)  // 选中浅灰/白
            .clickable(enabled = enabled) { onClick() }  // 点击
            .padding(horizontal = 14.dp, vertical = 11.dp),  // 内边距
        verticalAlignment = Alignment.CenterVertically,   // 垂直居中
    ) {
        Box(                                              // 在线点
            modifier = Modifier                          // 修饰
                .size(8.dp)                              // 8dp
                .clip(CircleShape)                       // 圆形
                .background(MuyunGreen)                  // 在线绿（NSD 发现即在线）
        )
        Text(                                             // 名称
            text = device.name,                           // 内容
            style = MaterialTheme.typography.bodyMedium,  // 字号
            fontWeight = FontWeight.Medium,               // 中粗
            color = MuyunText,                            // 主色
            modifier = Modifier.weight(1f).padding(horizontal = 10.dp),  // 占满
        )
        Text(                                             // 状态文字
            text = statusText,                            // 内容
            style = MaterialTheme.typography.labelSmall,  // 小字
            color = MuyunText3,                           // 三级灰
        )
    }
}

/** 局域网传输 ViewModel —— 真实 NSD 发现 + 真实 TCP 传输（core:lan）。 */
@HiltViewModel                                           // 注解：由 Hilt 创建
class LanViewModel @Inject constructor(                  // 构造函数注入
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,  // 注入应用上下文（临时文件）
    private val prefs: ExtPrefs,                         // 注入扩展偏好（接收方式/路径）
    private val transfer: TransferRepository,            // 注入传输编排器（真传输）
) : ViewModel() {                                        // 继承 ViewModel

    private val _type = MutableStateFlow(LanType.TEXT)   // 当前类型
    val type: StateFlow<LanType> = _type.asStateFlow()   // 只读暴露
    private val _sendDevice = MutableStateFlow<LanDevice?>(null)  // 选中的发送设备
    val sendDevice: StateFlow<LanDevice?> = _sendDevice.asStateFlow()  // 只读暴露
    private val _status = MutableStateFlow("" to StatusTone.NEUTRAL)  // 状态
    val status: StateFlow<Pair<String, StatusTone>> = _status.asStateFlow()  // 只读暴露
    private val _settingsVisible = MutableStateFlow(false)  // 设置弹窗
    val settingsVisible: StateFlow<Boolean> = _settingsVisible.asStateFlow()  // 只读暴露
    private val _receiveMode = MutableStateFlow("manual")  // 接收方式
    val receiveMode: StateFlow<String> = _receiveMode.asStateFlow()  // 只读暴露
    private val _savePath = MutableStateFlow("应用私有目录/transfer/in")  // 保存路径（真实接收目录）
    val savePath: StateFlow<String> = _savePath.asStateFlow()  // 只读暴露

    /** 设备列表（真实 NSD 发现，透传 TransferRepository）。 */
    val devices: StateFlow<List<LanDevice>> = transfer.devices  // 设备
    /** 是否扫描中。 */
    val scanning: StateFlow<Boolean> = transfer.scanning  // 扫描状态
    /** 发送会话（进度/速度/状态）。 */
    val session: StateFlow<com.memuo.core.lan.SendSession?> = transfer.sendSession  // 会话

    init {                                                // 初始化
        viewModelScope.launch { prefs.lanReceiveMode.collect { _receiveMode.value = it } }  // 加载接收方式
        viewModelScope.launch { prefs.lanSavePath.collect { _savePath.value = it } }  // 加载路径
        // 订阅会话状态 → 驱动状态条文案
        viewModelScope.launch {                          // 会话订阅
            transfer.sendSession.collect { s ->           // 每次会话变化
                when (s?.state) {                        // 按状态
                    SessionState.RUNNING -> _status.value = "正在发送「${s.name}」…" to StatusTone.INFO  // 传输中
                    SessionState.PAUSED -> _status.value = "已暂停，可继续传输" to StatusTone.NEUTRAL  // 暂停
                    SessionState.SUCCESS -> _status.value = "发送成功 · 已接收 ${fmtSize(s.size)}" to StatusTone.SUCCESS  // 成功
                    SessionState.FAILED -> _status.value = "发送失败，请检查网络后重试" to StatusTone.FAIL  // 失败
                    null, SessionState.IDLE -> Unit      // 空闲不更新
                }
            }
        }
    }

    /** 打开弹窗：开启接收 + 广播 + 扫描。 */
    fun open() {                                         // 打开
        _status.value = "" to StatusTone.NEUTRAL         // 清状态
        transfer.startAll()                              // 开启全部（服务/广播/扫描）
    }

    /** 关闭弹窗：停止一切。 */
    fun close() {                                        // 关闭
        transfer.stopAll()                               // 停止全部
    }

    /** 刷新设备（重扫 NSD）。 */
    fun refresh() {                                      // 刷新
        transfer.stopAll()                               // 停
        transfer.startAll()                              // 重启扫描
    }

    /** 切换类型。 */
    fun setType(t: LanType) { _type.value = t }          // 更新类型

    /** 选择发送设备。 */
    fun selectDevice(d: LanDevice) { _sendDevice.value = d }  // 更新选中

    /** 缓存 SAF 选中文件到应用缓存目录（Uri 权限只在会话内有效）。 */
    fun cachePicked(context: android.content.Context, uri: android.net.Uri): File? {  // 缓存文件
        val name = runCatching {                         // 取文件名
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->  // 查询元数据
                val i = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)  // 名称列
                if (c.moveToFirst() && i >= 0) c.getString(i) else null  // 读名称
            }
        }.getOrNull() ?: "file_${System.currentTimeMillis()}"  // 兜底名
        val cache = File(context.cacheDir, "lan_out")    // 缓存目录
        cache.mkdirs()                                    // 建目录
        val target = File(cache, name)                    // 目标文件
        return runCatching {                              // 复制
            context.contentResolver.openInputStream(uri)?.use { input ->  // 输入流
                target.outputStream().use { input.copyTo(it) }  // 写缓存
            }
            target                                        // 返回缓存文件
        }.getOrNull()
    }

    /** 开始传输（真实发送：文本写临时文件，文件/图片直接发缓存文件）。 */
    fun startTransfer(type: LanType, text: String, picked: File?) {  // 传输
        val device = _sendDevice.value ?: run { _status.value = "请先选择发送设备" to StatusTone.FAIL; return }  // 无设备
        viewModelScope.launch(Dispatchers.IO) {          // IO 线程准备文件
            val file = when (type) {                     // 按类型取文件
                LanType.TEXT -> File(context.cacheDir, "lan_text_${System.currentTimeMillis()}.txt").apply {  // 文本临时文件
                    writeText(text)                      // 写内容
                }
                else -> picked ?: run { _status.value = "请先选择文件" to StatusTone.FAIL; return@launch }  // 无文件
            }
            withContext(Dispatchers.Main) {              // 回主线程更新状态
                _status.value = "正在发送「${file.name}」到「${device.name}」…" to StatusTone.INFO  // 状态
            }
            transfer.send(device, file)                  // 真实发送（异步）
        }
    }

    /** 暂停传输（断点保留）。 */
    fun pause() { transfer.pause() }                     // 暂停

    /** 恢复传输（断点续传）。 */
    fun resume() { transfer.resume() }                   // 恢复

    /** 打开设置弹窗。 */
    fun openSettings() { _settingsVisible.value = true }  // 显示

    /** 关闭设置弹窗。 */
    fun closeSettings() {                                // 关闭
        _settingsVisible.value = false                   // 隐藏
        viewModelScope.launch { prefs.setLanReceiveMode(_receiveMode.value) }  // 持久化
        viewModelScope.launch { prefs.setLanSavePath(_savePath.value) }  // 持久化
    }

    /** 切换接收方式。 */
    fun setReceiveMode(mode: String) { _receiveMode.value = mode }  // 更新

    /** 字节格式化（B/KB/MB/GB）。 */
    fun fmtSize(bytes: Long): String = when {            // 格式化
        bytes >= 1L shl 30 -> "%.2f GB".format(java.util.Locale.getDefault(), bytes / 1024.0 / 1024 / 1024)  // GB
        bytes >= 1L shl 20 -> "%.1f MB".format(java.util.Locale.getDefault(), bytes / 1024.0 / 1024)  // MB
        bytes >= 1L shl 10 -> "%.0f KB".format(java.util.Locale.getDefault(), bytes / 1024.0)  // KB
        else -> "$bytes B"                               // B
    }
}
