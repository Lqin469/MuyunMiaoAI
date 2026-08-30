package com.memuo.feature.settings                         // 声明包名：设置业务模块

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
import androidx.compose.runtime.mutableStateOf            // 导入 mutableStateOf：可变状态
import androidx.compose.runtime.remember                  // 导入 remember：记住状态
import androidx.compose.runtime.setValue                  // 导入 setValue：by 委托写
import androidx.compose.ui.Alignment                      // 导入 Alignment：对齐
import androidx.compose.ui.Modifier                       // 导入 Modifier：修饰
import androidx.compose.ui.draw.clip                      // 导入 clip：裁剪
import androidx.compose.ui.draw.shadow                    // 导入 shadow：投影
import androidx.compose.ui.graphics.Color                 // 导入 Color：颜色
import androidx.compose.ui.text.font.FontWeight           // 导入 FontWeight：字重
import androidx.compose.ui.unit.dp                        // 导入 dp：尺寸单位
import androidx.hilt.navigation.compose.hiltViewModel     // 导入 hiltViewModel：Hilt 提供 ViewModel
import androidx.lifecycle.ViewModel                       // 导入 ViewModel：UI 数据持有者
import androidx.lifecycle.viewModelScope                  // 导入 viewModelScope：协程作用域
import com.memuo.core.ui.AppIcons                          // 导入应用图标集
import com.memuo.core.ui.components.LocalToast            // 导入 Toast 状态
import com.memuo.core.ui.components.MuyunToggle           // 导入 iOS 风格开关
import com.memuo.core.ui.components.SubBody               // 导入子页内容容器
import com.memuo.core.ui.components.SubHeader             // 导入子页顶栏
import com.memuo.core.ui.theme.MuyunBrandGradient         // 导入品牌渐变
import com.memuo.core.ui.theme.MuyunCard                  // 导入卡片白
import com.memuo.core.ui.theme.MuyunPurple                // 导入品牌紫
import com.memuo.core.ui.theme.MuyunPurpleBg              // 导入品牌紫底
import com.memuo.core.ui.theme.MuyunText                  // 导入主文字色
import com.memuo.core.ui.theme.MuyunText2                 // 导入次级文字色
import com.memuo.core.ui.theme.MuyunText3                 // 导入三级文字色
import dagger.hilt.android.lifecycle.HiltViewModel        // 导入 HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow            // 导入可变状态流
import kotlinx.coroutines.flow.StateFlow                  // 导入只读状态流
import kotlinx.coroutines.flow.asStateFlow                // 导入 asStateFlow
import kotlinx.coroutines.launch                           // 导入 launch：协程
import org.json.JSONArray                                  // 导入 JSONArray：JSON 数组
import javax.inject.Inject                                // 导入 Inject：构造函数注入

/** 默认知识库 ID（云端资料库沿用，历史文档不丢）。 */
const val DEFAULT_KB = "default"                          // 默认知识库标识

/** 本地私密库 ID（对应 HTML「本地的私密库」条目）。 */
const val PRIVATE_KB = "private"                          // 私密库标识

/** 知识库文件夹（对应 HTML 知识库条目：云端的资料库 / 本地的私密库 / 用户新建）。 */
data class KbFolder(val name: String, val localOnly: Boolean, val folderId: String = DEFAULT_KB)  // 文件夹（含库 ID）

/**
 * 知识库页 —— 文件夹列表 + 隐私开关 + 新建（HTML 知识库页迁移）。
 * 点击文件夹 → 进入详情页（展示该库文件列表 + 添加文件/文件夹）。
 */
