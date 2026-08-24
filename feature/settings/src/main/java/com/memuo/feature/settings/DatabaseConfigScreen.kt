package com.memuo.feature.settings                         // 声明包名：设置业务模块

import androidx.compose.foundation.layout.Column          // 导入 Column：纵向布局
import androidx.compose.foundation.layout.Spacer          // 导入 Spacer：占位
import androidx.compose.foundation.layout.fillMaxSize     // 导入 fillMaxSize：铺满
import androidx.compose.foundation.layout.fillMaxWidth    // 导入 fillMaxWidth：占满宽度
import androidx.compose.foundation.layout.height          // 导入 height：高度
import androidx.compose.foundation.layout.padding          // 导入 padding：内边距
import androidx.compose.material3.Card                    // 导入 Card：卡片
import androidx.compose.material3.ExperimentalMaterial3Api // 导入 ExperimentalMaterial3Api：实验性 API
import androidx.compose.material3.MaterialTheme           // 导入 MaterialTheme：主题
import androidx.compose.material3.Scaffold                // 导入 Scaffold：页面脚手架
import androidx.compose.material3.Text                    // 导入 Text：文本
import androidx.compose.material3.TopAppBar               // 导入 TopAppBar：顶部栏
import androidx.compose.runtime.Composable                // 导入 Composable：可组合函数注解
import androidx.compose.ui.Modifier                       // 导入 Modifier：修饰
import androidx.compose.ui.unit.dp                        // 导入 dp：尺寸单位
import androidx.hilt.navigation.compose.hiltViewModel     // 导入 hiltViewModel：Hilt 提供 ViewModel
import androidx.lifecycle.ViewModel                       // 导入 ViewModel：UI 数据持有者
import com.memuo.core.storage.StorageProvider             // 导入存储提供者
import dagger.hilt.android.lifecycle.HiltViewModel        // 导入 HiltViewModel
import javax.inject.Inject                                // 导入 Inject：注入

/**
 * 数据库配置页 —— 展示存储目录布局（R4/R5）。
 * 当前展示只读目录信息；自定义存储目录切换（R5）后续接入 StorageMigrator。
 */
@OptIn(ExperimentalMaterial3Api::class)                  // 声明使用实验性 API（TopAppBar）
@Composable                                               // 可组合 UI 函数
fun DatabaseConfigScreen(                                 // 数据库配置页
    viewModel: DatabaseViewModel = hiltViewModel(),       // Hilt 提供 ViewModel
) {
    Scaffold(                                             // 页面脚手架
        topBar = { TopAppBar(title = { Text("数据库配置") }) },  // 顶部栏
    ) { innerPadding ->                                   // 内容区
        Column(                                           // 纵向布局
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),  // 铺满 + 内边距
        ) {
            DirCard("存储根目录", viewModel.root)         // 根目录
            DirCard("数据库目录", viewModel.dbDir)         // 数据库目录
            DirCard("模型目录", viewModel.modelsDir)       // 模型目录
            DirCard("知识库目录", viewModel.knowledgeDir)  // 知识库目录
            DirCard("索引目录", viewModel.indexDir)        // 索引目录
            Spacer(Modifier.height(16.dp))                // 间距
            Text(                                         // 说明
                "自定义存储目录（R5）将在后续版本开放，当前使用应用私有目录。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 目录信息卡片。 */
@Composable                                               // 可组合 UI 函数
private fun DirCard(label: String, path: String) {        // 目录卡片
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {  // 卡片容器
        Column(modifier = Modifier.padding(16.dp)) {      // 卡片内部
            Text(label, style = MaterialTheme.typography.titleSmall)  // 目录标签
            Spacer(Modifier.height(4.dp))                 // 间距
            Text(path, style = MaterialTheme.typography.bodySmall)  // 目录路径
        }
    }
}

/** 数据库配置 ViewModel —— 暴露存储目录布局。 */
@HiltViewModel                                           // 注解：由 Hilt 创建
class DatabaseViewModel @Inject constructor(             // 构造函数注入
    storage: StorageProvider,                            // 注入存储提供者
) : ViewModel() {                                        // 继承 ViewModel

    /** 存储根目录。 */
    val root: String = storage.root.absolutePath         // 根目录路径

    /** 数据库目录。 */
    val dbDir: String = storage.dbDir().absolutePath     // 数据库路径

    /** 模型目录。 */
    val modelsDir: String = storage.modelsDir().absolutePath  // 模型路径

    /** 知识库素材目录。 */
    val knowledgeDir: String = storage.knowledgeDir().absolutePath  // 知识库路径

    /** 索引目录。 */
    val indexDir: String = storage.indexDir().absolutePath  // 索引路径
}
