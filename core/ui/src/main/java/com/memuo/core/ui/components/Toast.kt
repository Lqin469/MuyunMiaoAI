package com.memuo.core.ui.components                     // 声明包名：共享 UI 组件子包

import androidx.compose.animation.AnimatedVisibility      // 导入 AnimatedVisibility：显隐动画
import androidx.compose.animation.fadeIn                  // 导入 fadeIn：淡入
import androidx.compose.animation.fadeOut                 // 导入 fadeOut：淡出
import androidx.compose.animation.scaleIn                 // 导入 scaleIn：放大进入
import androidx.compose.animation.scaleOut                // 导入 scaleOut：缩小退出
import androidx.compose.foundation.background             // 导入 background：背景修饰
import androidx.compose.foundation.layout.Box             // 导入 Box：盒式布局
import androidx.compose.foundation.layout.BoxScope        // 导入 BoxScope：盒式作用域
import androidx.compose.foundation.layout.fillMaxSize     // 导入 fillMaxSize：铺满
import androidx.compose.foundation.layout.padding          // 导入 padding：内边距
import androidx.compose.foundation.shape.RoundedCornerShape  // 导入 RoundedCornerShape：圆角形状
import androidx.compose.material3.Text                    // 导入 Text：文本
import androidx.compose.runtime.Composable                // 导入 Composable：可组合函数注解
import androidx.compose.runtime.LaunchedEffect            // 导入 LaunchedEffect：副作用
import androidx.compose.runtime.getValue                  // 导入 getValue：by 委托
import androidx.compose.runtime.mutableStateOf            // 导入 mutableStateOf：可变状态
import androidx.compose.runtime.remember                  // 导入 remember：记住状态
import androidx.compose.runtime.setValue                  // 导入 setValue：by 委托写
import androidx.compose.runtime.staticCompositionLocalOf  // 导入 staticCompositionLocalOf：静态组合局部变量
import androidx.compose.ui.Alignment                      // 导入 Alignment：对齐
import androidx.compose.ui.Modifier                       // 导入 Modifier：修饰
import androidx.compose.ui.draw.clip                      // 导入 clip：裁剪
import androidx.compose.ui.graphics.Color                 // 导入 Color：颜色
import androidx.compose.ui.unit.dp                        // 导入 dp：尺寸单位

/**
 * Toast 状态（ToastState）—— 全局轻提示中枢。
 * 对应 HTML 的 #toast：居中深色胶囊、1.8 秒自动消失。
 * 通过 LocalToast 组合局部变量提供给任意页面调用 showToast("...")。
 */
class ToastState() {                                     // Toast 状态类（构造公开，写入仍限模块内）
    private var _message by mutableStateOf<String?>(null)  // 内部可变状态
    /** 当前提示文字（null = 隐藏）。 */
    var message: String?                                   // 对外只读
        get() = _message                                  // 读内部状态
        internal set(value) { _message = value }          // 仅模块内可写

    /** 显示一条提示（重复调用自动刷新计时）。 */
    fun show(text: String) {                              // 显示提示
        message = text                                   // 更新文字
    }
}

/** 组合局部变量：向 UI 树下发 Toast 状态。 */
val LocalToast = staticCompositionLocalOf { ToastState() }  // 默认空实现

/**
 * Toast 宿主（ToastHost）—— 放在应用最外层 Box 的末尾，
 * 渲染居中深色胶囊提示，1.8 秒后自动消失（对应 HTML showToast 的 1800ms）。
 */
@Composable                                              // 可组合函数
fun ToastHost(                                           // Toast 宿主
    state: ToastState,                                   // 状态来源
    modifier: Modifier = Modifier,                       // 外部修饰
) {
    val msg = state.message                              // 读取当前消息
    // 文字变化时启动自动隐藏计时
    LaunchedEffect(msg) {                                // 消息变化触发
        if (msg != null) {                               // 有消息
            kotlinx.coroutines.delay(1800)               // 等 1.8 秒（HTML 1800ms）
            state.message = null                         // 隐藏
        }
    }
    Box(                                                 // 覆盖层
        modifier = modifier.fillMaxSize(),               // 铺满（不拦截点击：无 clickable）
        contentAlignment = Alignment.Center,              // 居中
    ) {
        AnimatedVisibility(                              // 显隐动画
            visible = msg != null,                       // 有消息才显示
            enter = fadeIn() + scaleIn(initialScale = 0.9f),  // 淡入放大（HTML scale(0.9)→1）
            exit = fadeOut() + scaleOut(targetScale = 0.9f),  // 淡出缩小
        ) {
            Box(                                         // 胶囊
                modifier = Modifier                     // 修饰
                    .clip(RoundedCornerShape(12.dp))    // 圆角 12（HTML .toast）
                    .background(Color(0xE01E1E1E))      // 深色半透明底（HTML rgba(30,30,30,0.88)）
                    .padding(horizontal = 24.dp, vertical = 12.dp),  // 内边距
            ) {
                Text(                                    // 提示文字
                    text = msg.orEmpty(),                // 内容
                    color = Color.White,                 // 白字
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,  // 字号
                )
            }
        }
    }
}
