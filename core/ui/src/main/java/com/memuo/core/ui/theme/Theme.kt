package com.memuo.core.ui.theme                       // 声明包名：共享 UI 的主题子包

import androidx.compose.material3.ColorScheme          // 导入 ColorScheme：配色方案
import androidx.compose.material3.MaterialTheme        // 导入 MaterialTheme：Material3 主题入口
import androidx.compose.material3.darkColorScheme      // 导入 darkColorScheme：暗色配色构造器
import androidx.compose.material3.lightColorScheme     // 导入 lightColorScheme：浅色配色构造器
import androidx.compose.runtime.Composable             // 导入 Composable：可组合函数注解
import androidx.compose.ui.graphics.Color              // 导入 Color：颜色
import androidx.compose.ui.text.TextStyle              // 导入 TextStyle：文本样式
import androidx.compose.ui.text.font.FontWeight        // 导入 FontWeight：字重
import androidx.compose.ui.unit.sp                     // 导入 sp：字号单位

/**
 * 生成 Material3 ColorScheme（亮/暗各一套）。
 * primary/secondary 跟随「当前主题」强调色（MuyunThemeState.theme.accent/accent2），
 * 其余中性色/语义色来自亮/暗配色板。
 */
private fun colorScheme(dark: Boolean): ColorScheme {   // 配色方案构造
    val p = if (dark) DarkPalette else LightPalette     // 取亮/暗配色板
    val accent = MuyunThemeState.theme.accent            // 当前主题强调主色
    val accent2 = MuyunThemeState.theme.accent2          // 当前主题强调辅色
    val accentSoft = accent.copy(alpha = if (dark) 0.18f else 0.08f)   // 主色浅底
    val accent2Soft = accent2.copy(alpha = if (dark) 0.18f else 0.08f) // 辅色浅底
    return if (dark) {                                   // 暗色方案
        darkColorScheme(                                 // 构造暗色方案
            primary = accent, onPrimary = Color.White,
            primaryContainer = accentSoft, onPrimaryContainer = accent,
            secondary = accent2, onSecondary = Color.White,
            secondaryContainer = accent2Soft, onSecondaryContainer = accent2,
            tertiary = p.purple, onTertiary = Color.White,
            tertiaryContainer = p.purpleBg, onTertiaryContainer = p.purple,
            background = p.bg, onBackground = p.text,
            surface = p.card, onSurface = p.text,
            surfaceVariant = p.accentLight, onSurfaceVariant = p.text2,
            outline = p.border, outlineVariant = p.border,
            error = p.danger, onError = Color.White,
            errorContainer = p.dangerBg, onErrorContainer = p.danger,
        )
    } else {                                            // 亮色方案
        lightColorScheme(                                // 构造亮色方案
            primary = accent, onPrimary = Color.White,
            primaryContainer = accentSoft, onPrimaryContainer = accent,
            secondary = accent2, onSecondary = Color.White,
            secondaryContainer = accent2Soft, onSecondaryContainer = accent2,
            tertiary = p.purple, onTertiary = Color.White,
            tertiaryContainer = p.purpleBg, onTertiaryContainer = p.purple,
            background = p.bg, onBackground = p.text,
            surface = p.card, onSurface = p.text,
            surfaceVariant = p.accentLight, onSurfaceVariant = p.text2,
            outline = p.border, outlineVariant = p.border,
            error = p.danger, onError = Color.White,
            errorContainer = p.dangerBg, onErrorContainer = p.danger,
        )
    }
}

/**
 * 沐云杪主题（MuyunTheme）—— 全局 UI 统一入口。
 * 读取 MuyunThemeState.isDark + theme 动态选择配色方案；任一变化 → 本函数重组 →
 * 整棵 UI 树用新配色重绘，实现「主题切换即时生效、全局一致」。
 */
@Composable                                          // 可组合函数
fun MuyunTheme(content: @Composable () -> Unit) {     // 主题包装（内容即整棵 UI 树）
    val dark = MuyunThemeState.isDark                 // 读亮/暗状态（建立订阅）
    MuyunThemeState.theme                             // 读当前主题（建立订阅）
    MaterialTheme(                                   // Material3 主题
        colorScheme = colorScheme(dark),             // 注入当前配色方案
        content = content,                           // 渲染内容
    )
}

/**
 * 切换亮/暗主题（MuyunThemeState.isDark 取反），供切换按钮调用。
 */
fun toggleMuyunTheme() {                              // 切换主题
    MuyunThemeState.isDark = !MuyunThemeState.isDark  // 取反即时生效
}

/**
 * 品牌字号阶梯（对应 HTML --fs-* 令牌）。
 * 改为 getter：每次访问按当前主题取色，保证暗色下文字颜色正确。
 */
object MuyunType {                                   // 字号阶梯
    /** 超小字（HTML --fs-xs: 11px ≈ 11sp）。 */
    val xs: TextStyle get() = TextStyle(fontSize = 11.sp, color = MuyunText3)

    /** 小字（HTML --fs-sm: 12px ≈ 12sp）。 */
    val sm: TextStyle get() = TextStyle(fontSize = 12.sp, color = MuyunText2)

    /** 正文（HTML --fs-base: 14px ≈ 14sp）。 */
    val base: TextStyle get() = TextStyle(fontSize = 14.sp, color = MuyunText)

    /** 中字（HTML --fs-md: 15px ≈ 15sp）。 */
    val md: TextStyle get() = TextStyle(fontSize = 15.sp, color = MuyunText)

    /** 大字（HTML --fs-lg: 17px ≈ 17sp）。 */
    val lg: TextStyle get() = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = MuyunText)

    /** 超大标题（HTML --fs-xl: 20px ≈ 20sp）。 */
    val xl: TextStyle get() = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MuyunText)

    /** 巨型标题（HTML --fs-2xl: 32px ≈ 32sp）。 */
    val xxl: TextStyle get() = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MuyunText)
}
