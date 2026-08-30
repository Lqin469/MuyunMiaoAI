package com.memuo.feature.settings                         // 声明包名：设置业务模块

import android.content.Context                            // 导入 Context：应用上下文
import android.net.Uri                                    // 导入 Uri：SAF 标识
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
import androidx.compose.foundation.shape.RoundedCornerShape  // 导入 RoundedCornerShape：圆角形状
import androidx.compose.material3.Icon                    // 导入 Icon：图标
import androidx.compose.material3.MaterialTheme           // 导入 MaterialTheme：主题
import androidx.compose.material3.Text                    // 导入 Text：文本
import androidx.compose.runtime.Composable                // 导入 Composable：可组合函数注解
import androidx.compose.runtime.collectAsState            // 导入 collectAsState：状态流→状态
import androidx.compose.runtime.getValue                  // 导入 getValue：by 委托
import androidx.compose.ui.Alignment                      // 导入 Alignment：对齐
import androidx.compose.ui.Modifier                       // 导入 Modifier：修饰
import androidx.compose.ui.draw.clip                      // 导入 clip：裁剪
import androidx.compose.ui.draw.shadow                    // 导入 shadow：投影
import androidx.compose.ui.graphics.Color                 // 导入 Color：颜色
import androidx.compose.ui.text.font.FontWeight           // 导入 FontWeight：字重
import androidx.compose.ui.text.style.TextOverflow        // 导入 TextOverflow：溢出省略
import androidx.compose.ui.unit.dp                        // 导入 dp：尺寸单位
import androidx.documentfile.provider.DocumentFile         // 导入 DocumentFile：SAF 目录
import androidx.hilt.navigation.compose.hiltViewModel     // 导入 hiltViewModel：Hilt 提供 ViewModel
import androidx.lifecycle.SavedStateHandle                // 导入 SavedStateHandle：读取导航参数
import androidx.lifecycle.ViewModel                       // 导入 ViewModel：UI 数据持有者
import androidx.lifecycle.viewModelScope                  // 导入 viewModelScope：协程作用域
import com.memuo.core.db.dao.KbDao                        // 导入知识库 DAO
import com.memuo.core.db.entity.IngestStatus              // 导入入库状态
import com.memuo.core.db.entity.KbDocument                 // 导入文档实体
import com.memuo.core.ingest.KnowledgeRepository          // 导入知识库仓库
import com.memuo.core.ui.AppIcons                          // 导入应用图标集
import com.memuo.core.ui.components.EmptyState            // 导入空态组件
import com.memuo.core.ui.components.LocalToast            // 导入 Toast 状态
import com.memuo.core.ui.components.StatusPill            // 导入状态徽章
import com.memuo.core.ui.components.StatusTone            // 导入状态色调
import com.memuo.core.ui.components.SubBody               // 导入子页内容容器
import com.memuo.core.ui.components.SubHeader             // 导入子页顶栏
import com.memuo.core.ui.theme.MuyunAccentLight           // 导入浅灰底
import com.memuo.core.ui.theme.MuyunBrand                 // 导入品牌色
import com.memuo.core.ui.theme.MuyunBrandSoft             // 导入品牌浅底
import com.memuo.core.ui.theme.MuyunCard                  // 导入卡片白
import com.memuo.core.ui.theme.MuyunPurple                // 导入品牌紫
import com.memuo.core.ui.theme.MuyunText                  // 导入主文字色
import com.memuo.core.ui.theme.MuyunText2                 // 导入次级文字色
import com.memuo.core.ui.theme.MuyunText3                 // 导入三级文字色
import dagger.hilt.android.lifecycle.HiltViewModel        // 导入 HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext  // 导入 ApplicationContext
import kotlinx.coroutines.Dispatchers                      // 导入 Dispatchers：调度器
import kotlinx.coroutines.flow.MutableStateFlow            // 导入可变状态流
import kotlinx.coroutines.flow.StateFlow                  // 导入只读状态流
import kotlinx.coroutines.flow.asStateFlow                // 导入 asStateFlow
import kotlinx.coroutines.launch                           // 导入 launch：协程
import kotlinx.coroutines.withContext                      // 导入 withContext：切线程
import java.io.File                                        // 导入 File：临时文件
import javax.inject.Inject                                // 导入 Inject：构造函数注入

/**
 * 知识库详情页 —— 展示某知识库的已投喂文件列表 + 顶栏「添加文件/添加文件夹」。
 * 修复：列表页点击「本地的私密库」等文件夹后，正确跳转到这里并展示文件列表。
 */
