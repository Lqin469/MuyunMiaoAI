package com.memuo.core.ui.theme                       // 声明包名：共享 UI 的主题子包

import androidx.compose.runtime.getValue                // 导入 getValue：by 委托读
import androidx.compose.runtime.mutableStateOf          // 导入 mutableStateOf：可观察状态
import androidx.compose.runtime.setValue                // 导入 setValue：by 委托写
import androidx.compose.ui.graphics.Brush               // 导入 Brush：渐变画刷
import androidx.compose.ui.graphics.Color               // 导入 Color：颜色
import com.memuo.core.ui.ThemePreset                     // 导入主题项
import com.memuo.core.ui.ThemePresets                    // 导入主题库

/**
 * 全局主题状态（MuyunThemeState）—— 亮/暗 + 当前主题（背景渐变 + 强调色）的唯一开关。
 * 用 Compose 的 mutableStateOf 保存，任何 @Composable 里读取都会建立订阅，
 * 切换后全局自动重组、即时生效且保持一致。
 */
object MuyunThemeState {                                // 主题状态单例
    var isDark by mutableStateOf(false)                 // 当前是否暗色（默认亮色）
    var theme by mutableStateOf(ThemePresets.default)    // 当前主题（可观察状态，切换即全局重组）
}

/**
 * 配色板（MuyunPalette）—— 把 HTML v19 设计令牌的「亮色/暗色」两套色值集中定义，
 * 避免同一颜色在两处重复维护。dark=true 用暗色、false 用亮色。
 */
data class MuyunPalette(val dark: Boolean) {            // 配色板数据类
    val bg get() = if (dark) Color(0xFF121212) else Color(0xFFF7F7F5)          // 页面背景
    val card get() = if (dark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)         // 卡片
    val text get() = if (dark) Color(0xFFE8E8E8) else Color(0xFF1A1A1A)         // 主文字
    val text2 get() = if (dark) Color(0xFFB8B8B8) else Color(0xFF555555)        // 次级文字
    val text3 get() = if (dark) Color(0xFF9A9A9A) else Color(0xFF8A8A8A)        // 三级文字
    val border get() = if (dark) Color(0xFF2C2C2C) else Color(0xFFEAEAEA)       // 分割线
    val accent get() = if (dark) Color(0xFFE0E0E0) else Color(0xFF2D2D2D)       // 中性深色
    val accentLight get() = if (dark) Color(0xFF2A2A2A) else Color(0xFFF0F0EE)  // 浅灰底
    val bar get() = if (dark) Color(0xE6121212) else Color(0xE6F7F7F5)          // 顶栏半透明（透出壁纸）
    val green get() = if (dark) Color(0xFF3DDC77) else Color(0xFF34C759)        // 成功绿
    val greenBg get() = if (dark) Color(0xFF0F2A1B) else Color(0xFFEEFBF3)      // 绿浅底
    val purple get() = if (dark) Color(0xFF8B7CF7) else Color(0xFF6C5CE7)       // 品牌紫
    val purpleBg get() = if (dark) Color(0xFF1E1A3A) else Color(0xFFEEEDFF)     // 紫浅底
    val info get() = if (dark) Color(0xFF38BDF8) else Color(0xFF0EA5E9)         // 信息蓝
    val infoBg get() = if (dark) Color(0xFF0A2433) else Color(0xFFE0F2FE)       // 蓝浅底
    val danger get() = if (dark) Color(0xFFF05A4E) else Color(0xFFE74C3C)       // 危险红
    val dangerBg get() = if (dark) Color(0xFF2E1614) else Color(0xFFFDEEEE)     // 红浅底
    val disabled get() = if (dark) Color(0xFF3A3A3A) else Color(0xFFDCDCD9)     // 禁用色
    val scrim get() = Color(0x40000000)                                          // 遮罩黑（恒定）

    val userBubbleGradient get() = if (dark)                                  // 用户气泡渐变（暗色下更亮）
        Brush.linearGradient(listOf(Color(0xFF3D3D3D), Color(0xFF2B2B2B)))
    else Brush.linearGradient(listOf(Color(0xFF2D2D2D), Color(0xFF1A1A1A)))
}

/** 亮色配色板。 */
val LightPalette = MuyunPalette(dark = false)           // 亮色实例

/** 暗色配色板。 */
val DarkPalette = MuyunPalette(dark = true)             // 暗色实例

/** 当前生效配色板（读 isDark 状态，@Composable 内会订阅、切换即重组）。 */
private val current: MuyunPalette get() = if (MuyunThemeState.isDark) DarkPalette else LightPalette  // 取当前板

// ============================================================
// 对外颜色符号 —— 保持与旧版同名，页面代码零改动；
// 改为 getter 后随主题实时切换，@Composable 里读取即订阅状态。
// ============================================================

/** 页面背景色。 */
val MuyunBg: Color get() = current.bg

/** 卡片背景色。 */
val MuyunCard: Color get() = current.card

/** 主文字色。 */
val MuyunText: Color get() = current.text

/** 次级文字色。 */
val MuyunText2: Color get() = current.text2

/** 三级文字色。 */
val MuyunText3: Color get() = current.text3

/** 分割线/描边色。 */
val MuyunBorder: Color get() = current.border

/** 中性深色。 */
val MuyunAccent: Color get() = current.accent

/** 中性浅灰底。 */
val MuyunAccentLight: Color get() = current.accentLight

/** 顶栏半透明背景（透出全局壁纸）。 */
val MuyunBar: Color get() = current.bar

/** 成功绿。 */
val MuyunGreen: Color get() = current.green

/** 成功绿底。 */
val MuyunGreenBg: Color get() = current.greenBg

/** 品牌紫。 */
val MuyunPurple: Color get() = current.purple

/** 品牌紫底。 */
val MuyunPurpleBg: Color get() = current.purpleBg

/** 品牌主色（跟随当前主题强调色，切换主题即全局变色）。 */
val MuyunBrand: Color get() = MuyunThemeState.theme.accent

/** 品牌辅色（跟随当前主题辅色）。 */
val MuyunBrand2: Color get() = MuyunThemeState.theme.accent2

/** 品牌浅底·主色（强调色的浅色版，用于高亮背景）。 */
val MuyunBrandSoft: Color get() = MuyunBrand.copy(alpha = if (MuyunThemeState.isDark) 0.18f else 0.08f)

/** 品牌浅底·辅色（强调辅色的浅色版）。 */
val MuyunBrandSoft2: Color get() = MuyunBrand2.copy(alpha = if (MuyunThemeState.isDark) 0.18f else 0.08f)

/** 信息蓝。 */
val MuyunInfo: Color get() = current.info

/** 信息蓝底。 */
val MuyunInfoBg: Color get() = current.infoBg

/** 危险红。 */
val MuyunDanger: Color get() = current.danger

/** 危险红底。 */
val MuyunDangerBg: Color get() = current.dangerBg

/** 禁用底色。 */
val MuyunDisabled: Color get() = current.disabled

/** 遮罩黑。 */
val MuyunScrim: Color get() = current.scrim

/** 品牌渐变（跟随主题强调色，按钮/胶囊等主色渐变）。 */
val MuyunBrandGradient: Brush get() = Brush.linearGradient(listOf(MuyunBrand, MuyunBrand2))

/** 问候语/标题渐变（跟随主题）。 */
val MuyunTitleGradient: Brush get() = Brush.linearGradient(listOf(MuyunBrand, MuyunBrand2))

/** 用户气泡深色渐变。 */
val MuyunUserBubbleGradient: Brush get() = current.userBubbleGradient
