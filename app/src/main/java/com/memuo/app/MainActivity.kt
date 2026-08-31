package com.memuo.app                                    // 声明包名：com.memuo.app

import android.os.Bundle                                 // 导入 Bundle：Activity 状态传递
import android.net.Uri                                   // 导入 Uri：内容标识（壁纸位图加载）
import androidx.activity.ComponentActivity                // 导入 ComponentActivity：Compose 基础 Activity
import androidx.activity.compose.rememberLauncherForActivityResult  // 导入 rememberLauncherForActivityResult：SAF 选择器
import androidx.activity.compose.setContent               // 导入 setContent：把 Compose 界面挂载到 Activity
import androidx.activity.enableEdgeToEdge                 // 导入 enableEdgeToEdge：边到边显示（配合 imePadding 处理键盘）
import androidx.activity.result.contract.ActivityResultContracts  // 导入 ActivityResultContracts：系统契约
import androidx.compose.animation.core.tween              // 导入 tween：过渡动画时长
import androidx.compose.animation.fadeIn                  // 导入 fadeIn：淡入过渡
import androidx.compose.animation.fadeOut                 // 导入 fadeOut：淡出过渡
import androidx.compose.foundation.background             // 导入 background：背景修饰
import androidx.compose.foundation.clickable              // 导入 clickable：点击修饰
import androidx.compose.foundation.combinedClickable      // 导入 combinedClickable：短按+长按（引擎按钮 M-035）
import androidx.compose.foundation.gestures.detectHorizontalDragGestures  // 导入 detectHorizontalDragGestures：胶囊滑动切换
import androidx.compose.foundation.layout.Box             // 导入 Box：盒式布局
import androidx.compose.foundation.layout.Column          // 导入 Column：纵向布局
import androidx.compose.foundation.layout.Row             // 导入 Row：横向布局
import androidx.compose.foundation.layout.Spacer          // 导入 Spacer：占位
import androidx.compose.foundation.layout.WindowInsets     // 导入 WindowInsets：取消默认 insets
import androidx.compose.foundation.layout.fillMaxHeight   // 导入 fillMaxHeight：占满高度
import androidx.compose.foundation.layout.fillMaxSize     // 导入 fillMaxSize：铺满
import androidx.compose.foundation.layout.fillMaxWidth    // 导入 fillMaxWidth：占满宽度
import androidx.compose.foundation.layout.height          // 导入 height：固定高度
import androidx.compose.foundation.layout.navigationBarsPadding  // 导入 navigationBarsPadding：底部手势条避让
import androidx.compose.foundation.layout.padding          // 导入 padding：内边距
import androidx.compose.foundation.layout.size            // 导入 size：固定尺寸
import androidx.compose.foundation.layout.statusBarsPadding  // 导入 statusBarsPadding：状态栏避让
import androidx.compose.foundation.layout.width           // 导入 width：固定宽度
import androidx.compose.foundation.shape.CircleShape       // 导入 CircleShape：圆形
import androidx.compose.foundation.shape.RoundedCornerShape  // 导入 RoundedCornerShape：圆角形状
import androidx.compose.material3.ExperimentalMaterial3Api // 导入 ExperimentalMaterial3Api：实验性 API
import androidx.compose.material3.HorizontalDivider        // 导入 HorizontalDivider：分割线
import androidx.compose.material3.Icon                     // 导入 Icon：图标
import androidx.compose.material3.MaterialTheme            // 导入 MaterialTheme：Material3 主题
import androidx.compose.material3.ModalDrawerSheet         // 导入 ModalDrawerSheet：抽屉面板
import androidx.compose.material3.ModalNavigationDrawer    // 导入 ModalNavigationDrawer：侧边抽屉
import androidx.compose.material3.Scaffold                 // 导入 Scaffold：页面脚手架
import androidx.compose.material3.Text                     // 导入 Text：文本
import androidx.compose.material3.rememberDrawerState      // 导入 rememberDrawerState：抽屉状态
import androidx.compose.material3.DrawerValue              // 导入 DrawerValue：抽屉状态枚举
import androidx.compose.runtime.Composable                // 导入 Composable：可组合函数注解
import androidx.compose.runtime.CompositionLocalProvider   // 导入 CompositionLocalProvider：组合局部变量提供
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
import androidx.compose.ui.draw.shadow                     // 导入 shadow：投影
import androidx.compose.ui.graphics.Color                  // 导入 Color：颜色
import androidx.compose.ui.input.pointer.pointerInput       // 导入 pointerInput：滑动手势
import androidx.compose.ui.text.font.FontWeight            // 导入 FontWeight：字重
import androidx.compose.ui.unit.dp                         // 导入 dp：尺寸单位
import androidx.hilt.navigation.compose.hiltViewModel      // 导入 hiltViewModel：Hilt 提供 ViewModel
import androidx.navigation.compose.NavHost                 // 导入 NavHost：导航宿主
import androidx.navigation.compose.composable              // 导入 composable：注册路由
import androidx.navigation.compose.currentBackStackEntryAsState  // 导入 currentBackStackEntryAsState：当前路由
import androidx.navigation.compose.rememberNavController   // 导入 rememberNavController：导航控制器
import com.memuo.core.db.entity.EngineType                  // 导入引擎类型枚举
import com.memuo.core.storage.AppPrefs                     // 导入应用偏好（首次启动标记）
import com.memuo.core.storage.WallpaperConfig              // 导入壁纸配置
import com.memuo.core.storage.WallpaperMode                // 导入壁纸方式
import com.memuo.core.storage.WallpaperPrefs               // 导入壁纸偏好
import com.memuo.core.storage.WallpaperSource              // 导入壁纸来源
import com.memuo.core.ui.ThemePresets                       // 导入主题库
import com.memuo.core.ui.AppIcons                          // 导入应用图标集
import com.memuo.core.ui.components.HeaderIconButton       // 导入顶栏图标按钮
import com.memuo.core.ui.components.LocalToast             // 导入 Toast 状态
import com.memuo.core.ui.components.MuyunSegmented         // 导入分段胶囊
import com.memuo.core.ui.components.MuyunToggle            // 导入 iOS 风格开关（主题切换）
import com.memuo.core.ui.components.ToastHost              // 导入 Toast 宿主
import com.memuo.core.ui.components.ToastState             // 导入 Toast 状态类
import com.memuo.core.ui.components.WallpaperBackground    // 导入壁纸背景
import com.memuo.core.ui.components.WallpaperRenderMode    // 导入渲染方式
import com.memuo.core.ui.rememberBitmap                    // 导入位图加载
import com.memuo.core.ui.theme.MuyunAccentLight            // 导入浅灰底
import com.memuo.core.ui.theme.MuyunBar                    // 导入顶栏半透明背景
import com.memuo.core.ui.theme.MuyunBrand2                 // 导入品牌青
import com.memuo.core.ui.theme.MuyunBrandGradient          // 导入品牌渐变
import com.memuo.core.ui.theme.MuyunBrandSoft              // 导入品牌浅底
import com.memuo.core.ui.theme.MuyunBrandSoft2             // 导入品牌浅底·青
import com.memuo.core.ui.theme.MuyunCard                   // 导入卡片白
import com.memuo.core.ui.theme.MuyunInfo                   // 导入信息蓝
import com.memuo.core.ui.theme.MuyunPurple                 // 导入品牌紫
import com.memuo.core.ui.theme.MuyunPurpleBg               // 导入品牌紫底
import com.memuo.core.ui.theme.MuyunText                   // 导入主文字色
import com.memuo.core.ui.theme.MuyunText2                  // 导入次级文字色
import com.memuo.core.ui.theme.MuyunText3                  // 导入三级文字色
import com.memuo.core.ui.theme.MuyunTheme                  // 导入沐云杪主题
import com.memuo.core.ui.theme.MuyunThemeState              // 导入主题状态
import com.memuo.core.ui.theme.toggleMuyunTheme            // 导入主题切换函数
import com.memuo.feature.chat.ChatScreen                   // 导入对话页
import com.memuo.feature.chat.ChatViewModel                // 导入对话 ViewModel
import com.memuo.feature.notes.NoteEditScreen               // 导入笔记编辑页
import com.memuo.feature.notes.NoteListScreen               // 导入笔记列表页
import com.memuo.feature.notes.NoteListViewModel            // 导入笔记 ViewModel
import com.memuo.feature.notes.TodoListScreen               // 导入待办清单页
import com.memuo.feature.notes.TrashScreen                  // 导入回收站页
import com.memuo.feature.settings.ApiManageScreen           // 导入云端 API 管理页
import com.memuo.feature.settings.DeviceCheckScreen         // 导入设备自检页
import com.memuo.feature.settings.KnowledgeDetailScreen     // 导入知识库详情页
import com.memuo.feature.settings.KnowledgeDetailViewModel  // 导入知识库详情 ViewModel
import com.memuo.feature.settings.KnowledgeScreen           // 导入知识库页
import com.memuo.feature.settings.KnowledgeViewModel        // 导入知识库 ViewModel
import com.memuo.feature.settings.LanTransferDialog         // 导入局域网传输弹窗
import com.memuo.feature.settings.LocalModelSelectScreen    // 导入本地模型选择页（M-035）
import com.memuo.feature.settings.MemoryScreen              // 导入记忆库页
import com.memuo.feature.settings.MigrateScreen             // 导入数据迁移页
import com.memuo.feature.settings.ModelManageScreen         // 导入模型管理页
import com.memuo.feature.settings.ModelManageViewModel      // 导入模型管理 ViewModel
import com.memuo.feature.settings.PermissionScreen          // 导入权限管理页
import com.memuo.feature.settings.SettingsHomeScreen        // 导入设置主页
import com.memuo.feature.settings.SettingsViewModel         // 导入设置 ViewModel
import com.memuo.feature.settings.WallpaperScreen           // 导入自定义壁纸页
import dagger.hilt.android.AndroidEntryPoint               // 导入 AndroidEntryPoint：Hilt 注入入口
import kotlinx.coroutines.flow.first                          // 导入 first：取流首值
import kotlinx.coroutines.launch                            // 导入 launch：启动协程
import kotlinx.coroutines.runBlocking                       // 导入 runBlocking：同步初始化主题
import javax.inject.Inject                                 // 导入 Inject：字段注入

