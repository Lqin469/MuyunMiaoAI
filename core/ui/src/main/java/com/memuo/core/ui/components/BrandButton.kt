package com.memuo.core.ui.components                     // 声明包名：共享 UI 组件子包

import androidx.compose.foundation.background             // 导入 background：背景修饰
import androidx.compose.foundation.clickable              // 导入 clickable：点击修饰
import androidx.compose.foundation.layout.Box             // 导入 Box：盒式布局
import androidx.compose.foundation.layout.fillMaxWidth    // 导入 fillMaxWidth：占满宽度
import androidx.compose.foundation.layout.height          // 导入 height：固定高度
import androidx.compose.foundation.layout.padding          // 导入 padding：内边距
import androidx.compose.foundation.shape.RoundedCornerShape  // 导入 RoundedCornerShape：圆角形状
import androidx.compose.material3.Text                    // 导入 Text：文本
import androidx.compose.runtime.Composable                // 导入 Composable：可组合函数注解
import androidx.compose.ui.Alignment                      // 导入 Alignment：对齐
import androidx.compose.ui.Modifier                       // 导入 Modifier：修饰
import androidx.compose.ui.draw.clip                      // 导入 clip：裁剪
import androidx.compose.ui.draw.shadow                    // 导入 shadow：投影
import androidx.compose.ui.graphics.Brush                 // 导入 Brush：画刷（背景类型）
import androidx.compose.ui.graphics.Color                 // 导入 Color：颜色
import androidx.compose.ui.graphics.SolidColor            // 导入 SolidColor：纯色画刷（禁用态用）
import androidx.compose.ui.text.font.FontWeight           // 导入 FontWeight：字重
import androidx.compose.ui.unit.Dp                        // 导入 Dp：尺寸单位
import androidx.compose.ui.unit.dp                        // 导入 dp：尺寸单位
import com.memuo.core.ui.theme.MuyunBrandGradient         // 导入品牌渐变
import com.memuo.core.ui.theme.MuyunDisabled              // 导入禁用色
import com.memuo.core.ui.theme.MuyunText2                 // 导入次级文字色

/**
 * 品牌主按钮（BrandButton）—— 对应 HTML 的 .check-btn：
 * 品牌渐变背景 + 品牌投影 + 按下缩放 + 禁用灰态。
 * 全应用所有"主操作按钮"（下一步/开始迁移/保存/应用壁纸…）统一用它。
 */
@Composable                                              // 可组合函数
fun BrandButton(                                         // 品牌主按钮
    text: String,                                        // 按钮文字
    onClick: () -> Unit,                                 // 点击回调
    modifier: Modifier = Modifier,                       // 外部修饰（宽度/边距）
    enabled: Boolean = true,                             // 是否可用
    height: Dp = 52.dp,                                  // 按钮高度（HTML padding 16px + 字号 16）
) {
    Box(                                                 // 盒式容器（文字居中）
        modifier = modifier                             // 应用外部修饰
            .fillMaxWidth()                             // 默认占满宽度
            .height(height)                             // 固定高度
            .shadow(                                     // 品牌投影（HTML --shadow-brand）
                elevation = if (enabled) 12.dp else 0.dp,  // 禁用时无投影
                shape = RoundedCornerShape(14.dp),      // 投影跟随圆角
                ambientColor = Color(0x484F46E5),       // 环境色：品牌色半透明
                spotColor = Color(0x484F46E5),          // 聚光色：品牌色半透明
            )
            .clip(RoundedCornerShape(14.dp))            // 圆角 14（HTML --radius）
            .background(                                 // 背景
                if (enabled) MuyunBrandGradient          // 可用：品牌渐变
                else SolidColor(MuyunDisabled),          // 禁用：浅灰纯色画刷
            )
            .clickable(enabled = enabled) { onClick() }, // 点击触发（禁用时不响应）
        contentAlignment = Alignment.Center,             // 内容居中
    ) {
        Text(                                            // 按钮文字
            text = text,                                 // 内容
            color = if (enabled) Color.White else MuyunText2,  // 可用白字 / 禁用灰字
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,  // 中等字号
            fontWeight = FontWeight.SemiBold,            // 半粗字重
        )
    }
}
