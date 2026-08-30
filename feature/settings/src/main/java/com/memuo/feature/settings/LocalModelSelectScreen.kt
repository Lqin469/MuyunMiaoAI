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
import androidx.compose.runtime.LaunchedEffect            // 导入 LaunchedEffect：进入页面时扫描
import androidx.compose.runtime.collectAsState            // 导入 collectAsState：状态流→状态
import androidx.compose.runtime.getValue                  // 导入 getValue：by 委托
import androidx.compose.ui.Alignment                      // 导入 Alignment：对齐
import androidx.compose.ui.Modifier                       // 导入 Modifier：修饰
import androidx.compose.ui.draw.clip                      // 导入 clip：裁剪
import androidx.compose.ui.draw.shadow                    // 导入 shadow：投影
import androidx.compose.ui.text.font.FontWeight           // 导入 FontWeight：字重
import androidx.compose.ui.text.style.TextOverflow        // 导入 TextOverflow：溢出省略
import androidx.compose.ui.unit.dp                        // 导入 dp：尺寸单位
import androidx.hilt.navigation.compose.hiltViewModel     // 导入 hiltViewModel：Hilt 提供 ViewModel
import androidx.lifecycle.ViewModel                       // 导入 ViewModel：UI 数据持有者
import androidx.lifecycle.viewModelScope                  // 导入 viewModelScope：协程作用域
import com.memuo.core.models.LocalModelInfo                // 导入本地模型信息
import com.memuo.core.models.ModelImporter                 // 导入模型导入器（枚举本地模型）
import com.memuo.core.ui.AppIcons                          // 导入应用图标集
import com.memuo.core.ui.components.BrandButton            // 导入品牌主按钮
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
import com.memuo.core.ui.theme.MuyunGreen                 // 导入成功绿
import com.memuo.core.ui.theme.MuyunText                  // 导入主文字色
import com.memuo.core.ui.theme.MuyunText2                 // 导入次级文字色
import com.memuo.core.ui.theme.MuyunText3                 // 导入三级文字色
import dagger.hilt.android.lifecycle.HiltViewModel        // 导入 HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow            // 导入可变状态流
import kotlinx.coroutines.flow.StateFlow                  // 导入只读状态流
import kotlinx.coroutines.flow.asStateFlow                // 导入 asStateFlow
import kotlinx.coroutines.launch                           // 导入 launch：协程
import javax.inject.Inject                                // 导入 Inject：构造函数注入

/**
 * 本地模型选择页（M-035 新增）—— 长按顶栏「本地」按钮进入。
 * 列出所有已安装的本地模型（MNN 主模型 + GGUF 模型），显示当前选中，
 * 点击可切换运行模型（记录到偏好；MNN 直接生效，GGUF 标注「预览支持」）。
 */
@Composable                                               // 可组合 UI 函数
fun LocalModelSelectScreen(                               // 本地模型选择页
    onBack: () -> Unit,                                  // 返回回调
    onImportModel: () -> Unit,                           // 导入新模型（跳转模型管理页）
    viewModel: LocalModelSelectViewModel = hiltViewModel(),  // Hilt 提供 ViewModel
) {
    val models by viewModel.models.collectAsState()      // 订阅模型列表
    val currentId by viewModel.currentId.collectAsState()  // 订阅当前选中
    val toast = LocalToast.current                       // 取全局 Toast

    LaunchedEffect(Unit) {                               // 进入页面扫描一次
        viewModel.refresh()                              // 扫描本地模型
    }

    Column(modifier = Modifier.fillMaxSize()) {           // 纵向布局
        SubHeader(title = "本地模型", onBack = onBack)     // 顶栏
        SubBody(modifier = Modifier.fillMaxSize()) {       // 内容容器
            Column(modifier = Modifier.fillMaxSize()) {    // 纵向内容
                Text(                                    // 头部提示
                    text = "长按顶栏「本地/云端」按钮可进入本页；点击模型卡片即可切换当前运行模型。",  // 文案
                    style = MaterialTheme.typography.labelSmall,  // 小字
                    color = MuyunText3,                  // 三级灰
                    lineHeight = MaterialTheme.typography.labelSmall.lineHeight * 1.4f,  // 行距
                    modifier = Modifier.padding(bottom = 14.dp),  // 下留白
                )
                if (models.isEmpty()) {                  // 空态
                    EmptyState(                          // 空态组件
                        icon = AppIcons.Model,           // 模型图标
                        text = "还没有本地模型，点击下方按钮导入",  // 空态文案
                        modifier = Modifier.weight(1f),  // 占满剩余
                    )
                } else {                                 // 列表
                    LazyColumn(                          // 懒加载列表
                        modifier = Modifier.weight(1f),  // 占满剩余
                    ) {
                        items(models, key = { it.id }) { model ->  // 遍历模型
                            LocalModelCard(              // 模型卡片
                                model = model,           // 数据
                                isCurrent = model.id == currentId,  // 是否当前
                                onClick = {              // 点击切换
                                    viewModel.select(model.id)  // 选中
                                    toast.show(if (model.runnable) "已切换到「${model.name}」" else "已选中「${model.name}」（预览支持，运行需 llama.cpp 运行时）")  // 提示
                                },
                            )
                        }
                    }
                }
                Column(modifier = Modifier.padding(top = 10.dp)) {  // 底部按钮
                    BrandButton(                         // 导入新模型
                        text = "导入新模型",              // 文案
                        onClick = onImportModel,          // 跳转模型管理
                    )
                }
            }
        }
    }
}

