package com.memuo.feature.settings                         // 声明包名：设置业务模块

import android.net.Uri                                   // 导入 Uri：内容标识
import androidx.activity.compose.rememberLauncherForActivityResult  // 导入 rememberLauncherForActivityResult：系统启动器
import androidx.activity.result.PickVisualMediaRequest    // 导入 PickVisualMediaRequest：相册请求
import androidx.activity.result.contract.ActivityResultContracts  // 导入 ActivityResultContracts：系统契约
import androidx.compose.foundation.background             // 导入 background：背景修饰
import androidx.compose.foundation.clickable              // 导入 clickable：点击修饰
import androidx.compose.foundation.layout.Arrangement     // 导入 Arrangement：排列
import androidx.compose.foundation.layout.Box             // 导入 Box：盒式布局
import androidx.compose.foundation.layout.Column          // 导入 Column：纵向布局
import androidx.compose.foundation.layout.Row             // 导入 Row：横向布局
import androidx.compose.foundation.layout.fillMaxSize     // 导入 fillMaxSize：铺满
import androidx.compose.foundation.layout.fillMaxWidth    // 导入 fillMaxWidth：占满宽度
import androidx.compose.foundation.layout.height          // 导入 height：固定高度
import androidx.compose.foundation.layout.padding          // 导入 padding：内边距
import androidx.compose.foundation.layout.size            // 导入 size：固定尺寸
import androidx.compose.foundation.layout.width           // 导入 width：固定宽度
import androidx.compose.foundation.lazy.grid.GridCells     // 导入 GridCells：网格列
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid  // 导入 LazyVerticalGrid：纵向网格
import androidx.compose.foundation.lazy.grid.items         // 导入 items：网格项
import androidx.compose.foundation.rememberScrollState     // 导入 rememberScrollState：滚动状态
import androidx.compose.foundation.shape.CircleShape       // 导入 CircleShape：圆形
import androidx.compose.foundation.shape.RoundedCornerShape  // 导入 RoundedCornerShape：圆角形状
import androidx.compose.foundation.verticalScroll          // 导入 verticalScroll：纵向滚动
import kotlin.math.ceil                                   // 导入 ceil：向上取整（网格行数）
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
import androidx.compose.ui.unit.dp                        // 导入 dp：尺寸单位
import androidx.hilt.navigation.compose.hiltViewModel     // 导入 hiltViewModel：Hilt 提供 ViewModel
import androidx.lifecycle.ViewModel                       // 导入 ViewModel：UI 数据持有者
import androidx.lifecycle.viewModelScope                  // 导入 viewModelScope：协程作用域
import com.memuo.core.storage.WallpaperConfig              // 导入壁纸/主题配置
import com.memuo.core.storage.WallpaperMode                // 导入显示方式
import com.memuo.core.storage.WallpaperPrefs               // 导入壁纸偏好
import com.memuo.core.storage.WallpaperSource              // 导入来源
import com.memuo.core.ui.ThemePreset                       // 导入主题项
import com.memuo.core.ui.ThemePresets                      // 导入主题库
import com.memuo.core.ui.AppIcons                          // 导入应用图标集
import com.memuo.core.ui.components.BrandButton            // 导入品牌主按钮
import com.memuo.core.ui.components.LocalToast            // 导入 Toast 状态
import com.memuo.core.ui.components.MuyunSegmented        // 导入分段胶囊
import com.memuo.core.ui.components.SubBody               // 导入子页内容容器
import com.memuo.core.ui.components.SubHeader             // 导入子页顶栏
import com.memuo.core.ui.components.WallpaperBackground   // 导入背景渲染
import com.memuo.core.ui.components.WallpaperRenderMode   // 导入渲染方式
import com.memuo.core.ui.rememberBitmap                   // 导入位图加载
import com.memuo.core.ui.theme.MuyunBrand                 // 导入品牌色（当前主题色）
import com.memuo.core.ui.theme.MuyunCard                  // 导入卡片白
import com.memuo.core.ui.theme.MuyunText                  // 导入主文字色
import com.memuo.core.ui.theme.MuyunText2                 // 导入次级文字色
import com.memuo.core.ui.theme.MuyunText3                 // 导入三级文字色
import com.memuo.core.ui.theme.MuyunThemeState             // 导入主题状态
import dagger.hilt.android.lifecycle.HiltViewModel        // 导入 HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow            // 导入可变状态流
import kotlinx.coroutines.flow.StateFlow                  // 导入只读状态流
import kotlinx.coroutines.flow.asStateFlow                // 导入 asStateFlow
import kotlinx.coroutines.launch                           // 导入 launch：协程
import javax.inject.Inject                                // 导入 Inject：构造函数注入

