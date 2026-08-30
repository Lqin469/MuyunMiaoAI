package com.memuo.core.ui                                // 声明包名：共享 UI 模块

import androidx.compose.ui.graphics.Color                 // 导入 Color：颜色
import androidx.compose.ui.graphics.SolidColor             // 导入 SolidColor：纯色画刷（描边用）
import androidx.compose.ui.graphics.StrokeCap              // 导入 StrokeCap：描边端点样式
import androidx.compose.ui.graphics.StrokeJoin             // 导入 StrokeJoin：描边拐角样式
import androidx.compose.ui.graphics.vector.ImageVector     // 导入 ImageVector：矢量图标
import androidx.compose.ui.graphics.vector.PathParser       // 导入 PathParser：SVG path 解析器
import androidx.compose.ui.unit.dp                         // 导入 dp：图标默认尺寸单位

/**
 * 应用图标集（AppIcons）—— 把「沐云杪-界面原型.html」里的内联 SVG 图标
 * 原样翻译为 Compose ImageVector（feather 风格：24×24、描边 2、圆头圆角）。
 * 说明：项目只依赖 material-icons-core（无相机/麦克风等图标），
 * 这里用 SVG path 数据直接构造，保证与 HTML 原型视觉 100% 一致。
 */
object AppIcons {                                          // 图标集单例（全局复用）

    /** 构造一个描边风格矢量图标（HTML SVG 的 path 数据 → ImageVector）。 */
    private fun icon(name: String, vararg paths: String): ImageVector =  // 图标构造器
        ImageVector.Builder(                               // 矢量构建器
            name = name,                                   // 图标名（调试用）
            defaultWidth = 24.dp,                          // 默认宽度 24dp
            defaultHeight = 24.dp,                         // 默认高度 24dp
            viewportWidth = 24f,                           // 视口宽（与 HTML viewBox="0 0 24 24" 一致）
            viewportHeight = 24f,                          // 视口高
        ).apply {                                          // 逐条追加路径
            paths.forEach { d ->                           // 遍历 path 数据
                addPath(                                   // 追加一条路径
                    pathData = PathParser().parsePathString(d).toNodes(),  // SVG 字符串 → 路径节点
                    stroke = SolidColor(Color.Black),      // 描边黑色（实际颜色由 Icon tint 决定）
                    strokeLineWidth = 2f,                  // 描边宽度 2（对应 HTML stroke-width="2"）
                    strokeLineCap = StrokeCap.Round,       // 圆头端点
                    strokeLineJoin = StrokeJoin.Round,     // 圆角拐角
                )
            }
        }.build()                                          // 构建完成

    /** 菜单（汉堡）。 */
    val Menu = icon("menu", "M3 6h18", "M3 12h18", "M3 18h18")

    /** 返回箭头。 */
    val Back = icon("back", "M15 18l-6-6 6-6")

    /** 关闭（×）。 */
    val Close = icon("close", "M18 6L6 18", "M6 6l12 12")

    /** 加号。 */
    val Plus = icon("plus", "M12 5v14", "M5 12h14")

    /** 垃圾桶。 */
    val Trash = icon(
        "trash",
        "M3 6h18",
        "M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6",
        "M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2",
    )

    /** 云朵（云端模式）。 */
    val Cloud = icon("cloud", "M18 10h-1.26A8 8 0 1 0 9 20h9a5 5 0 0 0 0-10z")

    /** 相机（拍照）。 */
    val Camera = icon(
        "camera",
        "M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z",
        "M15 13a3 3 0 1 1-6 0 3 3 0 0 1 6 0z",
    )

    /** 相册（图片）。 */
    val Gallery = icon(
        "gallery",
        "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z",
        "M8.5 8.5m-1.5 0a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0 -3 0",
        "M21 15l-5-5L5 21",
    )

    /** 文件。 */
    val File = icon(
        "file",
        "M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z",
        "M14 2v6h6",
    )

