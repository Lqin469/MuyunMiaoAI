package com.memuo.feature.notes                         // 声明包名：笔记业务模块

import androidx.compose.foundation.background             // 导入 background：背景修饰
import androidx.compose.foundation.clickable              // 导入 clickable：点击修饰
import androidx.compose.foundation.layout.Box             // 导入 Box：盒式布局
import androidx.compose.foundation.layout.Column          // 导入 Column：纵向布局
import androidx.compose.foundation.layout.Row             // 导入 Row：横向布局
import androidx.compose.foundation.layout.fillMaxSize     // 导入 fillMaxSize：铺满
import androidx.compose.foundation.layout.fillMaxWidth    // 导入 fillMaxWidth：占满宽度
import androidx.compose.foundation.layout.padding          // 导入 padding：内边距
import androidx.compose.foundation.layout.size            // 导入 size：固定尺寸
import androidx.compose.foundation.lazy.LazyColumn         // 导入 LazyColumn：懒加载列表
import androidx.compose.foundation.lazy.items              // 导入 items：列表项
import androidx.compose.foundation.shape.CircleShape       // 导入 CircleShape：圆形
import androidx.compose.foundation.shape.RoundedCornerShape  // 导入 RoundedCornerShape：圆角形状
import androidx.compose.material3.Icon                    // 导入 Icon：图标
import androidx.compose.material3.MaterialTheme           // 导入 MaterialTheme：主题
import androidx.compose.material3.Text                    // 导入 Text：文本
import androidx.compose.runtime.Composable                // 导入 Composable
import androidx.compose.runtime.collectAsState            // 导入 collectAsState
import androidx.compose.runtime.getValue                  // 导入 getValue
import androidx.compose.runtime.mutableStateOf            // 导入 mutableStateOf
import androidx.compose.runtime.remember                  // 导入 remember
import androidx.compose.runtime.setValue                  // 导入 setValue
import androidx.compose.ui.Alignment                      // 导入 Alignment
import androidx.compose.ui.Modifier                       // 导入 Modifier
import androidx.compose.ui.draw.clip                      // 导入 clip：裁剪
import androidx.compose.ui.draw.shadow                    // 导入 shadow：投影
import androidx.compose.ui.graphics.Color                 // 导入 Color：颜色
import androidx.compose.ui.text.font.FontWeight           // 导入 FontWeight：字重
import androidx.compose.ui.text.style.TextDecoration      // 导入 TextDecoration：删除线
import androidx.compose.ui.unit.dp                        // 导入 dp
import androidx.hilt.navigation.compose.hiltViewModel     // 导入 hiltViewModel
import androidx.lifecycle.ViewModel                       // 导入 ViewModel
import androidx.lifecycle.viewModelScope                  // 导入 viewModelScope
import com.memuo.core.db.dao.NoteDao                      // 导入笔记 DAO
import com.memuo.core.db.entity.Note                       // 导入笔记实体
import com.memuo.core.db.entity.NoteType                   // 导入笔记类型
import com.memuo.core.db.entity.TodoItem                   // 导入待办实体
import com.memuo.core.ui.AppIcons                         // 导入应用图标集
import com.memuo.core.ui.components.EmptyState            // 导入空态组件
import com.memuo.core.ui.components.MuyunSegmented        // 导入分段胶囊
import com.memuo.core.ui.components.SubBody               // 导入子页内容容器
import com.memuo.core.ui.components.SubHeader             // 导入子页顶栏
import com.memuo.core.ui.theme.MuyunAccentLight           // 导入浅灰底
import com.memuo.core.ui.theme.MuyunBrandGradient         // 导入品牌渐变
import com.memuo.core.ui.theme.MuyunCard                  // 导入卡片白
import com.memuo.core.ui.theme.MuyunGreen                 // 导入成功绿
import com.memuo.core.ui.theme.MuyunText                  // 导入主文字色
import com.memuo.core.ui.theme.MuyunText2                 // 导入次级文字色
import com.memuo.core.ui.theme.MuyunText3                 // 导入三级文字色
import dagger.hilt.android.lifecycle.HiltViewModel        // 导入 HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow            // 导入 MutableStateFlow
import kotlinx.coroutines.flow.StateFlow                  // 导入 StateFlow
import kotlinx.coroutines.flow.asStateFlow                // 导入 asStateFlow
import kotlinx.coroutines.flow.first                       // 导入 first
import kotlinx.coroutines.launch                           // 导入 launch
import javax.inject.Inject                                // 导入 Inject

/** 待办过滤枚举（对应 HTML 任务页 tabs：全部/待办/已完成）。 */
enum class TaskFilter { ALL, TODO, DONE }                 // 三种过滤

