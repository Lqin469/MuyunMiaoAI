package com.memuo.core.ui.components                     // 声明包名：共享 UI 组件子包

import androidx.compose.animation.core.animateFloatAsState  // 导入 animateFloatAsState：位移动画
import androidx.compose.animation.core.tween              // 导入 tween：动画时长曲线
import androidx.compose.foundation.background             // 导入 background：背景修饰
import androidx.compose.foundation.clickable              // 导入 clickable：点击修饰
import androidx.compose.foundation.gestures.Orientation   // 导入 Orientation：方向枚举
import androidx.compose.foundation.gestures.draggable     // 导入 draggable：水平拖拽手势
import androidx.compose.foundation.gestures.rememberDraggableState  // 导入 rememberDraggableState：拖拽状态
import androidx.compose.foundation.layout.Box             // 导入 Box：盒式布局
import androidx.compose.foundation.layout.BoxScope        // 导入 BoxScope：盒式作用域
import androidx.compose.foundation.layout.Column          // 导入 Column：纵向布局
import androidx.compose.foundation.layout.fillMaxHeight   // 导入 fillMaxHeight：占满高度
import androidx.compose.foundation.layout.fillMaxWidth    // 导入 fillMaxWidth：占满宽度
import androidx.compose.foundation.layout.offset          // 导入 offset：位移修饰
import androidx.compose.foundation.layout.width           // 导入 width：固定宽度
import androidx.compose.foundation.shape.RoundedCornerShape  // 导入 RoundedCornerShape：圆角形状
import androidx.compose.material3.Icon                    // 导入 Icon：图标
import androidx.compose.material3.Text                    // 导入 Text：文本
import androidx.compose.runtime.Composable                // 导入 Composable：可组合函数注解
import androidx.compose.runtime.getValue                  // 导入 getValue：by 委托
import androidx.compose.runtime.mutableFloatStateOf       // 导入 mutableFloatStateOf：浮点状态
import androidx.compose.runtime.remember                  // 导入 remember：记住状态
import androidx.compose.runtime.setValue                  // 导入 setValue：by 委托写
import androidx.compose.ui.Alignment                      // 导入 Alignment：对齐
import androidx.compose.ui.Modifier                       // 导入 Modifier：修饰
import androidx.compose.ui.draw.clip                      // 导入 clip：裁剪
import androidx.compose.ui.graphics.Color                 // 导入 Color：颜色
import androidx.compose.ui.platform.LocalDensity           // 导入 LocalDensity：px/dp 换算
import androidx.compose.ui.unit.dp                        // 导入 dp：尺寸单位
import com.memuo.core.ui.AppIcons                         // 导入应用图标集
import com.memuo.core.ui.theme.MuyunDanger                // 导入危险红

/**
 * 左滑删除容器（SwipeToReveal）—— 对应 HTML 备忘录的左滑手势：
 * 向左滑动露出右侧 88dp 红色删除层；超过一半自动展开，否则回弹；
 * 展开后点击卡片先收起（HTML 的「点击已展开卡片 → 收起」行为）。
 * 列表页需维护「当前展开项 id」以保证同一时间只展开一张。
 */
@Composable                                              // 可组合函数
fun SwipeToReveal(                                       // 左滑删除容器
    revealed: Boolean,                                   // 当前是否展开（由父级统一管理）
    onRevealChanged: (Boolean) -> Unit,                  // 展开状态变化回调
    actionLabel: String,                                 // 删除层文字（如「删除」）
    onAction: () -> Unit,                                // 点击删除层
    modifier: Modifier = Modifier,                       // 外部修饰
    content: @Composable BoxScope.() -> Unit,            // 卡片内容
) {
    val revealPx = with(LocalDensity.current) { 88.dp.toPx() }  // 88dp → px（HTML .memo-delete-bg width）
    var dragging by remember { mutableFloatStateOf(0f) }  // 拖拽中的临时位移
    val target = if (revealed) -revealPx else 0f          // 目标位移：展开 -88 / 收起 0
    val offsetX by animateFloatAsState(                   // 位移动画
        targetValue = if (dragging != 0f) dragging else target,  // 拖拽时跟手，松手后动画
        animationSpec = tween(durationMillis = 250),      // 250ms 回弹（HTML transition 0.25s）
        label = "swipeOffset",                            // 动画标签
    )

    Box(                                                 // 容器
        modifier = modifier                             // 应用外部修饰
            .clip(RoundedCornerShape(14.dp)),           // 圆角 14（裁剪删除层直角）
    ) {
        // 底部红色删除层（固定在右侧 88dp，卡片左移时露出）
        Box(                                             // 删除层
            modifier = Modifier                         // 修饰
                .align(Alignment.CenterEnd)             // 靠右对齐
                .fillMaxHeight()                        // 占满高度
                .width(88.dp)                           // 88dp 宽
                .background(MuyunDanger)                // 危险红
                .clickable { onAction() },              // 点击触发删除
            contentAlignment = Alignment.Center,         // 内容居中
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {  // 图标+文字纵向
                Icon(                                    // 垃圾桶图标
                    imageVector = AppIcons.Trash,        // 图标
                    contentDescription = actionLabel,    // 描述
                    tint = Color.White,                  // 白
                )
                Text(                                    // 删除文字
                    text = actionLabel,                  // 「删除」
                    color = Color.White,                 // 白
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,  // 小字
                )
            }
        }
        // 卡片本体（可水平拖拽）
        Box(                                             // 卡片层
            modifier = Modifier                         // 修饰
                .fillMaxWidth()                         // 占满宽度
                .offset(x = with(LocalDensity.current) { offsetX.toDp() })  // 应用位移（-88~0）
                .draggable(                             // 水平拖拽手势
                    orientation = Orientation.Horizontal,  // 仅水平（垂直留给列表滚动）
                    state = rememberDraggableState { delta ->  // 拖拽增量回调
                        dragging = (dragging + delta).coerceIn(-revealPx, 0f)  // 累计位移并钳制 [-88, 0]
                    },
                    onDragStopped = {                    // 松手回调
                        val settle = if (dragging < -revealPx / 2) -revealPx else 0f  // 过半展开/否则回弹
                        dragging = 0f                    // 结束拖拽（交由动画接手）
                        onRevealChanged(settle != 0f)    // 通知父级最终状态
                    },
                ),
        ) {
            content()                                    // 渲染卡片内容
        }
    }
}
