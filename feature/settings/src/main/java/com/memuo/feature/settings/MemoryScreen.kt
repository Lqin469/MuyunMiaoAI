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
import androidx.compose.foundation.shape.CircleShape       // 导入 CircleShape：圆形
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
import androidx.compose.ui.text.style.TextOverflow        // 导入 TextOverflow：溢出省略
import androidx.compose.ui.unit.dp                        // 导入 dp：尺寸单位
import androidx.hilt.navigation.compose.hiltViewModel     // 导入 hiltViewModel：Hilt 提供 ViewModel
import androidx.lifecycle.ViewModel                       // 导入 ViewModel：UI 数据持有者
import androidx.lifecycle.viewModelScope                  // 导入 viewModelScope：协程作用域
import com.memuo.core.db.dao.MemoryDao                    // 导入记忆 DAO
import com.memuo.core.db.entity.KbMemory                   // 导入记忆实体
import com.memuo.core.db.entity.MemoryType                 // 导入记忆类型枚举
import com.memuo.core.ui.AppIcons                          // 导入应用图标集
import com.memuo.core.ui.components.EmptyState            // 导入空态组件
import com.memuo.core.ui.components.LocalToast            // 导入 Toast 状态
import com.memuo.core.ui.components.MuyunSegmented        // 导入分段胶囊
import com.memuo.core.ui.components.SubBody               // 导入子页内容容器
import com.memuo.core.ui.components.SubHeader             // 导入子页顶栏
import com.memuo.core.ui.theme.MuyunAccentLight           // 导入浅灰底
import com.memuo.core.ui.theme.MuyunBrand                 // 导入品牌色
import com.memuo.core.ui.theme.MuyunBrand2                // 导入品牌青
import com.memuo.core.ui.theme.MuyunBrandGradient         // 导入品牌渐变
import com.memuo.core.ui.theme.MuyunCard                  // 导入卡片白
import com.memuo.core.ui.theme.MuyunGreen                 // 导入成功绿
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
import java.text.SimpleDateFormat                          // 导入 SimpleDateFormat：时间格式化
import java.util.Calendar                                 // 导入 Calendar：日期
import java.util.Date                                     // 导入 Date：日期
import java.util.Locale                                   // 导入 Locale：区域
import javax.inject.Inject                                // 导入 Inject：构造函数注入

/**
 * 记忆页（重新设计）—— 画像卡 + 分类统计 + 搜索 + 时间线分组列表 + 手动添加。
 * 相比旧版（只有 tabs + 列表 + 输入），新增：类型统计卡片、关键词搜索、
 * 按「今天/昨天/更早」分组、每条记忆显示类型徽章/来源/时间，提升可看性与使用价值。
 */