@AndroidEntryPoint                                       // 注解：由 Hilt 管理本 Activity
class MainActivity : ComponentActivity() {               // 应用唯一 Activity（单 Activity 架构）

    @Inject lateinit var appPrefs: AppPrefs             // 注入应用偏好（首次启动标记）
    @Inject lateinit var wallPrefs: WallpaperPrefs      // 注入壁纸偏好（聊天页背景）

    override fun onCreate(savedInstanceState: Bundle?) { // 生命周期回调：Activity 创建时调用
        super.onCreate(savedInstanceState)               // 调用父类初始化
        enableEdgeToEdge()                               // 边到边显示（配合 imePadding 处理软键盘不顶页面）
        // 同步恢复上次主题（DataStore 首读极快，避免暗色/自定义主题用户启动时先闪默认样式）
        runCatching {                                    // 容错：DataStore 读取异常不阻塞启动
            runBlocking {                                // 同步初始化
                MuyunThemeState.isDark = appPrefs.isDarkMode()  // 恢复暗色标记
                val cfg = wallPrefs.config.first()       // 读一次壁纸/主题配置
                if (cfg.source == WallpaperSource.PRESET) {  // 用户选过主题
                    MuyunThemeState.theme = ThemePresets.byId(cfg.presetId) ?: ThemePresets.default  // 恢复主题
                }
            }
        }
        setContent { MuyunTheme { NoteApp(appPrefs, wallPrefs) } }  // 套品牌主题并渲染应用界面
    }
}

/**
 * 应用根界面 —— 按路由定制顶栏 + 侧边抽屉 + 单 Activity 导航（HTML 界面原型整体迁移）。
 * 顶栏（仅主页面显示）：菜单按钮 + 「常规|AI」会话胶囊 + 右侧操作（AI：新建会话/本地云端切换；
 * 常规：新建备忘录/回收站）。子页面自带 SubHeader，隐藏全局顶栏。
 * 首次启动进入设备自检页，完成后跳转常规备忘录主页。
 */
