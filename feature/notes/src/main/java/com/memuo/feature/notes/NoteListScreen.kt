package com.memuo.feature.notes                         // 声明包名：笔记业务模块

import androidx.compose.foundation.background             // 导入 background：背景修饰
import androidx.compose.foundation.clickable              // 导入 clickable：点击修饰
import androidx.compose.foundation.layout.Box             // 导入 Box：盒式布局
import androidx.compose.foundation.layout.Column          // 导入 Column：纵向布局
import androidx.compose.foundation.layout.fillMaxSize     // 导入 fillMaxSize：铺满
import androidx.compose.foundation.layout.fillMaxWidth    // 导入 fillMaxWidth：占满宽度
import androidx.compose.foundation.layout.navigationBarsPadding  // 导入 navigationBarsPadding：底部手势条避让
import androidx.compose.foundation.layout.padding          // 导入 padding：内边距
import androidx.compose.foundation.lazy.LazyColumn         // 导入 LazyColumn：懒加载列表
import androidx.compose.foundation.lazy.items              // 导入 items：列表项扩展
import androidx.compose.foundation.shape.RoundedCornerShape  // 导入 RoundedCornerShape：圆角形状
import androidx.compose.ui.draw.shadow                    // 导入 shadow：投影
import androidx.compose.material3.MaterialTheme           // 导入 MaterialTheme：主题
import androidx.compose.material3.Text                    // 导入 Text：文本
import androidx.compose.runtime.Composable                // 导入 Composable：可组合函数注解
import androidx.compose.runtime.collectAsState            // 导入 collectAsState：状态流→Compose 状态
import androidx.compose.runtime.getValue                  // 导入 getValue：by 委托
import androidx.compose.runtime.mutableStateOf            // 导入 mutableStateOf：可变状态
import androidx.compose.runtime.remember                   // 导入 remember：记住状态
import androidx.compose.runtime.setValue                  // 导入 setValue：by 委托写
import androidx.compose.ui.Alignment                      // 导入 Alignment：对齐
import androidx.compose.ui.Modifier                       // 导入 Modifier：修饰
import androidx.compose.ui.draw.clip                      // 导入 clip：裁剪
import androidx.compose.ui.text.font.FontWeight           // 导入 FontWeight：字重
import androidx.compose.ui.text.style.TextOverflow        // 导入 TextOverflow：溢出省略
import androidx.compose.ui.unit.dp                        // 导入 dp：尺寸单位
import androidx.hilt.navigation.compose.hiltViewModel     // 导入 hiltViewModel：Hilt 提供 ViewModel
import com.memuo.core.db.entity.Note                       // 导入笔记实体
import com.memuo.core.ui.AppIcons                         // 导入应用图标集
import com.memuo.core.ui.components.EmptyState            // 导入空态组件
import com.memuo.core.ui.components.LocalToast            // 导入 Toast 状态
import com.memuo.core.ui.components.SwipeToReveal         // 导入左滑删除容器
import com.memuo.core.ui.theme.MuyunCard                  // 导入卡片白
import com.memuo.core.ui.theme.MuyunText                  // 导入主文字色
import com.memuo.core.ui.theme.MuyunText2                 // 导入次级文字色
import com.memuo.core.ui.theme.MuyunText3                 // 导入三级文字色
import java.text.SimpleDateFormat                          // 导入 SimpleDateFormat：时间格式化
import java.util.Calendar                                 // 导入 Calendar：日期计算
import java.util.Date                                     // 导入 Date：日期
import java.util.Locale                                   // 导入 Locale：区域

/**
 * 笔记列表页 —— 常规备忘录的首页（HTML 界面原型的「常规」模式视图，v22 迁移）。
 * 对应 HTML 行为：计数头部 + 卡片（标题/两行摘要/时间）+ 左滑露出删除（移入回收站）
 * + 空态插图 + 点击卡片进编辑。
 */