    /** 麦克风（语音输入）。 */
    val Mic = icon(
        "mic",
        "M12 2a3 3 0 0 1 3 3v6a3 3 0 0 1-6 0V5a3 3 0 0 1 3-3z",
        "M5 10a7 7 0 0 0 14 0",
        "M12 17v4",
    )

    /** 发送（纸飞机，feather send）。 */
    val Send = icon("send", "M22 2L11 13", "M22 2l-7 20-4-9-9-4z")

    /** 设置齿轮。 */
    val Gear = icon(
        "gear",
        "M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z",
    )

    /** 知识库（书）。 */
    val Book = icon(
        "book",
        "M4 19.5A2.5 2.5 0 0 1 6.5 17H20",
        "M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z",
    )

    /** 记忆（收纳盒）。 */
    val Memory = icon("memory", "M21 8v13H3V8", "M1 3h22v5H1z", "M10 12h4")

    /** 任务（对勾）。 */
    val Task = icon("task", "M9 11l3 3L22 4", "M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11")

    /** WiFi（局域网）。 */
    val Wifi = icon(
        "wifi",
        "M5 12.55a11 11 0 0 1 14.08 0",
        "M1.42 9a16 16 0 0 1 21.16 0",
        "M8.53 16.11a6 6 0 0 1 6.95 0",
        "M12 20h.01",
    )

    /** 右箭头（列表项尾部）。 */
    val ChevronRight = icon("chevron-right", "M9 18l6-6-6-6")

    /** 下箭头（折叠区）。 */
    val ChevronDown = icon("chevron-down", "M6 9l6 6 6-6")

    /** 盾牌（权限管理）。 */
    val Shield = icon("shield", "M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z")

    /** 上传箭头（数据迁移）。 */
    val Upload = icon("upload", "M12 19V5", "M5 12l7-7 7 7", "M19 21H5")

    /** 眼睛（密钥可见性切换）。 */
    val Eye = icon(
        "eye",
        "M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z",
        "M12 9a3 3 0 1 0 0 6a3 3 0 0 0 0-6z",
    )

    /** 刷新。 */
    val Refresh = icon("refresh", "M23 4v6h-6", "M20.49 15a9 9 0 1 1-2.12-9.36L23 10")

    /** 重新生成（旋转箭头）。 */
    val Regenerate = icon("regenerate", "M1 4v6h6", "M3.51 15a9 9 0 1 0 2.13-9.36L1 10")

    /** 警告（圆圈感叹号）。 */
    val Alert = icon("alert", "M12 22a10 10 0 1 0 0-20a10 10 0 0 0 0 20z", "M12 8v4", "M12 16h.01")

    /** 信息（圆圈 i）。 */
    val Info = icon("info", "M12 22a10 10 0 1 0 0-20a10 10 0 0 0 0 20z", "M12 16v-4", "M12 8h.01")

    /** 对勾。 */
    val Check = icon("check", "M20 6L9 17l-5-5")

    /** 文件夹。 */
    val Folder = icon("folder", "M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z")

    /** 调色盘（壁纸）。 */
    val Palette = icon(
        "palette",
        "M13 21v-2a2 2 0 0 1 2-2h2a2 2 0 0 0 2-2 12 12 0 1 0-12 12",
        "M6.5 13.5m-1 0a1 1 0 1 0 2 0a1 1 0 1 0 -2 0",
        "M10.5 7.5m-1 0a1 1 0 1 0 2 0a1 1 0 1 0 -2 0",
        "M16.5 9.5m-1 0a1 1 0 1 0 2 0a1 1 0 1 0 -2 0",
        "M17.5 14.5m-1 0a1 1 0 1 0 2 0a1 1 0 1 0 -2 0",
    )

    /** 模型（勾圆）。 */
    val Model = icon("model", "M22 11.08V12a10 10 0 1 1-5.93-9.14", "M22 4L12 14.01l-3-3")

