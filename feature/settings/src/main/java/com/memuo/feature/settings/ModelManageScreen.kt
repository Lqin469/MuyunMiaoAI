package com.memuo.feature.settings                         // 声明包名：设置业务模块

import android.content.Intent                             // 导入 Intent：分享日志
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
import androidx.compose.foundation.rememberScrollState     // 导入 rememberScrollState：滚动状态（诊断弹窗）
import androidx.compose.foundation.shape.RoundedCornerShape  // 导入 RoundedCornerShape：圆角形状
import androidx.compose.foundation.text.selection.SelectionContainer  // 导入 SelectionContainer：文本长按选中
import androidx.compose.foundation.verticalScroll          // 导入 verticalScroll：纵向滚动（诊断弹窗）
import androidx.compose.material3.Icon                    // 导入 Icon：图标
import androidx.compose.material3.MaterialTheme           // 导入 MaterialTheme：主题
import androidx.compose.material3.OutlinedButton           // 导入 OutlinedButton：次要按钮
import androidx.compose.material3.Text                    // 导入 Text：文本
import androidx.compose.runtime.Composable                // 导入 Composable：可组合函数注解
import androidx.compose.runtime.collectAsState            // 导入 collectAsState：状态流→状态
import androidx.compose.runtime.getValue                  // 导入 getValue：by 委托
import androidx.compose.ui.Alignment                      // 导入 Alignment：对齐
import androidx.compose.ui.Modifier                       // 导入 Modifier：修饰
import androidx.compose.ui.draw.clip                      // 导入 clip：裁剪
import androidx.compose.ui.draw.shadow                    // 导入 shadow：投影
import androidx.compose.ui.platform.LocalContext          // 导入 LocalContext：上下文（分享）
import androidx.compose.ui.text.font.FontWeight           // 导入 FontWeight：字重
import androidx.compose.ui.text.style.TextOverflow        // 导入 TextOverflow：溢出省略
import androidx.compose.ui.unit.dp                        // 导入 dp：尺寸单位
import androidx.hilt.navigation.compose.hiltViewModel     // 导入 hiltViewModel：Hilt 提供 ViewModel
import androidx.lifecycle.ViewModel                       // 导入 ViewModel：UI 数据持有者
import androidx.lifecycle.viewModelScope                  // 导入 viewModelScope：协程作用域
import com.memuo.core.ai.engine.EngineRuntimeMonitor      // 导入运行状态监控器（M-027 状态监控）
import com.memuo.core.ai.engine.EngineRuntimeState        // 导入运行状态数据类
import com.memuo.core.ai.engine.LocalChatEngine           // 导入本地引擎（删除时释放已加载模型）
import com.memuo.core.ai.engine.ModelLoadDiagnostics      // 导入诊断器（M-031）
import com.memuo.core.ai.engine.RuntimePhase              // 导入运行阶段枚举
import com.memuo.core.models.ModelImporter                 // 导入模型导入器（真实导入）
import com.memuo.core.models.ModelRepository              // 导入模型仓库（硬件画像）
import com.memuo.core.ui.AppIcons                          // 导入应用图标集
import com.memuo.core.ui.components.BrandButton            // 导入品牌主按钮
import com.memuo.core.ui.components.EmptyState            // 导入空态组件
import com.memuo.core.ui.components.LocalToast            // 导入 Toast 状态
import com.memuo.core.ui.components.ModalCloseButton      // 导入弹窗关闭按钮
import com.memuo.core.ui.components.MuyunModal            // 导入弹窗容器
import com.memuo.core.ui.components.StatusPill            // 导入状态徽章
import com.memuo.core.ui.components.StatusTone            // 导入状态色调
import com.memuo.core.ui.components.SubBody               // 导入子页内容容器
import com.memuo.core.ui.components.SubHeader             // 导入子页顶栏
import com.memuo.core.ui.theme.MuyunAccentLight           // 导入浅灰底
import com.memuo.core.ui.theme.MuyunCard                  // 导入卡片白
import com.memuo.core.ui.theme.MuyunDanger                // 导入危险红
import com.memuo.core.ui.theme.MuyunDangerBg              // 导入危险红底
import com.memuo.core.ui.theme.MuyunGreen                 // 导入成功绿
import com.memuo.core.ui.theme.MuyunGreenBg               // 导入成功绿底
import com.memuo.core.ui.theme.MuyunText                  // 导入主文字色
import com.memuo.core.ui.theme.MuyunText2                 // 导入次级文字色
import com.memuo.core.ui.theme.MuyunText3                 // 导入三级文字色
import dagger.hilt.android.lifecycle.HiltViewModel        // 导入 HiltViewModel
import kotlinx.coroutines.Dispatchers                      // 导入 Dispatchers：IO 调度器（删除文件）
import kotlinx.coroutines.delay                            // 导入 delay：延迟
import kotlinx.coroutines.flow.MutableStateFlow            // 导入可变状态流
import kotlinx.coroutines.flow.StateFlow                  // 导入只读状态流
import kotlinx.coroutines.flow.asStateFlow                // 导入 asStateFlow
import kotlinx.coroutines.launch                           // 导入 launch：协程
import kotlinx.coroutines.withContext                      // 导入 withContext：切线程
import org.json.JSONArray                                  // 导入 JSONArray：JSON 数组
import org.json.JSONObject                                 // 导入 JSONObject：JSON 对象
import javax.inject.Inject                                // 导入 Inject：构造函数注入

