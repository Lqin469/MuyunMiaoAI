package com.memuo.feature.notes                         // 声明包名：笔记业务模块

import androidx.compose.foundation.layout.Column          // 导入 Column：纵向布局
import androidx.compose.foundation.layout.Row             // 导入 Row：横向布局
import androidx.compose.foundation.layout.fillMaxSize     // 导入 fillMaxSize：铺满
import androidx.compose.foundation.layout.fillMaxWidth    // 导入 fillMaxWidth：占满宽度
import androidx.compose.foundation.layout.padding          // 导入 padding：内边距
import androidx.compose.foundation.lazy.LazyColumn         // 导入 LazyColumn：懒加载列表
import androidx.compose.foundation.lazy.items              // 导入 items：列表项
import androidx.compose.material.icons.Icons               // 导入 Icons：图标集
import androidx.compose.material.icons.filled.Delete       // 导入 Delete：删除图标
import androidx.compose.material3.Button                  // 导入 Button：按钮
import androidx.compose.material3.Checkbox                // 导入 Checkbox：勾选框
import androidx.compose.material3.ExperimentalMaterial3Api // 导入 ExperimentalMaterial3Api
import androidx.compose.material3.Icon                    // 导入 Icon：图标
import androidx.compose.material3.IconButton              // 导入 IconButton：图标按钮
import androidx.compose.material3.MaterialTheme           // 导入 MaterialTheme：主题
import androidx.compose.material3.OutlinedTextField        // 导入 OutlinedTextField：输入框
import androidx.compose.material3.Scaffold                // 导入 Scaffold：脚手架
import androidx.compose.material3.Text                    // 导入 Text：文本
import androidx.compose.material3.TopAppBar               // 导入 TopAppBar：顶部栏
import androidx.compose.runtime.Composable                // 导入 Composable
import androidx.compose.runtime.collectAsState            // 导入 collectAsState
import androidx.compose.runtime.getValue                  // 导入 getValue
import androidx.compose.runtime.mutableStateOf            // 导入 mutableStateOf
import androidx.compose.runtime.remember                   // 导入 remember
import androidx.compose.runtime.setValue                  // 导入 setValue
import androidx.compose.ui.Alignment                      // 导入 Alignment
import androidx.compose.ui.Modifier                       // 导入 Modifier
import androidx.compose.ui.text.style.TextDecoration      // 导入 TextDecoration：删除线
import androidx.compose.ui.unit.dp                        // 导入 dp
import androidx.hilt.navigation.compose.hiltViewModel     // 导入 hiltViewModel
import androidx.lifecycle.ViewModel                       // 导入 ViewModel
import androidx.lifecycle.viewModelScope                  // 导入 viewModelScope
import com.memuo.core.db.dao.NoteDao                      // 导入笔记 DAO
import com.memuo.core.db.entity.Note                       // 导入笔记实体
import com.memuo.core.db.entity.NoteType                   // 导入笔记类型
import com.memuo.core.db.entity.TodoItem                   // 导入待办实体
import dagger.hilt.android.lifecycle.HiltViewModel        // 导入 HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow            // 导入 MutableStateFlow
import kotlinx.coroutines.flow.StateFlow                  // 导入 StateFlow
import kotlinx.coroutines.flow.asStateFlow                // 导入 asStateFlow
import kotlinx.coroutines.flow.first                       // 导入 first
import kotlinx.coroutines.launch                           // 导入 launch
import javax.inject.Inject                                // 导入 Inject

/**
 * 待办清单页 —— 待办事项的增删勾选（M-013 补全 v1 方案的 TodoItem）。
 */
