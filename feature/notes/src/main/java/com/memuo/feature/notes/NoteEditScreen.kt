package com.memuo.feature.notes                         // 声明包名：笔记业务模块

import androidx.activity.compose.BackHandler              // 导入 BackHandler：拦截系统返回键
import androidx.compose.foundation.background             // 导入 background：背景修饰
import androidx.compose.foundation.clickable              // 导入 clickable：点击修饰
import androidx.compose.foundation.layout.Box             // 导入 Box：盒式布局
import androidx.compose.foundation.layout.Column          // 导入 Column：纵向布局
import androidx.compose.foundation.layout.Row             // 导入 Row：横向布局
import androidx.compose.foundation.layout.fillMaxHeight   // 导入 fillMaxHeight：占满高度
import androidx.compose.foundation.layout.fillMaxSize     // 导入 fillMaxSize：铺满
import androidx.compose.foundation.layout.fillMaxWidth    // 导入 fillMaxWidth：占满宽度
import androidx.compose.foundation.layout.height          // 导入 height：固定高度
import androidx.compose.foundation.layout.padding          // 导入 padding：内边距
import androidx.compose.foundation.layout.size            // 导入 size：固定尺寸
import androidx.compose.foundation.layout.width           // 导入 width：固定宽度
import androidx.compose.foundation.shape.RoundedCornerShape  // 导入 RoundedCornerShape：圆角形状
import androidx.compose.foundation.text.BasicTextField     // 导入 BasicTextField：无边框输入框
import androidx.compose.material3.DropdownMenu             // 导入 DropdownMenu：下拉菜单
import androidx.compose.material3.DropdownMenuItem         // 导入 DropdownMenuItem：下拉菜单项
import androidx.compose.material3.Icon                    // 导入 Icon：图标
import androidx.compose.material3.MaterialTheme           // 导入 MaterialTheme：主题
import androidx.compose.material3.Text                    // 导入 Text：文本
import androidx.compose.runtime.Composable                // 导入 Composable：可组合函数注解
import androidx.compose.runtime.DisposableEffect           // 导入 DisposableEffect：生命周期副作用
import androidx.compose.runtime.LaunchedEffect            // 导入 LaunchedEffect：副作用
import androidx.compose.runtime.collectAsState            // 导入 collectAsState：状态流→状态
import androidx.compose.runtime.getValue                  // 导入 getValue：by 委托
import androidx.compose.runtime.mutableStateOf            // 导入 mutableStateOf：可变状态
import androidx.compose.runtime.remember                  // 导入 remember：记住状态
import androidx.compose.runtime.rememberUpdatedState      // 导入 rememberUpdatedState：最新状态引用
import androidx.compose.runtime.setValue                  // 导入 setValue：by 委托写
import androidx.compose.ui.Alignment                      // 导入 Alignment：对齐
import androidx.compose.ui.Modifier                       // 导入 Modifier：修饰
import androidx.compose.ui.draw.clip                      // 导入 clip：裁剪
import androidx.compose.ui.graphics.Color                 // 导入 Color：颜色
import androidx.compose.ui.graphics.SolidColor             // 导入 SolidColor：光标颜色
import androidx.compose.ui.platform.LocalLifecycleOwner    // 导入 LocalLifecycleOwner：生命周期所有者
import androidx.compose.ui.text.font.FontWeight           // 导入 FontWeight：字重
import androidx.compose.ui.unit.dp                        // 导入 dp：尺寸单位
import androidx.compose.ui.unit.sp                        // 导入 sp：字号单位
import androidx.hilt.navigation.compose.hiltViewModel     // 导入 hiltViewModel：Hilt 提供 ViewModel
import androidx.lifecycle.Lifecycle                       // 导入 Lifecycle：生命周期枚举
import androidx.lifecycle.LifecycleEventObserver           // 导入 LifecycleEventObserver：生命周期观察者
import com.memuo.core.db.entity.Note                       // 导入笔记实体
import com.memuo.core.ui.AppIcons                         // 导入应用图标集
import com.memuo.core.ui.components.LocalToast            // 导入 Toast 状态
import com.memuo.core.ui.components.ModalCloseButton      // 导入弹窗关闭按钮
import com.memuo.core.ui.components.MuyunModal            // 导入弹窗容器
import com.memuo.core.ui.components.MuyunToggle           // 导入 iOS 风格开关
import com.memuo.core.ui.components.SubBody               // 导入子页内容容器
import com.memuo.core.ui.components.SubHeader             // 导入子页顶栏
import com.memuo.core.ui.theme.MuyunAccentLight           // 导入浅灰底
import com.memuo.core.ui.theme.MuyunBorder                // 导入分割线色
import com.memuo.core.ui.theme.MuyunBrand                 // 导入品牌色
import com.memuo.core.ui.theme.MuyunCard                  // 导入卡片白
import com.memuo.core.ui.theme.MuyunDanger                // 导入危险红
import com.memuo.core.ui.theme.MuyunText                  // 导入主文字色
import com.memuo.core.ui.theme.MuyunText2                 // 导入次级文字色
import com.memuo.core.ui.theme.MuyunText3                 // 导入三级文字色
import kotlinx.coroutines.delay                           // 导入 delay：防抖延迟
import java.text.SimpleDateFormat                          // 导入 SimpleDateFormat：时间格式化
import java.util.Date                                     // 导入 Date：日期
import java.util.Locale                                   // 导入 Locale：区域

