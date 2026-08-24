package com.memuo.app                                    // 声明包名：com.memuo.app

import android.os.Bundle                                 // 导入 Bundle：Activity 状态传递
import androidx.activity.ComponentActivity                // 导入 ComponentActivity：Compose 基础 Activity
import androidx.activity.compose.rememberLauncherForActivityResult  // 导入 rememberLauncherForActivityResult：SAF 选择器
import androidx.activity.compose.setContent               // 导入 setContent：把 Compose 界面挂载到 Activity
import androidx.activity.result.contract.ActivityResultContracts  // 导入 ActivityResultContracts：系统契约
import androidx.compose.animation.animateColorAsState     // 导入 animateColorAsState：颜色动画
import androidx.compose.foundation.background             // 导入 background：背景
import androidx.compose.foundation.clickable              // 导入 clickable：点击
import androidx.compose.foundation.layout.Box             // 导入 Box：盒式布局
import androidx.compose.foundation.layout.Column          // 导入 Column：纵向布局
import androidx.compose.foundation.layout.Row             // 导入 Row：横向布局
import androidx.compose.foundation.layout.fillMaxHeight   // 导入 fillMaxHeight：占满高度
import androidx.compose.foundation.layout.fillMaxWidth    // 导入 fillMaxWidth：占满宽度
import androidx.compose.foundation.layout.padding          // 导入 padding：内边距
import androidx.compose.foundation.layout.width            // 导入 width：宽度
import androidx.compose.foundation.shape.RoundedCornerShape  // 导入 RoundedCornerShape：圆角
import androidx.compose.material.icons.Icons               // 导入 Icons：图标集
import androidx.compose.material.icons.filled.Menu         // 导入 Menu：汉堡菜单图标
import androidx.compose.material3.DrawerValue              // 导入 DrawerValue：抽屉状态枚举
import androidx.compose.material3.ExperimentalMaterial3Api // 导入 ExperimentalMaterial3Api：实验性 API
import androidx.compose.material3.HorizontalDivider        // 导入 HorizontalDivider：分割线
import androidx.compose.material3.Icon                     // 导入 Icon：图标
import androidx.compose.material3.IconButton               // 导入 IconButton：图标按钮
import androidx.compose.material3.MaterialTheme            // 导入 MaterialTheme：Material3 主题
import androidx.compose.material3.ModalDrawerSheet         // 导入 ModalDrawerSheet：抽屉面板
import androidx.compose.material3.ModalNavigationDrawer    // 导入 ModalNavigationDrawer：侧边抽屉
import androidx.compose.material3.RadioButton              // 导入 RadioButton：单选按钮
import androidx.compose.material3.Scaffold                 // 导入 Scaffold：页面脚手架
import androidx.compose.material3.Text                     // 导入 Text：文本
import androidx.compose.material3.TextButton               // 导入 TextButton：文字按钮
import androidx.compose.material3.TopAppBar               // 导入 TopAppBar：顶部栏
import androidx.compose.material3.rememberDrawerState      // 导入 rememberDrawerState：抽屉状态
import androidx.compose.runtime.Composable                // 导入 Composable：可组合函数注解
import androidx.compose.runtime.LaunchedEffect             // 导入 LaunchedEffect：副作用
import androidx.compose.runtime.collectAsState             // 导入 collectAsState：状态流→Compose 状态
import androidx.compose.runtime.getValue                   // 导入 getValue：by 委托
import androidx.compose.runtime.mutableStateOf             // 导入 mutableStateOf：可组合状态
import androidx.compose.runtime.remember                    // 导入 remember：记住状态
import androidx.compose.runtime.rememberCoroutineScope     // 导入 rememberCoroutineScope：协程作用域
import androidx.compose.runtime.setValue                   // 导入 setValue：by 委托写
import androidx.compose.ui.Alignment                       // 导入 Alignment：对齐
import androidx.compose.ui.Modifier                        // 导入 Modifier：修饰
import androidx.compose.ui.draw.clip                       // 导入 clip：裁剪圆角
import androidx.compose.ui.graphics.Color                  // 导入 Color：颜色
import androidx.compose.ui.unit.dp                         // 导入 dp：尺寸单位
import androidx.hilt.navigation.compose.hiltViewModel      // 导入 hiltViewModel：Hilt 提供 ViewModel
import androidx.navigation.compose.NavHost                 // 导入 NavHost：导航宿主
import androidx.navigation.compose.composable              // 导入 composable：注册路由
import androidx.navigation.compose.currentBackStackEntryAsState  // 导入 currentBackStackEntryAsState：当前路由
import androidx.navigation.compose.rememberNavController   // 导入 rememberNavController：导航控制器
import com.memuo.core.db.entity.EngineType                  // 导入引擎类型枚举
import com.memuo.feature.chat.ChatScreen                    // 导入对话页
import com.memuo.feature.chat.ChatViewModel                 // 导入对话 ViewModel
import com.memuo.feature.notes.NoteEditScreen               // 导入笔记编辑页
import com.memuo.feature.notes.NoteListScreen               // 导入笔记列表页
import com.memuo.feature.settings.CloudConfigScreen         // 导入云端配置页
import com.memuo.feature.settings.DatabaseConfigScreen      // 导入数据库配置页
import com.memuo.feature.settings.MemoryScreen              // 导入记忆库页
import com.memuo.feature.settings.SettingsViewModel         // 导入设置 ViewModel
import dagger.hilt.android.AndroidEntryPoint               // 导入 AndroidEntryPoint：Hilt 注入入口
import kotlinx.coroutines.launch                            // 导入 launch：启动协程

