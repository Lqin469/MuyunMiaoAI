package com.memuo.feature.chat                           // 声明包名：对话业务模块

import androidx.compose.foundation.layout.Column          // 导入 Column：纵向布局
import androidx.compose.foundation.layout.Row             // 导入 Row：横向布局
import androidx.compose.foundation.layout.fillMaxSize     // 导入 fillMaxSize：铺满
import androidx.compose.foundation.layout.fillMaxWidth    // 导入 fillMaxWidth：占满宽度
import androidx.compose.foundation.layout.padding          // 导入 padding：外边距
import androidx.compose.foundation.lazy.LazyColumn         // 导入 LazyColumn：消息列表
import androidx.compose.foundation.lazy.items              // 导入 items：列表项扩展
import androidx.compose.foundation.shape.RoundedCornerShape // 导入 RoundedCornerShape：圆角
import androidx.compose.material.icons.Icons               // 导入 Icons：图标集
import androidx.compose.material.icons.automirrored.filled.ArrowBack  // 导入 ArrowBack：返回箭头
import androidx.compose.material3.Button                  // 导入 Button：按钮
import androidx.compose.material3.ExperimentalMaterial3Api // 导入 ExperimentalMaterial3Api：实验性 API 注解
import androidx.compose.material3.Icon                    // 导入 Icon：图标
import androidx.compose.material3.IconButton              // 导入 IconButton：图标按钮
import androidx.compose.material3.MaterialTheme           // 导入 MaterialTheme：主题
import androidx.compose.material3.OutlinedTextField       // 导入 OutlinedTextField：输入框
import androidx.compose.material3.Scaffold                // 导入 Scaffold：页面脚手架
import androidx.compose.material3.Surface                 // 导入 Surface：气泡底板
import androidx.compose.material3.Text                    // 导入 Text：文本
import androidx.compose.material3.TopAppBar               // 导入 TopAppBar：顶部栏
import androidx.compose.runtime.Composable                // 导入 Composable：可组合函数注解
import androidx.compose.runtime.LaunchedEffect            // 导入 LaunchedEffect：副作用
import androidx.compose.runtime.collectAsState            // 导入 collectAsState：状态流→状态
import androidx.compose.runtime.getValue                  // 导入 getValue：by 委托
import androidx.compose.runtime.mutableStateOf            // 导入 mutableStateOf：可变状态
import androidx.compose.runtime.remember                  // 导入 remember：记住状态
import androidx.compose.runtime.setValue                  // 导入 setValue：by 委托写入
import androidx.compose.ui.Alignment                      // 导入 Alignment：对齐
import androidx.compose.ui.Modifier                       // 导入 Modifier：修饰
import androidx.compose.ui.unit.dp                        // 导入 dp：尺寸单位
import androidx.hilt.navigation.compose.hiltViewModel     // 导入 hiltViewModel：Hilt 提供 ViewModel
import com.memuo.core.db.entity.ChatMessage                // 导入消息实体

/**
 * 对话页 —— AI 对话界面（M3）。
 * 消息列表（用户/助手气泡）+ 流式生成实时显示 + 底部输入框发送。
 */
@OptIn(ExperimentalMaterial3Api::class)                  // 声明使用实验性 API
@Composable                                               // 可组合 UI 函数
fun ChatScreen(                                           // 对话页
    conversationId: Long,                                 // 当前会话 ID
    onBack: () -> Unit,                                   // 返回（回会话列表）
    viewModel: ChatViewModel = hiltViewModel(),           // 用 Hilt 获取 ViewModel
) {
    LaunchedEffect(conversationId) {                      // 会话 ID 变化时
        viewModel.openConversation(conversationId)        // 加载该会话的消息
    }
    val messages by viewModel.messages.collectAsState()   // 订阅消息列表
    val streaming by viewModel.streaming.collectAsState() // 订阅流式状态
    val streamText by viewModel.streamText.collectAsState()  // 订阅流式文本
    var input by remember { mutableStateOf("") }          // 输入框内容

    Scaffold(                                             // 页面脚手架
        topBar = {                                       // 顶部栏
            TopAppBar(                                   // 顶部栏组件
                title = { Text("AI 对话") },             // 标题
                navigationIcon = {                       // 返回按钮
                    IconButton(onClick = onBack) {       // 点击返回列表
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")  // 返回箭头
                    }
                },
            )
        },
    ) { innerPadding ->                                   // 内容区
        Column(                                           // 纵向布局
            modifier = Modifier.fillMaxSize().padding(innerPadding),  // 铺满 + 内边距
        ) {
            LazyColumn(                                   // 消息列表（占满剩余空间）
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),  // 权重 1 占满 + 左右留白
            ) {
                items(messages, key = { it.id }) { msg -> // 遍历历史消息
                    MessageBubble(msg)                    // 渲染气泡
                }
                if (streaming) {                          // 流式生成中：追加"正在生成"的气泡
                    item {                                // 单个列表项
                        MessageBubble(                    // 渲染流式中的助手气泡
                            ChatMessage(convId = conversationId, role = "assistant", content = streamText, ts = 0L),  // 临时消息
                        )
                    }
                }
            }
            Row(                                          // 底部输入区（横向）
                modifier = Modifier.fillMaxWidth().padding(12.dp),  // 占满宽度 + 边距
                verticalAlignment = Alignment.CenterVertically,     // 垂直居中
            ) {
                OutlinedTextField(                        // 输入框
                    value = input,                        // 绑定输入
                    onValueChange = { input = it },       // 更新输入
                    placeholder = { Text("问点什么…") },   // 占位提示
                    modifier = Modifier.weight(1f),       // 占满剩余宽度
                )
                Button(                                   // 发送按钮
                    onClick = {                          // 点击发送
                        viewModel.send(input)             // 发送消息
                        input = ""                        // 清空输入框
                    },
                    enabled = !streaming,                 // 流式中禁用（避免并发）
                    modifier = Modifier.padding(start = 8.dp),  // 左侧留白
                ) { Text("发送") }                        // 按钮文字
            }
        }
    }
}

/** 单条消息气泡：user 靠右、assistant 靠左；assistant 消息用 Markdown 渲染。 */
@Composable                                               // 可组合 UI 函数
private fun MessageBubble(msg: ChatMessage) {             // 消息气泡组件
    val isUser = msg.role == "user"                       // 判断是否用户消息
    Row(                                                  // 横向布局（实现左右对齐）
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),  // 占满宽度 + 上下留白
        horizontalArrangement = if (isUser) androidx.compose.foundation.layout.Arrangement.End
                                 else androidx.compose.foundation.layout.Arrangement.Start,  // 用户靠右、助手靠左
    ) {
        Surface(                                          // 气泡底板
            shape = RoundedCornerShape(12.dp),            // 圆角
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,  // 用户用主色容器、助手用灰底
        ) {
            if (isUser) {                                 // 用户消息：纯文本
                Text(                                     // 气泡文字
                    text = msg.content,                   // 内容
                    modifier = Modifier.padding(12.dp),   // 内边距
                    style = MaterialTheme.typography.bodyMedium,  // 正文样式
                )
            } else {                                      // 助手消息：Markdown 渲染
                MarkdownText(                             // 自研 Markdown 渲染
                    content = msg.content.ifBlank { "…" },  // 内容（空显示省略号）
                    modifier = Modifier.padding(12.dp),   // 内边距
                )
            }
        }
    }
}