/**
 * 笔记编辑页 —— 编辑单条笔记的标题与正文。
 * 交互：返回/退出时自动保存；右上角「⋮」下拉菜单（设置 / 存入知识库 / 删除）。
 */
@Composable                                               // 可组合 UI 函数
fun NoteEditScreen(                                       // 笔记编辑页
    noteId: Long,                                         // 要编辑的笔记 ID
    onBack: () -> Unit,                                   // 返回回调
    viewModel: NoteListViewModel = hiltViewModel(),       // Hilt 提供 ViewModel
) {
    val note by viewModel.observeNote(noteId).collectAsState(initial = null)  // 加载单条笔记
    val autoIngest by viewModel.autoIngest.collectAsState()  // 自动入库开关
    val trashDays by viewModel.trashDays.collectAsState()    // 回收站保留天数
    val toast = LocalToast.current                        // 取全局 Toast

    var title by remember { mutableStateOf("") }          // 标题编辑状态
    var content by remember { mutableStateOf("") }        // 正文编辑状态
    var menuOpen by remember { mutableStateOf(false) }    // 下拉菜单展开
    var settingsOpen by remember { mutableStateOf(false) }  // 设置面板展开
    var loaded by remember { mutableStateOf(false) }      // 是否已从数据库加载（防抖保存前提）

    LaunchedEffect(note?.id) {                            // 笔记加载后同步到编辑框
        title = note?.title ?: ""                         // 同步标题
        content = note?.content ?: ""                     // 同步正文
        loaded = true                                     // 标记已加载
    }

    // 保存当前内容（写库 + 发变更事件；空内容也保存，保留空笔记一致性）
    val save = {                                         // 保存
        viewModel.updateContent(noteId, title, content)   // 写库 + 发变更事件
    }
    // 保存并返回（返回箭头 / 系统返回键共用）
    val saveAndBack = {                                   // 保存并返回
        save()                                           // 保存
        onBack()                                          // 返回
    }

    // ① 防抖自动保存：内容变化 600ms 后落库，覆盖切后台/进程被回收（内容已入库，不丢）
    LaunchedEffect(loaded, title, content) {              // 内容变化触发
        if (loaded) {                                     // 已加载才防抖（避免初始加载空触发）
            delay(600)                                    // 防抖 600ms
            viewModel.updateContent(noteId, title, content)  // 自动入库
        }
    }

    // ② 系统返回键：拦截并保存后返回（否则 Navigation 直接 popBackStack 会丢内容）
    BackHandler(onBack = saveAndBack)                     // 拦截系统返回

    // ③ 切后台（onStop）时保存最新内容，覆盖防抖窗口内的最后输入
    val lifecycleOwner = LocalLifecycleOwner.current      // 生命周期所有者
    val latestTitle by rememberUpdatedState(title)        // 最新标题（避免闭包捕获旧值）
    val latestContent by rememberUpdatedState(content)    // 最新正文
    val latestNoteId by rememberUpdatedState(noteId)      // 最新笔记 ID
    DisposableEffect(lifecycleOwner) {                    // 注册生命周期观察
        val observer = LifecycleEventObserver { _, event ->  // 生命周期回调
            if (event == Lifecycle.Event.ON_STOP) {       // 切后台/离开页面时
                viewModel.updateContent(latestNoteId, latestTitle, latestContent)  // 保存最新内容
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)    // 注册
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }  // 注销
    }

    Column(modifier = Modifier.fillMaxSize()) {           // 纵向布局（占满）
        SubHeader(                                        // 子页顶栏
            title = "备忘录",                             // 居中标题
            onBack = saveAndBack,                         // 返回 = 自动保存并返回
            actions = {                                   // 右侧槽位：三个点按钮
                Box(                                      // 更多操作按钮容器（含下拉菜单锚点）
                    modifier = Modifier                  // 修饰
                        .fillMaxHeight(),                // 占满顶栏高度
                ) {
                    Box(                                  // 三个点图标按钮
                        modifier = Modifier              // 修饰
                            .align(Alignment.Center)     // 居中
                            .size(36.dp)                 // 36dp 热区
                            .clip(RoundedCornerShape(10.dp))  // 圆角 10
                            .clickable { menuOpen = true }  // 点击展开菜单
                            .background(Color.Transparent),  // 透明底
                        contentAlignment = Alignment.Center,  // 居中
                    ) {
                        Icon(                            // 竖向三点图标
                            imageVector = AppIcons.More, // 图标
                            contentDescription = "更多操作",  // 描述
                            tint = MuyunText,            // 主色
                            modifier = Modifier.size(20.dp),  // 20dp
                        )
                    }
                    // 下拉菜单（锚定在按钮下方，HTML .memo-menu）
                    DropdownMenu(                         // 下拉菜单
                        expanded = menuOpen,              // 展开状态
                        onDismissRequest = { menuOpen = false },  // 点外部关闭
                    ) {
                        DropdownMenuItem(                 // 设置
                            text = { Text("设置") },      // 文字
                            onClick = {                   // 点击
                                menuOpen = false          // 关菜单
                                settingsOpen = true       // 打开设置面板
                            },
                        )
                        DropdownMenuItem(                 // 存入知识库
                            text = { Text("存入知识库") },  // 文字
                            onClick = {                   // 点击
                                menuOpen = false          // 关菜单
                                viewModel.saveAndIngest(noteId, title, content) {  // 保存最新内容并入库
                                    toast.show("已存入知识库")  // 提示
                                }
                            },
                        )
                        DropdownMenuItem(                 // 删除
                            text = { Text("删除", color = MuyunDanger) },  // 红色文字
                            onClick = {                   // 点击
                                menuOpen = false          // 关菜单
                                viewModel.deleteNote(noteId)  // 移入回收站（软删除）
                                toast.show("已移入回收站")  // 提示
                                onBack()                  // 返回列表
                            },
                        )
                    }
                }
            },
        )
        SubBody(modifier = Modifier.fillMaxSize()) {       // 内容容器
            Column(modifier = Modifier.fillMaxSize()) {    // 纵向内容
                // 标题输入（无边框大字号，下划线分隔）
                BasicTextField(                           // 无边框输入框
                    value = title,                        // 绑定标题
                    onValueChange = { title = it },       // 输入更新
                    textStyle = MaterialTheme.typography.headlineSmall.copy(  // 大字号
                        color = MuyunText,                // 主文字色
                        fontWeight = FontWeight.Bold,     // 粗体
                    ),
                    cursorBrush = SolidColor(MuyunBrand),  // 光标品牌色
                    modifier = Modifier                  // 修饰
                        .fillMaxWidth()                  // 占满宽度
                        .padding(vertical = 6.dp),       // 上下留白
                    decorationBox = { innerTextField ->   // 自定义占位
                        if (title.isEmpty()) {            // 空标题显示占位
                            Text(                         // 占位文本
                                "标题",                   // 内容
                                style = MaterialTheme.typography.headlineSmall.copy(  // 大字号
                                    color = MuyunText3,   // 三级灰
                                    fontWeight = FontWeight.Bold,  // 粗体
                                ),
                            )
                        }
                        innerTextField()                  // 实际输入区
                    },
                )
                Box(                                      // 标题下分隔线
                    modifier = Modifier                  // 修饰
                        .fillMaxWidth()                  // 占满宽度
                        .height(1.dp)                    // 1dp
                        .background(MuyunBorder),        // 分割线色
                )
                Box(modifier = Modifier.height(10.dp))    // 分隔线与正文的留白
                // 元信息行（创建/最后编辑时间）
                Text(                                     // 元信息
                    text = "创建于 ${fmtMemoEditTime(note?.createdAt ?: 0L)} · 最后编辑 ${fmtMemoEditTime(note?.updatedAt ?: 0L)}",  // 内容
                    style = MaterialTheme.typography.labelSmall,  // 小字
                    color = MuyunText3,                   // 三级灰
                    modifier = Modifier.padding(vertical = 4.dp),  // 内边距
                )
                // 正文输入（全屏多行无边框）
                BasicTextField(                           // 无边框输入框
                    value = content,                      // 绑定正文
                    onValueChange = { content = it },     // 输入更新
                    textStyle = MaterialTheme.typography.bodyLarge.copy(  // 正文字体
                        color = MuyunText,                // 主文字色
                        lineHeight = 26.sp,               // 行距
                    ),
                    cursorBrush = SolidColor(MuyunBrand),  // 光标品牌色
                    modifier = Modifier                  // 修饰
                        .fillMaxWidth()                  // 占满宽度
                        .fillMaxHeight()                 // 占满高度
                        .padding(vertical = 16.dp),      // 上下留白
                    decorationBox = { innerTextField ->   // 自定义占位
                        if (content.isEmpty()) {          // 空正文显示占位
                            Text(                         // 占位文本
                                "开始输入内容…",           // 内容
                                style = MaterialTheme.typography.bodyLarge.copy(  // 正文字体
                                    color = MuyunText3,   // 三级灰
                                ),
                            )
                        }
                        innerTextField()                  // 实际输入区
                    },
                )
            }
        }
    }

    // 设置面板（回收站保留天数 + 自动入库开关）
    NoteSettingsDialog(                                   // 设置面板
        visible = settingsOpen,                           // 显示状态
        autoIngest = autoIngest,                          // 自动入库开关
        trashDays = trashDays,                            // 回收站天数
        onAutoIngest = { viewModel.setAutoIngest(it) },   // 切换开关
        onTrashDays = { viewModel.setTrashDays(it) },     // 调节天数
        onDismiss = { settingsOpen = false },             // 关闭
    )
}