    /** 代码（ADB 权限）。 */
    val Code = icon("code", "M16 18l6-6", "M8 6l-6 6")

    /** 终端（ROOT 权限）。 */
    val Terminal = icon(
        "terminal",
        "M12 2v8",
        "M4.93 10.93l1.41 1.41",
        "M2 18l2-2 2 2-2 2z",
        "M12 14v6",
        "M14 17H6",
    )

    /** 空备忘录文档（空态插图）。 */
    val DocEmpty = icon(
        "doc-empty",
        "M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z",
        "M14 2v6h6",
        "M9 13h6",
        "M9 17h4",
    )

    /** 搜索。 */
    val Search = icon("search", "M11 4a7 7 0 1 0 0 14a7 7 0 0 0 0-14z", "M21 21l-4.35-4.35")

    /** 用户（迁移·用户数据）。 */
    val User = icon("user", "M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2", "M12 11a4 4 0 1 0 0-8a4 4 0 0 0 0 8z")

    /** 数据库（迁移·缓存）。 */
    val Database = icon(
        "database",
        "M21 12c0 1.66-4 3-9 3s-9-1.34-9-3",
        "M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5",
        "M3 5c0-1.66 4-3 9-3s9 1.34 9 3",
    )

    /** 文档文本（迁移·日志）。 */
    val FileText = icon(
        "file-text",
        "M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z",
        "M14 2v6h6",
        "M16 13H8",
        "M16 17H8",
        "M10 9H8",
    )

    /** 暂停（迁移暂停）。 */
    val Pause = icon("pause", "M10 4H6v16h4z", "M18 4h-4v16h4z")

    /** 复制（复制模型下载地址）。 */
    val Copy = icon("copy", "M9 9h11v11H9z", "M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1")

    /** 品牌 logo（GitHub 猫，HTML 侧边栏原版）。 */
    val Logo = icon(
        "logo",
        "M12 2a10 10 0 0 0-3.6 19.3c.5.1.7-.2.7-.5v-1.7c-3 .7-3.6-1.5-3.6-1.5-.5-1.2-1.2-1.6-1.2-1.6-1-.7.1-.7.1-.7 1.1.1 1.7 1.1 1.7 1.1 1 1.7 2.6 1.2 3.2.9.1-.7.4-1.2.7-1.5-2.4-.3-5-1.2-5-5.3 0-1.2.4-2.2 1.1-2.9-.1-.3-.5-1.4.1-2.9 0 0 .9-.3 3 1.1a10.4 10.4 0 0 1 5.4 0c2.1-1.4 3-1.1 3-1.1.6 1.5.2 2.6.1 2.9.7.7 1.1 1.7 1.1 2.9 0 4.1-2.6 5-5 5.3.4.4.8 1.1.8 2.2v3.3c0 .3.2.6.7.5A10 10 0 0 0 12 2z",
    )

    /** 太阳（亮色主题，feather sun）。 */
    val Sun = icon(
        "sun",
        "M12 17a5 5 0 1 0 0-10a5 5 0 0 0 0 10z",
        "M12 1v2",
        "M12 21v2",
        "M4.22 4.22l1.42 1.42",
        "M18.36 18.36l1.42 1.42",
        "M1 12h2",
        "M21 12h2",
        "M4.22 19.78l1.42-1.42",
        "M18.36 5.64l1.42-1.42",
    )

    /** 月亮（暗色主题，feather moon）。 */
    val Moon = icon("moon", "M21 12.79A9 9 0 1 1 11.21 3a7 7 0 0 0 9.79 9.79z")

    /** 更多操作（竖向三点，feather more-vertical）。 */
    val More = icon(
        "more",
        "M12 12a1 1 0 1 0 0.01 0",
        "M12 5a1 1 0 1 0 0.01 0",
        "M12 19a1 1 0 1 0 0.01 0",
    )
}