@Composable                                               // 可组合 UI 函数
fun MemoryScreen(                                         // 记忆页
    onBack: () -> Unit,                                   // 返回回调
    viewModel: MemoryViewModel = hiltViewModel(),         // Hilt 提供 ViewModel
) {
    val memories by viewModel.memories.collectAsState()   // 订阅记忆列表
    val portrait by viewModel.portrait.collectAsState()   // 订阅画像
    val portraitLoading by viewModel.portraitLoading.collectAsState()  // 订阅画像生成中
    val filter by viewModel.filter.collectAsState()       // 订阅类型过滤
    val addType by viewModel.addType.collectAsState()     // 订阅添加类型
    val toast = LocalToast.current                       // 取全局 Toast
    var input by remember { mutableStateOf("") }          // 输入框内容
    var query by remember { mutableStateOf("") }          // 搜索关键词

    // 过滤 + 搜索后的可见列表
    val visible = memories.filter { m ->                  // 过滤
        (filter == null || m.type == filter) &&           // 类型过滤（null=全部）
            (query.isBlank() || m.text.contains(query, true) || m.topic.contains(query, true))  // 关键词
    }

    Column(modifier = Modifier.fillMaxSize()) {           // 纵向布局
        SubHeader(title = "记忆", onBack = onBack)         // 顶栏
        SubBody(modifier = Modifier.fillMaxSize()) {       // 内容容器
            Column(modifier = Modifier.fillMaxSize()) {    // 纵向内容
                Text(                                    // 顶部说明
                    text = "对话时会提供给助理，让它更懂你",  // 文案
                    style = MaterialTheme.typography.bodySmall,  // 字号
                    color = MuyunText3,                  // 三级灰
                    modifier = Modifier.padding(bottom = 14.dp),  // 下留白
                )
                // 画像卡（HTML .mem-card：紫色底）
                PortraitCard(                             // 画像卡
                    portrait = portrait,                  // 画像文字
                    loading = portraitLoading,            // 生成中
                    onRefresh = { viewModel.refreshPortrait() },  // 刷新
                )
                // 分类统计（4 张可点统计卡，点击切换过滤）
                Row(                                      // 统计行
                    modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),  // 上下留白
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),  // 间距 8
                ) {
                    StatChip("全部", memories.size, filter == null, MuyunBrand, Modifier.weight(1f)) { viewModel.setFilter(null) }  // 全部
                    StatChip("事实", memories.count { it.type == MemoryType.FACT }, filter == MemoryType.FACT, MuyunBrand2, Modifier.weight(1f)) { viewModel.setFilter(MemoryType.FACT) }  // 事实
                    StatChip("偏好", memories.count { it.type == MemoryType.PREFERENCE }, filter == MemoryType.PREFERENCE, MuyunPurple, Modifier.weight(1f)) { viewModel.setFilter(MemoryType.PREFERENCE) }  // 偏好
                    StatChip("待办", memories.count { it.type == MemoryType.TODO }, filter == MemoryType.TODO, MuyunGreen, Modifier.weight(1f)) { viewModel.setFilter(MemoryType.TODO) }  // 待办
                }
                // 搜索框
                SearchBar(query = query, onQuery = { query = it })  // 搜索框
                // 记忆列表（空态/时间线分组）
                Box(                                      // 列表容器（占满剩余）
                    modifier = Modifier                // 修饰
                        .weight(1f)                    // 占满剩余
                        .fillMaxWidth(),               // 占满宽度
                ) {
                    if (visible.isEmpty()) {              // 空态
                        EmptyState(                       // 空态组件
                            icon = AppIcons.Memory,      // 收纳盒插图
                            text = if (query.isNotBlank()) "没有匹配「$query」的记忆" else "还没有记忆\n在下方输入，添加你希望助理记住的事",  // 文案
                        )
                    } else {                              // 列表
                        LazyColumn(                       // 懒加载列表
                            modifier = Modifier.fillMaxSize().padding(top = 6.dp),  // 上留白
                        ) {
                            // 按「今天/昨天/更早」分组（时间线）
                            groupByDate(visible).forEach { (group, list) ->  // 遍历分组
                                item(key = "head_$group") {  // 分组头
                                    Text(                // 分组标题
                                        text = group,    // 今天/昨天/更早
                                        style = MaterialTheme.typography.labelMedium,  // 小字
                                        fontWeight = FontWeight.SemiBold,  // 半粗
                                        color = MuyunText3,  // 三级灰
                                        modifier = Modifier.padding(start = 4.dp, top = 10.dp, bottom = 8.dp),  // 内边距
                                    )
                                }
                                items(list, key = { it.id }) { m ->  // 遍历记忆
                                    MemoryCard(          // 记忆卡片
                                        memory = m,      // 数据
                                        onDelete = { viewModel.delete(m.id) },  // 删除
                                    )
                                }
                            }
                        }
                    }
                }
                // 添加类型选择 + 输入行
                MuyunSegmented(                           // 添加类型分段（事实/偏好/待办）
                    labels = listOf("事实", "偏好", "待办"),  // 三段
                    selectedIndex = addType,              // 当前
                    onSelect = { viewModel.setAddType(it) },  // 切换
                    modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),  // 上下留白
                )
                Row(                                      // 输入行
                    modifier = Modifier.padding(bottom = 4.dp),  // 下留白
                    verticalAlignment = Alignment.CenterVertically,  // 垂直居中
                ) {
                    Box(                                  // 圆角输入框
                        modifier = Modifier             // 修饰
                            .weight(1f)                 // 占满剩余
                            .clip(RoundedCornerShape(22.dp))  // 大圆角
                            .background(MuyunAccentLight)  // 浅灰底
                            .padding(horizontal = 18.dp, vertical = 12.dp),  // 内边距
                    ) {
                        androidx.compose.foundation.text.BasicTextField(  // 无边框输入
                            value = input,                // 绑定
                            onValueChange = { input = it },  // 更新
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MuyunText),  // 字体
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(MuyunPurple),  // 光标
                            modifier = Modifier.fillMaxWidth(),  // 占满
                            decorationBox = { inner ->     // 占位
                                if (input.isEmpty()) {    // 空
                                    Text("添加一条记忆...", color = MuyunText3, style = MaterialTheme.typography.bodyMedium)  // 占位
                                }
                                inner()                   // 输入区
                            },
                        )
                    }
                    Box(                                  // 圆形添加按钮
                        modifier = Modifier             // 修饰
                            .padding(start = 10.dp)     // 左留白
                            .size(44.dp)                // 44dp
                            .clip(CircleShape)          // 圆形
                            .background(MuyunBrandGradient)  // 品牌渐变
                            .shadow(8.dp, CircleShape)  // 品牌投影
                            .clickable {                // 点击添加
                                if (input.isNotBlank()) {  // 非空
                                    viewModel.add(input)  // 添加记忆
                                    input = ""          // 清空
                                    toast.show("记忆已添加")  // Toast
                                }
                            },
                        contentAlignment = Alignment.Center,  // 居中
                    ) {
                        Icon(                            // 加号图标
                            imageVector = AppIcons.Plus,  // 图标
                            contentDescription = "添加",  // 描述
                            tint = Color.White,          // 白
                            modifier = Modifier.size(18.dp),  // 18dp
                        )
                    }
                }
            }
        }
    }
}