@OptIn(ExperimentalMaterial3Api::class)                  // 声明使用实验性 API（Drawer/Scaffold）
@Composable                                               // 可组合 UI 函数
fun NoteApp(                                             // 应用根组件
    appPrefs: AppPrefs,                                  // 应用偏好
    wallPrefs: WallpaperPrefs,                           // 壁纸偏好
) {
    // 首次启动状态三态：null=DataStore 读取中（毫秒级）/ false=未自检 / true=已自检
    // （AppPrefs.firstRunDone 现为 Flow<Boolean>，读取完成必发确定值，不会永久停在加载态）
    val firstRunDone by appPrefs.firstRunDone.collectAsState(initial = null as Boolean?)  // 订阅读取
    val wallCfg by wallPrefs.config.collectAsState(initial = WallpaperConfig())  // 壁纸配置
    val isDark = MuyunThemeState.isDark                  // 读主题状态（getter 内订阅，切换即重组）
    val toast = remember { ToastState() }                // 全局 Toast 状态
    val scope = rememberCoroutineScope()                 // 协程作用域（持久化主题用）

    // 切换主题：取反即时生效 + 持久化（保证下次启动一致）
    val toggleTheme: () -> Unit = {                      // 切换回调
        toggleMuyunTheme()                               // 取反（全局重组）
        scope.launch { appPrefs.setDarkMode(MuyunThemeState.isDark) }  // 异步持久化
    }

    // 全局背景来源（覆盖所有界面）：当前主题渐变 / 上传图片
    val theme = MuyunThemeState.theme                    // 当前主题（读状态，切换即重组）
    val wallBitmap = if (wallCfg.source == WallpaperSource.UPLOAD) rememberBitmap(wallCfg.imageUri?.let(Uri::parse)) else null  // 上传图
    val bgBrush = if (wallCfg.source == WallpaperSource.PRESET) theme.brush else null  // 仅选中主题时用渐变；默认/上传图走纯色或图片

    CompositionLocalProvider(LocalToast provides toast) {  // 下发 Toast 状态
        Box(modifier = Modifier.fillMaxSize()) {         // 根容器
            // 全局主题背景（z=0，覆盖状态栏/主页/子页/自检页所有界面，跟随当前主题）
            WallpaperBackground(                         // 主题背景层
                brush = bgBrush,                         // 主题渐变
                bitmap = wallBitmap,                     // 上传位图（可选）
                mode = when (wallCfg.mode) {             // 映射渲染方式
                    WallpaperMode.TILE -> WallpaperRenderMode.TILE          // 平铺
                    WallpaperMode.STRETCH -> WallpaperRenderMode.STRETCH    // 拉伸
                    WallpaperMode.CENTER -> WallpaperRenderMode.CENTER      // 居中
                    WallpaperMode.BLUR -> WallpaperRenderMode.BLUR          // 模糊
                },
            )
            if (firstRunDone == null) {                  // 偏好读取中（首值未到，极短）
                Box(                                     // 启动占位（带品牌字样，避免感知为空白）
                    modifier = Modifier.fillMaxSize().background(MuyunAccentLight),  // 浅灰底
                    contentAlignment = Alignment.Center,  // 居中
                ) {
                    Text(                                // 品牌占位文字
                        text = "沐云杪",                  // 内容
                        style = MaterialTheme.typography.titleLarge,  // 大标题
                        color = MuyunText3,               // 三级灰
                    )
                }
            } else {                                     // 已读取
                RootNav(                                 // 导航主体
                    skipCheck = firstRunDone == true,    // 已完成自检 → 跳过自检页
                    appPrefs = appPrefs,                 // 应用偏好（自检完成写标记）
                    toast = toast,                       // Toast 状态
                    isDark = isDark,                     // 当前主题
                    onToggleTheme = toggleTheme,         // 切换主题回调
                )
            }
            ToastHost(state = toast)                     // Toast 宿主（最上层覆盖）
        }
    }
}