/** 设置面板弹窗：回收站保留天数（可调）+ 是否自动存入知识库开关。 */
@Composable                                               // 可组合函数
private fun NoteSettingsDialog(                           // 设置面板
    visible: Boolean,                                     // 是否显示
    autoIngest: Boolean,                                  // 自动入库开关
    trashDays: Int,                                       // 回收站天数
    onAutoIngest: (Boolean) -> Unit,                      // 切换开关回调
    onTrashDays: (Int) -> Unit,                           // 调节天数回调
    onDismiss: () -> Unit,                                // 关闭回调
) {
    MuyunModal(                                           // 弹窗容器
        visible = visible,                                // 显示状态
        onDismiss = onDismiss,                            // 点遮罩关闭
        title = "设置",                                   // 标题
        headerActions = { ModalCloseButton(onClick = onDismiss) },  // 右上角关闭
        body = {
        // 回收站保留天数（可调数值）
        Row(                                              // 配置行
            modifier = Modifier                          // 修饰
                .fillMaxWidth()                          // 占满宽度
                .padding(top = 18.dp),                   // 上留白
            verticalAlignment = Alignment.CenterVertically,  // 垂直居中
        ) {
            Column(modifier = Modifier.weight(1f)) {      // 说明区
                Text(                                     // 标题
                    text = "回收站保留天数",               // 内容
                    style = MaterialTheme.typography.titleSmall,  // 字号
                    fontWeight = FontWeight.SemiBold,     // 半粗
                    color = MuyunText,                    // 主色
                )
                Text(                                     // 说明
                    text = "删除的备忘录保留该天数后自动永久删除",  // 文案
                    style = MaterialTheme.typography.labelSmall,  // 小字
                    color = MuyunText3,                   // 三级灰
                    modifier = Modifier.padding(top = 4.dp),  // 上留白
                )
            }
            // 减号 / 数字 / 加号
            Box(                                          // 减号按钮
                modifier = Modifier                      // 修饰
                    .size(32.dp)                         // 32dp
                    .clip(RoundedCornerShape(8.dp))      // 圆角 8
                    .background(MuyunAccentLight)        // 浅灰底
                    .clickable { onTrashDays((trashDays - 1).coerceAtLeast(1)) },  // 减 1（下限 1）
                contentAlignment = Alignment.Center,      // 居中
            ) {
                Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MuyunText2)  // 减号
            }
            Text(                                         // 天数显示
                text = "$trashDays 天",                   // 内容
                style = MaterialTheme.typography.titleSmall,  // 字号
                fontWeight = FontWeight.SemiBold,         // 半粗
                color = MuyunText,                        // 主色
                modifier = Modifier.padding(horizontal = 12.dp),  // 两侧留白
            )
            Box(                                          // 加号按钮
                modifier = Modifier                      // 修饰
                    .size(32.dp)                         // 32dp
                    .clip(RoundedCornerShape(8.dp))      // 圆角 8
                    .background(MuyunAccentLight)        // 浅灰底
                    .clickable { onTrashDays((trashDays + 1).coerceAtMost(365)) },  // 加 1（上限 365）
                contentAlignment = Alignment.Center,      // 居中
            ) {
                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MuyunText2)  // 加号
            }
        }
        // 自动入库开关
        Row(                                              // 配置行
            modifier = Modifier                          // 修饰
                .fillMaxWidth()                          // 占满宽度
                .padding(top = 18.dp, bottom = 6.dp),    // 上下留白
            verticalAlignment = Alignment.CenterVertically,  // 垂直居中
        ) {
            Column(modifier = Modifier.weight(1f)) {      // 说明区
                Text(                                     // 标题
                    text = "自动存入知识库",               // 内容
                    style = MaterialTheme.typography.titleSmall,  // 字号
                    fontWeight = FontWeight.SemiBold,     // 半粗
                    color = MuyunText,                    // 主色
                )
                Text(                                     // 说明
                    text = "新建/编辑笔记后自动投喂到知识库",  // 文案
                    style = MaterialTheme.typography.labelSmall,  // 小字
                    color = MuyunText3,                   // 三级灰
                    modifier = Modifier.padding(top = 4.dp),  // 上留白
                )
            }
            MuyunToggle(                                  // iOS 开关
                checked = autoIngest,                     // 状态
                onCheckedChange = onAutoIngest,           // 切换
            )
        }
        },
    )
}

/** 编辑页时间格式化（今天 HH:MM / 昨天 HH:MM / M月D日）。 */
private fun fmtMemoEditTime(ts: Long): String {           // 时间格式化
    if (ts <= 0L) return "-"                              // 无时间占位
    val now = java.util.Calendar.getInstance()            // 当前时间
    val today = java.util.Calendar.getInstance().apply {  // 今天零点
        set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)  // 归零
    }
    val yesterday = (today.clone() as java.util.Calendar).apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }  // 昨天零点
    val hhmm = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))  // 时分
    return when {                                         // 按时间判断
        ts >= today.timeInMillis -> "今天 $hhmm"          // 今天
        ts >= yesterday.timeInMillis -> "昨天 $hhmm"      // 昨天
        else -> SimpleDateFormat("M月d日", Locale.getDefault()).format(Date(ts))  // 更早
    }
}