@Composable                                               // 可组合 UI 函数
fun NoteListScreen(                                       // 笔记列表页
    onOpenNote: (Long) -> Unit,                           // 打开某条笔记的回调（跳转编辑页）
    viewModel: NoteListViewModel = hiltViewModel(),       // 用 Hilt 获取 ViewModel（默认参数）
) {
    val notes by viewModel.notes.collectAsState()         // 订阅笔记列表状态流
    val toast = LocalToast.current                        // 取全局 Toast
    var revealedId by remember { mutableStateOf<Long?>(null) }  // 当前左滑展开的笔记 id（同一时间只展开一张）

    Column(                                              // 纵向布局（占满）
        modifier = Modifier                              // 修饰
            .fillMaxSize()                               // 铺满
            .navigationBarsPadding(),                    // 底部避让手势条（列表滚动到底不被导航栏遮挡）
    ) {
        // 列表头：计数（HTML .memo-list-head / .memo-list-count）
        Text(                                             // 计数文字
            text = "${notes.size} 条备忘",               // N 条备忘
            style = MaterialTheme.typography.labelMedium,  // 小字（HTML 12px）
            color = MuyunText3,                           // 三级灰
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),  // 左右 24dp 对齐卡片、上下统一 14dp
        )
        if (notes.isEmpty()) {                            // 空态
            EmptyState(                                   // 空态组件
                icon = AppIcons.DocEmpty,                 // 文档插图
                text = "暂无备忘录\n点击右上角 + 新建一条",  // HTML 空态文案
                modifier = Modifier.fillMaxWidth(),       // 占满宽度
            )
        } else {                                          // 列表
            LazyColumn(                                   // 懒加载列表
                modifier = Modifier.fillMaxSize(),        // 铺满
            ) {
                items(notes, key = { it.id }) { note ->   // 遍历笔记
                    SwipeToReveal(                        // 左滑删除容器
                        revealed = revealedId == note.id, // 当前项是否展开
                        onRevealChanged = { open ->       // 展开状态变化
                            revealedId = if (open) note.id else null  // 展开则记录 id / 收起则清空
                        },
                        actionLabel = "删除",             // 删除层文字
                        onAction = {                       // 点击删除层
                            viewModel.deleteNote(note.id) // 移入回收站（软删除）
                            revealedId = null             // 收起
                            toast.show("已移入回收站")     // Toast 提示
                        },
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 5.dp),  // 卡片间距
                    ) {
                        MemoCard(                         // 备忘录卡片
                            note = note,                  // 笔记数据
                            onClick = {                   // 点击卡片
                                if (revealedId == note.id) revealedId = null  // 已展开 → 先收起（HTML 行为）
                                else onOpenNote(note.id)  // 否则进编辑
                            },
                        )
                    }
                }
            }
        }
    }
}

/** 单条备忘录卡片：标题 + 两行摘要 + 时间（对应 HTML .memo-card）。 */
@Composable                                               // 可组合 UI 函数
private fun MemoCard(                                     // 备忘录卡片
    note: Note,                                           // 笔记数据
    onClick: () -> Unit,                                  // 点击回调
) {
    Column(                                               // 纵向布局
        modifier = Modifier                               // 修饰
            .fillMaxWidth()                               // 占满宽度
            .shadow(1.dp, RoundedCornerShape(14.dp))      // 轻投影（HTML --shadow）
            .clip(RoundedCornerShape(14.dp))              // 圆角 14
            .background(MuyunCard)                        // 白底（遮住底层红色删除层）
            .clickable { onClick() }                      // 点击
            .padding(horizontal = 16.dp, vertical = 14.dp),  // 内边距（HTML padding 14px 16px）
    ) {
        Text(                                             // 标题行
            text = note.title.ifBlank { "无标题" },        // 空标题占位
            style = MaterialTheme.typography.titleSmall,  // 字号（HTML 15px）
            fontWeight = FontWeight.SemiBold,             // 半粗（HTML 600）
            color = MuyunText,                            // 主文字色
            maxLines = 1,                                 // 单行
            overflow = TextOverflow.Ellipsis,             // 溢出省略
        )
        if (note.content.isNotBlank()) {                  // 有正文
            Text(                                         // 摘要行
                text = note.content,                      // 正文
                style = MaterialTheme.typography.bodySmall,  // 字号（HTML 13px）
                color = MuyunText2,                       // 次级灰
                maxLines = 2,                             // 最多两行（HTML -webkit-line-clamp: 2）
                overflow = TextOverflow.Ellipsis,         // 溢出省略
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.4f,  // 行距 1.6 近似
                modifier = Modifier.padding(top = 6.dp),  // 与标题留白（统一 6dp）
            )
        }
        Text(                                             // 时间行
            text = fmtMemoTime(note.updatedAt),           // 格式化时间
            style = MaterialTheme.typography.labelSmall,  // 小字（HTML 11px）
            color = MuyunText3,                           // 三级灰
            modifier = Modifier.padding(top = 6.dp),      // 与摘要留白（统一 6dp，与上方一致）
        )
    }
}

/**
 * 备忘录时间格式化（对应 HTML fmtMemoTime）：
 * 今天 → 「今天 HH:MM」；昨天 → 「昨天 HH:MM」；更早 → 「M月D日」。
 */
private fun fmtMemoTime(ts: Long): String {               // 时间格式化
    val now = Calendar.getInstance()                      // 当前时间
    val today = Calendar.getInstance().apply {            // 今天零点
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)  // 归零
    }
    val yesterday = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }  // 昨天零点
    val hhmm = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))  // 时分
    return when {                                         // 按时间判断
        ts >= today.timeInMillis -> "今天 $hhmm"          // 今天
        ts >= yesterday.timeInMillis -> "昨天 $hhmm"      // 昨天
        else -> SimpleDateFormat("M月d日", Locale.getDefault()).format(Date(ts))  // 更早
    }
}