/**
 * 待办清单页 —— 待办事项的增删勾选（HTML 任务页迁移）。
 * 对应 HTML：tabs 过滤 + 空态 + 圆形勾选行（完成删除线）+ 输入行（语音按钮 + 圆形添加按钮）。
 */
@Composable                                               // 可组合 UI 函数
fun TodoListScreen(                                       // 待办清单页
    onBack: () -> Unit,                                   // 返回回调
    viewModel: TodoViewModel = hiltViewModel(),           // Hilt 提供 ViewModel
) {
    val todos by viewModel.todos.collectAsState()         // 订阅待办列表
    val filter by viewModel.filter.collectAsState()       // 订阅过滤状态
    var input by remember { mutableStateOf("") }          // 输入框内容
    val visible = todos.filter {                          // 按过滤条件筛选
        when (filter) {                                   // 分支
            TaskFilter.ALL -> true                        // 全部
            TaskFilter.TODO -> !it.done                   // 待办
            TaskFilter.DONE -> it.done                    // 已完成
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {           // 纵向布局
        SubHeader(title = "任务", onBack = onBack)         // 顶栏（HTML .sub-title 任务）
        SubBody(modifier = Modifier.fillMaxSize()) {       // 内容容器
            Column(modifier = Modifier.fillMaxSize()) {    // 纵向内容
                MuyunSegmented(                            // tabs 分段（HTML .task-tabs）
                    labels = listOf("全部", "待办", "已完成"),  // 三段
                    selectedIndex = filter.ordinal,        // 当前选中
                    onSelect = { viewModel.setFilter(TaskFilter.entries[it]) },  // 切换过滤
                    modifier = Modifier.padding(bottom = 20.dp),  // 下留白（HTML margin-bottom 20px）
                )
                if (visible.isEmpty()) {                  // 空态
                    EmptyState(                           // 空态组件
                        icon = AppIcons.Task,             // 对勾插图
                        text = "还没有任务\n试试输入或说「提醒我明天下午三点给妈妈打电话」",  // HTML 空态文案
                    )
                } else {                                  // 列表
                    LazyColumn(                           // 懒加载列表
                        modifier = Modifier.weight(1f),   // 占满剩余
                    ) {
                        items(visible, key = { it.id }) { todo ->  // 遍历待办
                            TodoRow(                      // 待办行
                                todo = todo,              // 数据
                                onToggle = { viewModel.toggleTodo(todo.id, !todo.done) },  // 勾选切换
                                onDelete = { viewModel.deleteTodo(todo.id) },  // 删除
                            )
                        }
                    }
                }
                Row(                                      // 底部输入行（HTML .task-input-wrap）
                    modifier = Modifier                  // 修饰
                        .fillMaxWidth()                  // 占满宽度
                        .padding(top = 16.dp),           // 上留白
                    verticalAlignment = Alignment.CenterVertically,  // 垂直居中
                ) {
                    Box(                                  // 圆角输入框（HTML input：圆角 22 浅灰底）
                        modifier = Modifier              // 修饰
                            .weight(1f)                  // 占满剩余
                            .clip(RoundedCornerShape(22.dp))  // 大圆角
                            .background(MuyunAccentLight)  // 浅灰底
                            .padding(horizontal = 18.dp, vertical = 12.dp),  // 内边距
                    ) {
                        androidx.compose.foundation.text.BasicTextField(  // 无边框输入
                            value = input,                // 绑定输入
                            onValueChange = { input = it },  // 更新输入
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MuyunText),  // 字体
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(MuyunGreen),  // 光标
                            modifier = Modifier.fillMaxWidth(),  // 占满
                            decorationBox = { inner ->     // 占位符
                                if (input.isEmpty()) {    // 空输入
                                    Text("新任务，如：明天下午三点开会", color = MuyunText3, style = MaterialTheme.typography.bodyMedium)  // 占位（HTML placeholder）
                                }
                                inner()                   // 输入区
                            },
                        )
                    }
                    Box(                                  // 语音按钮（HTML 麦克风小按钮）
                        modifier = Modifier              // 修饰
                            .padding(start = 10.dp)      // 左留白
                            .size(44.dp)                 // 44dp
                            .clip(CircleShape)           // 圆形
                            .background(MuyunAccentLight)  // 浅灰底
                            .clickable { /* 语音输入由系统输入法承担，此处预留 */ },  // 预留
                        contentAlignment = Alignment.Center,  // 居中
                    ) {
                        Icon(                            // 麦克风图标
                            imageVector = AppIcons.Mic,  // 图标
                            contentDescription = "语音输入",  // 描述
                            tint = MuyunText2,           // 次级灰
                            modifier = Modifier.size(16.dp),  // 16dp
                        )
                    }
                    Box(                                  // 圆形添加按钮（HTML 渐变圆钮）
                        modifier = Modifier              // 修饰
                            .padding(start = 10.dp)      // 左留白
                            .size(44.dp)                 // 44dp
                            .clip(CircleShape)           // 圆形
                            .background(MuyunBrandGradient)  // 品牌渐变
                            .shadow(8.dp, CircleShape)   // 品牌投影近似
                            .clickable {                 // 点击添加
                                viewModel.addTodo(input)  // 添加待办
                                input = ""                // 清空输入
                            },
                        contentAlignment = Alignment.Center,  // 居中
                    ) {
                        Icon(                            // 加号图标
                            imageVector = AppIcons.Plus, // 图标
                            contentDescription = "添加", // 描述
                            tint = Color.White,          // 白
                            modifier = Modifier.size(18.dp),  // 18dp
                        )
                    }
                }
            }
        }
    }
}

/** 单条待办：圆形勾选框 + 文本（完成删除线）+ 删除按钮（对应 HTML 任务行）。 */
@Composable                                               // 可组合 UI 函数
private fun TodoRow(                                      // 待办行
    todo: TodoItem,                                       // 待办数据
    onToggle: () -> Unit,                                 // 勾选回调
    onDelete: () -> Unit,                                 // 删除回调
) {
    Row(                                                  // 横向布局
        modifier = Modifier                              // 修饰
            .fillMaxWidth()                              // 占满宽度
            .padding(vertical = 7.dp)                    // 上下留白
            .shadow(1.dp, RoundedCornerShape(10.dp))     // 轻投影
            .clip(RoundedCornerShape(10.dp))             // 圆角 10
            .background(MuyunCard)                       // 白底
            .padding(horizontal = 16.dp, vertical = 14.dp),  // 内边距（HTML padding 14px 16px）
        verticalAlignment = Alignment.CenterVertically,   // 垂直居中
    ) {
        Box(                                              // 圆形勾选框（HTML 22×22 圆）
            modifier = Modifier                          // 修饰
                .size(22.dp)                             // 22dp
                .clip(CircleShape)                       // 圆形
                .background(if (todo.done) MuyunGreen else Color.Transparent)  // 完成绿底
                .clickable { onToggle() },               // 点击切换
            contentAlignment = Alignment.Center,          // 居中
        ) {
            if (todo.done) {                              // 完成显示对勾
                Icon(                                     // 对勾图标
                    imageVector = AppIcons.Check,         // 图标
                    contentDescription = "已完成",         // 描述
                    tint = Color.White,                   // 白
                    modifier = Modifier.size(12.dp),      // 12dp
                )
            }
        }
        Text(                                             // 待办文本
            text = todo.text,                             // 内容
            style = MaterialTheme.typography.bodyMedium,  // 字号（HTML 14px）
            color = if (todo.done) MuyunText3 else MuyunText,  // 完成灰/未完成主色
            textDecoration = if (todo.done) TextDecoration.LineThrough else null,  // 完成删除线（HTML line-through）
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),  // 占满剩余
        )
        Box(                                              // 删除按钮
            modifier = Modifier                          // 修饰
                .size(28.dp)                             // 28dp 热区
                .clickable { onDelete() },               // 点击删除
            contentAlignment = Alignment.Center,          // 居中
        ) {
            Icon(                                         // 垃圾桶图标
                imageVector = AppIcons.Trash,             // 图标
                contentDescription = "删除",              // 描述
                tint = MuyunText3,                        // 三级灰
                modifier = Modifier.size(14.dp),          // 14dp（HTML svg 14）
            )
        }
    }
}