/** 导航主体：顶栏 + 抽屉 + NavHost。 */
@OptIn(ExperimentalMaterial3Api::class)                  // 实验性 API
@Composable                                               // 可组合 UI 函数
private fun RootNav(                                     // 导航主体
    skipCheck: Boolean,                                  // 是否跳过自检页
    appPrefs: AppPrefs,                                  // 应用偏好
    toast: ToastState,                                   // Toast 状态
    isDark: Boolean,                                     // 当前是否暗色主题
    onToggleTheme: () -> Unit,                           // 切换主题回调
) {
    val nav = rememberNavController()                    // 导航控制器
    val drawerState = rememberDrawerState(DrawerValue.Closed)  // 抽屉状态（初始关闭）
    val scope = rememberCoroutineScope()                 // 协程作用域（开/关抽屉用）
    val settingsVm: SettingsViewModel = hiltViewModel()  // 设置 ViewModel（引擎切换/模型导入）
    val chatVm: ChatViewModel = hiltViewModel()          // 对话 ViewModel（会话列表/新建/云本地切换）
    val noteVm: NoteListViewModel = hiltViewModel()      // 笔记 ViewModel（新建备忘录）
    val navBackStackEntry by nav.currentBackStackEntryAsState()  // 当前导航栈项
    val currentRoute = navBackStackEntry?.destination?.route  // 当前路由

    var lanVisible by remember { mutableStateOf(false) }  // 局域网传输弹窗

    LaunchedEffect(Unit) { chatVm.loadConversations() }  // 加载会话列表（顶栏会话号/抽屉用）

    // 云本地切换提示（HTML showToast 行为）
    val engineMessage by chatVm.engineMessage.collectAsState()  // 订阅切换提示
    LaunchedEffect(engineMessage) {                      // 提示变化
        engineMessage?.let { toast.show(it); chatVm.consumeEngineMessage() }  // 弹 Toast 并消费
    }

    // 顶栏是否显示：仅主页（常规备忘录 / AI 对话）显示
    val showTopBar = currentRoute == "note/list" || currentRoute?.startsWith("chat/") == true  // 主页路由
    val inNote = currentRoute == "note/list" || currentRoute == "trash" || currentRoute?.startsWith("note/edit") == true  // 常规侧路由
    val sessionCount by chatVm.conversations.collectAsState()  // 会话列表（会话号）
    val engineType by settingsVm.engineType.collectAsState()  // 引擎类型（云本地胶囊）

    ModalNavigationDrawer(                                // 侧边抽屉容器
        drawerState = drawerState,                        // 抽屉状态
        drawerContent = {                                 // 抽屉内容（侧边菜单）
            AppDrawer(                                    // 侧边菜单
                chatViewModel = chatVm,                   // 共用对话 ViewModel（会话列表）
                currentRoute = currentRoute,              // 当前路由（会话高亮）
                isDark = isDark,                          // 当前主题
                onToggleTheme = onToggleTheme,            // 切换主题
                onNavigate = { route ->                   // 点击菜单项跳转
                    scope.launch { drawerState.close() }  // 先关抽屉
                    nav.navigate(route) { launchSingleTop = true }  // 再跳转
                },
                onOpenConversation = { id ->              // 点会话
                    scope.launch { drawerState.close() }  // 关抽屉
                    nav.navigate("chat/$id") { launchSingleTop = true }  // 进对话
                },
                onLan = {                                 // 点局域网传输
                    scope.launch { drawerState.close() }  // 关抽屉
                    lanVisible = true                     // 打开弹窗
                },
            )
        },
    ) {
        Scaffold(                                         // 主内容脚手架
            containerColor = Color.Transparent,           // 透明背景（让全局壁纸透出，覆盖所有界面）
            contentWindowInsets = WindowInsets(0, 0, 0, 0),  // 取消默认 insets：顶栏/底部由各页面 statusBarsPadding/navigationBarsPadding 自理
            topBar = {                                   // 顶部栏（仅主页显示）
                if (showTopBar) {                        // 主页路由
                    MainTopBar(                          // 自定义顶栏（对应 HTML .chat-header）
                        inNote = inNote,                 // 常规侧选中
                        sessionCount = sessionCount.size,  // 会话数（AI·N）
                        engineType = engineType,         // 当前引擎
                        onMenu = { scope.launch { drawerState.open() } },  // 打开抽屉
                        onModeNormal = {                 // 点「常规」
                            nav.navigate("note/list") { launchSingleTop = true }
                        },
                        onModeAi = {                     // 点「AI」
                            chatVm.ensureConversation { id -> nav.navigate("chat/$id") { launchSingleTop = true } }  // 确保会话并进入
                        },
                        onNewMemo = {                    // 新建备忘录（HTML 常规模式 ⊕）
                            noteVm.createNote { id -> nav.navigate("note/edit/$id") }
                        },
                        onTrash = { nav.navigate("trash") { launchSingleTop = true } },  // 回收站（launchSingleTop 防重复入栈）
                        onNewSession = {                 // 新建会话（HTML AI 模式 ⊕）
                            chatVm.newConversation { id -> nav.navigate("chat/$id") { launchSingleTop = true } }
                        },
                        onToggleCloudLocal = { chatVm.toggleCloudLocal() },  // 云本地切换（短按）
                        onLongPressEngine = {              // 长按进入配置（M-035）
                            if (engineType == EngineType.CLOUD) {  // 云端模式 → 云端配置
                                nav.navigate("api") { launchSingleTop = true }  // 跳 API 管理
                            } else {                       // 本地模式 → 本地模型选择
                                nav.navigate("local-model-select") { launchSingleTop = true }  // 跳本地模型选择
                            }
                        },
                    )
                }
            },
        ) { innerPadding ->                               // 内容区
            NavHost(                                      // 导航宿主
                navController = nav,                      // 导航控制器
                startDestination = if (skipCheck) "note/list" else "device-check",  // 首次启动先自检
                modifier = Modifier.padding(innerPadding),  // 避开顶部栏
                // 统一快速淡入淡出过渡：去除默认的滑动/组合动画，切换迅速、干净、自然
                enterTransition = { fadeIn(animationSpec = tween(160)) },        // 进入：160ms 淡入
                exitTransition = { fadeOut(animationSpec = tween(120)) },        // 退出：120ms 淡出
                popEnterTransition = { fadeIn(animationSpec = tween(160)) },     // 返回进入：160ms 淡入
                popExitTransition = { fadeOut(animationSpec = tween(120)) },     // 返回退出：120ms 淡出
            ) {
                composable("note/list") {                 // 笔记列表（主页·常规）
                    NoteListScreen(onOpenNote = { id -> nav.navigate("note/edit/$id") })
                }
                composable("note/edit/{noteId}") { entry ->  // 笔记编辑
                    val noteId = entry.arguments?.getString("noteId")?.toLongOrNull() ?: 0L
                    NoteEditScreen(noteId = noteId, onBack = { nav.popBackStack() })
                }
                composable("trash") { TrashScreen(onBack = { nav.popBackStack() }) }  // 回收站
                composable("todo") { TodoListScreen(onBack = { nav.popBackStack() }) }  // 待办清单
                composable("chat/{convId}") { entry ->    // 对话页
                    val convId = entry.arguments?.getString("convId")?.toLongOrNull() ?: 0L
                    ChatScreen(conversationId = convId)  // 对话（壁纸由全局背景统一渲染）
                }
                composable("device-check") {              // 设备自检（首次启动）
                    DeviceCheckScreen(onNext = {          // 下一步
                        scope.launch {                    // 协程中写标记
                            appPrefs.setFirstRunDone()    // 标记自检完成
                            nav.navigate("note/list") { popUpTo("device-check") { inclusive = true } }  // 跳主页并清栈
                        }
                    })
                }
                composable("settings") {                  // 设置主页
                    SettingsHomeScreen(
                        onBack = { nav.popBackStack() },  // 返回
                        onWallpaper = { nav.navigate("wallpaper") },  // 壁纸
                        onMigrate = { nav.navigate("migrate") },    // 迁移
                        onApi = { nav.navigate("api") },  // API
                        onPermission = { nav.navigate("perm") },   // 权限
                        onModel = { nav.navigate("model") },       // 模型
                    )
                }
                composable("wallpaper") { WallpaperScreen(onBack = { nav.popBackStack() }) }  // 自定义壁纸
                composable("migrate") { MigrateScreen(onBack = { nav.popBackStack() }) }      // 数据迁移
                composable("perm") { PermissionScreen(onBack = { nav.popBackStack() }) }      // 权限管理
                composable("api") { ApiManageScreen(onBack = { nav.popBackStack() }) }        // 云端 API 管理
                composable("model") { ModelRoute(onBack = { nav.popBackStack() }) }           // 模型管理（含 SAF 选择器）
                composable("local-model-select") { LocalModelSelectScreen(  // 本地模型选择（长按「本地」进入，M-035）
                    onBack = { nav.popBackStack() },      // 返回
                    onImportModel = { nav.navigate("model") { launchSingleTop = true } },  // 导入新模型 → 模型管理
                ) }
                composable("memory") { MemoryScreen(onBack = { nav.popBackStack() }) }        // 记忆库
                composable("knowledge") {                 // 知识库（文件夹列表）
                    val vm: KnowledgeViewModel = hiltViewModel()  // 知识库 ViewModel
                    KnowledgeScreen(                      // 知识库页
                        onBack = { nav.popBackStack() },  // 返回
                        onOpenFolder = { folder ->        // 点击文件夹 → 详情页
                            nav.navigate("knowledge/detail/${folder.folderId}/${Uri.encode(folder.name)}")  // 跳详情（中文名 URL 编码）
                        },
                        viewModel = vm,                   // 共用 ViewModel
                    )
                }
                composable("knowledge/detail/{folderId}/{folderName}") { entry ->  // 知识库详情（文件列表 + 添加文件）
                    KnowledgeDetailRoute(                 // 详情路由（含 SAF 选择器）
                        onBack = { nav.popBackStack() },  // 返回
                    )
                }
            }
        }
    }

    // 局域网传输弹窗（抽屉入口触发）
    LanTransferDialog(                                   // 弹窗
        visible = lanVisible,                            // 绑定状态
        onDismiss = { lanVisible = false },              // 关闭
    )
}