@Composable                                               // 可组合 UI 函数
fun KnowledgeScreen(                                      // 知识库页
    onBack: () -> Unit,                                   // 返回回调
    onOpenFolder: (KbFolder) -> Unit,                     // 点击文件夹 → 详情页
    viewModel: KnowledgeViewModel = hiltViewModel(),      // Hilt 提供 ViewModel
) {
    val folders by viewModel.folders.collectAsState()     // 订阅文件夹列表
    val privacy by viewModel.privacy.collectAsState()     // 订阅隐私开关
    val toast = LocalToast.current                        // 取全局 Toast
    var newName by remember { mutableStateOf("") }        // 新知识库名称

    Column(modifier = Modifier.fillMaxSize()) {           // 纵向布局
        SubHeader(                                       // 顶栏
            title = "知识库",                            // 标题
            onBack = onBack,                             // 返回
        )
        SubBody(modifier = Modifier.fillMaxSize()) {       // 内容容器
            LazyColumn(modifier = Modifier.fillMaxSize()) {  // 整页滚动
                // 知识库文件夹条目（HTML .kb-item，点击进入详情页）
                items(folders, key = { it.folderId }) { folder ->  // 遍历文件夹
                    Row(                                  // 条目行
                        modifier = Modifier              // 修饰
                            .fillMaxWidth()              // 占满宽度
                            .padding(bottom = 10.dp)     // 下留白（HTML margin-bottom 10px）
                            .shadow(1.dp, RoundedCornerShape(14.dp))  // 轻投影
                            .clip(RoundedCornerShape(14.dp))  // 圆角
                            .background(MuyunCard)       // 白底
                            .clickable { onOpenFolder(folder) }  // 点击 → 进入详情页（修复私密库无法进入的 bug）
                            .padding(horizontal = 16.dp, vertical = 16.dp),  // 内边距（HTML padding 16px）
                        verticalAlignment = Alignment.CenterVertically,  // 垂直居中
                    ) {
                        Box(                             // 文件夹图标底（库标识）
                            modifier = Modifier         // 修饰
                                .size(34.dp)            // 34dp
                                .clip(RoundedCornerShape(10.dp))  // 圆角 10
                                .background(MuyunPurpleBg)  // 紫浅底
                                .padding(9.dp),         // 内边距
                            contentAlignment = Alignment.Center,  // 居中
                        ) {
                            Icon(                        // 文件夹图标
                                imageVector = AppIcons.Folder,  // 图标
                                contentDescription = null,  // 装饰
                                tint = MuyunPurple,      // 紫
                                modifier = Modifier.size(16.dp),  // 16dp
                            )
                        }
                        Text(                            // 名称（HTML .kb-item-name）
                            text = folder.name,          // 内容
                            style = MaterialTheme.typography.titleMedium,  // 字号（HTML 15px）
                            fontWeight = FontWeight.Medium,  // 中粗
                            color = MuyunText,           // 主色
                            modifier = Modifier.weight(1f).padding(start = 12.dp),  // 占满 + 留白
                        )
                        if (folder.localOnly) {           // 仅本地标签
                            Box(                          // 标签（HTML .kb-item-tag）
                                modifier = Modifier     // 修饰
                                    .padding(end = 8.dp)  // 右留白
                                    .clip(RoundedCornerShape(10.dp))  // 圆角
                                    .background(MuyunPurpleBg)  // 紫浅底
                                    .padding(horizontal = 10.dp, vertical = 4.dp),  // 内边距
                            ) {
                                Text(                    // 文字
                                    text = "仅本地",      // 内容
                                    style = MaterialTheme.typography.labelSmall,  // 小字（HTML 11px）
                                    fontWeight = FontWeight.Medium,  // 中粗
                                    color = MuyunPurple,  // 紫
                                )
                            }
                        }
                        Box(                             // 删除按钮（HTML .kb-item-delete）
                            modifier = Modifier         // 修饰
                                .size(32.dp)            // 32dp
                                .clip(RoundedCornerShape(8.dp))  // 圆角 8
                                .clickable {             // 点击删除
                                    viewModel.deleteFolder(folder.name)  // 删除文件夹
                                    toast.show("已删除")  // Toast（HTML 同款）
                                }
                                .padding(9.dp),         // 内边距
                        ) {
                            Icon(                        // 垃圾桶图标
                                imageVector = AppIcons.Trash,  // 图标
                                contentDescription = "删除",  // 描述
                                tint = MuyunText3,       // 三级灰
                                modifier = Modifier.size(16.dp),  // 16dp
                            )
                        }
                        Icon(                            // 右箭头（提示可进入，HTML .arrow-right）
                            imageVector = AppIcons.ChevronRight,  // 箭头
                            contentDescription = null,   // 装饰
                            tint = MuyunText3,           // 三级灰（主题自适应）
                            modifier = Modifier.size(14.dp),  // 14dp
                        )
                    }
                }
                // 隐私库开关区（HTML .kb-privacy）
                item {                                    // 列表项（隐私开关行）
                    Row(                                  // 开关行
                        modifier = Modifier             // 修饰
                            .fillMaxWidth()             // 占满宽度
                            .padding(vertical = 20.dp, horizontal = 2.dp),  // 内边距（HTML padding 20px 0）
                        verticalAlignment = Alignment.CenterVertically,  // 垂直居中
                    ) {
                        Column(modifier = Modifier.weight(1f)) {  // 信息区
                            Text(                        // 标题（HTML h4）
                                text = "隐私库（仅本地可用）",  // 内容
                                style = MaterialTheme.typography.titleSmall,  // 字号（HTML 14px）
                                fontWeight = FontWeight.SemiBold,  // 半粗
                                color = MuyunText,       // 主色
                            )
                            Text(                        // 说明（HTML p）
                                text = "端侧索引，内容不出设备；只有本地管家能引用",  // 文案
                                style = MaterialTheme.typography.labelSmall,  // 小字（HTML 12px）
                                color = MuyunText3,      // 三级灰
                                modifier = Modifier.padding(top = 6.dp),  // 上留白
                            )
                        }
                        MuyunToggle(                      // iOS 开关（HTML .toggle-switch）
                            checked = privacy,            // 状态
                            onCheckedChange = { viewModel.setPrivacy(it) },  // 切换
                        )
                    }
                }
                // 新建知识库输入行（HTML .kb-new-input）
                item {                                    // 列表项（新建输入行）
                    Row(                                  // 输入行
                        modifier = Modifier.padding(bottom = 12.dp),  // 下留白
                        verticalAlignment = Alignment.CenterVertically,  // 垂直居中
                    ) {
                        Box(                              // 输入框
                            modifier = Modifier         // 修饰
                                .weight(1f)             // 占满剩余
                                .clip(RoundedCornerShape(10.dp))  // 圆角
                                .background(MuyunCard)  // 白底
                                .padding(horizontal = 16.dp, vertical = 12.dp),  // 内边距
                        ) {
                            androidx.compose.foundation.text.BasicTextField(  // 无边框输入
                                value = newName,          // 绑定
                                onValueChange = { newName = it },  // 更新
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MuyunText),  // 字体
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(MuyunPurple),  // 光标
                                modifier = Modifier.fillMaxWidth(),  // 占满
                                decorationBox = { inner ->  // 占位
                                    if (newName.isEmpty()) {  // 空
                                        Text("新知识库名称", color = MuyunText3, style = MaterialTheme.typography.bodyMedium)  // 占位（HTML 同款）
                                    }
                                    inner()               // 输入区
                                },
                            )
                        }
                        Box(                              // 圆形添加按钮（HTML 渐变圆钮）
                            modifier = Modifier         // 修饰
                                .padding(start = 10.dp) // 左留白
                                .size(44.dp)            // 44dp
                                .clip(RoundedCornerShape(22.dp))  // 圆形
                                .background(MuyunBrandGradient)  // 品牌渐变
                                .clickable {            // 点击新建
                                    val name = newName.trim()  // 去首尾空格
                                    if (name.isNotEmpty()) {   // 非空才创建
                                        viewModel.addFolder(name)  // 添加文件夹
                                        newName = ""    // 清空
                                        toast.show("知识库「$name」已创建")  // Toast（HTML 同款）
                                    }
                                },
                            contentAlignment = Alignment.Center,  // 居中
                        ) {
                            Icon(                        // 加号图标
                                imageVector = AppIcons.Plus,  // 图标
                                contentDescription = "新建知识库",  // 描述
                                tint = Color.White,      // 白
                                modifier = Modifier.size(18.dp),  // 18dp
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 知识库 ViewModel —— 文件夹列表 + 隐私开关（文档列表与投喂已下沉到详情页 ViewModel）。 */
@HiltViewModel                                           // 注解：由 Hilt 创建
class KnowledgeViewModel @Inject constructor(            // 构造函数注入
    private val prefs: ExtPrefs,                         // 扩展偏好（文件夹/隐私开关）
) : ViewModel() {                                        // 继承 ViewModel

    private val _folders = MutableStateFlow(listOf(KbFolder("云端的资料库", false, DEFAULT_KB), KbFolder("本地的私密库", true, PRIVATE_KB)))  // 文件夹（HTML 默认两条）
    val folders: StateFlow<List<KbFolder>> = _folders.asStateFlow()  // 只读暴露
    private val _privacy = MutableStateFlow(true)        // 隐私开关（默认开，HTML 同款）
    val privacy: StateFlow<Boolean> = _privacy.asStateFlow()  // 只读暴露

    init {                                                // 初始化
        viewModelScope.launch {                          // 加载文件夹
            prefs.kbFoldersJson.collect { json ->        // JSON 变化
                if (json.isNotBlank()) {                 // 有自定义
                    _folders.value = runCatching {       // 解析
                        val arr = JSONArray(json)        // 数组
                        (0 until arr.length()).map { i ->  // 遍历
                            val o = arr.getJSONObject(i)  // 对象
                            KbFolder(
                                name = o.optString("name"),
                                localOnly = o.optBoolean("localOnly"),
                                folderId = o.optString("folderId", DEFAULT_KB),  // 兼容旧数据缺 folderId
                            )
                        }
                    }.getOrDefault(_folders.value)       // 失败用默认
                }
            }
        }
        viewModelScope.launch {                          // 加载隐私开关
            prefs.kbPrivacy.collect { _privacy.value = it }  // 同步
        }
    }

    /** 切换隐私库开关（HTML toggleKbPrivacy）。 */
    fun setPrivacy(on: Boolean) {                         // 切换开关
        _privacy.value = on                              // 更新 UI
        viewModelScope.launch { prefs.setKbPrivacy(on) } // 持久化
    }

    /** 新建知识库文件夹（HTML addKb）。 */
    fun addFolder(name: String) {                         // 新建文件夹
        if (name.isBlank()) return                        // 空忽略
        // 用名称拼音不可得，直接用时间戳生成稳定库 ID，避免中文路径问题
        val id = "kb_" + System.currentTimeMillis()       // 生成唯一库 ID
        _folders.value = _folders.value + KbFolder(name, false, id)  // 加入列表
        persistFolders()                                  // 持久化
    }

    /** 删除文件夹（HTML 条目删除按钮）。 */
    fun deleteFolder(name: String) {                      // 删除文件夹
        _folders.value = _folders.value.filterNot { it.name == name }  // 移除
        persistFolders()                                  // 持久化
    }

    /** 文件夹列表持久化。 */
    private fun persistFolders() {                        // 持久化
        viewModelScope.launch {                          // 协程中
            prefs.setKbFoldersJson(JSONArray().apply {    // 编码
                _folders.value.forEach { f ->             // 遍历
                    put(org.json.JSONObject().apply { put("name", f.name); put("localOnly", f.localOnly); put("folderId", f.folderId) })  // 逐条
                }
            }.toString())
        }
    }
}