/**
 * 主题页（原「自定义壁纸」重构）—— 完整主题系统：
 * 每个主题 = 背景渐变 + 强调色（accent），选中后全局背景、品牌色、按钮/导航栏同步切换。
 */
@Composable                                               // 可组合 UI 函数
fun WallpaperScreen(                                     // 主题页
    onBack: () -> Unit,                                  // 返回回调
    viewModel: WallpaperViewModel = hiltViewModel(),     // Hilt 提供 ViewModel
) {
    val config by viewModel.config.collectAsState()      // 订阅主题配置
    val toast = LocalToast.current                       // 取全局 Toast

    // 相册选择器（自定义背景：上传本地图片）
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->  // 选图结果
        uri?.let { viewModel.upload(it.toString()) }     // 上传即生效
    }

    // 当前主题（预览用）：选中主题的渐变 + 强调色
    val currentTheme = if (config.source == WallpaperSource.PRESET) ThemePresets.byId(config.presetId) ?: ThemePresets.default else ThemePresets.default  // 当前主题
    val previewBitmap = if (config.source == WallpaperSource.UPLOAD) rememberBitmap(config.imageUri?.let(Uri::parse)) else null  // 当前上传图

    Column(modifier = Modifier.fillMaxSize()) {           // 纵向布局
        SubHeader(title = "主题", onBack = onBack)         // 顶栏
        SubBody(modifier = Modifier.fillMaxSize()) {       // 内容容器
            Column(                                       // 纵向内容（可滚动，容纳 17 套主题）
                modifier = Modifier                        // 修饰
                    .fillMaxSize()                         // 铺满
                    .verticalScroll(rememberScrollState()),  // 可纵向滚动
            ) {
                // 预览框：当前主题背景 + 强调色圆点
                Box(                                     // 预览容器
                    modifier = Modifier                 // 修饰
                        .fillMaxWidth()                 // 占满宽度
                        .height(190.dp)                 // 高度 190
                        .clip(RoundedCornerShape(14.dp))  // 圆角 14
                        .background(MuyunCard),         // 白底
                ) {
                    WallpaperBackground(                 // 主题背景（复用全局同一渲染）
                        brush = if (config.source == WallpaperSource.PRESET) currentTheme.brush else null,  // 仅选中主题显示渐变；默认纯色/上传图
                        bitmap = previewBitmap,          // 上传图
                        mode = when (config.mode) {      // 映射方式
                            WallpaperMode.TILE -> WallpaperRenderMode.TILE        // 平铺
                            WallpaperMode.STRETCH -> WallpaperRenderMode.STRETCH  // 拉伸
                            WallpaperMode.CENTER -> WallpaperRenderMode.CENTER    // 居中
                            WallpaperMode.BLUR -> WallpaperRenderMode.BLUR        // 模糊
                        },
                    )
                    Column(                              // 白色占位条（模拟页面内容）
                        modifier = Modifier.align(Alignment.Center),  // 居中
                        horizontalAlignment = Alignment.CenterHorizontally,  // 水平居中
                        verticalArrangement = Arrangement.spacedBy(8.dp),  // 间距 8
                    ) {
                        listOf(0.38f, 0.58f, 0.48f).forEach { w ->  // 三种宽度
                            Box(                         // 白色占位条
                                modifier = Modifier    // 修饰
                                    .width((250 * w).dp)  // 按比例宽
                                    .height(20.dp)     // 高 20
                                    .clip(RoundedCornerShape(10.dp))  // 圆角 10
                                    .background(Color(0xC7FFFFFF)),  // 半透明白
                            )
                        }
                    }
                    // 强调色圆点（预览主题色，选中后按钮/导航栏变此色）
                    Row(                                 // 强调色指示
                        modifier = Modifier            // 修饰
                            .align(Alignment.TopStart) // 左上角
                            .padding(12.dp),           // 内边距
                        verticalAlignment = Alignment.CenterVertically,  // 垂直居中
                    ) {
                        Box(                             // 强调色圆点
                            modifier = Modifier        // 修饰
                                .size(14.dp)           // 14dp
                                .clip(CircleShape)     // 圆形
                                .background(currentTheme.accent),  // 主题强调色
                        )
                        Text(                            // 强调色说明
                            text = "主题色",             // 内容
                            style = MaterialTheme.typography.labelSmall,  // 小字
                            color = Color.White,        // 白字
                            modifier = Modifier.padding(start = 6.dp),  // 留白
                        )
                    }
                }
                Text(                                    // 显示方式标签
                    text = "显示方式",                    // 内容
                    style = MaterialTheme.typography.labelLarge,  // 字号
                    fontWeight = FontWeight.SemiBold,    // 半粗
                    color = MuyunText2,                  // 次级灰
                    modifier = Modifier.padding(top = 14.dp, bottom = 10.dp),  // 上下留白
                )
                MuyunSegmented(                          // 显示方式分段（上传图时生效）
                    labels = listOf("平铺", "拉伸", "居中", "模糊"),  // 四段
                    selectedIndex = config.mode.ordinal,  // 当前方式
                    onSelect = { viewModel.setMode(WallpaperMode.entries[it]) },  // 切换即生效
                )
                Box(                                     // 上传按钮（自定义背景）
                    modifier = Modifier                 // 修饰
                        .fillMaxWidth()                 // 占满宽度
                        .padding(top = 18.dp)           // 上留白
                        .clip(RoundedCornerShape(10.dp))  // 圆角
                        .background(MuyunCard)          // 白底
                        .clickable { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }  // 打开相册
                        .padding(vertical = 12.dp),     // 内边距
                    contentAlignment = Alignment.Center,  // 居中
                ) {
                    Text(                               // 按钮文字
                        text = "上传自定义背景",          // 内容
                        style = MaterialTheme.typography.bodyMedium,  // 字号
                        fontWeight = FontWeight.Medium,  // 中粗
                        color = MuyunText2,              // 次级灰
                    )
                }
                Text(                                    // 主题库标签
                    text = "主题 · 共 ${ThemePresets.all.size} 款",  // 内容
                    style = MaterialTheme.typography.labelLarge,  // 字号
                    fontWeight = FontWeight.SemiBold,    // 半粗
                    color = MuyunText2,                  // 次级灰
                    modifier = Modifier.padding(top = 18.dp, bottom = 10.dp),  // 上下留白
                )
                // 网格高度按主题数量动态计算（3 列，每格 64dp + 10dp 间距）
                val gridRows = ceil(ThemePresets.all.size / 3f).toInt()  // 行数（向上取整）
                LazyVerticalGrid(                        // 主题网格（3 列）
                    columns = GridCells.Fixed(3),        // 3 列
                    modifier = Modifier.height((gridRows * 64 + (gridRows - 1) * 10).dp),  // 动态高度
                    horizontalArrangement = Arrangement.spacedBy(10.dp),  // 列间距 10
                    verticalArrangement = Arrangement.spacedBy(10.dp),    // 行间距 10
                ) {
                    items(ThemePresets.all, key = { it.id }) { preset ->  // 遍历主题
                        val selected = config.source == WallpaperSource.PRESET && config.presetId == preset.id  // 是否选中
                        Box(                            // 主题色块
                            modifier = Modifier        // 修饰
                                .fillMaxWidth()        // 占满列宽
                                .height(64.dp)         // 高 64
                                .clip(RoundedCornerShape(10.dp))  // 圆角 10
                                .background(preset.brush)  // 主题渐变
                                .clickable { viewModel.selectPreset(preset.id) }  // 选中即全局生效
                                .padding(8.dp),        // 内边距
                        ) {
                            Text(                       // 主题名（左下角）
                                text = preset.name,     // 内容
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xEBFFFFFF)),  // 白字
                                fontWeight = FontWeight.Medium,  // 中粗
                                modifier = Modifier.align(Alignment.BottomStart),  // 左下角
                            )
                            Box(                        // 强调色圆点（右上角，预览主题色）
                                modifier = Modifier    // 修饰
                                    .align(Alignment.TopEnd)  // 右上角
                                    .size(14.dp)       // 14dp
                                    .clip(CircleShape) // 圆形
                                    .background(preset.accent),  // 强调色
                            )
                            if (selected) {              // 选中态
                                Icon(                   // 对勾（左上角）
                                    imageVector = AppIcons.Check,  // 图标
                                    contentDescription = null,  // 装饰
                                    tint = Color.White,  // 白
                                    modifier = Modifier.align(Alignment.TopStart).size(14.dp),  // 左上角 14dp
                                )
                            }
                        }
                    }
                }
                Row(modifier = Modifier.padding(top = 18.dp)) {  // 底部按钮行
                    Box(                                // 恢复默认
                        modifier = Modifier            // 修饰
                            .weight(1f)                // 占半
                            .clip(RoundedCornerShape(14.dp))  // 圆角
                            .background(MuyunCard)     // 白底
                            .clickable { viewModel.reset() }  // 点击恢复默认主题
                            .padding(vertical = 15.dp),  // 内边距
                        contentAlignment = Alignment.Center,  // 居中
                    ) {
                        Text(                           // 文字
                            text = "恢复默认",           // 内容
                            style = MaterialTheme.typography.titleMedium,  // 字号
                            fontWeight = FontWeight.Medium,  // 中粗
                            color = MuyunText2,         // 次级灰
                        )
                    }
                    Box(modifier = Modifier.weight(1f).padding(start = 10.dp)) {  // 应用按钮容器
                        BrandButton(                    // 应用主题
                            text = "应用主题",           // 文字
                            onClick = { viewModel.apply(); toast.show("主题已应用") },  // 应用 + Toast
                            height = 52.dp,             // 高度
                        )
                    }
                }
            }
        }
    }
}

