package com.memuo.feature.settings                         // 声明包名：设置业务模块

import androidx.compose.foundation.layout.Column          // 导入 Column：纵向布局
import androidx.compose.foundation.layout.Spacer          // 导入 Spacer：占位
import androidx.compose.foundation.layout.fillMaxSize     // 导入 fillMaxSize：铺满
import androidx.compose.foundation.layout.fillMaxWidth    // 导入 fillMaxWidth：占满宽度
import androidx.compose.foundation.layout.height          // 导入 height：高度
import androidx.compose.foundation.layout.padding          // 导入 padding：内边距
import androidx.compose.material3.Button                  // 导入 Button：按钮
import androidx.compose.material3.ExperimentalMaterial3Api // 导入 ExperimentalMaterial3Api：实验性 API
import androidx.compose.material3.MaterialTheme           // 导入 MaterialTheme：主题
import androidx.compose.material3.OutlinedTextField        // 导入 OutlinedTextField：输入框
import androidx.compose.material3.Scaffold                // 导入 Scaffold：页面脚手架
import androidx.compose.material3.Text                    // 导入 Text：文本
import androidx.compose.material3.TopAppBar               // 导入 TopAppBar：顶部栏
import androidx.compose.runtime.Composable                // 导入 Composable：可组合函数注解
import androidx.compose.runtime.collectAsState            // 导入 collectAsState：状态流→Compose 状态
import androidx.compose.runtime.getValue                  // 导入 getValue：by 委托
import androidx.compose.runtime.mutableStateOf            // 导入 mutableStateOf：可组合状态
import androidx.compose.runtime.remember                   // 导入 remember：记住状态
import androidx.compose.runtime.setValue                  // 导入 setValue：by 委托写
import androidx.compose.ui.Modifier                       // 导入 Modifier：修饰
import androidx.compose.ui.unit.dp                        // 导入 dp：尺寸单位
import androidx.hilt.navigation.compose.hiltViewModel     // 导入 hiltViewModel：Hilt 提供 ViewModel

/**
 * 云端 API 配置页 —— 用户自配 OpenAI 兼容服务（R1）。
 * 仅当引擎切换到「云端 AI」时，从侧边菜单进入本页。
 */
@OptIn(ExperimentalMaterial3Api::class)                  // 声明使用实验性 API（TopAppBar）
@Composable                                               // 可组合 UI 函数
fun CloudConfigScreen(                                    // 云端配置页
    viewModel: SettingsViewModel = hiltViewModel(),       // Hilt 提供 ViewModel
) {
    val cloud by viewModel.cloud.collectAsState()         // 订阅云端配置
    val message by viewModel.message.collectAsState()     // 订阅提示消息

    var baseUrl by remember(cloud) { mutableStateOf(cloud.baseUrl) }  // 地址输入（cloud 变化重置）
    var apiKey by remember(cloud) { mutableStateOf(cloud.apiKey) }    // 密钥输入
    var model by remember(cloud) { mutableStateOf(cloud.model) }      // 模型名输入

    Scaffold(                                             // 页面脚手架
        topBar = { TopAppBar(title = { Text("云端 API 配置") }) },  // 顶部栏
    ) { innerPadding ->                                   // 内容区
        Column(                                           // 纵向布局
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),  // 铺满 + 内边距
        ) {
            OutlinedTextField(                            // 地址输入框
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text("API 地址 baseUrl") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))                 // 间距
            OutlinedTextField(                            // 密钥输入框
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))                 // 间距
            OutlinedTextField(                            // 模型名输入框
                value = model,
                onValueChange = { model = it },
                label = { Text("模型名 model") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(16.dp))                // 间距
            Button(                                       // 保存按钮
                onClick = { viewModel.saveCloudConfig(baseUrl, apiKey, model) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("保存云端配置") }                    // 按钮文字
            Spacer(Modifier.height(16.dp))                // 间距
            if (message.isNotBlank()) {                   // 有提示消息
                Text(                                     // 提示文本
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
