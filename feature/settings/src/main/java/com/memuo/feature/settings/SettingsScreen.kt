package com.memuo.feature.settings                         // 声明包名：设置业务模块

import androidx.compose.foundation.layout.Column          // 导入 Column：纵向布局
import androidx.compose.foundation.layout.Row             // 导入 Row：横向布局
import androidx.compose.foundation.layout.Spacer          // 导入 Spacer：占位
import androidx.compose.foundation.layout.fillMaxSize     // 导入 fillMaxSize：铺满
import androidx.compose.foundation.layout.fillMaxWidth    // 导入 fillMaxWidth：占满宽度
import androidx.compose.foundation.layout.height          // 导入 height：高度
import androidx.compose.foundation.layout.padding          // 导入 padding：内边距
import androidx.compose.foundation.lazy.LazyColumn         // 导入 LazyColumn：懒加载列表
import androidx.compose.material3.Button                  // 导入 Button：按钮
import androidx.compose.material3.ExperimentalMaterial3Api // 导入 ExperimentalMaterial3Api：实验性 API
import androidx.compose.material3.MaterialTheme           // 导入 MaterialTheme：主题
import androidx.compose.material3.OutlinedTextField        // 导入 OutlinedTextField：输入框
import androidx.compose.material3.RadioButton              // 导入 RadioButton：单选按钮
import androidx.compose.material3.Scaffold                // 导入 Scaffold：页面脚手架
import androidx.compose.material3.Text                    // 导入 Text：文本
import androidx.compose.material3.TextButton               // 导入 TextButton：文字按钮
import androidx.compose.material3.TopAppBar               // 导入 TopAppBar：顶部栏
import androidx.compose.runtime.Composable                // 导入 Composable：可组合函数注解
import androidx.compose.runtime.collectAsState            // 导入 collectAsState：状态流→Compose 状态
import androidx.compose.runtime.getValue                  // 导入 getValue：by 委托
import androidx.compose.runtime.mutableStateOf            // 导入 mutableStateOf：可组合状态
import androidx.compose.runtime.remember                   // 导入 remember：记住状态
import androidx.compose.runtime.setValue                  // 导入 setValue：by 委托写
import androidx.compose.ui.Alignment                      // 导入 Alignment：对齐
import androidx.compose.ui.Modifier                       // 导入 Modifier：修饰
import androidx.compose.ui.unit.dp                        // 导入 dp：尺寸单位
import androidx.hilt.navigation.compose.hiltViewModel     // 导入 hiltViewModel：Hilt 提供 ViewModel
import com.memuo.core.db.entity.EngineType                 // 导入引擎类型枚举

/**
 * 设置页 —— 对话引擎切换、本地模型导入、云端 API 配置（M-010）。
 */
