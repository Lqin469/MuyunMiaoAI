package com.memuo.core.ui.components                     // 声明包名：共享 UI 组件子包

import androidx.compose.foundation.background             // 导入 background：背景修饰
import androidx.compose.foundation.clickable              // 导入 clickable：点击修饰
import androidx.compose.foundation.layout.Box             // 导入 Box：盒式布局
import androidx.compose.foundation.layout.Column          // 导入 Column：纵向布局
import androidx.compose.foundation.layout.Row             // 导入 Row：横向布局
import androidx.compose.foundation.layout.fillMaxWidth    // 导入 fillMaxWidth：占满宽度
import androidx.compose.foundation.layout.padding          // 导入 padding：内边距
import androidx.compose.foundation.layout.size            // 导入 size：固定尺寸
import androidx.compose.foundation.shape.RoundedCornerShape  // 导入 RoundedCornerShape：圆角形状
import androidx.compose.material3.Icon                    // 导入 Icon：图标
import androidx.compose.material3.Text                    // 导入 Text：文本
import androidx.compose.runtime.Composable                // 导入 Composable：可组合函数注解
import androidx.compose.ui.Alignment                      // 导入 Alignment：对齐
import androidx.compose.ui.Modifier                       // 导入 Modifier：修饰
import androidx.compose.ui.draw.clip                      // 导入 clip：裁剪
import androidx.compose.ui.draw.shadow                    // 导入 shadow：投影
import androidx.compose.ui.graphics.Color                 // 导入 Color：颜色
import androidx.compose.ui.graphics.vector.ImageVector    // 导入 ImageVector：矢量图标
import androidx.compose.ui.text.font.FontWeight           // 导入 FontWeight：字重
import androidx.compose.ui.unit.dp                        // 导入 dp：尺寸单位
import com.memuo.core.ui.AppIcons                         // 导入应用图标集
import com.memuo.core.ui.theme.MuyunAccentLight           // 导入浅灰底
import com.memuo.core.ui.theme.MuyunBorder                // 导入分割线色
import com.memuo.core.ui.theme.MuyunCard                  // 导入卡片白
import com.memuo.core.ui.theme.MuyunDanger                // 导入危险红
import com.memuo.core.ui.theme.MuyunDangerBg              // 导入危险红底
import com.memuo.core.ui.theme.MuyunGreen                 // 导入成功绿
import com.memuo.core.ui.theme.MuyunGreenBg               // 导入成功绿底
import com.memuo.core.ui.theme.MuyunInfo                  // 导入信息蓝
import com.memuo.core.ui.theme.MuyunInfoBg                // 导入信息蓝底
import com.memuo.core.ui.theme.MuyunPurple                // 导入品牌紫
import com.memuo.core.ui.theme.MuyunPurpleBg              // 导入品牌紫底
import com.memuo.core.ui.theme.MuyunText                  // 导入主文字色
import com.memuo.core.ui.theme.MuyunText2                 // 导入次级文字色
import com.memuo.core.ui.theme.MuyunText3                 // 导入三级文字色

/** 状态色调枚举（对应 HTML 的语义色：busy/success/fail/neutral）。 */
enum class StatusTone { INFO, SUCCESS, FAIL, NEUTRAL }    // 四种状态色

/**
 * 状态徽章（StatusPill）—— 小圆角胶囊文字：
 * 用于「已授予/满足/使用中/可运行/在线」等短状态标签。
 */
@Composable                                              // 可组合函数
fun StatusPill(                                          // 状态徽章
    text: String,                                        // 徽章文字
    tone: StatusTone = StatusTone.NEUTRAL,               // 色调
    modifier: Modifier = Modifier,                       // 外部修饰
) {
    val (fg, bg) = when (tone) {                         // 按色调取前景/背景
        StatusTone.INFO -> MuyunInfo to MuyunInfoBg       // 信息蓝
        StatusTone.SUCCESS -> MuyunGreen to MuyunGreenBg  // 成功绿
        StatusTone.FAIL -> MuyunDanger to MuyunDangerBg   // 危险红
        StatusTone.NEUTRAL -> MuyunText3 to MuyunAccentLight  // 中性灰
    }
    Box(                                                 // 胶囊容器
        modifier = modifier                             // 外部修饰
            .clip(RoundedCornerShape(20.dp))            // 大圆角胶囊
            .background(bg)                             // 背景色
            .padding(horizontal = 10.dp, vertical = 3.dp),  // 内边距（HTML padding 3px 10px）
    ) {
        Text(                                            // 徽章文字
            text = text,                                 // 内容
            color = fg,                                  // 前景色
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,  // 小字（HTML 12px）
            fontWeight = FontWeight.SemiBold,            // 半粗（HTML font-weight 600）
        )
    }
}

