package com.memuo.core.ui                                // 声明包名：共享 UI 模块

import androidx.compose.ui.graphics.Brush                // 导入 Brush：渐变画刷
import androidx.compose.ui.graphics.Color                // 导入 Color：颜色

/**
 * 主题项（ThemePreset）—— 完整主题数据类。
 * 一个主题 = 背景渐变（colors）+ 强调主色（accent）+ 强调辅色（accent2）。
 * 选中主题后，全局背景、品牌色（按钮/导航栏/胶囊高亮）都切换到该主题色，视觉统一。
 */
data class ThemePreset(                                  // 主题数据类
    val id: String,                                      // 唯一 ID
    val name: String,                                    // 主题名
    val colors: List<Color>,                             // 背景渐变色标（>=2 个，起点→终点）
    val accent: Color,                                   // 强调主色（品牌色：按钮/胶囊/高亮）
    val accent2: Color,                                  // 强调辅色（渐变第二色）
) {
    /** 背景渐变画刷（对角渐变，对应 HTML linear-gradient(135deg, ...)）。 */
    val brush: Brush get() = Brush.linearGradient(colors)  // 构造画刷（多色渐变）
}

/** 主题库（ThemePresets）—— 全局背景/品牌色共用，17 套主题。 */
object ThemePresets {                                    // 主题库
    /** 全部主题（默认「沐云」品牌主题在首位）。 */
    val all: List<ThemePreset> = listOf(                 // 主题列表
        // —— 默认品牌主题 ——
        ThemePreset("muyun", "沐云", listOf(Color(0xFF4F46E5), Color(0xFF06B6D4)), Color(0xFF4F46E5), Color(0xFF06B6D4)),  // 沐云（默认）
        // —— 暖色系 ——
        ThemePreset("sunset", "落日橙", listOf(Color(0xFFFF9A9E), Color(0xFFFECFEF), Color(0xFFFAD0C4)), Color(0xFFF97316), Color(0xFFFB7185)),  // 落日橙
        ThemePreset("peach", "蜜桃粉", listOf(Color(0xFFFBC2EB), Color(0xFFA6C1EE)), Color(0xFFEC4899), Color(0xFF8B5CF6)),   // 蜜桃粉
        ThemePreset("aurora-pink", "樱花雨", listOf(Color(0xFFFFE0EC), Color(0xFFF8C8DC), Color(0xFFD7BDE2)), Color(0xFFE11D8F), Color(0xFFC084FC)),  // 樱花雨
        ThemePreset("candy", "糖果彩", listOf(Color(0xFFFF9A9E), Color(0xFFFAD0C4), Color(0xFFFBC2EB)), Color(0xFFFF5E8E), Color(0xFFFF9A9E)),  // 糖果彩
        // —— 冷色系 ——
        ThemePreset("ocean", "海洋蓝", listOf(Color(0xFFA1C4FD), Color(0xFFC2E9FB)), Color(0xFF3B82F6), Color(0xFF60A5FA)),   // 海洋蓝
        ThemePreset("sky", "晴空蓝", listOf(Color(0xFF4FACFE), Color(0xFF00F2FE)), Color(0xFF0EA5E9), Color(0xFF22D3EE)),     // 晴空蓝
        ThemePreset("aurora-teal", "极光青", listOf(Color(0xFF43E97B), Color(0xFF38F9D7)), Color(0xFF10B981), Color(0xFF14B8A6)),  // 极光青
        ThemePreset("glacier", "冰川蓝", listOf(Color(0xFFE0EAFC), Color(0xFFCFDEF3)), Color(0xFF64748B), Color(0xFF94A3B8)),  // 冰川蓝
        // —— 深色系 ——
        ThemePreset("night", "夜空紫", listOf(Color(0xFF667EEA), Color(0xFF764BA2)), Color(0xFF6366F1), Color(0xFF8B5CF6)),   // 夜空紫
        ThemePreset("deep-purple", "暮光紫", listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)), Color(0xFF8B5CF6), Color(0xFFA855F7)),  // 暮光紫
        ThemePreset("midnight", "午夜蓝", listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)), Color(0xFF1E40AF), Color(0xFF3B82F6)),  // 午夜蓝
        ThemePreset("charcoal", "炭黑灰", listOf(Color(0xFF232526), Color(0xFF414345)), Color(0xFF52525B), Color(0xFF71717A)),  // 炭黑灰
        // —— 自然/中性 ——
        ThemePreset("forest", "森林绿", listOf(Color(0xFFD4FC79), Color(0xFF96E6A1)), Color(0xFF22C55E), Color(0xFF4ADE80)),  // 森林绿
        ThemePreset("mint", "薄荷绿", listOf(Color(0xFF84FAB0), Color(0xFF8FD3F4)), Color(0xFF14B8A6), Color(0xFF2DD4BF)),     // 薄荷绿
        ThemePreset("sand", "沙滩金", listOf(Color(0xFFF6D365), Color(0xFFFDA085)), Color(0xFFF59E0B), Color(0xFFFB923C)),     // 沙滩金
        ThemePreset("mono", "极简灰", listOf(Color(0xFFE0E0E0), Color(0xFFF5F5F5)), Color(0xFF6B7280), Color(0xFF9CA3AF)),     // 极简灰
    )

    /** 默认主题（「沐云」品牌主题）。 */
    val default: ThemePreset = all.first()               // 默认主题

    /** 按 ID 查找主题。 */
    fun byId(id: String?): ThemePreset? = all.firstOrNull { it.id == id }  // 查找
}
