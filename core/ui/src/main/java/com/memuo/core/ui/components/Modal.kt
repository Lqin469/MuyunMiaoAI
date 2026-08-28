package com.memuo.core.ui.components                     // 声明包名：共享 UI 组件子包

import androidx.compose.animation.AnimatedVisibility      // 导入 AnimatedVisibility：显隐动画
import androidx.compose.animation.fadeIn                  // 导入 fadeIn：淡入
import androidx.compose.animation.fadeOut                 // 导入 fadeOut：淡出
import androidx.compose.animation.scaleIn                 // 导入 scaleIn：放大进入
import androidx.compose.animation.scaleOut                // 导入 scaleOut：缩小退出
import androidx.compose.foundation.background             // 导入 background：背景修饰
import androidx.compose.foundation.clickable              // 导入 clickable：点击修饰
import androidx.compose.foundation.layout.Box             // 导入 Box：盒式布局
import androidx.compose.foundation.layout.Column          // 导入 Column：纵向布局
import androidx.compose.foundation.layout.ColumnScope     // 导入 ColumnScope：纵向作用域
import androidx.compose.foundation.layout.Row             // 导入 Row：横向布局（标题栏）
import androidx.compose.foundation.layout.fillMaxSize     // 导入 fillMaxSize：铺满
import androidx.compose.foundation.layout.fillMaxWidth    // 导入 fillMaxWidth：占满宽度
import androidx.compose.foundation.layout.heightIn        // 导入 heightIn：高度约束
import androidx.compose.foundation.layout.padding          // 导入 padding：内边距
import androidx.compose.foundation.shape.RoundedCornerShape  // 导入 RoundedCornerShape：圆角形状
import androidx.compose.material3.Icon                    // 导入 Icon：图标
import androidx.compose.material3.Text                    // 导入 Text：文本
import androidx.compose.runtime.Composable                // 导入 Composable：可组合函数注解
import androidx.compose.ui.Alignment                      // 导入 Alignment：对齐
import androidx.compose.ui.Modifier                       // 导入 Modifier：修饰
import androidx.compose.ui.draw.clip                      // 导入 clip：裁剪
import androidx.compose.ui.text.font.FontWeight           // 导入 FontWeight：字重
import androidx.compose.ui.unit.Dp                        // 导入 Dp：尺寸单位
import androidx.compose.ui.unit.dp                        // 导入 dp：尺寸单位
import com.memuo.core.ui.AppIcons                         // 导入应用图标集
import com.memuo.core.ui.theme.MuyunCard                  // 导入卡片白
import com.memuo.core.ui.theme.MuyunScrim                 // 导入遮罩黑
import com.memuo.core.ui.theme.MuyunText                  // 导入主文字色

/**
 * 弹窗容器（MuyunModal）—— 对应 HTML 的 .modal-overlay + .modal：
 * 半透明遮罩（点击关闭）+ 居中白色圆角卡片（20dp 圆角）+ 缩放淡入动画。
 * 局域网传输、添加 API、迁移报告等所有弹窗统一使用。
 */
@Composable                                              // 可组合函数
fun MuyunModal(                                          // 弹窗容器
    visible: Boolean,                                    // 是否显示
    onDismiss: () -> Unit,                               // 点遮罩关闭回调
    title: String,                                       // 弹窗标题
    maxWidth: Dp = 390.dp,                               // 最大宽度（HTML .modal max-width 390）
    modifier: Modifier = Modifier,                       // 卡片外部修饰
    headerActions: @Composable () -> Unit = {},          // 标题栏右侧操作（如设置/关闭）
    body: @Composable ColumnScope.() -> Unit,            // 弹窗主体
    footer: @Composable ColumnScope.() -> Unit = {},     // 弹窗底部（按钮区）
) {
    AnimatedVisibility(                                  // 显隐动画容器
        visible = visible,                               // 绑定显示状态
        enter = fadeIn(),                                // 进入淡入
        exit = fadeOut(),                                // 退出淡出
        modifier = Modifier.fillMaxSize(),               // 铺满全屏
    ) {
        Box(                                             // 遮罩层
            modifier = Modifier                         // 修饰
                .fillMaxSize()                          // 铺满
                .background(MuyunScrim)                 // 半透明黑（HTML rgba(0,0,0,0.25)）
                .clickable { onDismiss() },              // 点遮罩关闭
            contentAlignment = Alignment.Center,         // 内容居中
        ) {
            AnimatedVisibility(                          // 卡片动画（独立于遮罩淡入）
                visible = visible,                       // 绑定状态
                enter = scaleIn(initialScale = 0.92f),   // 从 92% 放大进入（HTML scale(0.92)→1）
                exit = scaleOut(targetScale = 0.92f),    // 缩小退出
            ) {
                Column(                                  // 卡片主体
                    modifier = modifier                 // 外部修饰
                        .fillMaxWidth()                 // 占满宽度（受父级 padding 约束）
                        .heightIn(max = 600.dp)         // 最大高度（HTML max-height 84vh 近似）
                        .clip(RoundedCornerShape(20.dp))  // 圆角 20（HTML .modal border-radius）
                        .background(MuyunCard)          // 白底
                        .padding(20.dp),                // 内边距 20（HTML .modal padding）
                ) {
                    Row(                                 // 标题栏
                        modifier = Modifier.fillMaxWidth(),  // 占满宽度
                        verticalAlignment = Alignment.CenterVertically,  // 垂直居中
                    ) {
                        Text(                            // 标题
                            text = title,                // 标题文字
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,  // 字体
                            fontWeight = FontWeight.Bold,  // 粗体（HTML .modal-title 700）
                            color = MuyunText,           // 主文字色
                            modifier = Modifier.weight(1f),  // 占满剩余
                        )
                        headerActions()                  // 右侧操作（设置/关闭按钮）
                    }
                    body()                               // 弹窗主体
                    footer()                             // 底部按钮区
                }
            }
        }
    }
}

/** 弹窗关闭按钮（右上角 ×，32dp 圆形，对应 HTML .modal-close）。 */
@Composable                                              // 可组合函数
fun ModalCloseButton(onClick: () -> Unit) {              // 关闭按钮
    Box(                                                 // 盒式容器
        modifier = Modifier                             // 修饰
            .padding(start = 8.dp)                      // 与标题留白
            .clip(RoundedCornerShape(16.dp))            // 圆形
            .clickable { onClick() }                    // 点击关闭
            .padding(8.dp),                             // 热区扩到 32dp
    ) {
        Icon(                                            // × 图标
            imageVector = AppIcons.Close,                // 关闭图标
            contentDescription = "关闭",                 // 描述
            tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,  // 灰
            modifier = Modifier.padding(0.dp),           // 无额外边距
        )
    }
}