@AndroidEntryPoint                                       // 注解：由 Hilt 管理本 Activity
class MainActivity : ComponentActivity() {               // 应用唯一 Activity（单 Activity 架构）
    override fun onCreate(savedInstanceState: Bundle?) { // 生命周期回调：Activity 创建时调用
        super.onCreate(savedInstanceState)               // 调用父类初始化
        setContent { NoteApp() }                         // 渲染应用界面
    }
}

/**
 * 应用根界面 —— 侧边抽屉（左上角汉堡图标，左滑出菜单）+ 单 Activity 导航。
 * 抽屉菜单：数据库配置 / 记忆库 / 云端·本地引擎切换 / 导入模型（仅本地）/ 云端 API 配置（仅云端）。
 */
@OptIn(ExperimentalMaterial3Api::class)                  // 声明使用实验性 API（TopAppBar/Drawer）
@Composable                                               // 可组合 UI 函数
fun NoteApp() {                                          // 应用根组件
    MaterialTheme {                                      // 套上 Material3 主题
        val nav = rememberNavController()                // 导航控制器
        val drawerState = rememberDrawerState(DrawerValue.Closed)  // 抽屉状态（初始关闭）
        val scope = rememberCoroutineScope()             // 协程作用域（开/关抽屉用）
        val settingsVm: SettingsViewModel = hiltViewModel()  // 设置 ViewModel（引擎切换/模型导入）
        val navBackStackEntry by nav.currentBackStackEntryAsState()  // 当前导航栈项（用于胶囊选中态）
        val currentRoute = navBackStackEntry?.destination?.route  // 当前路由

        // SAF 文件夹选择器：选中的目录 Uri 交给 ViewModel 检测并导入
        val modelPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->  // 选择器
            uri?.let { settingsVm.importModelFromUri(it) }  // 有结果则导入
        }

        ModalNavigationDrawer(                            // 侧边抽屉容器
            drawerState = drawerState,                    // 抽屉状态
            drawerContent = {                             // 抽屉内容（侧边菜单）
                AppDrawer(                                // 侧边菜单
                    viewModel = settingsVm,               // 共用设置 ViewModel
                    onNavigate = { route ->               // 点击菜单项跳转
                        scope.launch { drawerState.close() }  // 先关抽屉
                        nav.navigate(route) { launchSingleTop = true }  // 再跳转
                    },
                    onImportModel = {                     // 点击"导入模型"
                        scope.launch { drawerState.close() }  // 关抽屉
                        modelPicker.launch(null)          // 打开系统文件夹选择器
                    },
                )
            },
        ) {
            Scaffold(                                     // 主内容脚手架
                topBar = {                               // 顶部栏
                    TopAppBar(                           // 顶部栏组件
                        title = { Text("沐云杪AI") },     // 标题
                        navigationIcon = {               // 左上角图标
                            IconButton(                   // 汉堡按钮
                                onClick = { scope.launch { drawerState.open() } },  // 点击展开抽屉
                            ) {
                                Icon(Icons.Filled.Menu, contentDescription = "菜单")  // 汉堡图标
                            }
                        },
                        actions = {                       // 右侧：胶囊切换器（常规/AI）
                            CapsuleSwitch(                // 胶囊切换器
                                selectedLeft = currentRoute?.startsWith("note") == true,  // 当前在笔记 → 选中"常规"
                                onLeft = {                // 点"常规"
                                    nav.navigate("note/list") { launchSingleTop = true }
                                },
                                onRight = {               // 点"AI"
                                    nav.navigate("chat") { launchSingleTop = true }
                                },
                            )
                        },
                    )
                },
            ) { innerPadding ->                           // 内容区
                NavHost(                                  // 导航宿主
                    navController = nav,                  // 导航控制器
                    startDestination = "note/list",       // 起始路由：笔记列表
                    modifier = Modifier.padding(innerPadding),  // 避开顶部栏
                ) {
                    composable("note/list") {             // 笔记列表
                        NoteListScreen(onOpenNote = { id -> nav.navigate("note/edit/$id") })
                    }
                    composable("note/edit/{noteId}") { entry ->  // 笔记编辑
                        val noteId = entry.arguments?.getString("noteId")?.toLongOrNull() ?: 0L
                        NoteEditScreen(noteId = noteId, onBack = { nav.popBackStack() })
                    }
                    composable("chat") { ChatTab() }      // 对话
                    composable("db") { DatabaseConfigScreen() }   // 数据库配置
                    composable("memory") { MemoryScreen() }        // 记忆库
                    composable("cloud") { CloudConfigScreen() }    // 云端 API 配置
                }
            }
        }
    }
}

