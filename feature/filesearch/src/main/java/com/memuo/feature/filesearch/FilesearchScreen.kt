package com.memuo.feature.filesearch                       // 声明包名：文件检索业务模块

import android.content.ClipData                            // 导入 ClipData：剪贴板数据
import android.content.ClipboardManager                    // 导入 ClipboardManager：系统剪贴板
import android.content.Context                             // 导入 Context：系统服务
import androidx.compose.foundation.layout.Column           // 导入 Column：纵向布局
import androidx.compose.foundation.layout.fillMaxWidth     // 导入 fillMaxWidth：占满宽度
import androidx.compose.foundation.layout.padding          // 导入 padding：外边距
import androidx.compose.foundation.lazy.LazyColumn         // 导入 LazyColumn：结果列表
import androidx.compose.foundation.lazy.items              // 导入 items：列表项
import androidx.compose.material3.Button                  // 导入 Button：按钮
import androidx.compose.material3.Card                     // 导入 Card：结果卡片
import androidx.compose.material3.ExperimentalMaterial3Api // 导入 ExperimentalMaterial3Api：Card onClick 实验 API
import androidx.compose.material3.LinearProgressIndicator  // 导入 LinearProgressIndicator：进度条
import androidx.compose.material3.MaterialTheme           // 导入 MaterialTheme：主题
import androidx.compose.material3.OutlinedButton           // 导入 OutlinedButton：描边按钮
import androidx.compose.material3.OutlinedTextField        // 导入 OutlinedTextField：输入框
import androidx.compose.material3.Text                    // 导入 Text：文本
import androidx.compose.material3.TextButton              // 导入 TextButton：文字按钮
import androidx.compose.runtime.Composable                // 导入 Composable
import androidx.compose.runtime.collectAsState            // 导入 collectAsState
import androidx.compose.runtime.getValue                  // 导入 getValue
import androidx.compose.runtime.mutableStateOf            // 导入 mutableStateOf
import androidx.compose.runtime.remember                  // 导入 remember
import androidx.compose.runtime.setValue                  // 导入 setValue
import androidx.compose.ui.Modifier                       // 导入 Modifier
import androidx.compose.ui.platform.LocalContext           // 导入 LocalContext：Compose 上下文
import androidx.compose.ui.unit.dp                        // 导入 dp
import com.memuo.core.search.progress.SearchProgress      // 导入进度数据
import com.memuo.core.search.privilege.PrivilegeManager   // 导入能力等级
import com.memuo.core.search.service.FileHit              // 导入文件命中

/**
 * 文件检索页 —— M7 完整版：
 *  1) 常驻隐私提示 + 提权能力等级实时显示（L0/L1/L2）；
 *  2) Shizuku 引导：未连接显示安装/启动说明，未授权显示「授权」按钮；
 *  3) 索引：显式点击开始 → 实时进度 + 停止；
 *  4) 查询：关键词检索已建索引 → 结果列表，点击复制真实路径。
 */
@OptIn(ExperimentalMaterial3Api::class)                  // Card onClick 是实验 API
@Composable                                               // 可组合 UI 函数
fun FilesearchScreen(viewModel: FilesearchViewModel) {    // 文件检索页
    val ui by viewModel.ui.collectAsState()               // 索引状态
    val level by viewModel.level.collectAsState()         // 能力等级
    val authorized by viewModel.authorized.collectAsState()  // 授权状态
    val results by viewModel.results.collectAsState()     // 查询结果
    val context = LocalContext.current                    // 上下文（剪贴板）
    var query by remember { mutableStateOf("") }          // 查询关键词

    Column(modifier = Modifier.padding(16.dp)) {          // 纵向布局
        Text("文件检索", style = MaterialTheme.typography.titleLarge)  // 标题
        Text(                                            // 常驻隐私提示
            "隐私约束：索引仅在您点击「开始搜索」后运行；后台自动索引默认关闭；只存文件名/路径元数据，不存内容。",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp),
        )

        LevelBadge(level, authorized)                     // 能力等级徽章

        // ---- Shizuku 引导（L0 且未连接时）----
        if (level == PrivilegeManager.Level.NONE) {       // 未连接 Shizuku
            Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {  // 引导卡片
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("当前为 L0（仅应用私有目录）", style = MaterialTheme.typography.titleMedium)  // 说明
                    Text(                                // 提权引导
                        "提权后可搜索整个用户目录：\n" +
                            "1. 安装 Shizuku 应用；\n" +
                            "2. 用电脑执行 adb shell sh /storage/emulated/0/Android/data/moe.shizuku.privileged.api/start.sh\n" +
                            "   （或开启无线调试后用 Shizuku 内的无线启动）；\n" +
                            "3. 回到本页，Shizuku 服务上线后点「授权」。",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    OutlinedButton(                       // 授权按钮
                        onClick = viewModel::requestPermission,
                        modifier = Modifier.padding(top = 8.dp),
                    ) { Text("请求 Shizuku 授权") }        // 按钮文字
                }
            }
        }

        // ---- Shizuku 已连接但未授权 ----
        if (level != PrivilegeManager.Level.NONE && !authorized) {  // 已连接未授权
            OutlinedButton(                               // 授权按钮
                onClick = viewModel::requestPermission,
                modifier = Modifier.padding(top = 12.dp),
            ) { Text("Shizuku 已连接，点此授权") }          // 按钮文字
        }

        // ---- 索引区（始终可用：L0 也可索引应用私有目录）----
        when (val s = ui) {                               // 按索引状态渲染
            FilesearchUiState.Idle -> Button(             // 空闲：开始按钮
                onClick = viewModel::startIndex,
                modifier = Modifier.padding(top = 16.dp),
            ) { Text("开始搜索并建立索引") }               // 按钮文字

            is FilesearchUiState.Running -> SearchProgressBar(  // 运行中：进度条
                progress = s.progress,
                onCancel = viewModel::cancel,
            )

            is FilesearchUiState.Done -> Text(            // 完成：结果统计
                "完成：索引 ${s.result.indexedFiles} 个文件，跳过 ${s.result.skippedFiles} 个（${s.result.durationMs}ms）",
                modifier = Modifier.padding(top = 16.dp),
            )

            is FilesearchUiState.Error -> Text(           // 出错
                "错误：${s.message}",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp),
            )
        }

        // ---- 查询区 ----
        OutlinedTextField(                                // 查询输入框
            value = query,
            onValueChange = {                            // 输入变化
                query = it                               // 更新
                viewModel.query(it)                      // 实时查询
            },
            label = { Text("搜索已索引文件（文件名关键词）") },  // 标签
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )
        LazyColumn(modifier = Modifier.padding(top = 8.dp)) {  // 结果列表
            items(results, key = { it.path }) { hit ->    // 遍历命中
                FileHitCard(hit) {                        // 结果卡片
                    copyPath(context, hit.path)           // 点击复制路径
                }
            }
        }
    }
}