/**
 * 主页顶栏（MainTopBar）—— 对应 HTML 的 .chat-header：
 * 菜单按钮 + 居中「常规|AI」会话胶囊 + 右侧操作按钮组。
 */
@Composable                                               // 可组合 UI 函数
private fun MainTopBar(                                  // 主页顶栏
    inNote: Boolean,                                     // 是否常规侧
    sessionCount: Int,                                   // 会话数（AI·N 的 N）
    engineType: EngineType,                              // 当前引擎
    onMenu: () -> Unit,                                  // 菜单回调
    onModeNormal: () -> Unit,                            // 切常规
    onModeAi: () -> Unit,                                // 切 AI
    onNewMemo: () -> Unit,                               // 新建备忘录
    onTrash: () -> Unit,                                 // 回收站
    onNewSession: () -> Unit,                            // 新建会话
    onToggleCloudLocal: () -> Unit,                      // 云本地切换（短按）
    onLongPressEngine: () -> Unit,                       // 长按进入配置（本地模型选择/云端配置，M-035）
) {
    Row(                                                 // 横向布局
        modifier = Modifier                              // 修饰
            .fillMaxWidth()                              // 占满宽度
            .background(MuyunBar)                        // 半透明背景（含状态栏区，透出全局壁纸，暗色主题自适应）
            .statusBarsPadding()                         // 内容区避开状态栏（适配不同设备状态栏高度）
            .height(56.dp)                               // 内容高度 56（HTML .chat-header）
            .padding(horizontal = 16.dp),                // 左右内边距（HTML padding 12px 16px）
        verticalAlignment = Alignment.CenterVertically,   // 垂直居中
    ) {
        HeaderIconButton(                                // 菜单按钮（HTML .chat-menu-btn）
            icon = AppIcons.Menu,                        // 汉堡图标
            contentDescription = "菜单",                  // 描述
            onClick = onMenu,                            // 打开抽屉
        )
        // 会话胶囊容器：支持点击 + 左右滑动切换（HTML initCapsuleSwipe：>40px 阈值，
        // 左滑→AI、右滑→常规）。contentAlignment=Center 保证胶囊真居中（修复左右不对称）
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {  // 居中胶囊容器
            MuyunSegmented(                              // 会话胶囊（HTML .session-capsule）
                labels = listOf("常规", "AI"),           // 两段
                selectedIndex = if (inNote) 0 else 1,    // 当前选中
                onSelect = { if (it == 0) onModeNormal() else onModeAi() },  // 切换
                subLabels = mapOf(1 to "·$sessionCount"),  // AI 段副标签：会话号（HTML session-num）
                modifier = Modifier.pointerInput(inNote) {  // 滑动手势（HTML initCapsuleSwipe）
                    var dx = 0f                          // 本次手势累计水平位移
                    detectHorizontalDragGestures(        // 水平拖拽检测
                        onDragStart = { dx = 0f },       // 手势开始清零
                        onDragEnd = {                    // 松手判定
                            if (kotlin.math.abs(dx) > 40.dp.toPx()) {  // 超过 40px 阈值（HTML 同款）
                                if (dx < 0) onModeAi() else onModeNormal()  // 左滑→AI / 右滑→常规
                            }
                        },
                        onDragCancel = {},               // 取消忽略
                        onHorizontalDrag = { _, amount -> dx += amount },  // 拖动过程累积位移
                    )
                },
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {  // 右侧按钮组（HTML .chat-header-right）
            if (inNote) {                                // 常规模式按钮组（HTML #header-memo-group）
                HeaderIconButton(                        // 新建备忘录（⊕）
                    icon = AppIcons.Plus,                // 加号
                    contentDescription = "新建备忘录",     // 描述
                    onClick = onNewMemo,                 // 新建
                    tint = MuyunText2,                   // 次级灰
                )
                Box(                                     // 回收站胶囊（HTML .trash-switch）
                    modifier = Modifier                 // 修饰
                        .padding(start = 8.dp)          // 左留白
                        .clip(RoundedCornerShape(20.dp))  // 胶囊
                        .background(MuyunAccentLight)   // 浅灰底
                        .clickable { onTrash() }        // 点击
                        .padding(horizontal = 12.dp, vertical = 7.dp),  // 内边距（HTML padding 7px 12px）
                    contentAlignment = Alignment.Center,  // 居中
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {  // 图标 + 文字
                        Icon(                            // 垃圾桶小图标
                            imageVector = AppIcons.Trash,  // 图标
                            contentDescription = null,   // 装饰
                            tint = MuyunText3,           // 三级灰
                            modifier = Modifier.size(13.dp),  // 13dp（HTML svg 13）
                        )
                        Text(                            // 文字
                            text = "回收站",              // 内容
                            style = MaterialTheme.typography.labelMedium,  // 小字（HTML 12px）
                            fontWeight = FontWeight.Medium,  // 中粗
                            color = MuyunText2,          // 次级灰
                            modifier = Modifier.padding(start = 5.dp),  // 留白（HTML gap 5px）
                        )
                    }
                }
            } else {                                     // AI 模式按钮组（HTML #header-ai-group）
                HeaderIconButton(                        // 新建会话（⊕）
                    icon = AppIcons.Plus,                // 加号
                    contentDescription = "新建会话",       // 描述
                    onClick = onNewSession,              // 新建
                    tint = MuyunText2,                   // 次级灰
                )
                val isCloud = engineType == EngineType.CLOUD  // 是否云端
                Box(                                     // 本地/云端胶囊（HTML .chat-switch）
                    modifier = Modifier                 // 修饰
                        .padding(start = 8.dp)          // 左留白
                        .clip(RoundedCornerShape(20.dp))  // 胶囊
                        .background(if (isCloud) MuyunBrandSoft2 else MuyunAccentLight)  // 云端青浅底/浅灰（主题自适应）
                        .combinedClickable(              // 短按切换 / 长按进配置（M-035）
                            onClick = { onToggleCloudLocal() },       // 短按：切换模式
                            onLongClick = { onLongPressEngine() },    // 长按：进入配置
                        )
                        .padding(horizontal = 14.dp, vertical = 7.dp),  // 内边距（HTML padding 7px 14px）
                    contentAlignment = Alignment.Center,  // 居中
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {  // 圆点 + 文字
                        Box(                             // 状态圆点（HTML .dot）
                            modifier = Modifier         // 修饰
                                .size(6.dp)             // 6dp
                                .clip(CircleShape)      // 圆形
                                .background(if (isCloud) MuyunInfo else com.memuo.core.ui.theme.MuyunGreen),  // 云端蓝/本地绿
                        )
                        Text(                            // 文字
                            text = if (isCloud) "云端" else "本地",  // 内容（HTML switch-label）
                            style = MaterialTheme.typography.labelMedium,  // 小字（HTML 12px）
                            fontWeight = FontWeight.Medium,  // 中粗
                            color = if (isCloud) MuyunInfo else MuyunText2,  // 蓝/灰
                            modifier = Modifier.padding(start = 6.dp),  // 留白（HTML gap 6px）
                        )
                    }
                }
            }
        }
    }
}

/**
 * 侧边菜单（抽屉内容）—— 品牌头 + 知识库/记忆按钮 + 常用功能 + 会话区 + 设置。
 * 严格对应 HTML 侧边栏布局（旧版「对话引擎单选/导入模型/云端API配置」区域已随旧 UI 清除；
 * 引擎切换入口在聊天顶栏胶囊，模型导入入口在设置→模型管理页）。
 */
@Composable                                               // 可组合 UI 函数
private fun AppDrawer(                                    // 侧边菜单
    chatViewModel: ChatViewModel,                         // 对话 ViewModel（会话列表）
    currentRoute: String?,                                // 当前路由（会话高亮）
    isDark: Boolean,                                      // 当前是否暗色主题
    onToggleTheme: () -> Unit,                            // 切换主题回调
    onNavigate: (String) -> Unit,                         // 导航回调
    onOpenConversation: (Long) -> Unit,                   // 打开会话回调
    onLan: () -> Unit,                                    // 局域网传输回调
) {
    val conversations by chatViewModel.conversations.collectAsState()  // 订阅会话列表

    ModalDrawerSheet(modifier = Modifier.width(300.dp)) {  // 抽屉面板（宽 300dp，HTML .sidebar）
        Column(                                           // 纵向菜单
            modifier = Modifier                          // 修饰
                .fillMaxHeight()                         // 占满高度
                .statusBarsPadding()                     // 顶部避开状态栏（品牌头不被遮挡）
                .navigationBarsPadding()                 // 底部避开手势条（设置项不被遮挡）
                .padding(bottom = 8.dp),                 // 底部留白
        ) {
            // —— 品牌头（HTML .sidebar-header）——
            Row(                                          // 头部行
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),  // 内边距
                verticalAlignment = Alignment.CenterVertically,  // 垂直居中
            ) {
                Box(                                      // 品牌 logo（HTML .sidebar-logo 渐变底）
                    modifier = Modifier                 // 修饰
                        .size(34.dp)                    // 34dp
                        .clip(RoundedCornerShape(10.dp))  // 圆角 10
                        .background(MuyunBrandGradient)  // 品牌渐变
                        .shadow(8.dp, RoundedCornerShape(10.dp)),  // 品牌投影（HTML --shadow-brand）
                    contentAlignment = Alignment.Center,  // 居中
                ) {
                    Icon(                                // Logo 图标（GitHub 猫）
                        imageVector = AppIcons.Logo,      // 图标
                        contentDescription = null,        // 装饰
                        tint = Color.White,               // 白
                        modifier = Modifier.size(18.dp),  // 18dp
                    )
                }
                Text(                                    // 品牌名（HTML .sidebar-title）
                    text = "沐云杪",                      // 内容
                    style = MaterialTheme.typography.titleLarge.copy(  // 大标题（HTML 20px）
                        fontWeight = FontWeight.Bold,     // 粗体（HTML 700）
                        color = MuyunText,                // 主色
                    ),
                    modifier = Modifier.weight(1f).padding(start = 10.dp),  // 占满 + 留白
                )
                HeaderIconButton(                        // 关闭按钮（HTML .sidebar-close）
                    icon = AppIcons.Close,               // × 图标
                    contentDescription = "关闭",           // 描述
                    onClick = { onNavigate("note/list") },  // 关闭（回到主页，HTML 同效）
                    tint = MuyunText3,                   // 三级灰
                )
            }
            // —— 知识库/记忆大按钮（HTML .sidebar-ai-btns，简约实用：彩色图标底 + 文字）——
            Row(                                          // 两个按钮横排
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),  // 内边距
            ) {
                DrawerAiButton(AppIcons.Book, "知识库", iconTint = MuyunPurple, iconBg = MuyunPurpleBg, { onNavigate("knowledge") }, modifier = Modifier.weight(1f))  // 知识库（紫色辨识）
                Spacer(Modifier.width(10.dp))             // 间距（HTML gap 10）
                DrawerAiButton(AppIcons.Memory, "记忆", iconTint = MuyunBrand2, iconBg = MuyunBrandSoft2, { onNavigate("memory") }, modifier = Modifier.weight(1f))  // 记忆（青色辨识）
            }
            Text(                                         // 常用功能小标题（HTML .sidebar-hint）
                text = "常用功能",                         // 内容
                style = MaterialTheme.typography.labelSmall,  // 小字（HTML 11px）
                color = MuyunText3,                       // 三级灰
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),  // 内边距
            )
            DrawerMenuItem(AppIcons.Task, "任务") { onNavigate("todo") }        // 任务
            DrawerMenuItem(AppIcons.Wifi, "局域网传输") { onLan() }             // 局域网传输
            DrawerMenuItem(AppIcons.Upload, "数据迁移") { onNavigate("migrate") }  // 数据迁移
            // —— 会话区（HTML .sidebar-section-label 今天）——
            Text(                                         // 分组标签
                text = "今天",                            // 内容
                style = MaterialTheme.typography.labelSmall,  // 小字（HTML 11px）
                color = MuyunText3,                       // 三级灰
                modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp),  // 内边距
            )
            conversations.take(5).forEach { conv ->       // 最近 5 个会话（HTML 会话列表）
                val active = currentRoute == "chat/${conv.id}"  // 当前会话高亮
                Row(                                      // 会话行（HTML .sidebar-session）
                    modifier = Modifier                 // 修饰
                        .fillMaxWidth()                 // 占满宽度
                        .padding(horizontal = 20.dp, vertical = 4.dp)  // 内边距
                        .shadow(if (active) 2.dp else 1.dp, RoundedCornerShape(12.dp))  // 高亮投影
                        .clip(RoundedCornerShape(12.dp))  // 圆角 12
                        .background(if (active) MuyunBrandSoft else MuyunCard)  // 高亮品牌浅底/白
                        .clickable { onOpenConversation(conv.id) }  // 点击切换会话
                        .padding(horizontal = 16.dp, vertical = 14.dp),  // 内边距（HTML padding 14px 16px）
                    verticalAlignment = Alignment.CenterVertically,  // 垂直居中
                ) {
                    Text(                                // 会话名（HTML .sidebar-session-name）
                        text = conv.title.ifBlank { "新对话" },  // 内容
                        style = MaterialTheme.typography.bodyLarge,  // 字号（HTML 14px）
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,  // 高亮加粗
                        color = if (active) com.memuo.core.ui.theme.MuyunBrand else MuyunText,  // 高亮品牌色
                        modifier = Modifier.weight(1f),  // 占满
                    )
                    Text(                                // 类型（HTML .sidebar-session-type）
                        text = if (conv.engine == EngineType.LOCAL) "本地" else "云端",  // 内容
                        style = MaterialTheme.typography.labelSmall,  // 小字（HTML 12px）
                        color = if (active) com.memuo.core.ui.theme.MuyunBrand.copy(alpha = 0.75f) else MuyunText3,  // 高亮淡品牌色/灰
                    )
                }
            }
            Spacer(Modifier.weight(1f))                   // 底部占位（把设置压到下方）
            HorizontalDivider(color = MuyunAccentLight)   // 分割线（HTML sidebar-footer 上边框）
            // —— 主题切换行（太阳/月亮图标 + 开关）——
            Row(                                          // 主题切换行
                modifier = Modifier                       // 修饰
                    .fillMaxWidth()                       // 占满宽度
                    .clickable { onToggleTheme() }        // 点击切换
                    .padding(horizontal = 20.dp, vertical = 12.dp),  // 内边距
                verticalAlignment = Alignment.CenterVertically,  // 垂直居中
            ) {
                Box(                                      // 图标底
                    modifier = Modifier                  // 修饰
                        .size(32.dp)                     // 32dp
                        .clip(RoundedCornerShape(8.dp))  // 圆角 8
                        .background(MuyunAccentLight),   // 浅灰底
                    contentAlignment = Alignment.Center,  // 居中
                ) {
                    Icon(                                 // 太阳/月亮图标
                        imageVector = if (isDark) AppIcons.Sun else AppIcons.Moon,  // 暗色显示太阳（点击变亮）
                        contentDescription = "切换主题",   // 描述
                        tint = MuyunText2,                // 次级灰
                        modifier = Modifier.size(16.dp),  // 16dp
                    )
                }
                Text(                                     // 标签
                    text = "深色模式",                     // 内容
                    style = MaterialTheme.typography.bodyLarge,  // 字号
                    fontWeight = FontWeight.Medium,       // 中粗
                    color = MuyunText,                    // 主色
                    modifier = Modifier.weight(1f).padding(start = 12.dp),  // 占满 + 留白
                )
                MuyunToggle(                              // 开关
                    checked = isDark,                     // 绑定主题状态
                    onCheckedChange = { onToggleTheme() },  // 切换
                )
            }
            DrawerMenuItem(AppIcons.Gear, "设置") { onNavigate("settings") }  // 设置（HTML 底部入口）
        }
    }
}

