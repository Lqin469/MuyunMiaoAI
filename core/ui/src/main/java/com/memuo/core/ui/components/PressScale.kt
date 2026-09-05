package com.memuo.core.ui.components                     // 声明包名：共享 UI 组件子包

import androidx.compose.animation.core.animateFloatAsState  // 导入 animateFloatAsState：缩放动画
import androidx.compose.animation.core.tween              // 导入 tween：动画时长
import androidx.compose.foundation.LocalIndication        // 导入 LocalIndication：默认点击水波纹
import androidx.compose.foundation.clickable              // 导入 clickable：点击修饰
import androidx.compose.foundation.interaction.MutableInteractionSource  // 导入 MutableInteractionSource：按压态源
import androidx.compose.foundation.interaction.collectIsPressedAsState  // 导入 collectIsPressedAsState：订阅按压态
import androidx.compose.runtime.Composable                // 导入 Composable：可组合函数注解
import androidx.compose.runtime.getValue                  // 导入 getValue：by 委托
import androidx.compose.runtime.remember                  // 导入 remember：记住状态
import androidx.compose.ui.Modifier                       // 导入 Modifier：修饰
import androidx.compose.ui.graphics.graphicsLayer          // 导入 graphicsLayer：图形变换层（GPU，不触发重排）

/**
 * 按压缩放点击（pressClickable）—— 对应 HTML 的 :active transform: scale(0.96)。
 * 点击瞬间轻微缩放、松手回弹，给卡片/按钮/菜单行统一的触觉反馈，提升交互手感。
 * 内部自建 InteractionSource 驱动按压态，调用方无需额外传入；保留默认水波纹指示。
 * 缩放走 graphicsLayer（仅 GPU 变换，不触发布局重算），对列表/过渡动画零性能负担。
 */
@Composable                                              // 可组合函数
fun Modifier.pressClickable(                             // 按压缩放点击修饰
    scale: Float = 0.96f,                                // 按下缩放比（默认 0.96，HTML 同款）
    enabled: Boolean = true,                             // 是否可点击
    onClick: () -> Unit,                                 // 点击回调
): Modifier {
    val interaction = remember { MutableInteractionSource() }  // 按压态源
    val pressed by interaction.collectIsPressedAsState()  // 订阅是否按下
    val s by animateFloatAsState(                         // 缩放动画
        targetValue = if (pressed) scale else 1f,         // 按下缩小 / 松手回弹
        animationSpec = tween(durationMillis = 120),      // 120ms 快速回弹
        label = "pressScale",                             // 动画标签
    )
    return this                                          // 链式修饰
        .clickable(                                      // 点击（保留水波纹指示）
            interactionSource = interaction,             // 共享按压态源
            indication = LocalIndication.current,        // 默认指示（水波纹）
            enabled = enabled,                           // 是否可点
            onClick = onClick,                           // 回调
        )
        .graphicsLayer {                                 // 图形变换层
            scaleX = s                                   // 横向缩放
            scaleY = s                                   // 纵向缩放
        }
}
