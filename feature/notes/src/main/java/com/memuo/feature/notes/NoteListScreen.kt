package com.memuo.feature.notes                         // 声明包名：笔记业务模块

import androidx.compose.foundation.ExperimentalFoundationApi  // 导入 ExperimentalFoundationApi：combinedClickable 实验 API
import androidx.compose.foundation.combinedClickable       // 导入 combinedClickable：点击 + 长按组合
import androidx.compose.foundation.layout.Column          // 导入 Column：纵向布局
import androidx.compose.foundation.layout.PaddingValues   // 导入 PaddingValues：内边距
import androidx.compose.foundation.layout.fillMaxSize     // 导入 fillMaxSize：铺满
import androidx.compose.foundation.layout.fillMaxWidth    // 导入 fillMaxWidth：占满宽度
import androidx.compose.foundation.layout.padding          // 导入 padding：外边距
import androidx.compose.foundation.lazy.LazyColumn         // 导入 LazyColumn：懒加载列表
import androidx.compose.foundation.lazy.items              // 导入 items：列表项扩展
import androidx.compose.material.icons.Icons               // 导入 Icons：图标集
import androidx.compose.material.icons.filled.Add          // 导入 Add：加号图标
import androidx.compose.material3.AlertDialog              // 导入 AlertDialog：确认对话框
import androidx.compose.material3.Card                    // 导入 Card：卡片容器
import androidx.compose.material3.ExperimentalMaterial3Api // 导入 ExperimentalMaterial3Api：实验性 API
import androidx.compose.material3.FloatingActionButton    // 导入 FAB：悬浮新建按钮
import androidx.compose.material3.Icon                    // 导入 Icon：图标
import androidx.compose.material3.MaterialTheme           // 导入 MaterialTheme：主题
import androidx.compose.material3.Scaffold                // 导入 Scaffold：页面脚手架
import androidx.compose.material3.Text                    // 导入 Text：文本
import androidx.compose.material3.TextButton              // 导入 TextButton：文字按钮（对话框）
import androidx.compose.material3.TopAppBar               // 导入 TopAppBar：顶部栏
import androidx.compose.runtime.Composable                // 导入 Composable：可组合函数注解
import androidx.compose.runtime.collectAsState            // 导入 collectAsState：状态流→Compose 状态
import androidx.compose.runtime.getValue                  // 导入 getValue：by 委托
import androidx.compose.runtime.mutableStateOf            // 导入 mutableStateOf：可变状态
import androidx.compose.runtime.remember                   // 导入 remember：记住状态
import androidx.compose.runtime.setValue                  // 导入 setValue：by 委托写
import androidx.compose.ui.Modifier                       // 导入 Modifier：修饰
import androidx.compose.ui.text.style.TextOverflow        // 导入 TextOverflow：文本溢出省略
import androidx.compose.ui.unit.dp                        // 导入 dp：尺寸单位
import androidx.hilt.navigation.compose.hiltViewModel     // 导入 hiltViewModel：Hilt 提供 ViewModel
import com.memuo.core.db.entity.Note                       // 导入笔记实体

/**
 * 笔记列表页 —— 常规备忘录的首页（M2）。
 * 显示所有未删除笔记（置顶优先、更新时间倒序），右下角 FAB 新建，
 * 点击进入编辑，**长按弹出删除确认**。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)  // 声明使用实验性 API
@Composable                                               // 可组合 UI 函数
fun NoteListScreen(                                       // 笔记列表页
    onOpenNote: (Long) -> Unit,                           // 打开某条笔记的回调（跳转编辑页）
    viewModel: NoteListViewModel = hiltViewModel(),       // 用 Hilt 获取 ViewModel（默认参数）
) {
    val notes by viewModel.notes.collectAsState()         // 订阅笔记列表状态流
    var pendingDelete by remember { mutableStateOf<Note?>(null) }  // 待删除的笔记（null = 无）

    Scaffold(                                             // 页面脚手架：顶部栏 + FAB
        topBar = {                                       // 顶部栏
            TopAppBar(title = { Text("常规备忘录") })     // 标题
        },
        floatingActionButton = {                          // 悬浮按钮（右下角）
            FloatingActionButton(                         // FAB 组件
                onClick = {                              // 点击：新建笔记并跳转编辑
                    viewModel.createNote { id -> onOpenNote(id) }  // 回调式：拿到真实 ID 后跳转
                },
            ) { Icon(Icons.Filled.Add, contentDescription = "新建") }  // 加号图标
        },
    ) { innerPadding ->                                   // 内容区
        if (notes.isEmpty()) {                            // 空态
            Text(                                         // 空提示
                "还没有笔记，点右下角 ＋ 新建一条",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(innerPadding).padding(24.dp),
            )
        } else {                                          // 列表
            LazyColumn(                                   // 懒加载列表
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                items(notes, key = { it.id }) { note ->   // 遍历笔记
                    NoteCard(                             // 单条卡片
                        note = note,
                        onClick = { onOpenNote(note.id) },  // 点击打开
                        onLongClick = { pendingDelete = note },  // 长按弹删除
                    )
                }
            }
        }
    }

    // 删除确认对话框
    pendingDelete?.let { note ->                          // 有待删除笔记时显示
        AlertDialog(                                      // 确认对话框
            onDismissRequest = { pendingDelete = null },  // 点外部取消
            title = { Text("删除笔记") },                 // 标题
            text = { Text("确定删除「${note.title.ifBlank { "无标题" }}」吗？删除后不可恢复。") },  // 提示
            confirmButton = {                             // 确认按钮
                TextButton(                               // 删除按钮
                    onClick = {                           // 点击删除
                        viewModel.deleteNote(note.id)     // 软删除
                        pendingDelete = null              // 关闭对话框
                    },
                ) { Text("删除") }                       // 按钮文字
            },
            dismissButton = {                             // 取消按钮
                TextButton(                               // 取消按钮
                    onClick = { pendingDelete = null },   // 关闭
                ) { Text("取消") }                        // 按钮文字
            },
        )
    }
}

/** 单条笔记卡片：显示标题 + 正文摘要，支持点击打开 / 长按删除。 */
@Composable                                               // 可组合 UI 函数
private fun NoteCard(                                     // 笔记卡片组件
    note: Note,                                           // 笔记数据
    onClick: () -> Unit,                                  // 点击回调
    onLongClick: () -> Unit,                              // 长按回调
) {
    Card(                                                 // 卡片容器
        modifier = Modifier                                // 修饰
            .fillMaxWidth()                               // 占满宽度
            .padding(vertical = 4.dp)                     // 上下留白
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),  // 点击 + 长按
    ) {
        Column(modifier = Modifier.padding(16.dp)) {      // 卡片内部纵向布局
            Text(                                         // 标题行
                text = note.title.ifBlank { "无标题" },    // 空标题占位
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
