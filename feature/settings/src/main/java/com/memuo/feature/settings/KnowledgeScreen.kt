package com.memuo.feature.settings                         // 声明包名：设置业务模块

import android.content.Context                            // 导入 Context
import android.net.Uri                                    // 导入 Uri
import androidx.compose.foundation.layout.Column          // 导入 Column
import androidx.compose.foundation.layout.PaddingValues   // 导入 PaddingValues
import androidx.compose.foundation.layout.fillMaxSize     // 导入 fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth    // 导入 fillMaxWidth
import androidx.compose.foundation.layout.padding          // 导入 padding
import androidx.compose.foundation.lazy.LazyColumn         // 导入 LazyColumn
import androidx.compose.foundation.lazy.items              // 导入 items
import androidx.compose.material.icons.Icons               // 导入 Icons
import androidx.compose.material.icons.filled.CreateNewFolder  // 导入 CreateNewFolder：文件夹图标
import androidx.compose.material.icons.filled.NoteAdd     // 导入 NoteAdd：单文件图标
import androidx.compose.material3.Card                    // 导入 Card
import androidx.compose.material3.ExperimentalMaterial3Api // 导入 ExperimentalMaterial3Api
import androidx.compose.material3.Icon                    // 导入 Icon
import androidx.compose.material3.MaterialTheme           // 导入 MaterialTheme
import androidx.compose.material3.Scaffold                // 导入 Scaffold
import androidx.compose.material3.Text                    // 导入 Text
import androidx.compose.material3.TextButton              // 导入 TextButton
import androidx.compose.material3.TopAppBar               // 导入 TopAppBar
import androidx.compose.runtime.Composable                // 导入 Composable
import androidx.compose.runtime.collectAsState            // 导入 collectAsState
import androidx.compose.runtime.getValue                  // 导入 getValue
import androidx.compose.ui.Modifier                       // 导入 Modifier
import androidx.compose.ui.unit.dp                        // 导入 dp
import androidx.documentfile.provider.DocumentFile         // 导入 DocumentFile：SAF 目录
import androidx.hilt.navigation.compose.hiltViewModel     // 导入 hiltViewModel
import androidx.lifecycle.ViewModel                       // 导入 ViewModel
import androidx.lifecycle.viewModelScope                  // 导入 viewModelScope
import com.memuo.core.db.dao.KbDao                        // 导入 KbDao
import com.memuo.core.db.entity.IngestStatus              // 导入入库状态
import com.memuo.core.db.entity.KbDocument                // 导入知识库文档
import com.memuo.core.ingest.KnowledgeRepository          // 导入知识库仓库
import dagger.hilt.android.lifecycle.HiltViewModel        // 导入 HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext  // 导入 ApplicationContext
import kotlinx.coroutines.Dispatchers                      // 导入 Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow            // 导入 MutableStateFlow
import kotlinx.coroutines.flow.StateFlow                  // 导入 StateFlow
import kotlinx.coroutines.flow.asStateFlow                // 导入 asStateFlow
import kotlinx.coroutines.launch                           // 导入 launch
import kotlinx.coroutines.withContext                      // 导入 withContext
import java.io.File                                        // 导入 File
import javax.inject.Inject                                // 导入 Inject

/** 默认知识库 ID（单知识库 MVP）。 */
const val DEFAULT_KB = "default"                          // 默认知识库标识

/**
 * 知识库页 —— 投喂文档到 AI 知识库（M-013 补全 RAG 投喂入口）。
 * 支持：SAF 文件夹整体投喂、单文件投喂、已入库文档列表。
 */
@OptIn(ExperimentalMaterial3Api::class)                  // 实验性 API
@Composable                                               // 可组合 UI 函数
fun KnowledgeScreen(                                      // 知识库页
    onPickFolder: () -> Unit,                             // 选文件夹回调（由上层触发 SAF 选择器）
    onPickFile: () -> Unit,                               // 选单文件回调
    viewModel: KnowledgeViewModel = hiltViewModel(),      // Hilt 提供 ViewModel
) {
    val docs by viewModel.docs.collectAsState()           // 订阅文档列表
    val message by viewModel.message.collectAsState()     // 订阅提示

    Scaffold(                                             // 脚手架
        topBar = {                                       // 顶部栏
            TopAppBar(                                   // 顶部栏组件
                title = { Text("知识库") },               // 标题
                actions = {                              // 右侧操作
                    TextButton(onClick = onPickFile) {    // 单文件投喂
                        Icon(Icons.Filled.NoteAdd, contentDescription = "投喂文件")
                        Text(" 文件")
                    }
                    TextButton(onClick = onPickFolder) {  // 文件夹投喂
                        Icon(Icons.Filled.CreateNewFolder, contentDescription = "投喂文件夹")
                        Text(" 文件夹")
                    }
                },
            )
        },
    ) { innerPadding ->                                   // 内容区
        Column(                                           // 纵向布局
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            if (message.isNotBlank()) {                   // 提示消息
                Text(                                     // 提示文本
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp),
                )
            }
            if (docs.isEmpty()) {                         // 空态
                Text(                                     // 空提示
                    "知识库为空。点右上角「文件夹」投喂整个目录，或「文件」投喂单个文档（支持 txt/md/pdf/docx/压缩包/图片）。",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(24.dp),
                )
            } else {                                      // 文档列表
                LazyColumn(                               // 懒加载列表
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    items(docs, key = { it.docId }) { doc ->  // 遍历文档
                        DocCard(doc)                      // 文档卡片
                    }
                }
            }
        }
    }
}

