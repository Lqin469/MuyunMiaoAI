package com.memuo.app                                    // 声明包名：com.memuo.app

import android.os.Bundle                                 // 导入 Bundle：用于在 Activity 重建时保存/恢复界面状态
import androidx.activity.ComponentActivity                // 导入 ComponentActivity：AndroidX 提供的基础 Activity 基类
import androidx.activity.compose.setContent               // 导入 setContent：Compose 扩展函数，把界面挂载到 Activity
import androidx.compose.foundation.layout.Box             // 导入 Box：Compose 布局容器，可叠加/对齐子内容
import androidx.compose.foundation.layout.fillMaxSize     // 导入 fillMaxSize：让组件填满父容器（占满屏幕）
import androidx.compose.material3.MaterialTheme          // 导入 MaterialTheme：Material3 主题容器（统一颜色/字体）
import androidx.compose.material3.Surface                // 导入 Surface：带背景/圆角的界面底板
import androidx.compose.material3.Text                   // 导入 Text：显示文本的组件
import androidx.compose.runtime.Composable               // 导入 Composable：声明"可组合函数"的注解（Compose 组件）
import androidx.compose.ui.Alignment                     // 导入 Alignment：对齐方式（如居中）
import androidx.compose.ui.Modifier                      // 导入 Modifier：链式修饰组件的工具类（尺寸/边距等）
import dagger.hilt.android.AndroidEntryPoint             // 导入 AndroidEntryPoint：Hilt 注解，允许向 Activity 注入依赖

@AndroidEntryPoint                                       // 注解：声明本 Activity 由 Hilt 管理，可注入 ViewModel 等依赖
class MainActivity : ComponentActivity() {               // 应用唯一 Activity（单 Activity 架构），继承 Compose 基础 Activity
    override fun onCreate(savedInstanceState: Bundle?) { // 生命周期回调：Activity 创建时由系统自动调用
        super.onCreate(savedInstanceState)               // 必须调用父类实现，完成系统级初始化（放在最前）
        setContent { MemoPlaceholder() }                 // 用 Compose 渲染界面：挂载占位页面组件
    }
}

/** M0 占位页：仅验证工程可构建。后续接入抽屉双模式导航（常规备忘录 / AI 备忘录）。 */
@Composable                                               // 注解：声明这是一个可组合 UI 函数（Compose 组件）
fun MemoPlaceholder() {                                   // 占位页面：显示一行文字
    MaterialTheme {                                       // 套上 Material3 主题（全局配色/排版统一）
        Surface(modifier = Modifier.fillMaxSize()) {      // 白色底板，铺满整个屏幕
            Box(contentAlignment = Alignment.Center) {    // 盒子容器：子内容水平垂直居中
                Text("MuyunMiaoAI · M0 骨架就绪", style = MaterialTheme.typography.titleMedium)  // 居中显示一行标题文字
            }
        }
    }
}