/** 能力等级徽章 —— 实时显示三档能力与说明。 */
@Composable                                               // 可组合 UI 函数
private fun LevelBadge(level: PrivilegeManager.Level, authorized: Boolean) {  // 等级徽章
    val (label, desc) = when (level) {                    // 按等级取文案
        PrivilegeManager.Level.NONE -> "L0 无权限" to "仅应用私有目录"
        PrivilegeManager.Level.SHIZUKU_ADB -> "L1 Shizuku-adb" to (if (authorized) "已授权 · 可搜用户目录" else "已连接 · 待授权")
        PrivilegeManager.Level.SHIZUKU_ROOT -> "L2 Shizuku-root" to (if (authorized) "已授权 · 全盘检索" else "已连接 · 待授权")
    }
    val color = when (level) {                            // 等级配色
        PrivilegeManager.Level.NONE -> MaterialTheme.colorScheme.outline        // 灰
        PrivilegeManager.Level.SHIZUKU_ADB -> MaterialTheme.colorScheme.tertiary  // 黄绿
        PrivilegeManager.Level.SHIZUKU_ROOT -> MaterialTheme.colorScheme.primary  // 主色
    }
    Column(modifier = Modifier.padding(top = 12.dp)) {    // 徽章容器
        Text(label, style = MaterialTheme.typography.titleMedium, color = color)  // 等级名
        Text(desc, style = MaterialTheme.typography.bodySmall)  // 说明
    }
}

/** 结果卡片 —— 显示文件元数据，点击复制真实路径。 */
@Composable                                               // 可组合 UI 函数
private fun FileHitCard(hit: FileHit, onCopy: () -> Unit) {  // 结果卡片
    Card(                                                 // 卡片容器
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        onClick = onCopy,                                 // 点击复制
    ) {
        Column(modifier = Modifier.padding(12.dp)) {      // 卡片内容
            Text(hit.name, style = MaterialTheme.typography.bodyLarge)  // 文件名
            Text(                                         // 路径（可换行）
                hit.path,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Text(                                         // 大小 + 时间
                "%.1f KB".format(hit.sizeBytes / 1024.0),  // 千字节
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/** 复制路径到剪贴板并给出提示（后续可扩展为跳转文件管理器）。 */
private fun copyPath(context: Context, path: String) {    // 复制路径
    val cm = context.getSystemService(ClipboardManager::class.java)  // 剪贴板服务
    cm.setPrimaryClip(ClipData.newPlainText("file_path", path))  // 写入
}

/** 进度条组件：与 SearchProgress 契约一一对应（ADR-002 的 UI 落地）。 */
@Composable                                               // 可组合 UI 函数
fun SearchProgressBar(progress: SearchProgress, onCancel: () -> Unit) {  // 进度条组件
    Column(modifier = Modifier.padding(top = 16.dp)) {    // 容器
        Text(                                            // 阶段 + 当前路径
            "${progress.phase.label}" +
                (progress.currentPath?.let { " · $it" } ?: "") +
                (progress.message.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
            style = MaterialTheme.typography.bodyMedium,
        )
        LinearProgressIndicator(                          // 进度条
            progress = { progress.percent },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        Text(                                            // 数量统计
            if (progress.totalItems > 0)
                "${progress.scannedItems} / ${progress.totalItems}（${(progress.percent * 100).toInt()}%）"
            else
                "已扫描 ${progress.scannedItems} 个文件",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp),
        )
        TextButton(onClick = onCancel) { Text("停止") }   // 停止按钮
    }
}