/** 画像卡（助理眼中的你 + 刷新）。 */
@Composable                                               // 可组合函数
private fun PortraitCard(                                 // 画像卡
    portrait: String,                                     // 画像文字
    loading: Boolean,                                     // 生成中
    onRefresh: () -> Unit,                                // 刷新
) {
    Column(                                               // 纵向
        modifier = Modifier                             // 修饰
            .fillMaxWidth()                             // 占满宽度
            .clip(RoundedCornerShape(14.dp))            // 圆角
            .background(MuyunPurpleBg)                  // 紫浅底
            .padding(20.dp),                            // 内边距
    ) {
        Row(                                             // 头部
            modifier = Modifier.fillMaxWidth(),          // 占满
            verticalAlignment = Alignment.CenterVertically,  // 垂直居中
        ) {
            Text(                                        // 标题
                text = "助理眼中的你",                    // 内容
                style = MaterialTheme.typography.titleSmall,  // 字号
                fontWeight = FontWeight.SemiBold,        // 半粗
                color = MuyunPurple,                     // 紫
                modifier = Modifier.weight(1f),          // 占满
            )
            Box(                                         // 刷新按钮
                modifier = Modifier                     // 修饰
                    .clip(RoundedCornerShape(14.dp))    // 胶囊
                    .background(Color(0x99FFFFFF))      // 半透明白
                    .clickable(enabled = !loading) { onRefresh() }  // 点击刷新
                    .padding(horizontal = 12.dp, vertical = 4.dp),  // 内边距
            ) {
                Text(                                    // 文字
                    text = if (loading) "生成中..." else "刷新",  // 状态
                    style = MaterialTheme.typography.bodySmall,  // 字号
                    fontWeight = FontWeight.Medium,      // 中粗
                    color = MuyunText,                   // 主色
                )
            }
        }
        Text(                                            // 画像内容
            text = portrait,                             // 画像文字
            style = MaterialTheme.typography.bodyMedium,  // 字号
            color = MuyunText2,                          // 次级灰
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4f,  // 行距
            modifier = Modifier.padding(top = 12.dp),    // 上留白
        )
    }
}

