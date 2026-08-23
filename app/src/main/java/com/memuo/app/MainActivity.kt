package com.memuo.app                                    // 声明包名：com.memuo.app

import android.os.Bundle                                 // 导入 Bundle：Activity 状态传递
import androidx.activity.ComponentActivity                // 导入 ComponentActivity：Compose 基础 Activity
import androidx.activity.compose.setContent               // 导入 setContent：把 Compose 界面挂载到 Activity
import androidx.compose.material3.MaterialTheme          // 导入 MaterialTheme：Material3 主题
import androidx.compose.runtime.Composable               // 导入 Composable：可组合函数注解
import androidx.navigation.compose.NavHost               // 导入 NavHost：导航宿主容器
import androidx.navigation.compose.composable            // 导入 composable：注册单个路由
import androidx.navigation.compose.rememberNavController  // 导入 rememberNavController：创建导航控制器
import com.memuo.feature.notes.NoteEditScreen             // 导入笔记编辑页
import com.memuo.feature.notes.NoteListScreen             // 导入笔记列表页
import dagger.hilt.android.AndroidEntryPoint             // 导入 AndroidEntryPoint：Hilt 注入入口

@AndroidEntryPoint                                       // 注解：由 Hilt 管理本 Activity
class MainActivity : ComponentActivity() {               // 应用唯一 Activity（单 Activity 架构）
    override fun onCreate(savedInstanceState: Bundle?) { // 生命周期回调：Activity 创建时调用
        super.onCreate(savedInstanceState)               // 调用父类初始化
        setContent { NoteApp() }                         // 渲染应用界面（导航到笔记）
    }
}

/**
 * 应用根界面 —— 导航宿主：常规备忘录（列表 ⇄ 编辑）。
 * 后续 M3 起将在此接入抽屉双模式（常规备忘录 / AI 备忘录）。
 */
@Composable                                               // 可组合 UI 函数
fun NoteApp() {                                          // 应用根组件
    MaterialTheme {                                      // 套上 Material3 主题
        val nav = rememberNavController()                // 创建导航控制器（记住状态）
        NavHost(navController = nav, startDestination = "note/list") {  // 导航宿主，起始路由为笔记列表
            composable("note/list") {                    // 注册笔记列表路由
                NoteListScreen(onOpenNote = { id -> nav.navigate("note/edit/$id") })  // 点击笔记跳转编辑页
            }
            composable("note/edit/{noteId}") { entry ->  // 注册笔记编辑路由（带参数 noteId）
                val noteId = entry.arguments?.getString("noteId")?.toLongOrNull() ?: 0L  // 从路由参数取笔记 ID
                NoteEditScreen(noteId = noteId, onBack = { nav.popBackStack() })  // 渲染编辑页，返回时弹栈
            }
        }
    }
}