/**
 * 侧边菜单（抽屉内容）—— 数据库配置 / 记忆库 / 引擎切换 / 导入模型（本地）/ 云端 API 配置（云端）。
 */
@Composable                                               // 可组合 UI 函数
private fun AppDrawer(                                    // 侧边菜单
    viewModel: SettingsViewModel,                         // 设置 ViewModel
    onNavigate: (String) -> Unit,                         // 导航回调
    onImportModel: () -> Unit,                            // 导入模型回调（打开 SAF 选择器）
) {
    val engineType by viewModel.engineType.collectAsState()  // 订阅引擎类型
    val hasLocalModel by viewModel.hasLocalModel.collectAsState()  // 订阅本地模型状态
    val message by viewModel.message.collectAsState()     // 订阅提示消息

    ModalDrawerSheet(modifier = Modifier.width(300.dp)) {  // 抽屉面板（宽 300dp）
        Column(modifier = Modifier.fillMaxHeight().padding(vertical = 8.dp)) {  // 纵向菜单
            Text(                                         // 应用名
                "沐云杪AI",
                style = MaterialTheme.typography.titleLarge,  // 大标题
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),  // 内边距
            )
            HorizontalDivider()                           // 分割线

            DrawerItem("数据库配置") { onNavigate("db") }  // 数据库配置
            DrawerItem("记忆库") { onNavigate("memory") }  // 记忆库

            HorizontalDivider()                           // 分割线
            Text(                                         // 引擎小节标题
                "对话引擎",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            // 云端选项
            Row(                                          // 横向
                verticalAlignment = Alignment.CenterVertically,  // 垂直居中
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            ) {
                RadioButton(                              // 单选按钮
                    selected = engineType == EngineType.CLOUD,
                    onClick = { viewModel.switchEngine(EngineType.CLOUD) },
                )
                Text("云端 AI")                           // 标签
            }
            // 本地选项
            Row(                                          // 横向
                verticalAlignment = Alignment.CenterVertically,  // 垂直居中
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            ) {
                RadioButton(                              // 单选按钮
                    selected = engineType == EngineType.LOCAL,
                    onClick = { viewModel.switchEngine(EngineType.LOCAL) },
                )
                Text("本地 AI")                           // 标签
            }

            HorizontalDivider()                           // 分割线

            // 导入模型始终显示（避免"切本地需模型、导入需切本地"的死循环）
            DrawerItem("导入模型") { onImportModel() }     // 导入模型（打开 SAF 选择器）
            Text(                                         // 模型状态
                "本地模型：${if (hasLocalModel) "✅ 已就绪" else "❌ 未就绪"}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            // 云端 API 配置：仅云端模式显示
            if (engineType == EngineType.CLOUD) {         // 云端引擎
                DrawerItem("云端 API 配置") { onNavigate("cloud") }  // 云端配置
            }

            if (message.isNotBlank()) {                   // 有提示消息
                Text(                                     // 显示提示
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

/** 侧边菜单单项。 */
@Composable                                               // 可组合 UI 函数
private fun DrawerItem(                                   // 菜单项
    label: String,                                        // 标签
    onClick: () -> Unit,                                  // 点击回调
) {
    TextButton(                                           // 文字按钮
        onClick = onClick,                                // 点击触发
        modifier = Modifier.fillMaxWidth(),               // 占满宽度
    ) {
        Text(                                             // 标签文本
            label,
            style = MaterialTheme.typography.bodyLarge,   // 正文字体
            modifier = Modifier.padding(vertical = 6.dp), // 内边距
        )
    }
}

/**
 * 胶囊切换器（CapsuleSwitch）—— 顶栏右侧的「常规 | AI」切换。
 * 外观：圆角胶囊（长方形 + 中间分割），选中段有颜色高亮 + 动画过渡。
 */
@Composable                                               // 可组合 UI 函数
private fun CapsuleSwitch(                                // 胶囊切换器
    selectedLeft: Boolean,                                // 是否选中左段（常规）
    onLeft: () -> Unit,                                   // 点左段
    onRight: () -> Unit,                                  // 点右段
) {
    Row(                                                  // 横向容器
        modifier = Modifier                                // 修饰
            .clip(RoundedCornerShape(50))                 // 大圆角（胶囊形）
            .background(MaterialTheme.colorScheme.surfaceVariant)  // 胶囊底色
            .padding(3.dp),                               // 内边距
        verticalAlignment = Alignment.CenterVertically,   // 垂直居中
    ) {
        CapsuleSegment("常规", selectedLeft, onLeft)       // 左段：常规
        CapsuleSegment("AI", !selectedLeft, onRight)       // 右段：AI
    }
}

/** 胶囊单段 —— 选中态有背景色高亮（带动画）与文字颜色变化。 */
@Composable                                               // 可组合 UI 函数
private fun CapsuleSegment(                               // 胶囊单段
    label: String,                                        // 标签
    selected: Boolean,                                    // 选中态
    onClick: () -> Unit,                                  // 点击回调
) {
    val bg by animateColorAsState(                         // 背景色动画
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,  // 选中高亮
        label = "segBg",                                  // 动画标签
    )
    val textColor by animateColorAsState(                  // 文字色动画
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,  // 选中反色
        label = "segText",                                // 动画标签
    )
    Box(                                                  // 段容器
        modifier = Modifier                                // 修饰
            .clip(RoundedCornerShape(50))                 // 大圆角
            .background(bg)                               // 背景（动画色）
            .clickable(onClick = onClick)                 // 点击
            .padding(horizontal = 16.dp, vertical = 6.dp),  // 内边距
    ) {
        Text(                                             // 标签文本
            label,
            color = textColor,                            // 文字色（动画）
            style = MaterialTheme.typography.labelLarge,  // 字体
        )
    }
}

/**
 * 对话页 —— 进入时自动确保会话（有则复用最新，无则新建）。
 */
@Composable                                               // 可组合 UI 函数
private fun ChatTab(                                      // 对话页
    viewModel: ChatViewModel = hiltViewModel(),           // Hilt 提供 ViewModel
) {
    var conversationId by remember { mutableStateOf(0L) } // 当前会话 ID（0 = 加载中）

    LaunchedEffect(Unit) {                                // 进入时执行一次
        viewModel.ensureConversation { id -> conversationId = id }  // 确保会话存在（回调拿真实 ID）
    }

    if (conversationId != 0L) {                           // 会话就绪
        ChatScreen(conversationId = conversationId, viewModel = viewModel)  // 渲染对话页
    } else {                                              // 加载中
        Text("正在加载对话…", modifier = Modifier.padding(24.dp))  // 占位提示
    }
}