/** 抽屉 AI 大按钮（简约实用：彩色圆角图标底 + 文字，提高辨识度）。 */
@Composable                                               // 可组合函数
private fun DrawerAiButton(                              // AI 大按钮
    icon: androidx.compose.ui.graphics.vector.ImageVector,  // 图标
    label: String,                                       // 文字
    iconTint: Color,                                     // 图标色（品牌色）
    iconBg: Color,                                       // 图标底色（浅底）
    onClick: () -> Unit,                                 // 点击
    modifier: Modifier = Modifier,                       // 外部修饰（父级 Row 传入 weight 均分）
) {
    Row(                                                  // 横向布局
        modifier = modifier                             // 应用外部修饰（weight 由父级作用域提供）
            .shadow(1.dp, RoundedCornerShape(12.dp))     // 轻投影（HTML --shadow）
            .clip(RoundedCornerShape(12.dp))             // 圆角 12
            .background(MuyunCard)                       // 白底
            .clickable { onClick() }                     // 点击
            .padding(horizontal = 14.dp, vertical = 13.dp),  // 内边距
        verticalAlignment = Alignment.CenterVertically,   // 垂直居中
    ) {
        Box(                                              // 彩色图标底（提高辨识度）
            modifier = Modifier                          // 修饰
                .size(32.dp)                             // 32dp
                .clip(RoundedCornerShape(9.dp))          // 圆角 9
                .background(iconBg),                     // 图标浅底
            contentAlignment = Alignment.Center,          // 居中
        ) {
            Icon(                                         // 图标
                imageVector = icon,                       // 矢量
                contentDescription = label,               // 描述
                tint = iconTint,                          // 品牌色
                modifier = Modifier.size(17.dp),          // 17dp
            )
        }
        Text(                                             // 文字
            text = label,                                 // 内容
            style = MaterialTheme.typography.bodyMedium,  // 字号（HTML 14px）
            fontWeight = FontWeight.SemiBold,             // 半粗
            color = MuyunText,                            // 主色
            modifier = Modifier.padding(start = 8.dp),    // 留白（HTML gap 8px）
        )
    }
}

