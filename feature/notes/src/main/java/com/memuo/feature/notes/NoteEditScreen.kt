package com.memuo.feature.notes                         // 声明包名：笔记业务模块

import androidx.compose.foundation.layout.Column          // 导入 Column：纵向布局
import androidx.compose.foundation.layout.fillMaxSize     // 导入 fillMaxSize：铺满
import androidx.compose.foundation.layout.fillMaxWidth    // 导入 fillMaxWidth：占满宽度
import androidx.compose.foundation.layout.padding          // 导入 padding：外边距
import androidx.compose.material3.ExperimentalMaterial3Api // 导入 ExperimentalMaterial3Api：实验性 API 注解
import androidx.compose.material3.IconButton              // 导入 IconButton：图标按钮（返回/删除）
import androidx.compose.material3.MaterialTheme           // 导入 MaterialTheme：主题
import androidx.compose.material3.OutlinedTextField       // 导入 OutlinedTextField：输入框
import androidx.compose.material3.Scaffold                // 导入 Scaffold：页面脚手架
import androidx.compose.material3.Text                    // 导入 Text：文本
import androidx.compose.material3.TextButton              // 导入 TextButton：文字按钮（保存/删除）
import androidx.compose.material3.TopAppBar               // 导入 TopAppBar：顶部栏
import androidx.compose.runtime.Composable                // 导入 Composable：可组合函数注解
import androidx.compose.runtime.LaunchedEffect            // 导入 LaunchedEffect：副作用（加载时执行）
import androidx.compose.runtime.collectAsState            // 导入 collectAsState：状态流→Compose 状态
import androidx.compose.runtime.getValue                  // 导入 getValue：by 委托
import androidx.compose.runtime.mutableStateOf            // 导入 mutableStateOf：可变状态
import androidx.compose.runtime.remember                  // 导入 remember：记住状态（重组不丢失）
import androidx.compose.runtime.setValue                  // 导入 setValue：by 委托写入
import androidx.compose.ui.Modifier                       // 导入 Modifier：修饰
import androidx.compose.ui.unit.dp                        // 导入 dp：尺寸单位
import androidx.hilt.navigation.compose.hiltViewModel     // 导入 hiltViewModel：Hilt 提供 ViewModel
import com.memuo.core.db.entity.Note                       // 导入笔记实体

/**
 * 笔记编辑页 —— 编辑单条笔记的标题与正文（M2）。
 * 保存时触发 NoteListViewModel.updateContent → 写库 + 发 NoteBridge 变更事件（供 R7 知识库同步）。
 */
@OptIn(ExperimentalMaterial3Api::class)                  // 声明使用实验性 API
@Composable                                               // 可组合 UI 函数
fun NoteEditScreen(                                       // 笔记编辑页
    noteId: Long,                                         // 要编辑的笔记 ID
    onBack: () -> Unit,                                   // 返回回调
    viewModel: NoteListViewModel = hiltViewModel(),       // 用 Hilt 获取 ViewModel（与列表页共享实例）
) {
    val note by viewModel.observeNote(noteId).collectAsState(initial = null)  // 加载单条笔记（响应式）

    var title by remember { mutableStateOf("") }          // 标题编辑状态（本地，避免每键入即写库）
    var content by remember { mutableStateOf("") }        // 正文编辑状态

    LaunchedEffect(note?.id) {                            // 当加载的笔记 ID 变化时，把内容同步到编辑框
        title = note?.title ?: ""                         // 同步标题
        content = note?.content ?: ""                     // 同步正文
    }

    Scaffold(                                             // 页面脚手架
        topBar = {                                       // 顶部栏
            TopAppBar(                                   // 顶部栏组件
                title = { Text("编辑笔记") },            // 标题
                navigationIcon = {                        // 左侧返回按钮
                    IconButton(onClick = onBack) { Text("←") }  // 点击返回
                },
                actions = {                              // 右侧操作按钮
                    TextButton(                          // 保存按钮
                        onClick = {                      // 点击：保存并返回
                            viewModel.updateContent(noteId, title, content)  // 写库 + 发变更事件
                            onBack()                     // 返回列表
                        },
                    ) { Text("保存") }                  // 按钮文字
                },
            )
        },
    ) { innerPadding ->                                   // 内容区
        Column(                                           // 纵向布局
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),  // 铺满 + 内边距
        ) {
            OutlinedTextField(                            // 标题输入框
                value = title,                            // 绑定标题状态
                onValueChange = { title = it },           // 输入时更新状态
                label = { Text("标题") },                 // 占位标签
                modifier = Modifier.fillMaxWidth(),       // 占满宽度
            )
            OutlinedTextField(                            // 正文输入框（多行）
                value = content,                          // 绑定正文状态
                onValueChange = { content = it },         // 输入时更新状态
                label = { Text("正文（支持 Markdown）") }, // 占位标签
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),  // 占满宽度 + 顶部留白
                minLines = 8,                             // 至少 8 行高
            )
        }
    }
}