/**
 * 分组卡片（SectionCard）—— 对应 HTML 的 .set-card / .permission-section：
 * 白色圆角卡片 + 阴影 + 内边距，设置页的通用容器。
 */
@Composable                                              // 可组合函数
fun SectionCard(                                         // 分组卡片
    modifier: Modifier = Modifier,                       // 外部修饰
    content: @Composable () -> Unit,                     // 内容
) {
    Box(                                                 // 卡片容器
        modifier = modifier                             // 外部修饰
            .fillMaxWidth()                             // 占满宽度
            .shadow(1.dp, RoundedCornerShape(14.dp))    // 轻投影（HTML --shadow）
            .clip(RoundedCornerShape(14.dp))            // 圆角 14
            .background(MuyunCard)                      // 白底
            .padding(horizontal = 20.dp, vertical = 4.dp),  // 内边距（HTML padding 4px 20px 8px）
    ) {
        Column { content() }                             // 渲染内容
    }
}

/**
 * 卡片分组标题（SectionCardTitle）—— 对应 HTML 的 .set-card-title。
 */
@Composable                                              // 可组合函数
fun SectionCardTitle(text: String) {                     // 分组标题
    Text(                                                // 标题文本
        text = text,                                     // 内容
        style = androidx.compose.material3.MaterialTheme.typography.titleSmall,  // 字号（HTML 15px）
        fontWeight = FontWeight.SemiBold,                // 半粗
        color = MuyunText,                               // 主文字色
        modifier = Modifier.padding(top = 16.dp, bottom = 2.dp),  // 上 16 下 2（HTML padding）
    )
}

/**
 * 设置菜单行（SettingsMenuRow）—— 对应 HTML 的 .set-menu-item：
 * 左侧圆角图标 + 名称 + 右侧描述 + 箭头，整行可点击。
 */
@Composable                                              // 可组合函数
fun SettingsMenuRow(                                     // 设置菜单行
    icon: ImageVector,                                   // 左侧图标
    name: String,                                        // 名称
    desc: String = "",                                   // 右侧描述（可空）
    onClick: () -> Unit,                                 // 点击回调
    modifier: Modifier = Modifier,                       // 外部修饰
) {
    Row(                                                 // 横向布局
        modifier = modifier                             // 外部修饰
            .fillMaxWidth()                             // 占满宽度
            .clickable { onClick() }                    // 点击
            .padding(vertical = 14.dp),                 // 上下 14（HTML padding 14px 0）
        verticalAlignment = Alignment.CenterVertically,  // 垂直居中
    ) {
        Box(                                             // 图标底
            modifier = Modifier                         // 修饰
                .size(34.dp)                            // 34dp（HTML 34×34）
                .clip(RoundedCornerShape(10.dp))        // 圆角 10
                .background(MuyunAccentLight),          // 浅灰底
            contentAlignment = Alignment.Center,         // 居中
        ) {
            Icon(                                        // 图标
                imageVector = icon,                      // 矢量
                contentDescription = name,               // 描述
                tint = MuyunText2,                       // 次级灰
                modifier = Modifier.size(16.dp),         // 16dp
            )
        }
        Text(                                            // 名称
            text = name,                                 // 内容
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,  // 字号（HTML 14px）
            fontWeight = FontWeight.Medium,              // 中粗
            color = MuyunText,                           // 主文字色
            modifier = Modifier.padding(start = 12.dp),  // 与图标留白
        )
        Text(                                            // 右侧描述
            text = desc,                                 // 内容
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,  // 小字（HTML 11px）
            color = MuyunText3,                          // 三级灰
            modifier = Modifier.weight(1f).padding(end = 8.dp),  // 占满剩余
            textAlign = androidx.compose.ui.text.style.TextAlign.End,  // 右对齐
        )
        Icon(                                            // 右箭头
            imageVector = AppIcons.ChevronRight,         // 箭头
            contentDescription = null,                   // 装饰性
            tint = MuyunText3,                           // 三级灰（主题自适应）
            modifier = Modifier.size(14.dp),             // 14dp
        )
    }
}