@Composable                                               // 可组合 UI 函数
fun KnowledgeDetailScreen(                                // 知识库详情页
    onBack: () -> Unit,                                   // 返回回调
    onPickFolder: () -> Unit,                             // 选文件夹回调
    onPickFile: () -> Unit,                               // 选单文件回调
    viewModel: KnowledgeDetailViewModel = hiltViewModel(),  // Hilt 提供 ViewModel
) {
    val docs by viewModel.docs.collectAsState()           // 订阅文档列表
    val message by viewModel.message.collectAsState()     // 订阅提示
    val toast = LocalToast.current                        // 取全局 Toast

    Column(modifier = Modifier.fillMaxSize()) {           // 纵向布局
        SubHeader(                                       // 顶栏
            title = viewModel.folderName,                 // 标题 = 文件夹名
            onBack = onBack,                             // 返回
            actions = {                                  // 右侧投喂按钮组
                Row(                                     // 两个小按钮
                    modifier = Modifier.fillMaxSize(),   // 占满槽位
                    verticalAlignment = Alignment.CenterVertically,  // 垂直居中
                ) {
                    Box(                                 // 单文件投喂
                        modifier = Modifier             // 修饰
                            .size(36.dp)                // 36dp
                            .clip(RoundedCornerShape(10.dp))  // 圆角
                            .clickable { onPickFile() }  // 点击
                            .padding(9.dp),             // 内边距
                    ) {
                        Icon(                            // 文件图标
                            imageVector = AppIcons.File,  // 图标
                            contentDescription = "添加文件",  // 描述
                            tint = MuyunText2,           // 次级灰
                            modifier = Modifier.size(16.dp),  // 16dp
                        )
                    }
                    Box(                                 // 文件夹投喂
                        modifier = Modifier             // 修饰
                            .size(36.dp)                // 36dp
                            .clip(RoundedCornerShape(10.dp))  // 圆角
                            .clickable { onPickFolder() }  // 点击
                            .padding(9.dp),             // 内边距
                    ) {
                        Icon(                            // 文件夹图标
                            imageVector = AppIcons.Folder,  // 图标
                            contentDescription = "添加文件夹",  // 描述
                            tint = MuyunText2,           // 次级灰
                            modifier = Modifier.size(16.dp),  // 16dp
                        )
                    }
                }
            },
        )
        SubBody(modifier = Modifier.fillMaxSize()) {       // 内容容器
            Column(modifier = Modifier.fillMaxSize()) {    // 纵向内容
                // 操作提示（添加文件/文件夹）
                Row(                                      // 提示条
                    modifier = Modifier                 // 修饰
                        .fillMaxWidth()                 // 占满宽度
                        .padding(bottom = 12.dp)        // 下留白
                        .clip(RoundedCornerShape(12.dp))  // 圆角
                        .background(MuyunBrandSoft)     // 品牌浅底
                        .padding(horizontal = 14.dp, vertical = 12.dp),  // 内边距
                    verticalAlignment = Alignment.CenterVertically,  // 垂直居中
                ) {
                    Icon(                                // 信息图标
                        imageVector = AppIcons.Info,     // 图标
                        contentDescription = null,       // 装饰
                        tint = MuyunBrand,               // 品牌色
                        modifier = Modifier.size(16.dp), // 16dp
                    )
                    Text(                                // 提示文字
                        text = "点击右上角图标添加文件 / 文件夹，内容会端侧索引供本地管家引用",  // 文案
                        style = MaterialTheme.typography.labelSmall,  // 小字
                        color = MuyunText2,              // 次级灰
                        modifier = Modifier.padding(start = 8.dp),  // 留白
                    )
                }
                // 投喂进度提示
                if (message.isNotBlank()) {               // 有提示
                    Text(                                // 提示文字
                        text = message,                  // 内容
                        style = MaterialTheme.typography.bodySmall,  // 字号
                        color = MuyunPurple,             // 紫
                        modifier = Modifier.padding(bottom = 10.dp),  // 下留白
                    )
                }
                Text(                                    // 文件计数头
                    text = "已投喂 ${docs.size} 个文件",  // 内容
                    style = MaterialTheme.typography.labelMedium,  // 小字
                    color = MuyunText3,                  // 三级灰
                    modifier = Modifier.padding(bottom = 10.dp),  // 下留白
                )
                if (docs.isEmpty()) {                     // 空态
                    EmptyState(                          // 空态组件
                        icon = AppIcons.File,            // 文件插图
                        text = "还没有文件\n点击右上角添加文件或文件夹",  // 空态文案
                        modifier = Modifier.fillMaxWidth(),  // 占满宽度
                    )
                } else {                                  // 文档列表
                    LazyColumn(                          // 懒加载列表
                        modifier = Modifier.fillMaxSize(),  // 铺满
                    ) {
                        items(docs, key = { it.docId }) { doc ->  // 遍历文档
                            DocCard(doc)                 // 文档卡片
                        }
                    }
                }
            }
        }
    }
}