/** 分类统计卡（数字 + 标签，点击过滤）。 */
@Composable                                               // 可组合函数
private fun StatChip(                                     // 统计卡
    label: String,                                        // 标签
    count: Int,                                           // 数量
    selected: Boolean,                                    // 是否选中
    accent: Color,                                        // 强调色
    modifier: Modifier = Modifier,                        // 外部修饰（父级 Row 传入 weight 均分）
    onClick: () -> Unit,                                  // 点击
) {
    Column(                                               // 纵向
        modifier = modifier                             // 应用外部修饰
            .clip(RoundedCornerShape(12.dp))            // 圆角
            .background(if (selected) accent.copy(alpha = 0.12f) else MuyunCard)  // 选中浅色底/白
            .shadow(if (selected) 0.dp else 1.dp, RoundedCornerShape(12.dp))  // 非选中轻投影
            .clickable { onClick() }                    // 点击过滤
            .padding(vertical = 10.dp),                 // 内边距
        horizontalAlignment = Alignment.CenterHorizontally,  // 水平居中
    ) {
        Text(                                            // 数字
            text = "$count",                             // 数量
            style = MaterialTheme.typography.titleMedium,  // 字号
            fontWeight = FontWeight.Bold,                // 粗体
            color = if (selected) accent else MuyunText,  // 选中强调色/主色
        )
        Text(                                            // 标签
            text = label,                                // 标签
            style = MaterialTheme.typography.labelSmall,  // 小字
            color = if (selected) accent else MuyunText3,  // 选中强调色/三级灰
            modifier = Modifier.padding(top = 2.dp),     // 上留白
        )
    }
}

/** 搜索框（带搜索图标 + 清除）。 */
@Composable                                               // 可组合函数
private fun SearchBar(                                    // 搜索框
    query: String,                                        // 关键词
    onQuery: (String) -> Unit,                            // 更新
) {
    Box(                                                  // 搜索框容器
        modifier = Modifier                             // 修饰
            .fillMaxWidth()                             // 占满宽度
            .padding(bottom = 12.dp)                    // 下留白
            .clip(RoundedCornerShape(22.dp))            // 大圆角
            .background(MuyunCard)                      // 白底
            .padding(horizontal = 16.dp, vertical = 11.dp),  // 内边距
    ) {
        Row(                                             // 图标 + 输入
            verticalAlignment = Alignment.CenterVertically,  // 垂直居中
        ) {
            Icon(                                        // 搜索图标
                imageVector = AppIcons.Search,           // 图标
                contentDescription = null,               // 装饰
                tint = MuyunText3,                       // 三级灰
                modifier = Modifier.size(16.dp),         // 16dp
            )
            androidx.compose.foundation.text.BasicTextField(  // 无边框输入
                value = query,                            // 绑定
                onValueChange = onQuery,                  // 更新
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MuyunText),  // 字体
                cursorBrush = androidx.compose.ui.graphics.SolidColor(MuyunPurple),  // 光标
                modifier = Modifier.weight(1f).padding(start = 8.dp),  // 占满 + 留白
                decorationBox = { inner ->                // 占位
                    if (query.isEmpty()) {               // 空
                        Text("搜索记忆...", color = MuyunText3, style = MaterialTheme.typography.bodyMedium)  // 占位
                    }
                    inner()                              // 输入区
                },
            )
            if (query.isNotEmpty()) {                    // 有输入
                Icon(                                    // 清除图标
                    imageVector = AppIcons.Close,        // × 图标
                    contentDescription = "清除",          // 描述
                    tint = MuyunText3,                   // 三级灰
                    modifier = Modifier                // 修饰
                        .size(16.dp)                    // 16dp
                        .clickable { onQuery("") },     // 点击清除
                )
            }
        }
    }
}