/** 主题 ViewModel —— 配置读写 + 全局主题切换（选中即保存生效 + 更新 MuyunThemeState.theme）。 */
@HiltViewModel                                           // 注解：由 Hilt 创建
class WallpaperViewModel @Inject constructor(            // 构造函数注入
    private val prefs: WallpaperPrefs,                   // 注入壁纸偏好
) : ViewModel() {                                        // 继承 ViewModel

    private val _config = MutableStateFlow(WallpaperConfig())  // 当前配置
    val config: StateFlow<WallpaperConfig> = _config.asStateFlow()  // 只读暴露

    init {                                                // 初始化
        viewModelScope.launch {                          // 协程中收集
            prefs.config.collect { _config.value = it }  // 同步配置
        }
    }

    /** 切换显示方式（对上传背景生效）。 */
    fun setMode(mode: WallpaperMode) {                    // 切换方式
        val cfg = _config.value.copy(mode = mode)        // 复制更新
        save(cfg)                                        // 保存生效
    }

    /** 选中主题：更新全局主题状态（背景 + 品牌色同步切换）+ 持久化。 */
    fun selectPreset(id: String) {                        // 选中主题
        MuyunThemeState.theme = ThemePresets.byId(id) ?: ThemePresets.default  // 全局主题切换（即时生效）
        val cfg = _config.value.copy(source = WallpaperSource.PRESET, presetId = id, imageUri = null)  // 更新来源
        save(cfg)                                        // 保存生效
    }

    /** 上传自定义背景（上传即生效）。 */
    fun upload(uri: String) {                             // 上传图片
        val cfg = _config.value.copy(source = WallpaperSource.UPLOAD, imageUri = uri, presetId = null)  // 更新来源
        save(cfg)                                        // 保存生效
    }

    /** 应用主题（再保存一次，确保持久化）。 */
    fun apply() { save(_config.value) }                   // 再保存一次

    /** 恢复默认主题（HTML resetWallpaper）。 */
    fun reset() {                                         // 恢复默认
        MuyunThemeState.theme = ThemePresets.default      // 全局恢复默认主题
        save(WallpaperConfig())                           // 默认配置
    }

    /** 统一保存入口（DataStore 持久化）。 */
    private fun save(cfg: WallpaperConfig) {              // 保存
        _config.value = cfg                              // 立即更新 UI
        viewModelScope.launch { prefs.save(cfg) }        // 异步持久化
    }
}
