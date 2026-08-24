package com.memuo.feature.notes                         // 声明包名：笔记业务模块

import androidx.compose.foundation.layout.Column          // 导入 Column：纵向布局
import androidx.compose.foundation.layout.fillMaxHeight   // 导入 fillMaxHeight：占满高度
import androidx.compose.foundation.layout.fillMaxSize     // 导入 fillMaxSize：铺满
import androidx.compose.foundation.layout.fillMaxWidth    // 导入 fillMaxWidth：占满宽度
import androidx.compose.foundation.layout.padding          // 导入 padding：外边距
import androidx.compose.foundation.text.BasicTextField     // 导入 BasicTextField：无边框输入框
import androidx.compose.material.icons.Icons               // 导入 Icons：图标集
import androidx.compose.material.icons.automirrored.filled.ArrowBack  // 导入 ArrowBack：返回箭头
import androidx.compose.material3.ExperimentalMaterial3Api // 导入 ExperimentalMaterial3Api：实验性 API
import androidx.compose.material3.HorizontalDivider        // 导入 HorizontalDivider：分割线
import androidx.compose.material3.Icon                     // 导入 Icon：图标
import androidx.compose.material3.IconButton               // 导入 IconButton：图标按钮
import androidx.compose.material3.MaterialTheme            // 导入 MaterialTheme：主题
import androidx.compose.material3.Scaffold                 // 导入 Scaffold：页面脚手架
import androidx.compose.material3.Text                     // 导入 Text：文本
import androidx.compose.material3.TextButton               // 导入 TextButton：文字按钮
import androidx.compose.material3.TopAppBar                // 导入 TopAppBar：顶部栏
import androidx.compose.runtime.Composable                 // 导入 Composable：可组合函数注解
import androidx.compose.runtime.LaunchedEffect             // 导入 LaunchedEffect：副作用
import androidx.compose.runtime.collectAsState             // 导入 collectAsState：状态流→Compose 状态
import androidx.compose.runtime.getValue                   // 导入 getValue：by 委托
import androidx.compose.runtime.mutableStateOf             // 导入 mutableStateOf：可变状态
import androidx.compose.runtime.remember                    // 导入 remember：记住状态
import androidx.compose.runtime.setValue                   // 导入 setValue：by 委托写
import androidx.compose.ui.Modifier                        // 导入 Modifier：修饰
import androidx.compose.ui.graphics.SolidColor              // 导入 SolidColor：光标颜色
import androidx.compose.ui.text.TextStyle                  // 导入 TextStyle：文本样式
import androidx.compose.ui.unit.dp                         // 导入 dp：尺寸单位
import androidx.hilt.navigation.compose.hiltViewModel      // 导入 hiltViewModel：Hilt 提供 ViewModel
import com.memuo.core.db.entity.Note                        // 导入笔记实体

/**
 * 笔记编辑页 —— 编辑单条笔记的标题与正文（M2，M-012 美化）。
 * 无边框大字号标题 + 全屏正文，顶部返回/保存，底部字数统计。
 */
@OptIn(ExperimentalMaterial3Api::class)                  // 声明使用实验性 API
@Composable                                               // 可组合 UI 函数
fun NoteEditScreen(                                       // 笔记编辑页
    noteId: Long,                                         // 要编辑的笔记 ID
    onBack: () -> Unit,                                   // 返回回调
    viewModel: NoteListViewModel = hiltViewModel(),       // Hilt 提供 ViewModel
) {
    val note by viewModel.observeNote(noteId).collectAsState(initial = null)  // 加载单条笔记

    var title by remember { mutableStateOf("") }          // 标题编辑状态
    var content by remember { mutableStateOf("") }        // 正文编辑状态

    LaunchedEffect(note?.id) {                            // 笔记加载后同步到编辑框
        title = note?.title ?: ""                         // 同步标题
        content = note?.content ?: ""                     // 同步正文
    }

    Scaffold(                                             // 页面脚手架
        topBar = {                                       // 顶部栏
            TopAppBar(                                   // 顶部栏组件
                title = { Text("") },                    // 标题留空（编辑页用内容本身）
                navigationIcon = {                       // 返回按钮
                    IconButton(onClick = onBack) {       // 点击返回
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")  // 返回箭头
                    }
                },
                actions = {                              // 保存按钮
                    TextButton(                          // 文字按钮
                        onClick = {                      // 点击保存
                            viewModel.updateContent(noteId, title, content)  // 写库 + 发变更事件
                            onBack()                     // 返回
                        },
                    ) {
                        Text(                            // 保存文字（主题色加粗）
                            "保存",
                            color = MaterialTheme.colorScheme.primary,  // 主题色
                            style = MaterialTheme.typography.titleMedium,  // 标题字体
                        )
                    }
                },
            )
        },
    ) { innerPadding ->                                   // 内容区
        Column(                                           // 纵向布局
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 20.dp),  // 铺满 + 内边距
        ) {
            // 标题输入（无边框大字号）
            BasicTextField(                               // 无边框输入框
                value = title,                            // 绑定标题
                onValueChange = { title = it },           // 输入更新
                textStyle = MaterialTheme.typography.headlineSmall.copy(  // 大字号
                    color = MaterialTheme.colorScheme.onSurface,  // 文字色
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),  // 光标主题色
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),  // 占满宽度 + 上下留白
                decorationBox = { innerTextField ->       // 自定义占位
                    if (title.isEmpty()) {                // 空标题显示占位
                        Text(                             // 占位文本
                            "标题",
                            style = MaterialTheme.typography.headlineSmall.copy(  // 大字号
                                color = MaterialTheme.colorScheme.outline,  // 灰色占位
                            ),
                        )
                    }
                    innerTextField()                      // 实际输入区
                },
            )

            HorizontalDivider()                           // 分割线（标题/正文之间）

            // 正文输入（全屏多行无边框）
            BasicTextField(                               // 无边框输入框
                value = content,                          // 绑定正文
                onValueChange = { content = it },         // 输入更新
                textStyle = MaterialTheme.typography.bodyLarge.copy(  // 正文字体
                    color = MaterialTheme.colorScheme.onSurface,  // 文字色
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,  // 行高
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),  // 光标
                modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(vertical = 12.dp),  // 全屏
                decorationBox = { innerTextField ->       // 自定义占位
                    if (content.isEmpty()) {              // 空正文显示占位
                        Text(                             // 占位文本
                            "开始输入，支持 Markdown…",
                            style = MaterialTheme.typography.bodyLarge.copy(  // 正文字体
                                color = MaterialTheme.colorScheme.outline,  // 灰色占位
                            ),
                        )
                    }
                    innerTextField()                      // 实际输入区
                },
            )

            // 底部字数统计
            Text(                                         // 字数
                "${content.length} 字",
                style = MaterialTheme.typography.labelSmall,  // 小字
                color = MaterialTheme.colorScheme.outline,  // 灰色
                modifier = Modifier.padding(vertical = 8.dp),  // 内边距
            )
        }
    }
}