/** 抽屉菜单行（HTML .sidebar-menu-item：图标 + 文字 + 右箭头）。 */
@Composable                                               // 可组合函数
private fun DrawerMenuItem(                              // 菜单行
    icon: androidx.compose.ui.graphics.vector.ImageVector,  // 图标
    label: String,                                       // 文字
    onClick: () -> Unit,                                 // 点击
) {
    Row(                                                  // 横向布局
        modifier = Modifier                              // 修饰
            .fillMaxWidth()                              // 占满宽度
            .clickable { onClick() }                     // 点击
            .padding(horizontal = 20.dp, vertical = 12.dp),  // 内边距（HTML padding 14px 12px）
        verticalAlignment = Alignment.CenterVertically,   // 垂直居中
    ) {
        Box(                                              // 图标底（HTML .icon）
            modifier = Modifier                          // 修饰
                .size(32.dp)                             // 32dp
                .clip(RoundedCornerShape(8.dp))          // 圆角 8
                .background(MuyunAccentLight),           // 浅灰底
            contentAlignment = Alignment.Center,          // 居中
        ) {
            Icon(                                         // 图标
                imageVector = icon,                       // 矢量
                contentDescription = label,               // 描述
                tint = MuyunText2,                        // 次级灰
                modifier = Modifier.size(16.dp),          // 16dp
            )
        }
        Text(                                             // 文字
            text = label,                                 // 内容
            style = MaterialTheme.typography.bodyLarge,   // 字号（HTML 15px）
            fontWeight = FontWeight.Medium,               // 中粗
            color = MuyunText,                            // 主色
            modifier = Modifier.weight(1f).padding(start = 12.dp),  // 占满 + 留白
        )
        Icon(                                             // 右箭头（HTML .arrow-right）
            imageVector = AppIcons.ChevronRight,          // 箭头
            contentDescription = null,                    // 装饰
            tint = MuyunText3,                            // 三级灰（主题自适应）
            modifier = Modifier.size(14.dp),              // 14dp
        )
    }
}