/** 记忆卡片：类型徽章 + 内容 + 来源/时间 + 删除。 */
@Composable                                               // 可组合函数
private fun MemoryCard(                                   // 记忆卡片
    memory: KbMemory,                                     // 记忆数据
    onDelete: () -> Unit,                                 // 删除回调
) {
    val (badgeColor, badgeText) = when (memory.type) {    // 类型徽章
        MemoryType.FACT -> MuyunBrand2 to "事实"          // 青
        MemoryType.PREFERENCE -> MuyunPurple to "偏好"    // 紫
        MemoryType.TODO -> MuyunGreen to "待办"           // 绿
    }
    Row(                                                  // 横向布局
        modifier = Modifier                              // 修饰
            .fillMaxWidth()                              // 占满宽度
            .padding(bottom = 8.dp)                      // 下留白
            .shadow(1.dp, RoundedCornerShape(12.dp))     // 轻投影
            .clip(RoundedCornerShape(12.dp))             // 圆角 12
            .background(MuyunCard)                       // 白底
            .padding(horizontal = 14.dp, vertical = 12.dp),  // 内边距
        verticalAlignment = Alignment.Top,               // 顶部对齐
    ) {
        Box(                                              // 类型徽章
            modifier = Modifier                         // 修饰
                .padding(top = 2.dp)                    // 上留白
                .clip(RoundedCornerShape(6.dp))         // 小圆角
                .background(badgeColor.copy(alpha = 0.12f))  // 浅色底
                .padding(horizontal = 7.dp, vertical = 3.dp),  // 内边距
        ) {
            Text(                                         // 徽章文字
                text = badgeText,                         // 类型名
                style = MaterialTheme.typography.labelSmall,  // 小字
                fontWeight = FontWeight.SemiBold,        // 半粗
                color = badgeColor,                      // 强调色
            )
        }
        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {  // 内容区
            Text(                                         // 记忆内容
                text = memory.text,                       // 内容
                style = MaterialTheme.typography.bodyMedium,  // 字号
                color = MuyunText,                        // 主色
            )
            Row(                                          // 来源 + 时间
                modifier = Modifier.padding(top = 5.dp),  // 上留白
                verticalAlignment = Alignment.CenterVertically,  // 垂直居中
            ) {
                Text(                                     // 来源
                    text = sourceLabel(memory.source),    // 来源标签
                    style = MaterialTheme.typography.labelSmall,  // 小字
                    color = MuyunText3,                   // 三级灰
                )
                Text(                                     // 时间
                    text = " · " + fmtMemTime(memory.ts), // 时间
                    style = MaterialTheme.typography.labelSmall,  // 小字
                    color = MuyunText3,                   // 三级灰
                )
            }
        }
        Box(                                              // 删除按钮
            modifier = Modifier                         // 修饰
                .size(28.dp)                            // 28dp 热区
                .clickable { onDelete() },              // 点击删除
            contentAlignment = Alignment.Center,         // 居中
        ) {
            Icon(                                         // × 图标
                imageVector = AppIcons.Close,             // 图标
                contentDescription = "删除",               // 描述
                tint = MuyunText3,                        // 三级灰
                modifier = Modifier.size(14.dp),          // 14dp
            )
        }
    }
}

/** 记忆来源 → 中文标签。 */
private fun sourceLabel(source: String): String = when (source) {  // 来源标签
    "chat" -> "来自对话"                                  // 对话
    "memo" -> "来自笔记"                                  // 笔记
    "import" -> "来自投喂"                                // 投喂
    else -> "手动添加"                                    // 手动
}

/** 按「今天/昨天/更早」分组（时间线）。 */
private fun groupByDate(list: List<KbMemory>): List<Pair<String, List<KbMemory>>> {  // 分组
    val now = Calendar.getInstance()                      // 当前时间
    val today = Calendar.getInstance().apply {            // 今天零点
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)  // 归零
    }
    val yesterday = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }  // 昨天零点
    val sorted = list.sortedByDescending { it.ts }        // 按时间倒序
    val groups = linkedMapOf<String, MutableList<KbMemory>>()  // 有序分组
    sorted.forEach { m ->                                 // 遍历
        val key = when {                                 // 判断日期
            m.ts >= today.timeInMillis -> "今天"          // 今天
            m.ts >= yesterday.timeInMillis -> "昨天"      // 昨天
            else -> "更早"                                // 更早
        }
        groups.getOrPut(key) { mutableListOf() }.add(m)   // 加入分组
    }
    return groups.map { it.key to it.value }              // 转列表
}

/** 记忆时间格式化：今天/昨天 → HH:mm，更早 → M月d日。 */
private fun fmtMemTime(ts: Long): String {                // 时间格式化
    val now = Calendar.getInstance()                      // 当前时间
    val today = Calendar.getInstance().apply {            // 今天零点
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)  // 归零
    }
    val yesterday = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }  // 昨天零点
    val hhmm = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))  // 时分
    return when {                                         // 按时间判断
        ts >= today.timeInMillis -> hhmm                  // 今天 → 时分
        ts >= yesterday.timeInMillis -> "昨天 $hhmm"      // 昨天
        else -> SimpleDateFormat("M月d日", Locale.getDefault()).format(Date(ts))  // 更早
    }
}