/**
 * 空态提示（EmptyState）—— 对应 HTML 各页面的 .*-empty：
 * 大图标（淡化）+ 多行说明文字，居中显示。
 */
@Composable                                              // 可组合函数
fun EmptyState(                                          // 空态提示
    icon: ImageVector? = null,                           // 插图图标（可空）
    text: String,                                        // 说明文字（支持 \n 换行）
    modifier: Modifier = Modifier,                       // 外部修饰
) {
    Column(                                              // 纵向布局
        modifier = modifier                             // 外部修饰
            .fillMaxWidth()                             // 占满宽度
            .padding(vertical = 50.dp, horizontal = 24.dp),  // 内边距（HTML padding 50~70px）
        horizontalAlignment = Alignment.CenterHorizontally,  // 水平居中
    ) {
        icon?.let {                                      // 有图标
            Icon(                                        // 图标
                imageVector = it,                        // 矢量
                contentDescription = null,               // 装饰性
                tint = MuyunText3.copy(alpha = 0.25f),   // 淡化 25%（HTML opacity 0.25）
                modifier = Modifier.size(56.dp).padding(bottom = 14.dp),  // 56dp + 下留白
            )
        }
        Text(                                            // 说明文字
            text = text,                                 // 内容
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,  // 字号（HTML 13~14px）
            color = MuyunText3,                          // 三级灰
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,  // 居中
            lineHeight = androidx.compose.material3.MaterialTheme.typography.bodyMedium.lineHeight * 1.4f,  // 行距（HTML 1.6）
        )
    }
}

/**
 * 状态条（StatusBar）—— 对应 HTML 的 .lan-status / .migrate-check-status：
 * 全宽提示条，busy=蓝 / success=绿 / fail=红，可隐藏。
 */
@Composable                                              // 可组合函数
fun StatusBar(                                           // 状态条
    text: String,                                        // 状态文字（空 = 隐藏）
    tone: StatusTone,                                    // 色调
    modifier: Modifier = Modifier,                       // 外部修饰
) {
    if (text.isBlank()) return                           // 空文字隐藏
    val (fg, bg) = when (tone) {                         // 按色调取色
        StatusTone.INFO -> MuyunInfo to MuyunInfoBg       // 蓝
        StatusTone.SUCCESS -> MuyunGreen to MuyunGreenBg  // 绿
        StatusTone.FAIL -> MuyunDanger to MuyunDangerBg   // 红
        StatusTone.NEUTRAL -> MuyunText2 to MuyunAccentLight  // 灰
    }
    Box(                                                 // 状态条容器
        modifier = modifier                             // 外部修饰
            .fillMaxWidth()                             // 占满宽度
            .clip(RoundedCornerShape(10.dp))            // 圆角 10
            .background(bg)                             // 背景色
            .padding(horizontal = 14.dp, vertical = 10.dp),  // 内边距
        contentAlignment = Alignment.Center,             // 居中
    ) {
        Text(                                            // 状态文字
            text = text,                                 // 内容
            color = fg,                                  // 前景色
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,  // 字号（HTML 13px）
            fontWeight = FontWeight.Medium,              // 中粗
        )
    }
}

/**
 * 分隔线（MuyunDivider）—— 对应 HTML 的 border-bottom 行分隔。
 */
@Composable                                              // 可组合函数
fun MuyunDivider(modifier: Modifier = Modifier) {        // 分隔线
    Box(                                                 // 细线
        modifier = modifier                             // 外部修饰
            .fillMaxWidth()                             // 占满宽度
            .padding(vertical = 0.5.dp)                 // 微调高度
            .background(MuyunBorder),                   // 分割线色
    )
}