/**
 * 模型管理路由 —— 创建 SAF 文件夹选择器 + ViewModel，交给 ModelManageScreen。
 */
@Composable                                               // 可组合 UI 函数
private fun ModelRoute(                                   // 模型管理路由
    onBack: () -> Unit,                                   // 返回回调
) {
    val viewModel: ModelManageViewModel = hiltViewModel()  // 模型管理 ViewModel
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->  // 文件夹选择器
        uri?.let { viewModel.importFromUri(it) }         // 真实导入
    }
    ModelManageScreen(                                    // 模型管理页
        onBack = onBack,                                  // 返回
        onPickModel = { folderPicker.launch(null) },      // 打开 SAF
        viewModel = viewModel,                            // 共用 ViewModel
    )
}

/**
 * 知识库详情路由 —— 创建 SAF 选择器（文件夹/单文件）+ ViewModel，交给 KnowledgeDetailScreen。
 */
@Composable                                               // 可组合 UI 函数
private fun KnowledgeDetailRoute(                         // 知识库详情路由
    onBack: () -> Unit,                                   // 返回回调
) {
    val viewModel: KnowledgeDetailViewModel = hiltViewModel()  // 知识库详情 ViewModel
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->  // 文件夹选择器
        uri?.let { viewModel.ingestFolder(it) }           // 投喂文件夹
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->  // 单文件选择器
        uri?.let { viewModel.ingestFile(it) }             // 投喂单文件
    }
    KnowledgeDetailScreen(                                // 知识库详情页
        onBack = onBack,                                  // 返回
        onPickFolder = { folderPicker.launch(null) },     // 点文件夹按钮
        onPickFile = { filePicker.launch(arrayOf("*/*")) },  // 点文件按钮
        viewModel = viewModel,                            // 共用 ViewModel
    )
}
