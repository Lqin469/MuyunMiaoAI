package com.memuo.feature.chat                           // 声明包名：对话业务模块

import androidx.compose.foundation.ExperimentalFoundationApi  // 导入 ExperimentalFoundationApi：combinedClickable
import androidx.compose.foundation.combinedClickable       // 导入 combinedClickable：点击+长按
import androidx.compose.foundation.layout.Column          // 导入 Column：纵向布局
import androidx.compose.foundation.layout.PaddingValues   // 导入 PaddingValues：内边距
import androidx.compose.foundation.layout.fillMaxSize     // 导入 fillMaxSize：铺满
import androidx.compose.foundation.layout.fillMaxWidth    // 导入 fillMaxWidth：占满宽度
import androidx.compose.foundation.layout.padding          // 导入 padding：内边距
import androidx.compose.foundation.lazy.LazyColumn         // 导入 LazyColumn：懒加载列表
import androidx.compose.foundation.lazy.items              // 导入 items：列表项
import androidx.compose.material.icons.Icons               // 导入 Icons：图标集
import androidx.compose.material.icons.filled.Add          // 导入 Add：加号图标
import androidx.compose.material3.AlertDialog              // 导入 AlertDialog：删除确认
import androidx.compose.material3.Card                    // 导入 Card：卡片
import androidx.compose.material3.ExperimentalMaterial3Api // 导入 ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton    // 导入 FAB：新建按钮
import androidx.compose.material3.Icon                    // 导入 Icon：图标
import androidx.compose.material3.MaterialTheme           // 导入 MaterialTheme：主题
import androidx.compose.material3.Scaffold                // 导入 Scaffold：脚手架
import androidx.compose.material3.Text                    // 导入 Text：文本
import androidx.compose.material3.TextButton              // 导入 TextButton：文字按钮
import androidx.compose.material3.TopAppBar               // 导入 TopAppBar：顶部栏
import androidx.compose.runtime.Composable                // 导入 Composable
import androidx.compose.runtime.LaunchedEffect            // 导入 LaunchedEffect
import androidx.compose.runtime.collectAsState            // 导入 collectAsState
import androidx.compose.runtime.getValue                  // 导入 getValue
import androidx.compose.runtime.mutableStateOf            // 导入 mutableStateOf
import androidx.compose.runtime.remember                   // 导入 remember
import androidx.compose.runtime.setValue                  // 导入 setValue
import androidx.compose.ui.Modifier                       // 导入 Modifier
import androidx.compose.ui.text.style.TextOverflow        // 导入 TextOverflow
import androidx.compose.ui.unit.dp                        // 导入 dp
import androidx.hilt.navigation.compose.hiltViewModel     // 导入 hiltViewModel
import com.memuo.core.db.entity.Conversation               // 导入会话实体
import java.text.SimpleDateFormat                          // 导入 SimpleDateFormat：时间格式化
import java.util.Date                                     // 导入 Date
import java.util.Locale                                   // 导入 Locale

/**
 * 会话列表页 —— AI 对话的会话列表（M3 补全）。
 * 新建会话（FAB）、点击进入对话、长按删除。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)  // 实验性 API
@Composable                                               // 可组合 UI 函数
fun ChatListScreen(                                       // 会话列表页
    onOpenConversation: (Long) -> Unit,                   // 点击会话 → 进对话
    viewModel: ChatViewModel = hiltViewModel(),           // Hilt 提供 ViewModel
) {
    val conversations by viewModel.conversations.collectAsState()  // 订阅会话列表
    var pendingDelete by remember { mutableStateOf<Conversation?>(null) }  // 待删除会话

    LaunchedEffect(Unit) {                                // 进入时加载
        viewModel.loadConversations()                     // 加载会话列表
    }

    Scaffold(                                             // 脚手架
        topBar = { TopAppBar(title = { Text("AI 对话") }) },  // 顶部栏
        floatingActionButton = {                          // 新建按钮
            FloatingActionButton(                         // FAB
                onClick = {                              // 点击新建
                    viewModel.newConversation { id -> onOpenConversation(id) }  // 新建并进对话
                },
            ) { Icon(Icons.Filled.Add, contentDescription = "新建对话") }  // 加号
        },
    ) { innerPadding ->                                   // 内容区
        if (conversations.isEmpty()) {                    // 空态
            Text(                                         // 空提示
                "还没有对话，点右下角 ＋ 开始新对话",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(innerPadding).padding(24.dp),
            )
        } else {                                          // 列表
            LazyColumn(                                   // 懒加载列表
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                items(conversations, key = { it.id }) { conv ->  // 遍历会话
                    ConversationCard(                     // 会话卡片
                        conv = conv,
                        onClick = { onOpenConversation(conv.id) },  // 点击进入
                        onLongClick = { pendingDelete = conv },      // 长按删除
                    )
                }
            }
        }
    }

    // 删除确认对话框
    pendingDelete?.let { conv ->                          // 有待删除会话
        AlertDialog(                                      // 确认框
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除对话") },
            text = { Text("确定删除「${conv.title}」吗？其消息将一并删除。") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteConversation(conv.id); pendingDelete = null }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } },
        )
    }
}

/** 会话卡片：标题 + 更新时间。 */
@Composable                                               // 可组合 UI 函数
private fun ConversationCard(                             // 会话卡片
    conv: Conversation,                                   // 会话数据
    onClick: () -> Unit,                                  // 点击
    onLongClick: () -> Unit,                              // 长按
) {
    Card(                                                 // 卡片
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),  // 点击+长按
    ) {
        Column(modifier = Modifier.padding(16.dp)) {      // 卡片内部
            Text(                                         // 标题
                conv.title.ifBlank { "新对话" },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(                                         // 时间
                formatTime(conv.updatedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** 时间戳 → 可读时间。 */
private fun formatTime(ts: Long): String =               // 时间格式化
    SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(ts))  // 格式化
