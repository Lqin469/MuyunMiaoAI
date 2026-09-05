package com.memuo.core.ui.components                     // 声明包名：共享 UI 组件子包

import androidx.compose.animation.animateColorAsState    // 导入 animateColorAsState：颜色动画
import androidx.compose.animation.core.tween              // 导入 tween：动画时长
import androidx.compose.foundation.background             // 导入 background：背景修饰
import androidx.compose.foundation.clickable              // 导入 clickable：点击修饰
import androidx.compose.foundation.layout.Box             // 导入 Box：盒式布局
import androidx.compose.foundation.layout.Row             // 导入 Row：横向布局
import androidx.compose.foundation.layout.padding          // 导入 padding：内边距
import androidx.compose.foundation.shape.RoundedCornerShape  // 导入 RoundedCornerShape：圆角形状
import androidx.compose.material3.MaterialTheme           // 导入 MaterialTheme：字体样式
import androidx.compose.material3.Text                    // 导入 Text：文本
import androidx.compose.runtime.Composable                // 导入 Composable：可组合函数注解
import androidx.compose.runtime.getValue                  // 导入 getValue：by 委托
import androidx.compose.ui.Alignment                      // 导入 Alignment：对齐
import androidx.compose.ui.Modifier                       // 导入 Modifier：修饰
import androidx.compose.ui.draw.clip                      // 导入 clip：裁剪
import androidx.compose.ui.graphics.Color                 // 导入 Color：颜色
import androidx.compose.ui.text.font.FontWeight           // 导入 FontWeight：字重
import androidx.compose.ui.unit.dp                        // 导入 dp：尺寸单位
import com.memuo.core.ui.theme.MuyunAccentLight           // 导入浅灰底
import com.memuo.core.ui.theme.MuyunBrand                 // 导入品牌色
import com.memuo.core.ui.theme.MuyunText2                 // 导入次级文字色
import com.memuo.core.ui.theme.MuyunText3                 // 导入三级文字色

/**
 * 分段胶囊（MuyunSegmented）—— 对应 HTML 的各处胶囊/Tab 分段：
 * 会话模式（常规|AI·N）、局域网 tabs、壁纸显示方式、任务/记忆 tabs 等。
 *
 * 设计要点：
 * - 顶层 Row 内容自适应宽度（wrap content），在父容器居中时保持紧凑，不撑满整条可用区域；
 * - 选中段背景与文字色「同时」用 animateColorAsState 渐变——背景慢淡、文字同步渐变，
 *   消除旧版「背景淡 + 文字 if(selected) 硬切」的割裂闪顿；
 * - 纯颜色插值（GPU 友好、不触发布局重算），无 offset 滑动块，切换零额外重组开销。
 */
@Composable                                              // 可组合函数
fun MuyunSegmented(                                      // 分段胶囊
    labels: List<String>,                                // 各段文字
    selectedIndex: Int,                                  // 当前选中下标
    onSelect: (Int) -> Unit,                             // 点选回调
    modifier: Modifier = Modifier,                       // 外部修饰
    subLabels: Map<Int, String> = emptyMap(),            // 副标签（如 AI 段的「·1」会话号）
) {
    Row(                                                 // 横向排布（内容自适应宽度，紧凑）
        modifier = modifier                             // 应用外部修饰
            .clip(RoundedCornerShape(10.dp))            // 外层圆角（HTML 胶囊 10px）
            .background(MuyunAccentLight)               // 胶囊底色（浅灰）
            .padding(3.dp),                             // 内边距 3dp
        verticalAlignment = Alignment.CenterVertically,  // 垂直居中
    ) {
        labels.forEachIndexed { index, label ->          // 遍历各段
            val selected = index == selectedIndex        // 是否选中
            val bg by animateColorAsState(               // 背景色动画（GPU 插值）
                targetValue = if (selected) MuyunBrand else Color.Transparent,  // 选中→品牌色
                animationSpec = tween(200),              // 200ms 短时长
                label = "segBg",                         // 动画标签
            )
            val textColor by animateColorAsState(        // 文字色动画（与背景同步，消除硬切）
                targetValue = if (selected) Color.White else MuyunText2,  // 选中白/未选中灰
                animationSpec = tween(200),              // 200ms
                label = "segText",                       // 标签
            )
            val subColor by animateColorAsState(         // 副标签色动画
                targetValue = if (selected) Color.White.copy(alpha = 0.8f) else MuyunText3,  // 选中淡白/未选中浅灰
                animationSpec = tween(200),              // 200ms
                label = "segSub",                        // 标签
            )
            Box(                                         // 单段容器
                modifier = Modifier                     // 修饰
                    .clip(RoundedCornerShape(8.dp))     // 段内圆角 8dp
                    .background(bg)                     // 背景（动画色）
                    .clickable { onSelect(index) }      // 点击选段
                    .padding(horizontal = 15.dp, vertical = 6.dp),  // 内边距（HTML 6px/15px）
                contentAlignment = Alignment.Center,     // 内容居中
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {  // 文字 + 副标签横排
                    Text(                                // 主文字
                        text = label,                    // 段名
                        color = textColor,               // 动画色
                        style = MaterialTheme.typography.labelLarge,  // 字体
                        fontWeight = FontWeight.SemiBold,  // 半粗
                    )
                    subLabels[index]?.let { sub ->       // 有副标签
                        Text(                            // 副标签（如 AI 的 ·N）
                            text = sub,                  // 内容
                            color = subColor,            // 动画色
                            style = MaterialTheme.typography.labelSmall,  // 小字
                        )
                    }
                }
            }
        }
    }
}