/** 本地模型卡片：图标 + 名称/格式/大小 + 当前标记 + 可运行标记。 */
@Composable                                               // 可组合函数
private fun LocalModelCard(                               // 模型卡片
    model: LocalModelInfo,                                // 数据
    isCurrent: Boolean,                                   // 是否当前
    onClick: () -> Unit,                                  // 点击
) {
    Column(                                              // 纵向布局
        modifier = Modifier                             // 修饰
            .fillMaxWidth()                             // 占满宽度
            .padding(bottom = 10.dp)                    // 下留白
            .shadow(1.dp, RoundedCornerShape(14.dp))    // 轻投影
            .clip(RoundedCornerShape(14.dp))            // 圆角 14
            .background(if (isCurrent) MuyunBrandSoft else MuyunCard)  // 当前品牌浅底/白
            .clickable { onClick() }                    // 点击切换
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
                Icon(                                   // 模型图标
                    imageVector = AppIcons.Model,        // 图标
                    contentDescription = null,           // 装饰
                    tint = if (isCurrent) MuyunBrand else MuyunText2,  // 品牌/灰
                    modifier = Modifier.size(16.dp),     // 16dp
                )
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {  // 信息区
                Text(                                   // 模型名
                    text = model.name,                   // 内容
                    style = MaterialTheme.typography.titleSmall,  // 字号
                    fontWeight = FontWeight.SemiBold,    // 半粗
                    color = MuyunText,                   // 主色
                    maxLines = 1,                        // 单行
                    overflow = TextOverflow.Ellipsis,    // 省略
                )
                Text(                                   // 格式·大小
                    text = "${model.format} · ${fmtSize(model.sizeBytes)}",  // 格式·大小
                    style = MaterialTheme.typography.labelSmall,  // 小字
                    color = MuyunText3,                  // 三级灰
                    modifier = Modifier.padding(top = 3.dp),  // 上留白
                )
            }
            if (!model.runnable) {                      // 不可运行（GGUF）
                StatusPill(                             // 预览支持徽章
                    text = "预览支持",                    // 文案
                    tone = StatusTone.NEUTRAL,           // 中性
                )
            } else if (isCurrent) {                     // 当前可运行
                StatusPill(                             // 使用中徽章
                    text = "使用中",                      // 文案
                    tone = StatusTone.SUCCESS,           // 成功绿
                )
            }
        }
    }
}

/** 字节格式化。 */
private fun fmtSize(b: Long): String = when {            // 格式化
    b >= 1L shl 30 -> "%.2f GB".format(java.util.Locale.getDefault(), b / 1024.0 / 1024 / 1024)  // GB
    b >= 1L shl 20 -> "%.1f MB".format(java.util.Locale.getDefault(), b / 1024.0 / 1024)  // MB
    b >= 1L shl 10 -> "%.0f KB".format(java.util.Locale.getDefault(), b / 1024.0)  // KB
    else -> "$b B"                                       // B
}

/** 本地模型选择 ViewModel —— 枚举本地模型 + 当前选中 + 切换（M-035）。 */
@HiltViewModel                                           // 注解：由 Hilt 创建
class LocalModelSelectViewModel @Inject constructor(     // 构造函数注入
    private val importer: ModelImporter,                 // 注入模型导入器（枚举本地模型）
    private val prefs: ExtPrefs,                         // 注入扩展偏好（当前选中）
) : ViewModel() {                                        // 继承 ViewModel

    private val _models = MutableStateFlow<List<LocalModelInfo>>(emptyList())  // 模型列表
    val models: StateFlow<List<LocalModelInfo>> = _models.asStateFlow()  // 只读暴露
    private val _currentId = MutableStateFlow("mnn-llm")  // 当前选中 id
    val currentId: StateFlow<String> = _currentId.asStateFlow()  // 只读暴露

    init {                                                // 初始化
        viewModelScope.launch {                          // 加载当前选中
            prefs.localModelId.collect { id ->           // 偏好变化
                _currentId.value = id                    // 更新
            }
        }
        refresh()                                        // 扫描本地模型
    }

    /** 扫描本地模型。 */
    fun refresh() {                                      // 扫描
        _models.value = importer.listLocalModels()       // 枚举已安装模型
    }

    /** 切换当前选中模型。 */
    fun select(id: String) {                             // 选中
        _currentId.value = id                            // 更新 UI
        viewModelScope.launch { prefs.setLocalModelId(id) }  // 持久化
    }
}
