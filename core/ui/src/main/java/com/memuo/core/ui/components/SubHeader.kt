package com.memuo.core.ui.components                     // 声明包名：共享 UI 组件子包

import androidx.compose.foundation.background             // 导入 background：背景修饰
import androidx.compose.foundation.clickable              // 导入 clickable：点击修饰
import androidx.compose.foundation.layout.Box             // 导入 Box：盒式布局
import androidx.compose.foundation.layout.Row             // 导入 Row：横向布局
import androidx.compose.foundation.layout.fillMaxWidth    // 导入 fillMaxWidth：占满宽度
import androidx.compose.foundation.layout.height          // 导入 height：固定高度
import androidx.compose.foundation.layout.navigationBarsPadding  // 导入 navigationBarsPadding：底部手势条避让
import androidx.compose.foundation.layout.padding          // 导入 padding：内边距
import androidx.compose.foundation.layout.size            // 导入 size：固定尺寸
import androidx.compose.foundation.layout.statusBarsPadding  // 导入 statusBarsPadding：状态栏避让
import androidx.compose.foundation.layout.width           // 导入 width：固定宽度
import androidx.compose.foundation.shape.RoundedCornerShape  // 导入 RoundedCornerShape：圆角形状
import androidx.compose.material3.Icon                    // 导入 Icon：图标
import androidx.compose.material3.Text                    // 导入 Text：文本
import androidx.compose.runtime.Composable                // 导入 Composable：可组合函数注解
import androidx.compose.ui.Alignment                      // 导入 Alignment：对齐
import androidx.compose.ui.Modifier                       // 导入 Modifier：修饰
import androidx.compose.ui.draw.clip                      // 导入 clip：裁剪
import androidx.compose.ui.graphics.vector.ImageVector    // 导入 ImageVector：矢量图标
import androidx.compose.ui.text.font.FontWeight           // 导入 FontWeight：字重
import androidx.compose.ui.unit.dp                        // 导入 dp：尺寸单位
import com.memuo.core.ui.AppIcons                         // 导入应用图标集
import com.memuo.core.ui.theme.MuyunBar                   // 导入顶栏半透明背景
import com.memuo.core.ui.theme.MuyunBorder                // 导入分割线色
import com.memuo.core.ui.theme.MuyunText                  // 导入主文字色
import com.memuo.core.ui.theme.MuyunText3                 // 导入三级文字色

/**
 * 子页面通用顶栏（SubHeader）—— 对应 HTML 的 .sub-header：
 * 左返回按钮 + 居中标题 + 右侧槽位（保持 36dp 占位实现真正居中）。
 * 设置/知识库/记忆/任务等所有子页面统一使用。
 */
@Composable                                              // 可组合函数
fun SubHeader(                                           // 子页面顶栏
    title: String,                                       // 居中标题
    onBack: () -> Unit,                                  // 返回回调
    modifier: Modifier = Modifier,                       // 外部修饰
    actions: @Composable () -> Unit = {},                // 右侧操作槽位（可空）
) {
    Row(                                                 // 横向布局
        modifier = modifier                             // 应用外部修饰
            .fillMaxWidth()                             // 占满宽度
            .background(MuyunBar)                       // 半透明背景先铺满（含状态栏区，透出全局壁纸，暗色自适应）
            .statusBarsPadding()                        // 内容区避开状态栏（适配不同设备状态栏高度）
            .height(56.dp)                              // 内容高度 56（HTML .sub-header height）
            .padding(horizontal = 16.dp),               // 左右内边距
        verticalAlignment = Alignment.CenterVertically,  // 垂直居中
    ) {
        HeaderIconButton(icon = AppIcons.Back, onClick = onBack)  // 左：返回按钮
        Text(                                            // 中：标题
            text = title,                                // 标题文字
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,  // 字体
            fontWeight = FontWeight.SemiBold,            // 半粗
            color = MuyunText,                           // 主文字色
            modifier = Modifier.weight(1f),              // 占满剩余空间
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,  // 居中
        )
        Box(modifier = Modifier.size(36.dp)) {           // 右：36dp 槽位（保证标题真居中）
            actions()                                    // 渲染右侧操作
        }
    }
}

/** 顶栏圆形图标按钮（36dp，按下出浅灰底，对应 HTML .sub-back / .chat-menu-btn）。 */
@Composable                                              // 可组合函数
fun HeaderIconButton(                                    // 顶栏图标按钮
    icon: ImageVector,                                   // 图标
    onClick: () -> Unit,                                 // 点击回调
    contentDescription: String? = null,                  // 无障碍描述
    tint: androidx.compose.ui.graphics.Color = MuyunText,  // 图标色
) {
    Box(                                                 // 盒式容器
        modifier = Modifier                             // 修饰
            .size(36.dp)                                // 36dp（触控达标）
            .clip(RoundedCornerShape(10.dp))            // 圆角 10
            .clickable { onClick() }                    // 点击触发
            .background(androidx.compose.ui.graphics.Color.Transparent),  // 透明底（按压态由 ripple 承担）
        contentAlignment = Alignment.Center,             // 居中
    ) {
        Icon(                                            // 图标
            imageVector = icon,                          // 矢量
            contentDescription = contentDescription,     // 描述
            tint = tint,                                 // 颜色
            modifier = Modifier.size(20.dp),             // 20dp（HTML svg width 20）
        )
    }
}

/**
 * 子页面内容容器 —— 对应 HTML .sub-body：20dp 内边距 + 可滚动背景。
 * 简化版：只提供统一的 padding，滚动由各页面的 LazyColumn 自理。
 */
@Composable                                              // 可组合函数
fun SubBody(                                             // 子页面内容容器
    modifier: Modifier = Modifier,                       // 外部修饰
    content: @Composable () -> Unit,                     // 内容
) {
    Box(                                                 // 容器
        modifier = modifier                            // 应用修饰
            .fillMaxWidth()                             // 占满宽度
            .padding(horizontal = 20.dp, vertical = 20.dp)  // HTML .sub-body padding 20
            .navigationBarsPadding(),                   // 底部避让手势条（内容不被导航栏遮挡）
    ) {
        content()                                        // 渲染内容
    }
}