@OptIn(ExperimentalMaterial3Api::class)                  // 声明使用实验性 API（TopAppBar）
@Composable                                               // 可组合 UI 函数
fun SettingsScreen(                                       // 设置页
    onBack: () -> Unit,                                   // 返回回调
    viewModel: SettingsViewModel = hiltViewModel(),       // Hilt 提供 ViewModel
) {
    val engineType by viewModel.engineType.collectAsState()  // 订阅引擎类型
    val hasLocalModel by viewModel.hasLocalModel.collectAsState()  // 订阅模型状态
    val message by viewModel.message.collectAsState()     // 订阅提示消息
    val cloud by viewModel.cloud.collectAsState()         // 订阅云端配置

    // 本地输入状态（云端配置回显；cloud 变化时重置）
    var baseUrl by remember(cloud) { mutableStateOf(cloud.baseUrl) }  // 地址输入
    var apiKey by remember(cloud) { mutableStateOf(cloud.apiKey) }    // 密钥输入
    var model by remember(cloud) { mutableStateOf(cloud.model) }      // 模型名输入
    var importPath by remember { mutableStateOf("") }      // 导入路径输入

    Scaffold(                                             // 页面脚手架
        topBar = {                                       // 顶部栏
            TopAppBar(                                   // 顶部栏组件
                title = { Text("设置") },                 // 标题
                navigationIcon = {                       // 返回按钮
                    TextButton(onClick = onBack) { Text("返回") }  // 点击返回
                },
            )
        },
    ) { innerPadding ->                                   // 内容区
        LazyColumn(                                       // 懒加载列表（内容可滚动）
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),  // 铺满 + 内边距
        ) {
            // ---- 对话引擎 ----
            item {                                       // 引擎标题
                Text("对话引擎", style = MaterialTheme.typography.titleMedium)  // 小节标题
                Spacer(Modifier.height(8.dp))            // 间距
            }
            item {                                       // 云端选项
                Row(verticalAlignment = Alignment.CenterVertically) {  // 横向居中
                    RadioButton(                          // 单选按钮
                        selected = engineType == EngineType.CLOUD,  // 选中态
                        onClick = { viewModel.switchEngine(EngineType.CLOUD) },  // 切云端
                    )
                    Text("云端（自配 API）")             // 标签
                }
            }
            item {                                       // 本地选项
                Row(verticalAlignment = Alignment.CenterVertically) {  // 横向居中
                    RadioButton(                          // 单选按钮
                        selected = engineType == EngineType.LOCAL,  // 选中态
                        onClick = { viewModel.switchEngine(EngineType.LOCAL) },  // 切本地
                    )
                    Text("本地（MNN 离线）")             // 标签
                }
            }
            item {                                       // 模型状态
                Spacer(Modifier.height(8.dp))            // 间距
                Text(                                    // 状态行
                    "本地模型状态：${if (hasLocalModel) "✅ 已就绪" else "❌ 未就绪"}",
                    style = MaterialTheme.typography.bodyMedium,  // 正文样式
                )
                Spacer(Modifier.height(16.dp))           // 间距
            }

            // ---- 模型导入 ----
            item {                                       // 导入标题
                Text("导入本地模型", style = MaterialTheme.typography.titleMedium)  // 小节标题
                Spacer(Modifier.height(8.dp))            // 间距
            }
            item {                                       // 路径输入框
                OutlinedTextField(                        // 输入框
                    value = importPath,                   // 值
                    onValueChange = { importPath = it },  // 更新
                    label = { Text("模型目录绝对路径（含 config.json）") },  // 标签
                    modifier = Modifier.fillMaxWidth(),   // 占满宽度
                    singleLine = true,                    // 单行
                )
                Spacer(Modifier.height(8.dp))            // 间距
            }
            item {                                       // 导入按钮
                Button(                                  // 按钮
                    onClick = { viewModel.importModel(importPath) },  // 触发导入
                    modifier = Modifier.fillMaxWidth(),   // 占满宽度
                ) { Text("导入模型到应用") }              // 按钮文字
                Spacer(Modifier.height(16.dp))           // 间距
            }

            // ---- 云端配置 ----
            item {                                       // 云端配置标题
                Text("云端 API 配置", style = MaterialTheme.typography.titleMedium)  // 小节标题
                Spacer(Modifier.height(8.dp))            // 间距
            }
            item {                                       // baseUrl 输入
                OutlinedTextField(                        // 输入框
                    value = baseUrl,                      // 值
                    onValueChange = { baseUrl = it },     // 更新
                    label = { Text("API 地址 baseUrl") }, // 标签
                    modifier = Modifier.fillMaxWidth(),   // 占满宽度
                    singleLine = true,                    // 单行
                )
                Spacer(Modifier.height(8.dp))            // 间距
            }
            item {                                       // apiKey 输入
                OutlinedTextField(                        // 输入框
                    value = apiKey,                       // 值
                    onValueChange = { apiKey = it },      // 更新
                    label = { Text("API Key") },          // 标签
                    modifier = Modifier.fillMaxWidth(),   // 占满宽度
                    singleLine = true,                    // 单行
                )
                Spacer(Modifier.height(8.dp))            // 间距
            }
            item {                                       // model 输入
                OutlinedTextField(                        // 输入框
                    value = model,                        // 值
                    onValueChange = { model = it },       // 更新
                    label = { Text("模型名 model") },     // 标签
                    modifier = Modifier.fillMaxWidth(),   // 占满宽度
                    singleLine = true,                    // 单行
                )
                Spacer(Modifier.height(8.dp))            // 间距
            }
            item {                                       // 保存按钮
                Button(                                  // 按钮
                    onClick = { viewModel.saveCloudConfig(baseUrl, apiKey, model) },  // 触发保存
                    modifier = Modifier.fillMaxWidth(),   // 占满宽度
                ) { Text("保存云端配置") }                // 按钮文字
                Spacer(Modifier.height(16.dp))           // 间距
            }

            // ---- 消息提示 ----
            item {                                       // 消息
                if (message.isNotBlank()) {               // 有消息则显示
                    Text(                                // 提示文本
                        message,
                        style = MaterialTheme.typography.bodyMedium,  // 正文样式
                        color = MaterialTheme.colorScheme.primary,     // 主题色
                    )
                    Spacer(Modifier.height(16.dp))       // 间距
                }
            }
        }
    }
}