/** 记忆页 ViewModel —— 列表/画像/过滤/增删。 */
@HiltViewModel                                           // 注解：由 Hilt 创建
class MemoryViewModel @Inject constructor(               // 构造函数注入
    private val memoryDao: MemoryDao,                    // 注入记忆 DAO
) : ViewModel() {                                        // 继承 ViewModel

    private val _memories = MutableStateFlow<List<KbMemory>>(emptyList())  // 记忆列表
    val memories: StateFlow<List<KbMemory>> = _memories.asStateFlow()  // 只读暴露
    private val _portrait = MutableStateFlow("还没生成画像，点「刷新」让助理总结你")  // 画像
    val portrait: StateFlow<String> = _portrait.asStateFlow()  // 只读暴露
    private val _portraitLoading = MutableStateFlow(false)  // 画像生成中
    val portraitLoading: StateFlow<Boolean> = _portraitLoading.asStateFlow()  // 只读暴露
    private val _filter = MutableStateFlow<MemoryType?>(null)  // 类型过滤（null=全部）
    val filter: StateFlow<MemoryType?> = _filter.asStateFlow()  // 只读暴露
    private val _addType = MutableStateFlow(0)           // 添加类型下标（0=事实/1=偏好/2=待办）
    val addType: StateFlow<Int> = _addType.asStateFlow()  // 只读暴露

    init {                                                // 初始化
        viewModelScope.launch {                          // 协程中观察
            memoryDao.observeRecent().collect { _memories.value = it }  // 实时记忆列表
        }
    }

    /** 设置类型过滤（null=全部）。 */
    fun setFilter(type: MemoryType?) { _filter.value = type }  // 更新过滤

    /** 设置添加类型下标。 */
    fun setAddType(index: Int) { _addType.value = index }  // 更新添加类型

    /** 手动添加记忆（按当前选中的类型）。 */
    fun add(text: String) {                               // 添加记忆
        if (text.isBlank()) return                        // 空忽略
        viewModelScope.launch {                          // 协程中写入
            val type = listOf(MemoryType.FACT, MemoryType.PREFERENCE, MemoryType.TODO)[_addType.value]  // 下标 → 类型
            memoryDao.upsert(                             // 插入记忆
                KbMemory(
                    type = type,                          // 类型
                    topic = when (type) {                 // 主题 = 类型名
                        MemoryType.FACT -> "事实"
                        MemoryType.PREFERENCE -> "偏好"
                        MemoryType.TODO -> "待办"
                    },
                    text = text,                          // 内容
                    source = "user",                      // 来源：用户手动
                    ts = System.currentTimeMillis(),      // 时间
                )
            )
        }
    }

    /** 删除记忆。 */
    fun delete(id: Long) {                                // 删除
        viewModelScope.launch { memoryDao.delete(id) }   // 物理删除
    }

    /** 刷新画像（基于本地记忆即时统计，无假延迟）。 */
    fun refreshPortrait() {                               // 刷新画像
        if (_portraitLoading.value) return                // 生成中忽略
        _portraitLoading.value = true                     // 生成中
        viewModelScope.launch {                           // 协程中即时生成
            val count = _memories.value.size              // 记忆数
            val facts = _memories.value.count { it.type == MemoryType.FACT }  // 事实数
            val prefs = _memories.value.count { it.type == MemoryType.PREFERENCE }  // 偏好数
            val todos = _memories.value.count { it.type == MemoryType.TODO }  // 待办数
            _portrait.value = if (count == 0) {           // 无记忆
                "还没有记忆。多聊几轮或手动添加后，助理会为你生成画像。"  // 空画像
            } else {                                      // 有记忆
                "你是沐云杪的活跃用户，已积累 $count 条记忆（事实 $facts 条、偏好 $prefs 条、待办 $todos 条）。" +  // 数据画像
                    "你的记录会帮助本地助理在对话中更懂你。"
            }
            _portraitLoading.value = false                // 结束
        }
    }
}