@OptIn(ExperimentalMaterial3Api::class)                  // 实验性 API
@Composable                                               // 可组合 UI 函数
fun TodoListScreen(                                       // 待办清单页
    viewModel: TodoViewModel = hiltViewModel(),           // Hilt 提供 ViewModel
) {
    val todos by viewModel.todos.collectAsState()         // 订阅待办列表
    var input by remember { mutableStateOf("") }          // 输入框内容

    Scaffold(                                             // 脚手架
        topBar = { TopAppBar(title = { Text("待办清单") }) },  // 顶部栏
    ) { innerPadding ->                                   // 内容区
        Column(                                           // 纵向布局
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            if (todos.isEmpty()) {                        // 空态
                Text(                                     // 空提示
                    "暂无待办，在下方输入添加一条",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(24.dp),
                )
            } else {                                      // 列表
                LazyColumn(                               // 懒加载列表
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                ) {
                    items(todos, key = { it.id }) { todo ->  // 遍历待办
                        TodoRow(                          // 待办行
                            todo = todo,
                            onToggle = { viewModel.toggleTodo(todo.id, !todo.done) },  // 勾选切换
                            onDelete = { viewModel.deleteTodo(todo.id) },  // 删除
                        )
                    }
                }
            }
            // 底部输入区
            Row(                                          // 横向
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(                        // 输入框
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("添加待办…") },
                    modifier = Modifier.weight(1f),
                )
                Button(                                   // 添加按钮
                    onClick = {                           // 点击添加
                        viewModel.addTodo(input)          // 添加
                        input = ""                        // 清空
                    },
                    modifier = Modifier.padding(start = 8.dp),
                ) { Text("添加") }                        // 按钮文字
            }
        }
    }
}

/** 单条待办：勾选框 + 文本 + 删除。 */
@Composable                                               // 可组合 UI 函数
private fun TodoRow(                                      // 待办行
    todo: TodoItem,                                       // 待办数据
    onToggle: () -> Unit,                                 // 勾选回调
    onDelete: () -> Unit,                                 // 删除回调
) {
    Row(                                                  // 横向布局
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(                                         // 勾选框
            checked = todo.done,
            onCheckedChange = { onToggle() },
        )
        Text(                                             // 待办文本
            todo.text,
            style = MaterialTheme.typography.bodyLarge,
            textDecoration = if (todo.done) TextDecoration.LineThrough else null,  // 完成加删除线
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDelete) {                  // 删除按钮
            Icon(Icons.Filled.Delete, contentDescription = "删除")  // 删除图标
        }
    }
}

/** 待办 ViewModel —— 确保待办清单存在 + 增删勾选。 */
@HiltViewModel                                           // 注解：由 Hilt 创建
class TodoViewModel @Inject constructor(                 // 构造函数注入
    private val noteDao: NoteDao,                        // 注入笔记 DAO
) : ViewModel() {                                        // 继承 ViewModel

    private var todoNoteId: Long = 0L                    // 待办清单 Note 的 ID
    private val _todos = MutableStateFlow<List<TodoItem>>(emptyList())  // 可变待办列表
    val todos: StateFlow<List<TodoItem>> = _todos.asStateFlow()  // 只读暴露

    init {                                                // 初始化
        viewModelScope.launch {                          // 协程中
            todoNoteId = ensureTodoNote()                // 确保待办清单 Note 存在
            noteDao.observeTodos(todoNoteId).collect { _todos.value = it }  // 观察待办
        }
    }

    /** 确保存在一个 type=TODO 的待办清单 Note，返回其 ID。 */
    private suspend fun ensureTodoNote(): Long {          // 确保待办清单
        val notes = noteDao.observeActive().first()       // 取一次活跃笔记
        val existing = notes.firstOrNull { it.type == NoteType.TODO }  // 找已有的待办清单
        return if (existing != null) {                    // 已有
            existing.id                                    // 返回其 ID
        } else {                                          // 没有则新建
            val now = System.currentTimeMillis()          // 时间戳
            noteDao.upsert(                               // 新建待办清单 Note
                Note(
                    title = "待办清单",
                    content = "",
                    type = NoteType.TODO,
                    createdAt = now,
                    updatedAt = now,
                )
            )
        }
    }

    /** 添加一条待办。 */
    fun addTodo(text: String) {                           // 添加待办
        if (text.isBlank()) return                        // 空文本忽略
        viewModelScope.launch {                          // 协程中写入
            noteDao.upsertTodo(                           // 插入待办
                TodoItem(noteId = todoNoteId, text = text, order = _todos.value.size)
            )
        }
    }

    /** 切换待办完成状态。 */
    fun toggleTodo(id: Long, done: Boolean) {             // 勾选切换
        viewModelScope.launch { noteDao.updateTodoDone(id, done) }  // 更新完成状态
    }

    /** 删除一条待办。 */
    fun deleteTodo(id: Long) {                            // 删除待办
        viewModelScope.launch { noteDao.deleteTodo(id) }  // 删除
    }
}
