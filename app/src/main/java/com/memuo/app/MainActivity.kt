package com.memuo.app                                    // 声明包名：com.memuo.app

import android.os.Bundle                                 // 导入 Bundle：Activity 状态传递
import androidx.activity.ComponentActivity                // 导入 ComponentActivity：Compose 基础 Activity
import androidx.activity.compose.setContent               // 导入 setContent：把 Compose 界面挂载到 Activity
import androidx.compose.foundation.layout.padding          // 导入 padding：外边距
import androidx.compose.material3.NavigationBar           // 导入 NavigationBar：底部导航栏（Material3）
import androidx.compose.material3.NavigationBarItem       // 导入 NavigationBarItem：底部导航项（Material3）
import androidx.compose.material3.MaterialTheme          // 导入 MaterialTheme：Material3 主题
import androidx.compose.material3.Scaffold                // 导入 Scaffold：页面脚手架（含 bottomBar 槽位）
import androidx.compose.material3.Text                    // 导入 Text：文本
import androidx.compose.runtime.Composable               // 导入 Composable：可组合函数注解
import androidx.compose.runtime.LaunchedEffect            // 导入 LaunchedEffect：副作用（加载会话）
import androidx.compose.runtime.getValue                  // 导入 getValue：by 委托
import androidx.compose.runtime.mutableStateOf            // 导入 mutableStateOf：可组合状态
import androidx.compose.runtime.remember                   // 导入 remember：记住状态
import androidx.compose.runtime.setValue                  // 导入 setValue：by 委托写
import androidx.compose.ui.Modifier                       // 导入 Modifier：修饰
import androidx.compose.ui.unit.dp                        // 导入 dp：尺寸单位
import androidx.hilt.navigation.compose.hiltViewModel     // 导入 hiltViewModel：Hilt 提供 ViewModel
import androidx.navigation.compose.NavHost               // 导入 NavHost：导航宿主容器
import androidx.navigation.compose.composable            // 导入 composable：注册单个路由
import androidx.navigation.compose.rememberNavController  // 导入 rememberNavController：创建导航控制器
import com.memuo.feature.chat.ChatScreen                   // 导入对话页
import com.memuo.feature.chat.ChatViewModel                // 导入对话 ViewModel
import com.memuo.feature.notes.NoteEditScreen             // 导入笔记编辑页
import com.memuo.feature.notes.NoteListScreen             // 导入笔记列表页
import com.memuo.feature.settings.SettingsScreen           // 导入设置页
import dagger.hilt.android.AndroidEntryPoint             // 导入 AndroidEntryPoint：Hilt 注入入口

@AndroidEntryPoint                                       // 注解：由 Hilt 管理本 Activity
class MainActivity : ComponentActivity() {               // 应用唯一 Activity（单 Activity 架构）
    override fun onCreate(savedInstanceState: Bundle?) { // 生命周期回调：Activity 创建时调用
        super.onCreate(savedInstanceState)               // 调用父类初始化
        setContent { NoteApp() }                         // 渲染应用界面
    }
}

/**
 * 应用根界面 —— 底部导航三栏（笔记 / 对话 / 设置），单 Activity 架构。
 * 笔记 tab 内含 列表 ⇄ 编辑 的二级导航；对话 tab 自动管理会话；设置 tab 提供引擎切换/模型导入/云端配置。
 */
@Composable                                               // 可组合 UI 函数
fun NoteApp() {                                          // 应用根组件
    MaterialTheme {                                      // 套上 Material3 主题
        val nav = rememberNavController()                // 创建导航控制器
        var selectedTab by remember { mutableStateOf("note") }  // 当前选中的底部 tab

        Scaffold(                                        // 页面脚手架（含底部导航）
            bottomBar = {                                // 底部导航栏
                NavigationBar {                          // 底部导航容器（Material3）
                    NavigationBarItem(                   // 笔记 tab
                        selected = selectedTab == "note",  // 选中态
                        onClick = {                       // 点击切换
                            selectedTab = "note"          // 更新选中
                            nav.navigate("note/list") {   // 切到笔记列表（清空返回栈到起点）
                                popUpTo("note/list") { inclusive = false }
                                launchSingleTop = true    // 避免重复入栈
                            }
                        },
                        icon = { Text("📝") },            // 图标（文字）
                        label = { Text("笔记") },          // 标签
                    )
                    NavigationBarItem(                   // 对话 tab
                        selected = selectedTab == "chat",  // 选中态
                        onClick = {                       // 点击切换
                            selectedTab = "chat"          // 更新选中
                            nav.navigate("chat") {        // 切到对话页
                                popUpTo("note/list") { inclusive = false }
                                launchSingleTop = true    // 避免重复入栈
                            }
                        },
                        icon = { Text("💬") },            // 图标
                        label = { Text("对话") },          // 标签
                    )
                    NavigationBarItem(                   // 设置 tab
                        selected = selectedTab == "settings",  // 选中态
                        onClick = {                       // 点击切换
                            selectedTab = "settings"      // 更新选中
                            nav.navigate("settings") {    // 切到设置页
                                popUpTo("note/list") { inclusive = false }
                                launchSingleTop = true    // 避免重复入栈
                            }
                        },
                        icon = { Text("⚙️") },            // 图标
                        label = { Text("设置") },          // 标签
                    )
                }
            },
        ) { innerPadding ->                               // 内容区（带内边距）
            NavHost(                                      // 导航宿主
                navController = nav,                      // 导航控制器
                startDestination = "note/list",           // 起始路由：笔记列表
                modifier = Modifier.padding(innerPadding),  // 避开底部导航栏
            ) {
                composable("note/list") {                 // 注册笔记列表路由
                    NoteListScreen(onOpenNote = { id -> nav.navigate("note/edit/$id") })  // 点击笔记跳转编辑页
                }
                composable("note/edit/{noteId}") { entry ->  // 注册笔记编辑路由
                    val noteId = entry.arguments?.getString("noteId")?.toLongOrNull() ?: 0L  // 取笔记 ID
                    NoteEditScreen(noteId = noteId, onBack = { nav.popBackStack() })  // 编辑页，返回弹栈
                }
                composable("chat") {                      // 注册对话路由
                    ChatTab()                             // 对话 tab（自动管理会话）
                }
                composable("settings") {                  // 注册设置路由
                    SettingsScreen(onBack = {             // 设置页，返回切回笔记 tab
                        selectedTab = "note"
                        nav.navigate("note/list") { popUpTo("note/list") { inclusive = false }; launchSingleTop = true }
                    })
                }
            }
        }
    }
}

/**
 * 对话 tab —— 进入时自动加载会话列表：有则用最新会话，无则新建（回调拿到真实 ID 后再渲染对话页）。
 */
@Composable                                               // 可组合 UI 函数
private fun ChatTab(                                      // 对话 tab
    viewModel: ChatViewModel = hiltViewModel(),           // Hilt 提供 ViewModel
) {
    var conversationId by remember { mutableStateOf(0L) } // 当前会话 ID（0 = 加载中）

    LaunchedEffect(Unit) {                                // 进入时执行一次
        viewModel.ensureConversation { id -> conversationId = id }  // 确保会话存在（有则复用，无则新建），回调拿真实 ID
    }

    if (conversationId != 0L) {                           // 会话就绪
        ChatScreen(conversationId = conversationId, viewModel = viewModel)  // 渲染对话页（共用同一 ViewModel）
    } else {                                              // 加载中
        Text("正在加载对话…", modifier = Modifier.padding(24.dp))  // 占位提示
    }
}
