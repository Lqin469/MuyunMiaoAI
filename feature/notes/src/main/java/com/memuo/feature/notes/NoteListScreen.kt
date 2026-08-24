package com.memuo.feature.notes                         // 声明包名：笔记业务模块

import androidx.compose.foundation.layout.Column          // 导入 Column：纵向布局
import androidx.compose.foundation.layout.PaddingValues   // 导入 PaddingValues：内边距
import androidx.compose.foundation.layout.fillMaxSize     // 导入 fillMaxSize：铺满
import androidx.compose.foundation.layout.fillMaxWidth    // 导入 fillMaxWidth：占满宽度
import androidx.compose.foundation.layout.padding          // 导入 padding：外边距
import androidx.compose.foundation.lazy.LazyColumn         // 导入 LazyColumn：懒加载列表
import androidx.compose.foundation.lazy.items              // 导入 items：列表项扩展
import androidx.compose.material3.Card                    // 导入 Card：卡片容器
import androidx.compose.material3.ExperimentalMaterial3Api // 导入 ExperimentalMaterial3Api：实验性 API 注解
import androidx.compose.material3.FloatingActionButton    // 导入 FAB：悬浮新建按钮
import androidx.compose.material3.MaterialTheme           // 导入 MaterialTheme：主题
import androidx.compose.material3.Scaffold                // 导入 Scaffold：页面脚手架（含 FAB 槽位）
import androidx.compose.material3.Text                    // 导入 Text：文本
import androidx.compose.material3.TopAppBar               // 导入 TopAppBar：顶部栏
import androidx.compose.runtime.Composable                // 导入 Composable：可组合函数注解
import androidx.compose.runtime.collectAsState            // 导入 collectAsState：状态流→Compose 状态
import androidx.compose.runtime.getValue                  // 导入 getValue：by 委托
import androidx.compose.ui.Modifier                       // 导入 Modifier：修饰
import androidx.compose.ui.text.style.TextOverflow        // 导入 TextOverflow：文本溢出省略
import androidx.compose.ui.unit.dp                        // 导入 dp：尺寸单位
import androidx.hilt.navigation.compose.hiltViewModel     // 导入 hiltViewModel：Hilt 提供 ViewModel
import com.memuo.core.db.entity.Note                       // 导入笔记实体

/**
 * 笔记列表页 —— 常规备忘录的首页（M2）。
 * 显示所有未删除笔记（置顶优先、更新时间倒序），右下角 FAB 新建，点击进入编辑。
 */
@OptIn(ExperimentalMaterial3Api::class)                  // 声明使用实验性 API（TopAppBar）
@Composable                                               // 可组合 UI 函数
fun NoteListScreen(                                       // 笔记列表页
    onOpenNote: (Long) -> Unit,                           // 打开某条笔记的回调（跳转编辑页）
    viewModel: NoteListViewModel = hiltViewModel(),       // 用 Hilt 获取 ViewModel（默认参数）
) {
    val notes by viewModel.notes.collectAsState()         // 订阅笔记列表状态流

    Scaffold(                                             // 页面脚手架：提供顶部栏 + FAB 布局
        topBar = {                                       // 顶部栏
            TopAppBar(title = { Text("常规备忘录") })     // 标题"常规备忘录"
        },
        floatingActionButton = {                          // 悬浮按钮（右下角）
            FloatingActionButton(                         // FAB 组件
                onClick = {                              // 点击：新建笔记并跳转编辑
                    viewModel.createNote { id -> onOpenNote(id) }  // 回调式：拿到真实 ID 后再跳转
                },
            ) { Text("＋") }                             // FAB 文字（加号）
        },
    ) { innerPadding ->                                   // 内容区（带内边距）
        if (notes.isEmpty()) {                            // 列表为空时显示提示
            Text(                                         // 空态提示
                "还没有笔记，点右下角 ＋ 新建一条",
                style = MaterialTheme.typography.bodyMedium,  // 正文样式
                modifier = Modifier.padding(innerPadding).padding(24.dp),  // 内边距
            )
        } else {                                          // 列表非空：显示笔记列表
            LazyColumn(                                   // 懒加载列表
                modifier = Modifier.fillMaxSize().padding(innerPadding),  // 铺满 + 内边距
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),  // 列表内边距
            ) {
                items(notes, key = { it.id }) { note ->   // 遍历笔记，以 id 为 key（高效复用）
                    NoteCard(note = note, onClick = { onOpenNote(note.id) })  // 渲染单个笔记卡片
                }
            }
        }
    }
}

/** 单条笔记卡片：显示标题 + 正文摘要。 */
@Composable                                               // 可组合 UI 函数
private fun NoteCard(                                     // 笔记卡片组件（私有，仅本文件用）
    note: Note,                                           // 笔记数据
    onClick: () -> Unit,                                  // 点击回调
) {
    Card(                                                 // 卡片容器
        onClick = onClick,                                // 点击卡片触发
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),  // 占满宽度 + 上下留白
    ) {
        Column(modifier = Modifier.padding(16.dp)) {      // 卡片内部纵向布局 + 内边距
            Text(                                         // 标题行
                text = note.title.ifBlank { "无标题" },    // 空标题显示"无标题"
                style = MaterialTheme.typography.titleMedium,  // 标题样式
            )
            Text(                                         // 摘要行
                text = note.content.ifBlank { "（空内容）" },  // 空内容占位
                style = MaterialTheme.typography.bodyMedium,   // 正文样式
                maxLines = 2,                             // 最多两行
                overflow = TextOverflow.Ellipsis,         // 超出省略号
            )
        }
    }
}