/** 已导入模型条目（对应 HTML modelList 元素 + 兼容性结论）。 */
data class ModelEntry(                                   // 模型条目
    val id: Long,                                        // ID
    val name: String,                                    // 名称
    val format: String,                                  // 格式（MNN/GGUF）
    val sizeGb: Double,                                  // 体积 GB
    val arch: String,                                    // 架构
    val needFp16: Boolean,                               // 是否需要 fp16
    val time: Long,                                      // 导入时间
)

/**
 * 模型管理页 —— 已导入模型列表 + 兼容性检测 + 导入（HTML 模型管理页迁移）。
 * 对应 HTML：模型卡片（格式/大小/架构 + 可运行/不可运行徽章 + 原因说明）、底部导入按钮。
 * 导入走真实 ModelImporter（SAF 文件夹选择器，由 MainActivity 传入回调）。
 */
@Composable                                               // 可组合 UI 函数
fun ModelManageScreen(                                   // 模型管理页
    onBack: () -> Unit,                                  // 返回回调
    onPickModel: () -> Unit,                             // 打开 SAF 选择器（由上层提供）
    viewModel: ModelManageViewModel = hiltViewModel(),   // Hilt 提供 ViewModel
) {
    val models by viewModel.models.collectAsState()      // 订阅模型列表
    val importing by viewModel.importing.collectAsState()  // 订阅导入中状态
    val deleting by viewModel.deleting.collectAsState()  // 订阅删除中状态
    val runtime by viewModel.runtime.collectAsState()    // 订阅运行状态（M-027 状态监控）
    val diagnosing by viewModel.diagnosing.collectAsState()  // 订阅诊断中状态
    val diagnosticResult by viewModel.diagnosticResult.collectAsState()  // 订阅诊断结果
    val toast = LocalToast.current                       // 取全局 Toast

    Column(modifier = Modifier.fillMaxSize()) {           // 纵向布局
        SubHeader(title = "模型管理", onBack = onBack)     // 顶栏
        SubBody(modifier = Modifier.fillMaxSize()) {       // 内容容器
            Column(modifier = Modifier.fillMaxSize()) {    // 纵向内容
                Text(                                    // 头部提示
                    text = "支持导入 MNN / GGUF 等端侧模型文件；导入后自动检测当前设备是否可运行。",  // 文案
                    style = MaterialTheme.typography.labelSmall,  // 小字
                    color = MuyunText3,                  // 三级灰
                    lineHeight = MaterialTheme.typography.labelSmall.lineHeight * 1.4f,  // 行距
                    modifier = Modifier.padding(bottom = 14.dp),  // 下留白
                )
                RuntimeCard(runtime)                     // 运行状态监控卡（M-027）
                if (models.isEmpty()) {                  // 空态
                    EmptyState(                          // 空态组件
                        icon = AppIcons.Model,           // 勾圆插图
                        text = "还没有模型，点击下方按钮导入",  // 空态文案
                        modifier = Modifier.weight(1f),  // 占满剩余
                    )
                } else {                                 // 列表
                    LazyColumn(                          // 懒加载列表
                        modifier = Modifier.weight(1f),  // 占满剩余
                    ) {
                        items(models, key = { it.id }) { model ->  // 遍历模型
                            ModelCard(                   // 模型卡片
                                entry = model,           // 数据
                                check = viewModel.check(model),  // 兼容性结论
                                deleting = deleting == model.id,  // 删除中
                                onDelete = { viewModel.delete(model) { ok ->  // 删除
                                    toast.show(if (ok) "已删除「${model.name}」" else "删除失败")  // 结果 Toast
                                } },
                            )
                        }
                    }
                }
                Column(modifier = Modifier.padding(top = 10.dp)) {  // 底部按钮
                    OutlinedButton(                       // 运行诊断按钮（M-031）
                        onClick = { viewModel.runDiagnostics() },  // 触发诊断
                        enabled = !diagnosing,            // 诊断中禁用
                        modifier = Modifier.fillMaxWidth(),  // 占满宽度
                    ) {
                        Text(if (diagnosing) "诊断中…（加载模型约需 10~30 秒）" else "运行诊断 · 生成日志")  // 状态文字
                    }
                    BrandButton(                         // 导入模型按钮
                        text = if (importing) "正在导入…" else "导入模型",  // 状态文字
                        enabled = !importing,            // 导入中禁用
                        onClick = onPickModel,           // 打开 SAF 选择器
                        modifier = Modifier.padding(top = 8.dp),  // 上留白
                    )
                }
            }
        }
    }

    // —— 诊断结果弹窗（M-031）——
    val shareContext = LocalContext.current             // 分享用上下文
    MuyunModal(                                         // 结果弹窗
        visible = diagnosticResult != null,             // 有结果显示
        onDismiss = { viewModel.closeDiagnostic() },    // 关闭
        title = "模型诊断日志",                          // 标题
        headerActions = { ModalCloseButton { viewModel.closeDiagnostic() } },  // 关闭按钮
        body = {                                        // 主体：可滚动日志（可长按选中复制）
            SelectionContainer {                        // 长按选中复制
                Text(                                   // 日志文本
                    text = diagnosticResult.orEmpty(),  // 内容
                    style = MaterialTheme.typography.labelSmall,  // 小字（等宽日志）
                    color = MuyunText2,                 // 次级灰
                    lineHeight = MaterialTheme.typography.labelSmall.lineHeight * 1.5f,  // 行距
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),  // 可滚动
                )
            }
        },
        footer = {                                      // 底部：复制（主按钮）+ 分享
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {  // 两按钮排
                BrandButton(                           // 复制按钮（主按钮，醒目）
                    text = "复制日志",                  // 文案
                    onClick = { viewModel.copyDiagnostic { toast.show("日志已复制到剪贴板") } },  // 复制 + Toast
                    modifier = Modifier.weight(1f),     // 平分
                )
                OutlinedButton(                         // 分享按钮
                    onClick = {                          // 分享日志
                        val text = diagnosticResult.orEmpty()  // 日志内容
                        val intent = Intent(Intent.ACTION_SEND).apply {  // 分享 Intent
                            type = "text/plain"          // 纯文本
                            putExtra(Intent.EXTRA_TEXT, text)  // 日志内容
                        }
                        runCatching { shareContext.startActivity(Intent.createChooser(intent, "分享诊断日志")) }  // 弹系统分享
                    },
                    modifier = Modifier.weight(1f),     // 平分
                ) { Text("分享") }                       // 文案
            }
        },
    )
}