/** 待办 ViewModel —— 确保待办清单存在 + 增删勾选 + tabs 过滤。 */
@HiltViewModel                                           // 注解：由 Hilt 创建
class TodoViewModel @Inject constructor(                 // 构造函数注入
    private val noteDao: NoteDao,                        // 注入笔记 DAO
) : ViewModel() {                                        // 继承 ViewModel

    private var todoNoteId: Long = 0L                    // 待办清单 Note 的 ID
    private val _todos = MutableStateFlow<List<TodoItem>>(emptyList())  // 可变待办列表
    val todos: StateFlow<List<TodoItem>> = _todos.asStateFlow()  // 只读暴露
    private val _filter = MutableStateFlow(TaskFilter.ALL)  // 当前过滤（默认全部）
    val filter: StateFlow<TaskFilter> = _filter.asStateFlow()  // 只读暴露

    init {                                                // 初始化
        viewModelScope.launch {                          // 协程中
            todoNoteId = ensureTodoNote()                // 确保待办清单 Note 存在
            noteDao.observeTodos(todoNoteId).collect { _todos.value = it }  // 观察待办
        }
    }

    /** 切换 tabs 过滤。 */
    fun setFilter(f: TaskFilter) {                        // 设置过滤
        _filter.value = f                                 // 更新状态
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
