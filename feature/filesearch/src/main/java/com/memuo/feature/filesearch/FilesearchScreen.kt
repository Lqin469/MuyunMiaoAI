package com.memuo.feature.filesearch                       // 声明包名：文件检索业务模块

import androidx.compose.foundation.layout.Column           // 导入 Column：纵向排列的布局容器
import androidx.compose.foundation.layout.fillMaxWidth     // 导入 fillMaxWidth：让组件占满横向宽度
import androidx.compose.foundation.layout.padding          // 导入 padding：设置组件外边距
import androidx.compose.material3.Button                  // 导入 Button：可点击按钮
import androidx.compose.material3.LinearProgressIndicator // 导入 LinearProgressIndicator：水平进度条
import androidx.compose.material3.MaterialTheme           // 导入 MaterialTheme：主题（取颜色/字体）
import androidx.compose.material3.Text                    // 导入 Text：文本组件
import androidx.compose.material3.TextButton              // 导入 TextButton：文字按钮（停止用）
import androidx.compose.runtime.Composable                // 导入 Composable：可组合函数注解
import androidx.compose.runtime.collectAsState            // 导入 collectAsState：把状态流转为 Compose 状态
import androidx.compose.runtime.getValue                  // 导入 getValue：支持 by 委托（val x by state）
import androidx.compose.ui.Modifier                       // 导入 Modifier：链式修饰
import androidx.compose.ui.unit.dp                        // 导入 dp：尺寸单位（16.dp 等）
import com.memuo.core.search.progress.SearchProgress      // 导入进度数据（进度条组件入参）

/**
 * 文件检索页 —— 用户强制约束的可视化承诺：
 *  - 未开始时仅有「开始搜索」按钮，绝不静默扫描；
 *  - 运行中显示实时进度条（百分比 / 已扫描数 / 当前路径）+ 「停止」按钮；
 *  - 常驻隐私提示文案。
 */
@Composable                                               // 注解：可组合 UI 函数
fun FilesearchScreen(viewModel: FilesearchViewModel) {    // 文件检索页面（接收 ViewModel 作为参数）
    val ui by viewModel.ui.collectAsState()               // 订阅状态流：状态变化时自动重组界面

    Column(modifier = Modifier.padding(16.dp)) {          // 纵向布局，四周留 16dp 边距
        Text("文件检索", style = MaterialTheme.typography.titleLarge)  // 页面标题
        Text(                                            // 隐私提示文案（常驻显示）
            "隐私约束：索引仅在您点击「开始搜索」后运行；后台自动索引默认关闭（可在设置中显式开启）。",
            style = MaterialTheme.typography.bodySmall,   // 用小号正文样式（弱化视觉）
            modifier = Modifier.padding(top = 4.dp),      // 与标题留 4dp 间距
        )

        when (val s = ui) {                               // 按当前状态分支渲染（四种状态）
            FilesearchUiState.Idle -> Button(             // 状态1 空闲：显示"开始"按钮
                onClick = viewModel::startIndex,          // 点击后调用 ViewModel.startIndex（唯一触发入口）
                modifier = Modifier.padding(top = 16.dp), // 顶部留 16dp
            ) { Text("开始搜索并建立索引") }               // 按钮文字

            is FilesearchUiState.Running -> SearchProgressBar(  // 状态2 运行中：显示进度条组件
                progress = s.progress,                    // 传入实时进度数据
                onCancel = viewModel::cancel,             // 传入停止回调（点击"停止"触发）
            )

            is FilesearchUiState.Done -> Text(            // 状态3 完成：显示结果统计
                "完成：索引 ${s.result.indexedFiles} 个文件，跳过 ${s.result.skippedFiles} 个（${s.result.durationMs}ms）",
                modifier = Modifier.padding(top = 16.dp), // 顶部留 16dp
            )

            is FilesearchUiState.Error -> Text(           // 状态4 出错：显示错误信息
                "错误：${s.message}",                      // 错误文案
                color = MaterialTheme.colorScheme.error,  // 用主题的错误色（红色）
                modifier = Modifier.padding(top = 16.dp), // 顶部留 16dp
            )
        }
    }
}

/** 进度条组件：与 SearchProgress 契约一一对应（ADR-002 的 UI 落地）。 */
@Composable                                               // 注解：可组合 UI 函数
fun SearchProgressBar(progress: SearchProgress, onCancel: () -> Unit) {  // 进度条组件（入参：进度数据 + 停止回调）
    Column(modifier = Modifier.padding(top = 16.dp)) {    // 纵向布局，顶部留 16dp
        Text(                                            // 第一行：当前阶段 + 当前文件路径 + 附加消息
            "${progress.phase.label}" +                   // 阶段文案（如"扫描目录"）
                (progress.currentPath?.let { " · $it" } ?: "") +        // 如果有当前路径就拼上（"·" 分隔）
                (progress.message.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),  // 如果有附加消息也拼上
            style = MaterialTheme.typography.bodyMedium,  // 用中等正文样式
        )
        LinearProgressIndicator(                          // 水平进度条组件
            progress = { progress.percent },              // 进度值（0f~1f），用 lambda 形式支持动画
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),  // 占满宽度，顶部留 8dp
        )
        Text(                                            // 第三行：数量与百分比（如 1234 / 5678（21%））
            "${progress.scannedItems} / ${progress.totalItems}（${(progress.percent * 100).toInt()}%）",
            style = MaterialTheme.typography.bodySmall,   // 用小号正文样式
            modifier = Modifier.padding(top = 4.dp),      // 顶部留 4dp
        )
        TextButton(onClick = onCancel) { Text("停止") }   // 停止按钮：点击调用取消回调
    }
}