/** 运行状态监控卡（M-027）：模型运行阶段/加载耗时/首 token 延迟/token 计数/错误。 */
@Composable                                               // 可组合函数
private fun RuntimeCard(                                  // 运行状态卡
    state: EngineRuntimeState,                            // 运行状态
) {
    if (state.phase == RuntimePhase.IDLE && state.loadMs == 0L && state.error == null) {  // 完全空闲（无任何历史）
        return                                           // 不显示（避免无意义空卡）
    }
    Column(                                              // 卡片容器
        modifier = Modifier                             // 修饰
            .fillMaxWidth()                             // 占满宽度
            .padding(bottom = 14.dp)                    // 下留白
            .shadow(1.dp, RoundedCornerShape(14.dp))    // 轻投影
            .clip(RoundedCornerShape(14.dp))            // 圆角 14
            .background(MuyunCard)                      // 白底
            .padding(horizontal = 16.dp, vertical = 12.dp),  // 内边距
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {  // 头部行
            Text(                                       // 标题
                text = "本地模型运行状态",                // 内容
                style = MaterialTheme.typography.titleSmall,  // 字号
                fontWeight = FontWeight.SemiBold,        // 半粗
                color = MuyunText,                       // 主色
                modifier = Modifier.weight(1f),          // 占满
            )
            val (tone, label) = when (state.phase) {    // 阶段 → 徽章
                RuntimePhase.LOADING -> StatusTone.INFO to "加载中"       // 加载
                RuntimePhase.RUNNING -> StatusTone.SUCCESS to "推理中"    // 推理
                RuntimePhase.ERROR -> StatusTone.FAIL to "出错"           // 错误
                RuntimePhase.IDLE -> StatusTone.NEUTRAL to "已就绪"       // 空闲
            }
            StatusPill(text = label, tone = tone)       // 阶段徽章
        }
        if (state.modelName.isNotEmpty()) {             // 有模型名
            Text(                                       // 模型名
                text = "模型：${state.modelName}",       // 内容
                style = MaterialTheme.typography.labelSmall,  // 小字
                color = MuyunText2,                      // 次级灰
                modifier = Modifier.padding(top = 6.dp), // 上留白
            )
        }
        // 统计行（有数据才显示）
        val stats = buildList {                          // 组装统计项
            if (state.loadMs > 0) add("加载耗时 ${state.loadMs}ms")  // 加载耗时
            if (state.firstTokenMs > 0) add("首 token ${state.firstTokenMs}ms")  // 首 token
            if (state.totalTokens > 0) add("已生成 ${state.totalTokens} tokens")  // token 数
        }
        if (stats.isNotEmpty()) {                        // 有统计
            Text(                                       // 统计文字
                text = stats.joinToString(" · "),        // 拼接
                style = MaterialTheme.typography.labelSmall,  // 小字
                color = MuyunText3,                      // 三级灰
                modifier = Modifier.padding(top = 4.dp), // 上留白
            )
        }
        state.error?.let { err ->                        // 有错误
            Text(                                       // 错误文字
                text = err,                              // 内容
                style = MaterialTheme.typography.labelSmall,  // 小字
                color = MuyunDanger,                     // 红色
                lineHeight = MaterialTheme.typography.labelSmall.lineHeight * 1.4f,  // 行距
                modifier = Modifier.padding(top = 6.dp), // 上留白
            )
        }
    }
}

