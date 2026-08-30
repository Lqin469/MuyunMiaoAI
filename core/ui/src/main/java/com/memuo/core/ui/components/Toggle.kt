package com.memuo.core.ui.components                     // 声明包名：共享 UI 组件子包

import androidx.compose.animation.animateColorAsState    // 导入 animateColorAsState：颜色动画
import androidx.compose.animation.core.animateDpAsState   // 导入 animateDpAsState：位移动画
import androidx.compose.animation.core.tween              // 导入 tween：动画时长
import androidx.compose.foundation.background             // 导入 background：背景修饰
import androidx.compose.foundation.clickable              // 导入 clickable：点击修饰
import androidx.compose.foundation.layout.Box             // 导入 Box：盒式布局
import androidx.compose.foundation.layout.offset          // 导入 offset：位移修饰
import androidx.compose.foundation.layout.padding          // 导入 padding：内边距
import androidx.compose.foundation.layout.size            // 导入 size：固定尺寸
import androidx.compose.foundation.shape.CircleShape      // 导入 CircleShape：圆形
import androidx.compose.runtime.Composable                // 导入 Composable：可组合函数注解
import androidx.compose.runtime.getValue                  // 导入 getValue：by 委托
import androidx.compose.ui.Modifier                       // 导入 Modifier：修饰
import androidx.compose.ui.draw.clip                      // 导入 clip：裁剪
import androidx.compose.ui.graphics.Color                 // 导入 Color：颜色
import androidx.compose.ui.unit.dp                        // 导入 dp：尺寸单位
import com.memuo.core.ui.theme.MuyunDisabled             // 导入禁用色（未选中轨道）
import com.memuo.core.ui.theme.MuyunGreen                 // 导入成功绿

/**
 * iOS 风格开关（MuyunToggle）—— 对应 HTML 的 .toggle-switch：
 * 48×28 胶囊 + 24dp 白色圆钮，开=绿色、关=浅灰，切换带动画。
 * 用于：隐私库开关等场景。
 */
@Composable                                              // 可组合函数
fun MuyunToggle(                                         // iOS 风格开关
    checked: Boolean,                                    // 当前是否开启
    onCheckedChange: (Boolean) -> Unit,                  // 切换回调
) {
    val trackColor by animateColorAsState(               // 轨道色动画
        targetValue = if (checked) MuyunGreen else MuyunDisabled,  // 开绿/关灰（主题自适应）
        animationSpec = tween(200),                      // 200ms 短时长
        label = "toggleTrack",                           // 动画标签
    )
    val thumbOffset by animateDpAsState(                 // 圆钮位移动画
        targetValue = if (checked) 20.dp else 0.dp,      // 开→右移 20dp（HTML translateX(20px)）
        animationSpec = tween(200),                      // 200ms 短时长
        label = "toggleThumb",                           // 动画标签
    )
    Box(                                                 // 轨道容器
        modifier = Modifier                             // 修饰
            .size(width = 48.dp, height = 28.dp)        // 48×28（HTML 尺寸）
            .clip(CircleShape)                          // 胶囊形
            .background(trackColor)                     // 轨道色（动画）
            .clickable { onCheckedChange(!checked) },   // 点击切换
    ) {
        Box(                                             // 白色圆钮
            modifier = Modifier                         // 修饰
                .offset(x = thumbOffset, y = 2.dp)      // 位移（动画）
                .size(24.dp)                            // 24dp（HTML ::after 24×24）
                .clip(CircleShape)                      // 圆形
                .background(Color.White),               // 白底
        )
    }
}
