package com.memuo.feature.settings                         // 声明包名：设置业务模块

import androidx.compose.foundation.layout.Column          // 导入 Column：纵向布局
import androidx.compose.foundation.layout.PaddingValues   // 导入 PaddingValues：内边距
import androidx.compose.foundation.layout.fillMaxSize     // 导入 fillMaxSize：铺满
import androidx.compose.foundation.layout.fillMaxWidth    // 导入 fillMaxWidth：占满宽度
import androidx.compose.foundation.layout.padding          // 导入 padding：内边距
import androidx.compose.foundation.lazy.LazyColumn         // 导入 LazyColumn：懒加载列表
import androidx.compose.foundation.lazy.items              // 导入 items：列表项扩展
import androidx.compose.material3.Card                    // 导入 Card：卡片
import androidx.compose.material3.ExperimentalMaterial3Api // 导入 ExperimentalMaterial3Api：实验性 API
import androidx.compose.material3.MaterialTheme           // 导入 MaterialTheme：主题
import androidx.compose.material3.Scaffold                // 导入 Scaffold：页面脚手架
import androidx.compose.material3.Text                    // 导入 Text：文本
import androidx.compose.material3.TopAppBar               // 导入 TopAppBar：顶部栏
import androidx.compose.runtime.Composable                // 导入 Composable：可组合函数注解
import androidx.compose.runtime.collectAsState            // 导入 collectAsState：状态流→Compose 状态
import androidx.compose.runtime.getValue                  // 导入 getValue：by 委托
import androidx.compose.ui.Modifier                       // 导入 Modifier：修饰
import androidx.compose.ui.unit.dp                        // 导入 dp：尺寸单位
import androidx.hilt.navigation.compose.hiltViewModel     // 导入 hiltViewModel：Hilt 提供 ViewModel
import androidx.lifecycle.ViewModel                       // 导入 ViewModel：UI 数据持有者
import androidx.lifecycle.viewModelScope                  // 导入 viewModelScope：协程作用域
import com.memuo.core.db.dao.MemoryDao                    // 导入记忆 DAO
import com.memuo.core.db.entity.KbMemory                  // 导入记忆实体
import com.memuo.core.db.entity.MemoryType                // 导入记忆类型枚举
import dagger.hilt.android.lifecycle.HiltViewModel        // 导入 HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow           // 导入可变状态流
import kotlinx.coroutines.flow.StateFlow                  // 导入只读状态流
import kotlinx.coroutines.flow.asStateFlow                // 导入 asStateFlow
import kotlinx.coroutines.launch                           // 导入 launch
import javax.inject.Inject                                // 导入 Inject

/**
 * 记忆库页 —— 展示 AI 自动提炼的长期记忆（R6，M5 产出）。
 */
@OptIn(ExperimentalMaterial3Api::class)                  // 声明使用实验性 API（TopAppBar）
@Composable                                               // 可组合 UI 函数
fun MemoryScreen(                                         // 记忆库页
    viewModel: MemoryViewModel = hiltViewModel(),         // Hilt 提供 ViewModel
) {
    val memories by viewModel.memories.collectAsState()   // 订阅记忆列表

    Scaffold(                                             // 页面脚手架
        topBar = { TopAppBar(title = { Text("记忆库") }) },  // 顶部栏
    ) { innerPadding ->                                   // 内容区
        if (memories.isEmpty()) {                         // 空态
            Text(                                         // 空提示
                "暂无记忆。多聊几轮后，AI 会自动提炼事实/偏好/待办到这里。",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(innerPadding).padding(24.dp),
            )
        } else {                                          // 有记忆
            LazyColumn(                                   // 懒加载列表
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                items(memories, key = { it.id }) { m ->   // 遍历记忆
                    MemoryCard(m)                         // 渲染单条记忆
                }
            }
        }
    }
}

/** 单条记忆卡片。 */
@Composable                                               // 可组合 UI 函数
private fun MemoryCard(memory: KbMemory) {                // 记忆卡片
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {  // 卡片容器
        Column(modifier = Modifier.padding(16.dp)) {      // 卡片内部
            Text(                                         // 类型 + 主题
                "${typeLabel(memory.type)} · ${memory.topic}",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(                                         // 内容
                memory.text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** 记忆类型 → 中文标签。 */
private fun typeLabel(type: MemoryType): String = when (type) {  // 类型标签
    MemoryType.FACT -> "事实"                             // 事实
    MemoryType.PREFERENCE -> "偏好"                        // 偏好
    MemoryType.TODO -> "待办"                              // 待办
}

/** 记忆库 ViewModel —— 加载长期记忆列表。 */
@HiltViewModel                                           // 注解：由 Hilt 创建
class MemoryViewModel @Inject constructor(               // 构造函数注入
    private val memoryDao: MemoryDao,                    // 注入记忆 DAO
) : ViewModel() {                                        // 继承 ViewModel

    private val _memories = MutableStateFlow<List<KbMemory>>(emptyList())  // 可变记忆列表
    val memories: StateFlow<List<KbMemory>> = _memories.asStateFlow()  // 只读暴露

    init {                                                // 初始化
        load()                                           // 加载记忆
    }

    /** 加载最近的记忆。 */
    fun load() {                                          // 加载方法
        viewModelScope.launch {                          // 协程中读取
            _memories.value = memoryDao.recent()         // 读最近 500 条
        }
    }
}
