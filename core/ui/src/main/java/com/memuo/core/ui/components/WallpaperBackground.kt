package com.memuo.core.ui.components                     // 声明包名：共享 UI 组件子包

import androidx.compose.foundation.Image                  // 导入 Image：位图渲染
import androidx.compose.foundation.background             // 导入 background：背景修饰
import androidx.compose.foundation.layout.Box             // 导入 Box：盒式布局
import androidx.compose.foundation.layout.fillMaxSize     // 导入 fillMaxSize：铺满
import androidx.compose.runtime.Composable                // 导入 Composable：可组合函数注解
import androidx.compose.ui.Modifier                       // 导入 Modifier：修饰
import androidx.compose.ui.draw.blur                      // 导入 blur：高斯模糊
import androidx.compose.ui.draw.scale                     // 导入 scale：缩放
import androidx.compose.ui.graphics.Brush                 // 导入 Brush：渐变画刷
import androidx.compose.ui.graphics.Color                 // 导入 Color：颜色
import androidx.compose.ui.graphics.ImageBitmap           // 导入 ImageBitmap：位图
import androidx.compose.ui.layout.ContentScale            // 导入 ContentScale：内容缩放模式
import androidx.compose.ui.unit.dp                        // 导入 dp：尺寸单位
import com.memuo.core.ui.theme.MuyunBg                    // 导入页面背景色（纯色兜底，亮白/暗黑）
import com.memuo.core.ui.theme.MuyunThemeState            // 导入主题状态（遮罩随亮暗切换）

/** 壁纸渲染方式（core:ui 侧的镜像枚举，调用方把 WallpaperMode 映射过来）。 */
enum class WallpaperRenderMode { TILE, STRETCH, CENTER, BLUR }  // 四种方式

/**
 * 壁纸/主题背景（WallpaperBackground）—— 全局背景 + 主题预览页共用。
 * 渲染规则（三种来源）：
 * - 上传图片（bitmap 非空）→ 按显示方式渲染图片（Crop 铺满无死角 / FillBounds 拉伸）+ 遮罩；
 * - 主题渐变（brush 非空）→ 渲染渐变 + 遮罩（保证文字可读）；
 * - 两者皆空（默认/未选主题）→ 渲染纯色 MuyunBg（亮色近白 / 暗色近黑），无遮罩。
 * 说明：默认状态不再套用「沐云」渐变，回归干净的白/黑，仅在用户主动选择主题后显示渐变。
 */
@Composable                                              // 可组合函数
fun WallpaperBackground(                                 // 主题/壁纸背景
    brush: Brush?,                                       // 背景渐变（主题 brush，非空则用渐变）
    bitmap: ImageBitmap?,                                // 上传图片（非空则用图片）
    mode: WallpaperRenderMode = WallpaperRenderMode.CENTER,  // 显示方式
    modifier: Modifier = Modifier,                       // 外部修饰
) {
    val hasContent = brush != null || bitmap != null     // 是否有渐变/图片（决定是否加遮罩）
    Box(modifier = modifier.fillMaxSize()) {             // 容器（占满）
        when {                                           // 选择内容源
            mode == WallpaperRenderMode.BLUR && bitmap != null -> Image(  // 模糊模式 + 上传图
                bitmap = bitmap,                         // 位图
                contentDescription = null,               // 装饰性
                contentScale = ContentScale.Crop,        // 裁切铺满
                modifier = Modifier                     // 修饰
                    .fillMaxSize()                      // 铺满
                    .scale(1.12f)                       // 放大 12%
                    .blur(22.dp),                       // 模糊 22px
            )
            mode == WallpaperRenderMode.BLUR && brush != null -> Box(  // 模糊模式 + 渐变
                modifier = Modifier                     // 修饰
                    .fillMaxSize()                      // 铺满
                    .scale(1.12f)                       // 放大
                    .blur(22.dp)                        // 模糊
                    .background(brush),                 // 渐变底
            )
            bitmap != null -> Image(                     // 普通模式 + 上传图
                bitmap = bitmap,                         // 位图
                contentDescription = null,               // 装饰性
                contentScale = when (mode) {             // 按方式选缩放
                    WallpaperRenderMode.STRETCH -> ContentScale.FillBounds  // 拉伸（真正全屏）
                    else -> ContentScale.Crop            // 居中/平铺 → 裁切铺满（无空白）
                },
                modifier = Modifier.fillMaxSize(),       // 铺满
            )
            brush != null -> Box(                        // 普通模式 + 主题渐变
                modifier = Modifier                     // 修饰
                    .fillMaxSize()                      // 铺满
                    .background(brush),                 // 渐变
            )
            else -> Box(                                 // 默认/未选主题 → 纯色背景
                modifier = Modifier                     // 修饰
                    .fillMaxSize()                      // 铺满
                    .background(MuyunBg),               // 亮色近白 / 暗色近黑（随主题切换）
            )
        }
        // 遮罩（HTML #chat-wall-overlay）：仅当有渐变/图片时加，保证文字可读；
        // 纯色背景本身对比度足够，无需遮罩（避免默认背景被冲成不自然的白/黑）。
        if (hasContent) {                                // 有内容才加遮罩
            val overlayTop = if (MuyunThemeState.isDark) Color(0x29121212) else Color(0x29F7F7F5)  // 顶部遮罩
            val overlayBottom = if (MuyunThemeState.isDark) Color(0x66121212) else Color(0x66F7F7F5)  // 底部遮罩
            Box(                                         // 遮罩层
                modifier = Modifier                     // 修饰
                    .fillMaxSize()                      // 铺满
                    .background(                        // 渐变
                        Brush.verticalGradient(         // 纵向渐变
                            listOf(overlayTop, overlayBottom)  // 顶部/底部遮罩
                        )
                    ),
            )
        }
    }
}