/** 文档卡片：文件名 + 状态 + 分块数。 */
@Composable                                               // 可组合 UI 函数
private fun DocCard(doc: KbDocument) {                    // 文档卡片
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {  // 卡片
        Column(modifier = Modifier.padding(16.dp)) {      // 内部
            Text(doc.fileName, style = MaterialTheme.typography.titleSmall)  // 文件名
            Text(                                         // 状态 + 分块数
                "${statusLabel(doc.status)} · ${doc.chunkCount} 块",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** 入库状态 → 中文标签。 */
private fun statusLabel(status: IngestStatus): String = when (status) {  // 状态标签
    IngestStatus.INDEXED -> "已索引"                     // 已入库
    IngestStatus.PARSING -> "解析中"                     // 解析中
    IngestStatus.PENDING -> "待处理"                     // 待处理
    IngestStatus.FAILED -> "失败"                        // 失败
}

/** 知识库 ViewModel —— 文档列表 + 投喂。 */
@HiltViewModel                                           // 注解：由 Hilt 创建
class KnowledgeViewModel @Inject constructor(            // 构造函数注入
    @ApplicationContext private val context: Context,    // 应用上下文
    private val kbDao: KbDao,                            // 知识库 DAO
    private val repo: KnowledgeRepository,               // 知识库仓库
) : ViewModel() {                                        // 继承 ViewModel

    private val _docs = MutableStateFlow<List<KbDocument>>(emptyList())  // 文档列表
    val docs: StateFlow<List<KbDocument>> = _docs.asStateFlow()  // 只读暴露

    private val _message = MutableStateFlow("")          // 提示消息
    val message: StateFlow<String> = _message.asStateFlow()  // 只读暴露

    init {                                                // 初始化
        viewModelScope.launch {                          // 观察文档列表
            kbDao.observeDocuments(DEFAULT_KB).collect { _docs.value = it }
        }
    }

    /** 从 SAF 文件夹投喂：遍历文件复制到缓存后逐个入库。 */
    fun ingestFolder(uri: Uri) {                          // 文件夹投喂
        viewModelScope.launch {                          // 协程中执行
            _message.value = "正在投喂文件夹…"            // 提示
            val root = DocumentFile.fromTreeUri(context, uri)  // 取目录根
            if (root == null) {                           // 无法访问
                _message.value = "无法访问该文件夹"
                return@launch
            }
            val count = withContext(Dispatchers.IO) {     // IO 线程
                var n = 0                                 // 成功计数
                root.listFiles().forEach { doc ->         // 遍历子项
                    if (!doc.isFile) return@forEach       // 只处理文件
                    val tmp = copyToCache(doc) ?: return@forEach  // 复制到缓存
                    runCatching { repo.ingestFile(tmp, DEFAULT_KB) }.onSuccess { n++ }  // 入库
                    tmp.delete()                          // 清理临时文件
                }
                n
            }
            _message.value = "投喂完成：$count 个文件"    // 完成提示
        }
    }

    /** 从 SAF 单文件投喂。 */
    fun ingestFile(uri: Uri) {                            // 单文件投喂
        viewModelScope.launch {                          // 协程中执行
            _message.value = "正在投喂文件…"              // 提示
            val doc = DocumentFile.fromSingleUri(context, uri)  // 取文件
            if (doc == null) {                            // 无法访问
                _message.value = "无法访问该文件"
                return@launch
            }
            val ok = withContext(Dispatchers.IO) {        // IO 线程
                val tmp = copyToCache(doc) ?: return@withContext false  // 复制
                val r = runCatching { repo.ingestFile(tmp, DEFAULT_KB) }.isSuccess  // 入库
                tmp.delete()                              // 清理
                r
            }
            _message.value = if (ok) "投喂完成" else "投喂失败（格式不支持或解析出错）"  // 提示
        }
    }

    /** 把 SAF 文件复制到缓存目录，返回临时文件（失败返回 null）。 */
    private fun copyToCache(doc: DocumentFile): File? {   // 复制到缓存
        return runCatching {                              // 捕获异常
            val dir = File(context.cacheDir, "kb").apply { mkdirs() }  // 缓存目录
            val target = File(dir, doc.name ?: "file_${System.currentTimeMillis()}")  // 目标文件
            context.contentResolver.openInputStream(doc.uri)?.use { input ->  // 读输入流
                target.outputStream().use { output -> input.copyTo(output) }  // 写本地
            }
            target
        }.getOrNull()                                     // 失败返回 null
    }
}