/** 文档卡片：文件名 + 状态徽章 + 分块数（对应 HTML 已投喂文档列表）。 */
@Composable                                               // 可组合函数
private fun DocCard(doc: KbDocument) {                    // 文档卡片
    Row(                                                  // 横向布局
        modifier = Modifier                              // 修饰
            .fillMaxWidth()                              // 占满宽度
            .padding(bottom = 8.dp)                      // 下留白
            .shadow(1.dp, RoundedCornerShape(12.dp))     // 轻投影
            .clip(RoundedCornerShape(12.dp))             // 圆角 12
            .background(MuyunCard)                       // 白底
            .padding(horizontal = 16.dp, vertical = 14.dp),  // 内边距
        verticalAlignment = Alignment.CenterVertically,   // 垂直居中
    ) {
        Box(                                              // 文件图标底
            modifier = Modifier                          // 修饰
                .size(38.dp)                             // 38dp
                .clip(RoundedCornerShape(10.dp))         // 圆角 10
                .background(MuyunBrandSoft),             // 品牌浅底
            contentAlignment = Alignment.Center,          // 居中
        ) {
            Icon(                                         // 文件图标
                imageVector = AppIcons.FileText,          // 图标
                contentDescription = null,                // 装饰
                tint = MuyunBrand,                        // 品牌色
                modifier = Modifier.size(18.dp),          // 18dp
            )
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {  // 文件名 + 分块数
            Text(                                         // 文件名
                text = doc.fileName,                      // 内容
                style = MaterialTheme.typography.bodyLarge,  // 字号
                fontWeight = FontWeight.Medium,           // 中粗
                color = MuyunText,                        // 主色
                maxLines = 1,                             // 单行
                overflow = TextOverflow.Ellipsis,         // 溢出省略
            )
            Text(                                         // 分块数
                text = "${doc.chunkCount} 块",            // 内容
                style = MaterialTheme.typography.labelSmall,  // 小字
                color = MuyunText3,                       // 三级灰
                modifier = Modifier.padding(top = 3.dp),  // 上留白
            )
        }
        StatusPill(                                       // 状态徽章
            text = statusLabel(doc.status),               // 状态文字
            tone = when (doc.status) {                    // 映射色调
                IngestStatus.INDEXED -> StatusTone.SUCCESS  // 已索引 → 绿
                IngestStatus.FAILED -> StatusTone.FAIL     // 失败 → 红
                IngestStatus.PARSING -> StatusTone.INFO    // 解析中 → 蓝
                IngestStatus.PENDING -> StatusTone.NEUTRAL // 待处理 → 灰
            },
        )
    }
}

/** 入库状态 → 中文标签。 */
private fun statusLabel(status: IngestStatus): String = when (status) {  // 状态标签
    IngestStatus.INDEXED -> "已索引"                     // 已入库
    IngestStatus.PARSING -> "解析中"                     // 解析中
    IngestStatus.PENDING -> "待处理"                     // 待处理
    IngestStatus.FAILED -> "失败"                        // 失败
}

/** 知识库详情 ViewModel —— 某库的文件列表 + 投喂（用 SavedStateHandle 读导航参数）。 */
@HiltViewModel                                           // 注解：由 Hilt 创建
class KnowledgeDetailViewModel @Inject constructor(      // 构造函数注入
    @ApplicationContext private val context: Context,    // 应用上下文
    private val kbDao: KbDao,                            // 知识库 DAO
    private val repo: KnowledgeRepository,               // 知识库仓库
    savedStateHandle: SavedStateHandle,                  // 导航参数（folderId/folderName）
) : ViewModel() {                                        // 继承 ViewModel

    val folderId: String = savedStateHandle.get<String>("folderId") ?: DEFAULT_KB  // 当前库 ID
    val folderName: String = savedStateHandle.get<String>("folderName") ?: "知识库"  // 当前库名

    private val _docs = MutableStateFlow<List<KbDocument>>(emptyList())  // 文档列表
    val docs: StateFlow<List<KbDocument>> = _docs.asStateFlow()  // 只读暴露
    private val _message = MutableStateFlow("")          // 提示消息
    val message: StateFlow<String> = _message.asStateFlow()  // 只读暴露

    init {                                                // 初始化
        viewModelScope.launch {                          // 观察本库文档列表
            kbDao.observeDocuments(folderId).collect { _docs.value = it }  // 实时推送
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
                    runCatching { repo.ingestFile(tmp, folderId) }.onSuccess { n++ }  // 入库
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
                val r = runCatching { repo.ingestFile(tmp, folderId) }.isSuccess  // 入库
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