/** 模型卡片：图标 + 名称/格式信息 + 可运行徽章 + 删除按钮 + 原因说明（M-027 加删除）。 */
@Composable                                               // 可组合函数
private fun ModelCard(                                   // 模型卡片
    entry: ModelEntry,                                   // 模型条目
    check: Pair<Boolean, String>,                        // 兼容性结论（ok + 原因）
    deleting: Boolean = false,                           // 是否删除中
    onDelete: () -> Unit = {},                           // 删除回调
) {
    Column(                                              // 纵向布局
        modifier = Modifier                             // 修饰
            .fillMaxWidth()                             // 占满宽度
            .padding(bottom = 10.dp)                    // 下留白
            .shadow(1.dp, RoundedCornerShape(14.dp))    // 轻投影
            .clip(RoundedCornerShape(14.dp))            // 圆角 14
            .background(MuyunCard)                      // 白底
            .padding(horizontal = 16.dp, vertical = 14.dp),  // 内边距
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {  // 头部行
            Box(                                        // 图标底
                modifier = Modifier                    // 修饰
                    .size(36.dp)                       // 36dp
                    .clip(RoundedCornerShape(10.dp))   // 圆角 10
                    .background(MuyunAccentLight),     // 浅灰底
                contentAlignment = Alignment.Center,    // 居中
            ) {
                Icon(                                   // 勾圆图标
                    imageVector = AppIcons.Model,        // 图标
                    contentDescription = null,           // 装饰
                    tint = MuyunText2,                   // 次级灰
                    modifier = Modifier.size(16.dp),     // 16dp
                )
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {  // 信息区
                Text(                                   // 模型名
                    text = entry.name,                   // 内容
                    style = MaterialTheme.typography.titleSmall,  // 字号（HTML 14px）
                    fontWeight = FontWeight.SemiBold,    // 半粗
                    color = MuyunText,                   // 主文字色
                    maxLines = 1,                        // 单行
                    overflow = TextOverflow.Ellipsis,    // 省略
                )
                Text(                                   // 格式信息（HTML .model-item-sub）
                    text = "${entry.format} · ${entry.sizeGb}GB · ${entry.arch}",  // 格式·大小·架构
                    style = MaterialTheme.typography.labelSmall,  // 小字（HTML 11px）
                    color = MuyunText3,                  // 三级灰
                    modifier = Modifier.padding(top = 3.dp),  // 上留白
                )
            }
            StatusPill(                                 // 可运行徽章（HTML .model-badge）
                text = if (check.first) "可运行" else "不可运行",  // 文案
                tone = if (check.first) StatusTone.SUCCESS else StatusTone.FAIL,  // 色调
            )
            Box(                                         // 删除按钮（M-027 新增）
                modifier = Modifier                     // 修饰
                    .padding(start = 8.dp)             // 左留白
                    .size(30.dp)                       // 30dp
                    .clip(RoundedCornerShape(8.dp))    // 圆角 8
                    .clickable(enabled = !deleting) { onDelete() }  // 点击删除
                ,
                contentAlignment = Alignment.Center,     // 居中
            ) {
                Icon(                                    // 删除图标
                    imageVector = AppIcons.Trash,        // 垃圾桶
                    contentDescription = "删除模型",      // 描述
                    tint = if (deleting) MuyunText3 else MuyunDanger,  // 删除中灰/红
                    modifier = Modifier.size(15.dp),     // 15dp
                )
            }
        }
        Box(                                             // 原因说明条（HTML .model-reason）
            modifier = Modifier                        // 修饰
                .fillMaxWidth()                        // 占满宽度
                .padding(top = 10.dp)                  // 上留白
                .clip(RoundedCornerShape(8.dp))        // 圆角 8
                .background(if (check.first) MuyunGreenBg else MuyunDangerBg)  // 绿底/红底
                .padding(horizontal = 12.dp, vertical = 8.dp),  // 内边距
        ) {
            Text(                                        // 原因文字
                text = check.second,                     // 内容
                style = MaterialTheme.typography.labelSmall,  // 小字（HTML 11px）
                color = if (check.first) MuyunGreen else MuyunDanger,  // 绿/红
                lineHeight = MaterialTheme.typography.labelSmall.lineHeight * 1.4f,  // 行距
            )
        }
    }
}

/** 模型管理 ViewModel —— 列表持久化 + 兼容性检测 + 导入/删除 + 运行状态监控 + 诊断（M-027/M-031）。 */
@HiltViewModel                                           // 注解：由 Hilt 创建
class ModelManageViewModel @Inject constructor(          // 构造函数注入
    private val prefs: ExtPrefs,                         // 注入扩展偏好
    private val importer: ModelImporter,                 // 注入模型导入器
    private val repo: ModelRepository,                   // 注入模型仓库（硬件画像）
    private val monitor: EngineRuntimeMonitor,           // 注入运行状态监控器（M-027）
    private val localEngine: LocalChatEngine,            // 注入本地引擎（删除时释放）
    private val diagnostics: ModelLoadDiagnostics,       // 注入诊断器（M-031）
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,  // 注入应用上下文（SAF 导入）
) : ViewModel() {                                        // 继承 ViewModel

    private val _models = MutableStateFlow<List<ModelEntry>>(emptyList())  // 模型列表
    val models: StateFlow<List<ModelEntry>> = _models.asStateFlow()  // 只读暴露
    private val _importing = MutableStateFlow(false)     // 导入中
    val importing: StateFlow<Boolean> = _importing.asStateFlow()  // 只读暴露
    private val _deleting = MutableStateFlow<Long?>(null)  // 删除中的模型 id（null = 无）
    val deleting: StateFlow<Long?> = _deleting.asStateFlow()  // 只读暴露
    /** 运行状态（透传监控器：加载/推理/统计/错误）。 */
    val runtime: StateFlow<EngineRuntimeState> = monitor.state  // 运行状态
    private val _diagnosing = MutableStateFlow(false)   // 诊断中
    val diagnosing: StateFlow<Boolean> = _diagnosing.asStateFlow()  // 只读暴露
    private val _diagnosticResult = MutableStateFlow<String?>(null)  // 诊断结果（null = 未诊断）
    val diagnosticResult: StateFlow<String?> = _diagnosticResult.asStateFlow()  // 只读暴露

    init {                                                // 初始化
        viewModelScope.launch {                          // 协程中收集
            prefs.modelListJson.collect { json ->        // JSON 变化
                _models.value = decode(json)             // 解码
            }
        }
    }

    /** JSON → 列表。 */
    private fun decode(json: String): List<ModelEntry> =  // 解码
        if (json.isBlank()) emptyList() else runCatching {  // 空/解析
            val arr = JSONArray(json)                    // 数组
            (0 until arr.length()).map { i ->            // 遍历
                val o = arr.getJSONObject(i)             // 对象
                ModelEntry(                              // 组装
                    id = o.optLong("id"),                // id
                    name = o.optString("name"),          // 名称
                    format = o.optString("format"),      // 格式
                    sizeGb = o.optDouble("sizeGb"),      // 体积
                    arch = o.optString("arch"),          // 架构
                    needFp16 = o.optBoolean("needFp16"), // fp16
                    time = o.optLong("time"),            // 时间
                )
            }
        }.getOrDefault(emptyList())                      // 失败空列表

    /** 列表 → JSON。 */
    private fun encode(list: List<ModelEntry>): String =  // 编码
        JSONArray().apply {                              // 数组
            list.forEach { m ->                          // 遍历
                put(JSONObject().apply {                 // 对象
                    put("id", m.id); put("name", m.name); put("format", m.format)  // 基本字段
                    put("sizeGb", m.sizeGb); put("arch", m.arch); put("needFp16", m.needFp16)  // 规格
                    put("time", m.time)                  // 时间
                })
            }
        }.toString()                                     // 转字符串

    /** 持久化列表。 */
    private fun persist() {                              // 持久化
        viewModelScope.launch { prefs.setModelListJson(encode(_models.value)) }  // 写 DataStore
    }

    /** 兼容性检测（M-027 修复：真实内存 + arm64 自动满足 fp16，不再误判）。 */
    fun check(m: ModelEntry): Pair<Boolean, String> {    // 检测
        val hw = repo.probeHardware()                    // 硬件画像（真实物理内存）
        val ramGb = hw.totalRamMb / 1024.0               // 总内存 GB
        val reasons = mutableListOf<String>()            // 原因集
        if (m.sizeGb > ramGb * 0.7) reasons.add("模型体积 ${m.sizeGb}GB，超过设备可用运行内存（约 ${ramGb.toInt()}GB）")  // 内存不足
        if (m.arch != hw.abi) reasons.add("模型架构 ${m.arch} 与设备架构 ${hw.abi} 不兼容，需重新导出")  // 架构不兼容
        if (m.needFp16 && !hw.abi.contains("arm64")) reasons.add("模型需要 fp16 指令集，当前架构 ${hw.abi} 不支持")  // fp16（arm64 默认支持，仅非 arm64 提示）
        return if (reasons.isEmpty()) {                  // 全部通过
            true to "设备 ${hw.abi}，可流畅运行"          // 可运行
        } else {                                         // 有问题
            false to reasons.joinToString("；")           // 拼接原因
        }
    }

    /** 真实导入（MainActivity 的 SAF 回调调用）：导入成功后加入列表。 */
    fun importFromUri(uri: android.net.Uri) {            // 真实导入
        _importing.value = true                          // 导入中
        viewModelScope.launch {                          // 协程中执行
            delay(300)                                   // 给 UI 一点反馈时间
            val ok = importer.importFromUri(context, uri)  // 真实复制到 app 私有目录
            if (ok) {                                    // 导入成功
                val entry = ModelEntry(                  // 构造条目
                    id = System.currentTimeMillis(),     // id
                    name = uri.lastPathSegment ?: "已导入模型",  // 名称用路径尾段
                    format = "MNN",                      // 格式（MNN 目录导入）
                    sizeGb = 0.5,                        // 体积未知（占位）
                    arch = repo.probeHardware().abi,     // 按设备架构
                    needFp16 = true,                     // fp16
                    time = System.currentTimeMillis(),   // 时间
                )
                _models.value = _models.value + entry    // 入列
                persist()                                // 持久化
            }
            _importing.value = false                     // 结束导入
        }
    }

    /** 删除模型（M-027 新增）：释放已加载引擎 → 删磁盘文件 → 移除列表。 */
    fun delete(model: ModelEntry, onResult: (Boolean) -> Unit = {}) {  // 删除模型
        if (_deleting.value != null) return              // 已有删除中
        _deleting.value = model.id                       // 标记删除中
        viewModelScope.launch(Dispatchers.IO) {          // IO 线程删除
            localEngine.release()                        // 释放已加载模型（防文件占用）
            val ok = importer.deleteLocalModel(model.format, model.name)  // 删磁盘文件
            withContext(Dispatchers.Main) {              // 回主线程更新 UI
                if (ok) {                                // 删除成功
                    _models.value = _models.value.filterNot { it.id == model.id }  // 移除列表
                    persist()                            // 持久化
                }
                _deleting.value = null                   // 清删除标记
                onResult(ok)                             // 结果回调
            }
        }
    }

    /** 运行诊断（M-031 新增）：生成完整诊断日志，定位模型加载失败原因。 */
    fun runDiagnostics() {                                // 运行诊断
        if (_diagnosing.value) return                     // 诊断中忽略
        _diagnosing.value = true                          // 诊断中
        _diagnosticResult.value = null                    // 清旧结果
        viewModelScope.launch {                           // 协程执行（诊断器内部切 IO）
            val log = runCatching { diagnostics.runDiagnostics() }.getOrElse { e ->  // 容错
                "诊断执行异常: ${e.javaClass.simpleName}: ${e.message}"  // 异常兜底
            }
            _diagnosticResult.value = log                 // 发布结果
            _diagnosing.value = false                     // 结束诊断
        }
    }

    /** 复制诊断结果到剪贴板。 */
    fun copyDiagnostic(onDone: () -> Unit) {              // 复制诊断结果
        val text = _diagnosticResult.value ?: return      // 无结果忽略
        val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager  // 剪贴板服务
        cm.setPrimaryClip(android.content.ClipData.newPlainText("诊断日志", text))  // 写入剪贴板
        onDone()                                          // 回调（Toast 提示）
    }

    /** 关闭诊断结果弹窗。 */
    fun closeDiagnostic() { _diagnosticResult.value = null }  // 关闭
}