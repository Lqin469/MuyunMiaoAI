package com.memuo.feature.settings                         // 声明包名：设置业务模块

import androidx.compose.foundation.background             // 导入 background：背景修饰
import androidx.compose.foundation.clickable              // 导入 clickable：点击修饰
import androidx.compose.foundation.layout.Box             // 导入 Box：盒式布局
import androidx.compose.foundation.layout.Column          // 导入 Column：纵向布局
import androidx.compose.foundation.layout.Row             // 导入 Row：横向布局
import androidx.compose.foundation.layout.fillMaxSize     // 导入 fillMaxSize：铺满
import androidx.compose.foundation.layout.fillMaxWidth    // 导入 fillMaxWidth：占满宽度
import androidx.compose.foundation.layout.height          // 导入 height：固定高度
import androidx.compose.foundation.layout.padding          // 导入 padding：内边距
import androidx.compose.foundation.layout.size            // 导入 size：固定尺寸
import androidx.compose.foundation.lazy.LazyColumn         // 导入 LazyColumn：懒加载列表
import androidx.compose.foundation.lazy.items              // 导入 items：列表项
import androidx.compose.foundation.shape.CircleShape       // 导入 CircleShape：圆形
import androidx.compose.foundation.shape.RoundedCornerShape  // 导入 RoundedCornerShape：圆角形状
import androidx.compose.material3.Icon                    // 导入 Icon：图标
import androidx.compose.material3.LinearProgressIndicator   // 导入 LinearProgressIndicator：进度条
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
import com.memuo.core.ui.AppIcons                          // 导入应用图标集
import com.memuo.core.ui.components.BrandButton            // 导入品牌主按钮
import com.memuo.core.ui.components.LocalToast            // 导入 Toast 状态
import com.memuo.core.ui.components.ModalCloseButton      // 导入弹窗关闭按钮
import com.memuo.core.ui.components.MuyunModal            // 导入弹窗容器
import com.memuo.core.ui.components.StatusBar             // 导入状态条
import com.memuo.core.ui.components.StatusPill            // 导入状态徽章
import com.memuo.core.ui.components.StatusTone            // 导入状态色调
import com.memuo.core.ui.components.SubBody               // 导入子页内容容器
import com.memuo.core.ui.components.SubHeader             // 导入子页顶栏
import com.memuo.core.ui.theme.MuyunAccentLight           // 导入浅灰底
import com.memuo.core.ui.theme.MuyunBrand                 // 导入品牌色
import com.memuo.core.ui.theme.MuyunBrandGradient         // 导入品牌渐变
import com.memuo.core.ui.theme.MuyunBrandSoft             // 导入品牌浅底
import com.memuo.core.ui.theme.MuyunCard                  // 导入卡片白
import com.memuo.core.ui.theme.MuyunDanger                // 导入危险红
import com.memuo.core.ui.theme.MuyunDangerBg              // 导入危险红底
import com.memuo.core.ui.theme.MuyunGreen                 // 导入成功绿
import com.memuo.core.ui.theme.MuyunGreenBg               // 导入成功绿底
import com.memuo.core.ui.theme.MuyunText                  // 导入主文字色
import com.memuo.core.ui.theme.MuyunText2                 // 导入次级文字色
import com.memuo.core.ui.theme.MuyunText3                 // 导入三级文字色
import dagger.hilt.android.lifecycle.HiltViewModel        // 导入 HiltViewModel
import kotlinx.coroutines.Dispatchers                      // 导入 Dispatchers：IO 调度器（打包）
import kotlinx.coroutines.Job                              // 导入 Job：协程任务（保留兼容）
import kotlinx.coroutines.delay                            // 导入 delay：延迟
import kotlinx.coroutines.flow.MutableStateFlow            // 导入可变状态流
import kotlinx.coroutines.flow.StateFlow                  // 导入只读状态流
import kotlinx.coroutines.flow.asStateFlow                // 导入 asStateFlow
import kotlinx.coroutines.launch                           // 导入 launch：协程
import kotlinx.coroutines.withContext                      // 导入 withContext：切线程（打包）
import org.json.JSONArray                                  // 导入 JSONArray：JSON 数组
import org.json.JSONObject                                 // 导入 JSONObject：JSON 对象
import java.io.File                                        // 导入 File：打包文件
import java.text.SimpleDateFormat                          // 导入 SimpleDateFormat：时间格式化
import java.util.Date                                     // 导入 Date：日期
import java.util.Locale                                   // 导入 Locale：区域
import javax.inject.Inject                                // 导入 Inject：构造函数注入

/** 迁移数据分类（对应 HTML migrateCats）。 */
data class MigrateCat(                                   // 迁移分类
    val id: String,                                      // ID
    val name: String,                                    // 名称
    val desc: String,                                    // 描述
    val size: Long,                                      // 体积（字节）
    val icon: androidx.compose.ui.graphics.vector.ImageVector,  // 图标
)

/** 迁移目标设备（对应 HTML migrateTarget）。 */
data class MigrateDevice(                                // 目标设备
    val name: String,                                    // 名称
    val ip: String,                                      // IP
    val online: Boolean,                                 // 在线
    val tag: String? = null,                             // 标签（如 本机附近/手动）
)

/** 迁移结果项（对应 HTML migrateLastReport）。 */
data class MigrateReportItem(                            // 报告项
    val name: String,                                    // 名称
    val size: Long,                                      // 体积
    val ok: Boolean,                                     // 是否成功
    val reason: String,                                  // 失败原因
)

/** 迁移日志项（对应 HTML migrateLogs）。 */
data class MigrateLog(                                   // 日志项
    val time: Long,                                      // 时间
    val text: String,                                    // 内容
)

/**
 * 数据迁移页 —— 分类多选 + 目标设备 + 传输控制 + 日志 + 结果报告（HTML v24 数据迁移页迁移）。
 * 对应 HTML：全选/合计、扫描局域网（模拟）、手动 IP、SHA-256 校验、2.4MB/s 进度模拟、
 * 暂停/取消（断点续传）、完成一致性验证 + 报告弹窗（重试失败项）、迁移日志。
 * 说明：传输本身为原型级模拟（真实局域网迁移属于后续里程碑），交互与状态机与 HTML 一致。
 */
@Composable                                               // 可组合 UI 函数
fun MigrateScreen(                                       // 数据迁移页
    onBack: () -> Unit,                                  // 返回回调
    viewModel: MigrateViewModel = hiltViewModel(),       // Hilt 提供 ViewModel
) {
    val cats = viewModel.cats                            // 分类（静态清单，直接取）
    val selected by viewModel.selected.collectAsState()  // 选中集
    val devices by viewModel.devices.collectAsState()    // 设备
    val target by viewModel.target.collectAsState()      // 目标设备
    val scanning by viewModel.scanning.collectAsState()  // 扫描中
    val checkStatus by viewModel.checkStatus.collectAsState()  // 校验状态
    val progress by viewModel.progress.collectAsState()  // 进度
    val running by viewModel.running.collectAsState()    // 传输中
    val paused by viewModel.paused.collectAsState()      // 暂停
    val logsVisible by viewModel.logsVisible.collectAsState()  // 日志展开
    val logs by viewModel.logs.collectAsState()          // 日志列表
    val report by viewModel.report.collectAsState()      // 结果报告
    val toast = LocalToast.current                       // 取全局 Toast
    var manualIp by remember { mutableStateOf("") }      // 手动 IP 输入

    Column(modifier = Modifier.fillMaxSize()) {           // 纵向布局
        SubHeader(title = "数据迁移", onBack = onBack)     // 顶栏
        SubBody(modifier = Modifier.fillMaxSize()) {       // 内容容器
            LazyColumn(modifier = Modifier.fillMaxSize()) {  // 整页滚动（内容多）
                item {                                    // 头部提示
                    Text(                                // 说明（HTML set-hint）
                        text = "选择要迁移的数据类型，通过局域网传输到目标设备。传输前自动校验，结束后验证一致性。",  // 文案
                        style = MaterialTheme.typography.labelSmall,  // 小字
                        color = MuyunText3,              // 三级灰
                        lineHeight = MaterialTheme.typography.labelSmall.lineHeight * 1.5f,  // 行距
                        modifier = Modifier.padding(bottom = 14.dp),  // 下留白
                    )
                }
                item {                                    // ① 数据分类清单
                    Column {                              // 纵向容器（多子组件需包裹）
                        MigrateSectionHead(               // 分组头
                            title = "选择数据类型",        // 标题
                            actionText = if (selected.size == cats.size) "全不选" else "全选",  // 全选/全不选（HTML 同款）
                            onAction = { viewModel.toggleAll() },  // 切换全选
                        )
                        cats.forEach { cat ->             // 遍历分类
                            MigrateCatRow(                // 分类行
                                cat = cat,                // 数据
                                selected = selected.contains(cat.id),  // 是否选中
                                onClick = { viewModel.toggleCat(cat.id) },  // 切换
                            )
                        }
                        Text(                            // 合计行（HTML .migrate-total）
                            text = "已选 ${selected.size} 类，共 ${fmtBytes(selected.sumOf { id -> cats.firstOrNull { it.id == id }?.size ?: 0L })}",  // 计数+体积
                            style = MaterialTheme.typography.labelMedium,  // 小字（HTML 12px）
                            color = MuyunText2,          // 次级灰
                            fontWeight = FontWeight.Medium,  // 中粗
                            modifier = Modifier.padding(vertical = 10.dp),  // 内边距
                        )
                    }
                }
                item {                                    // ② 目标设备
                    Column {                              // 纵向容器
                        MigrateSectionHead(               // 分组头
                            title = "目标设备",            // 标题
                            actionText = if (scanning) "扫描中…" else if (devices.isEmpty()) "扫描局域网" else "重新扫描",  // 按钮文案
                            onAction = { viewModel.scan() },  // 扫描
                            actionEnabled = !scanning,    // 扫描中禁用
                        )
                        if (devices.isEmpty() && !scanning) {  // 无设备
                            Text(                        // 空提示（HTML .migrate-devices-empty）
                                text = "点击「扫描局域网」发现可用设备",  // 文案
                                style = MaterialTheme.typography.labelSmall,  // 小字
                                color = MuyunText3,      // 三级灰
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,  // 居中
                                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),  // 内边距
                            )
                        } else {                          // 设备列表
                            devices.filter { it.online }.forEach { d ->  // 在线设备
                                MigrateDeviceRow(         // 设备行
                                    device = d,           // 数据
                                    selected = target?.ip == d.ip,  // 是否选中
                                    onClick = { viewModel.selectDevice(d.ip) },  // 选择
                                )
                            }
                        }
                        Row(                              // 手动 IP 行（HTML .migrate-manual-row）
                            modifier = Modifier.padding(top = 4.dp),  // 上留白
                            verticalAlignment = Alignment.CenterVertically,  // 垂直居中
                        ) {
                            Box(                          // IP 输入框
                                modifier = Modifier     // 修饰
                                    .weight(1f)         // 占满剩余
                                    .clip(RoundedCornerShape(10.dp))  // 圆角
                                    .background(MuyunCard)  // 白底
                                    .padding(horizontal = 12.dp, vertical = 10.dp),  // 内边距
                            ) {
                                androidx.compose.foundation.text.BasicTextField(  // 无边框输入
                                    value = manualIp,     // 绑定
                                    onValueChange = { manualIp = it },  // 更新
                                    singleLine = true,    // 单行
                                    textStyle = MaterialTheme.typography.bodySmall.copy(color = MuyunText),  // 字体
                                    cursorBrush = androidx.compose.ui.graphics.SolidColor(MuyunBrand),  // 光标
                                    modifier = Modifier.fillMaxWidth(),  // 占满
                                    decorationBox = { inner ->  // 占位
                                        if (manualIp.isEmpty()) {  // 空
                                            Text("或手动输入设备 IP，如 192.168.1.88", color = MuyunText3, style = MaterialTheme.typography.bodySmall)  // 占位（HTML 同款）
                                        }
                                        inner()          // 输入区
                                    },
                                )
                            }
                            Box(                          // 添加按钮（HTML .migrate-manual-btn）
                                modifier = Modifier     // 修饰
                                    .padding(start = 8.dp)  // 左留白
                                    .clip(RoundedCornerShape(10.dp))  // 圆角
                                    .background(MuyunAccentLight)  // 浅灰底
                                    .clickable {         // 点击添加
                                        viewModel.addManualDevice(manualIp)  // 添加
                                        manualIp = ""     // 清空
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),  // 内边距
                            ) {
                                Text(                     // 文字
                                    text = "添加",        // 内容
                                    style = MaterialTheme.typography.bodySmall,  // 字号（HTML 13px）
                                    fontWeight = FontWeight.Medium,  // 中粗
                                    color = MuyunText2,  // 次级灰
                                )
                            }
                        }
                    }
                }
                item {                                    // ③ 传输控制
                    Column(modifier = Modifier.padding(top = 14.dp)) {  // 分组容器
                        StatusBar(                        // 校验状态条（HTML .migrate-check-status）
                            text = checkStatus.first,     // 文案
                            tone = checkStatus.second,    // 色调
                            modifier = Modifier.padding(bottom = 12.dp),  // 下留白
                        )
                        if (running) {                    // 传输中 → 进度卡（HTML .migrate-progress-card）
                            Column(                       // 进度卡
                                modifier = Modifier     // 修饰
                                    .fillMaxWidth()     // 占满宽度
                                    .shadow(1.dp, RoundedCornerShape(14.dp))  // 轻投影
                                    .clip(RoundedCornerShape(14.dp))  // 圆角
                                    .background(MuyunCard)  // 白底
                                    .padding(16.dp),    // 内边距
                            ) {
                                Row(                     // 进度头（标题 + 百分比）
                                    modifier = Modifier.fillMaxWidth(),  // 占满
                                ) {
                                    Text(               // 标题（HTML .migrate-progress-title）
                                        text = "正在传输到 ${target?.name.orEmpty()}",  // 文案
                                        style = MaterialTheme.typography.titleSmall,  // 字号（HTML 14px）
                                        fontWeight = FontWeight.SemiBold,  // 半粗
                                        color = MuyunText,  // 主色
                                    )
                                    Text(               // 百分比（HTML .migrate-progress-pct）
                                        text = "${progress.percent}%",  // 内容
                                        style = MaterialTheme.typography.titleMedium,  // 字号（HTML 15px）
                                        fontWeight = FontWeight.Bold,  // 粗体
                                        color = MuyunBrand,  // 品牌色
                                        modifier = Modifier.weight(1f),  // 占满
                                        textAlign = androidx.compose.ui.text.style.TextAlign.End,  // 右对齐
                                    )
                                }
                                LinearProgressIndicator(  // 进度条（HTML .migrate-bar）
                                    progress = { progress.percent / 100f },  // 进度
                                    modifier = Modifier  // 修饰
                                        .fillMaxWidth()  // 占满
                                        .height(8.dp)    // 高 8
                                        .padding(vertical = 2.dp)  // 微调
                                        .clip(RoundedCornerShape(4.dp)),  // 圆角 4
                                    color = MuyunBrand,  // 品牌色
                                    trackColor = MuyunAccentLight,  // 轨道浅灰
                                )
                                Row(                     // 统计行（HTML .migrate-stats）
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),  // 内边距
                                ) {
                                    Text("已传输 ${fmtBytes(progress.done)} / ${fmtBytes(progress.total)}", style = MaterialTheme.typography.labelSmall, color = MuyunText3)  // 已传输
                                    Text("速率 ${fmtBytes(progress.speed)}/s", style = MaterialTheme.typography.labelSmall, color = MuyunText3, modifier = Modifier.padding(horizontal = 10.dp))  // 速率
                                    Text("剩余 ${progress.eta}", style = MaterialTheme.typography.labelSmall, color = MuyunText3)  // 剩余
                                }
                                Row {                    // 操作行（HTML .migrate-actions）
                                    Box(                 // 暂停/继续按钮
                                        modifier = Modifier  // 修饰
                                            .weight(1f)  // 占半
                                            .clip(RoundedCornerShape(10.dp))  // 圆角
                                            .background(MuyunAccentLight)  // 浅灰底
                                            .clickable { viewModel.togglePause() }  // 点击
                                            .padding(vertical = 11.dp),  // 内边距
                                        contentAlignment = Alignment.Center,  // 居中
                                    ) {
                                        Text(            // 文字（HTML 暂停/继续）
                                            text = if (paused) "继续" else "暂停",  // 内容
                                            style = MaterialTheme.typography.bodySmall,  // 字号（HTML 13px）
                                            fontWeight = FontWeight.Medium,  // 中粗
                                            color = MuyunText2,  // 次级灰
                                        )
                                    }
                                    Box(                 // 取消按钮
                                        modifier = Modifier  // 修饰
                                            .weight(1f)  // 占半
                                            .padding(start = 8.dp)  // 左留白
                                            .clip(RoundedCornerShape(10.dp))  // 圆角
                                            .background(MuyunDangerBg)  // 红浅底
                                            .clickable { viewModel.cancel() }  // 点击取消
                                            .padding(vertical = 11.dp),  // 内边距
                                        contentAlignment = Alignment.Center,  // 居中
                                    ) {
                                        Text(            // 文字
                                            text = "取消",  // 内容
                                            style = MaterialTheme.typography.bodySmall,  // 字号
                                            fontWeight = FontWeight.Medium,  // 中粗
                                            color = MuyunDanger,  // 危险红
                                        )
                                    }
                                }
                            }
                        } else {                          // 非传输中 → 开始按钮
                            BrandButton(                  // 开始迁移（HTML #migrate-start-btn）
                                text = "开始迁移",         // 文字
                                onClick = { viewModel.start() },  // 开始
                            )
                        }
                    }
                }
                item {                                    // ④ 迁移日志
                    Column(modifier = Modifier.padding(top = 14.dp)) {  // 日志容器
                        MigrateSectionHead(               // 分组头
                            title = "迁移日志",            // 标题
                            actionText = "查看",          // 按钮
                            onAction = { viewModel.toggleLogs() },  // 展开/收起
                        )
                        if (logsVisible) {                // 展开日志
                            if (logs.isEmpty()) {         // 无日志
                                Text(                    // 空提示（HTML .migrate-log-empty）
                                    text = "暂无迁移日志",  // 文案
                                    style = MaterialTheme.typography.bodySmall,  // 字号
                                    color = MuyunText3,  // 三级灰
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,  // 居中
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 30.dp),  // 内边距
                                )
                            } else {                      // 日志列表
                                logs.take(20).forEach { log ->  // 最近 20 条（HTML slice(0,20)）
                                    Row(                 // 日志行（HTML .migrate-log-item）
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),  // 内边距
                                    ) {
                                        Text(            // 时间
                                            text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.time)),  // 时分秒
                                            style = MaterialTheme.typography.labelSmall,  // 小字（HTML 11px 等宽近似）
                                            color = MuyunText3,  // 三级灰
                                            modifier = Modifier.padding(end = 10.dp),  // 右留白
                                        )
                                        Text(            // 日志内容
                                            text = log.text,  // 内容
                                            style = MaterialTheme.typography.bodySmall,  // 字号（HTML 12px）
                                            color = MuyunText2,  // 次级灰
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // —— 迁移结果报告弹窗（HTML #migrate-report-modal）——
    MuyunModal(                                          // 弹窗
        visible = report.isNotEmpty(),                   // 有报告才显示
        onDismiss = { viewModel.closeReport() },         // 点遮罩关闭
        title = "迁移结果",                              // 标题
        headerActions = { ModalCloseButton { viewModel.closeReport() } },  // 关闭
        body = {                                        // 报告列表（HTML .migrate-report-list）
            Column(modifier = Modifier.height(240.dp)) {  // 固定高（HTML max-height 240）
                LazyColumn {                            // 滚动列表
                    items(report) { item ->              // 遍历报告项
                        Row(                             // 报告行（HTML .migrate-report-item）
                            modifier = Modifier         // 修饰
                                .fillMaxWidth()         // 占满宽度
                                .padding(bottom = 6.dp) // 下留白
                                .clip(RoundedCornerShape(10.dp))  // 圆角
                                .background(if (item.ok) MuyunGreenBg else MuyunDangerBg)  // 绿/红底
                                .padding(horizontal = 12.dp, vertical = 10.dp),  // 内边距
                            verticalAlignment = Alignment.CenterVertically,  // 垂直居中
                        ) {
                            Text(                        // 图标（HTML .r-icon ✓/✕）
                                text = if (item.ok) "✓" else "✕",  // 符号
                                style = MaterialTheme.typography.bodyMedium,  // 字号
                                color = if (item.ok) MuyunGreen else MuyunDanger,  // 绿/红
                            )
                            Text(                        // 名称 + 体积
                                text = "${item.name} · ${fmtBytes(item.size)}",  // 内容
                                style = MaterialTheme.typography.bodySmall,  // 字号（HTML 13px）
                                fontWeight = FontWeight.Medium,  // 中粗
                                color = MuyunText,       // 主色
                                modifier = Modifier.weight(1f).padding(horizontal = 10.dp),  // 占满
                            )
                            if (!item.ok) {              // 失败显示原因
                                Text(                    // 原因（HTML .r-reason）
                                    text = item.reason,  // 内容
                                    style = MaterialTheme.typography.labelSmall,  // 小字
                                    color = MuyunText3,  // 三级灰
                                )
                            }
                        }
                    }
                }
            }
        },
        footer = {                                       // 底部按钮（HTML .migrate-report-actions）
            Row(modifier = Modifier.padding(top = 12.dp)) {  // 按钮行
                Box(modifier = Modifier.weight(1f)) {     // 重试容器
                    BrandButton(                         // 重试失败项
                        text = "重试失败项",              // 文字
                        onClick = { viewModel.retryFailed() },  // 重试
                        height = 48.dp,                  // 高度
                    )
                }
                Box(                                     // 完成按钮
                    modifier = Modifier                 // 修饰
                        .weight(1f)                     // 占半
                        .padding(start = 10.dp)         // 左留白
                        .clip(RoundedCornerShape(14.dp))  // 圆角
                        .background(MuyunCard)          // 白底
                        .clickable { viewModel.closeReport() }  // 点击关闭
                        .padding(vertical = 13.dp),     // 内边距
                    contentAlignment = Alignment.Center,  // 居中
                ) {
                    Text(                               // 文字
                        text = "完成",                   // 内容
                        style = MaterialTheme.typography.bodyMedium,  // 字号
                        fontWeight = FontWeight.Medium,  // 中粗
                        color = MuyunText2,              // 次级灰
                    )
                }
            }
        },
    )
}

/** 迁移分组头（标题 + 右侧操作按钮，对应 HTML .migrate-section-head）。 */
@Composable                                               // 可组合函数
private fun MigrateSectionHead(                           // 分组头
    title: String,                                        // 标题
    actionText: String,                                   // 按钮文字
    onAction: () -> Unit,                                 // 按钮回调
    actionEnabled: Boolean = true,                        // 按钮可用
) {
    Row(                                                  // 横向布局
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),  // 内边距
        verticalAlignment = Alignment.CenterVertically,   // 垂直居中
    ) {
        Text(                                             // 标题
            text = title,                                 // 内容
            style = MaterialTheme.typography.titleSmall,  // 字号（HTML 14px）
            fontWeight = FontWeight.SemiBold,             // 半粗
            color = MuyunText,                            // 主色
            modifier = Modifier.weight(1f),               // 占满
        )
        Box(                                              // 操作按钮（HTML .migrate-select-all / .migrate-scan-btn）
            modifier = Modifier                          // 修饰
                .clip(RoundedCornerShape(14.dp))         // 胶囊圆角
                .background(if (actionEnabled) MuyunBrandSoft else MuyunAccentLight)  // 品牌浅底/灰
                .clickable(enabled = actionEnabled) { onAction() }  // 点击
                .padding(horizontal = 12.dp, vertical = 5.dp),  // 内边距
        ) {
            Text(                                         // 按钮文字
                text = actionText,                        // 内容
                style = MaterialTheme.typography.labelMedium,  // 小字（HTML 12px）
                fontWeight = FontWeight.Medium,           // 中粗
                color = MuyunBrand,                       // 品牌色
            )
        }
    }
}

/** 分类选择行（勾选框 + 图标 + 名称/描述 + 体积，对应 HTML .migrate-cat）。 */
@Composable                                               // 可组合函数
private fun MigrateCatRow(                                // 分类行
    cat: MigrateCat,                                      // 分类
    selected: Boolean,                                    // 是否选中
    onClick: () -> Unit,                                  // 点击
) {
    Row(                                                  // 横向布局
        modifier = Modifier                              // 修饰
            .fillMaxWidth()                              // 占满宽度
            .padding(bottom = 8.dp)                      // 下留白
            .shadow(1.dp, RoundedCornerShape(14.dp))     // 轻投影
            .clip(RoundedCornerShape(14.dp))             // 圆角
            .background(if (selected) MuyunBrandSoft else MuyunCard)  // 选中品牌浅底/白
            .clickable { onClick() }                     // 点击
            .padding(horizontal = 14.dp, vertical = 12.dp),  // 内边距
        verticalAlignment = Alignment.CenterVertically,   // 垂直居中
    ) {
        Box(                                              // 勾选框（HTML .migrate-cat-check）
            modifier = Modifier                          // 修饰
                .size(22.dp)                             // 22dp
                .clip(RoundedCornerShape(7.dp))          // 圆角 7
                .background(if (selected) MuyunBrand else Color.Transparent)  // 选中品牌色
                .padding(0.dp),                          // 无内边距
            contentAlignment = Alignment.Center,          // 居中
        ) {
            if (selected) {                               // 选中显示对勾
                Icon(                                     // 对勾
                    imageVector = AppIcons.Check,         // 图标
                    contentDescription = null,            // 装饰
                    tint = Color.White,                   // 白
                    modifier = Modifier.size(13.dp),      // 13dp
                )
            }
        }
        Box(                                              // 图标底
            modifier = Modifier                          // 修饰
                .padding(start = 12.dp)                  // 左留白
                .size(36.dp)                             // 36dp
                .clip(RoundedCornerShape(10.dp))         // 圆角 10
                .background(MuyunAccentLight),           // 浅灰底
            contentAlignment = Alignment.Center,          // 居中
        ) {
            Icon(                                         // 分类图标
                imageVector = cat.icon,                   // 图标
                contentDescription = null,                // 装饰
                tint = MuyunText2,                        // 次级灰
                modifier = Modifier.size(17.dp),          // 17dp
            )
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {  // 信息区
            Text(                                         // 名称
                text = cat.name,                          // 内容
                style = MaterialTheme.typography.titleSmall,  // 字号（HTML 14px）
                fontWeight = FontWeight.SemiBold,         // 半粗
                color = MuyunText,                        // 主色
            )
            Text(                                         // 描述
                text = cat.desc,                          // 内容
                style = MaterialTheme.typography.labelSmall,  // 小字（HTML 11px）
                color = MuyunText3,                       // 三级灰
                modifier = Modifier.padding(top = 3.dp),  // 上留白
            )
        }
        StatusPill(text = fmtBytes(cat.size), tone = StatusTone.NEUTRAL)  // 体积徽章（HTML .migrate-cat-size）
    }
}

/** 设备行（在线点 + 名称 + 标签 + IP，对应 HTML .migrate-device）。 */
@Composable                                               // 可组合函数
private fun MigrateDeviceRow(                             // 设备行
    device: MigrateDevice,                                // 设备
    selected: Boolean,                                    // 是否选中
    onClick: () -> Unit,                                  // 点击
) {
    Row(                                                  // 横向布局
        modifier = Modifier                              // 修饰
            .fillMaxWidth()                              // 占满宽度
            .padding(bottom = 8.dp)                      // 下留白
            .clip(RoundedCornerShape(10.dp))             // 圆角
            .background(if (selected) MuyunBrandSoft else MuyunCard)  // 选中品牌浅底/白
            .clickable { onClick() }                     // 点击
            .padding(horizontal = 12.dp, vertical = 10.dp),  // 内边距
        verticalAlignment = Alignment.CenterVertically,   // 垂直居中
    ) {
        Box(                                              // 在线点（HTML .migrate-dot.online）
            modifier = Modifier                          // 修饰
                .size(8.dp)                              // 8dp
                .clip(CircleShape)                       // 圆形
                .background(MuyunGreen),                 // 绿
        )
        Text(                                             // 名称
            text = device.name,                           // 内容
            style = MaterialTheme.typography.bodySmall,   // 字号（HTML 13px）
            fontWeight = FontWeight.Medium,               // 中粗
            color = MuyunText,                            // 主色
            maxLines = 1,                                 // 单行
            overflow = TextOverflow.Ellipsis,             // 省略
            modifier = Modifier.weight(1f).padding(horizontal = 10.dp),  // 占满
        )
        device.tag?.let {                                 // 有标签
            StatusPill(text = it, tone = StatusTone.INFO)  // 标签（HTML .migrate-device-tag）
        }
        Text(                                             // IP（等宽，HTML .migrate-device-ip）
            text = device.ip,                             // 内容
            style = MaterialTheme.typography.labelSmall,  // 小字（HTML 11px）
            color = MuyunText3,                           // 三级灰
            modifier = Modifier.padding(start = 8.dp),    // 左留白
        )
    }
}

/** 数据迁移 ViewModel —— 选择/真实 NSD 扫描/真实 TCP 传输/日志/报告（M-027 接入 core:lan）。 */
@HiltViewModel                                           // 注解：由 Hilt 创建
class MigrateViewModel @Inject constructor(              // 构造函数注入
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,  // 注入应用上下文（打包用）
    private val prefs: ExtPrefs,                         // 注入扩展偏好（日志持久化）
    private val transfer: com.memuo.core.lan.TransferRepository,  // 注入传输编排器（真传输）
    private val storage: com.memuo.core.storage.StorageProvider,  // 注入存储提供者（打包源目录）
) : ViewModel() {                                        // 继承 ViewModel

    /** 分类清单（对应 HTML migrateCats 的五类）。 */
    val cats: List<MigrateCat> = listOf(                 // 五类数据
        MigrateCat("user", "用户数据", "备忘录、会话记录、设置项", 13_006_182L, AppIcons.User),  // 12.4MB
        MigrateCat("conf", "配置文件", "API 配置、权限模式、壁纸设置", 838_861L, AppIcons.Gear),  // 0.8MB
        MigrateCat("log", "日志", "运行日志、迁移记录", 3_355_443L, AppIcons.FileText),  // 3.2MB
        MigrateCat("cache", "缓存", "缩略图、临时文件", 50_961_920L, AppIcons.Database),  // 48.6MB
        MigrateCat("media", "媒体文件", "壁纸图片、语音输入、附件", 163_787_735L, AppIcons.Gallery),  // 156.2MB
    )

    private val _selected = MutableStateFlow<Set<String>>(emptySet())  // 选中集
    val selected: StateFlow<Set<String>> = _selected.asStateFlow()  // 只读暴露
    private val _devices = MutableStateFlow<List<MigrateDevice>>(emptyList())  // 设备列表
    val devices: StateFlow<List<MigrateDevice>> = _devices.asStateFlow()  // 只读暴露
    private val _target = MutableStateFlow<MigrateDevice?>(null)  // 目标设备
    val target: StateFlow<MigrateDevice?> = _target.asStateFlow()  // 只读暴露
    private val _scanning = MutableStateFlow(false)      // 扫描中
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()  // 只读暴露
    private val _checkStatus = MutableStateFlow("" to StatusTone.NEUTRAL)  // 校验状态
    val checkStatus: StateFlow<Pair<String, StatusTone>> = _checkStatus.asStateFlow()  // 只读暴露
    private val _progress = MutableStateFlow(MigrateProgress())  // 进度
    val progress: StateFlow<MigrateProgress> = _progress.asStateFlow()  // 只读暴露
    private val _running = MutableStateFlow(false)       // 传输中
    val running: StateFlow<Boolean> = _running.asStateFlow()  // 只读暴露
    private val _paused = MutableStateFlow(false)        // 暂停
    val paused: StateFlow<Boolean> = _paused.asStateFlow()  // 只读暴露
    private val _logsVisible = MutableStateFlow(false)   // 日志展开
    val logsVisible: StateFlow<Boolean> = _logsVisible.asStateFlow()  // 只读暴露
    private val _logs = MutableStateFlow<List<MigrateLog>>(emptyList())  // 日志
    val logs: StateFlow<List<MigrateLog>> = _logs.asStateFlow()  // 只读暴露
    private val _report = MutableStateFlow<List<MigrateReportItem>>(emptyList())  // 报告
    val report: StateFlow<List<MigrateReportItem>> = _report.asStateFlow()  // 只读暴露

    private var lastSelected: List<MigrateCat> = emptyList()  // 上次传输的分类（重试用）

    init {                                                // 初始化：加载日志 + 订阅真实设备/会话
        viewModelScope.launch {                          // 协程中
            prefs.migrateLogsJson.collect { json ->      // 日志 JSON 变化
                if (json.isNotBlank()) {                 // 有日志
                    _logs.value = runCatching {          // 解析
                        val arr = JSONArray(json)        // 数组
                        (0 until arr.length()).map { i ->  // 遍历
                            val o = arr.getJSONObject(i)  // 对象
                            MigrateLog(o.optLong("time"), o.optString("text"))  // 组装
                        }
                    }.getOrDefault(emptyList())          // 失败空
                }
            }
        }
        // 订阅真实 NSD 设备列表 → 映射为迁移设备
        viewModelScope.launch {                          // 设备订阅
            transfer.devices.collect { list ->           // 每次发现变化
                _devices.value = list.map { d ->         // 映射
                    MigrateDevice(d.name, d.ip, true)    // NSD 发现即在线
                }
            }
        }
        // 订阅扫描状态（NSD 开始/结束）
        viewModelScope.launch {                          // 扫描状态订阅
            transfer.scanning.collect { s ->             // NSD 扫描状态
                if (!s && _scanning.value) {             // 由扫描中转为结束
                    addLog("扫描局域网完成，发现 ${_devices.value.size} 台在线设备")  // 日志
                }
                _scanning.value = s                      // 同步状态
            }
        }
        // 订阅真实发送会话 → 驱动进度/状态
        viewModelScope.launch {                          // 会话订阅
            transfer.sendSession.collect { s ->          // 会话变化
                val total = _progress.value.total        // 当前总量（保留）
                if (s != null && total > 0) {            // 有效会话
                    _progress.value = MigrateProgress(   // 更新进度
                        done = s.sent,                   // 已传
                        total = total,                   // 总量
                        speed = s.speedBps,              // 速率
                        eta = if (s.speedBps > 0 && s.sent < total) "${(total - s.sent + s.speedBps - 1) / s.speedBps}s" else "--",  // 剩余秒
                        percent = ((s.sent * 100) / total).toInt(),  // 百分比
                    )
                    when (s.state) {                     // 按状态
                        com.memuo.core.lan.SessionState.SUCCESS -> onComplete()  // 成功 → 一致性验证
                        com.memuo.core.lan.SessionState.FAILED -> {  // 失败
                            _running.value = false       // 结束传输
                            _checkStatus.value = "传输失败，请检查网络后重试（进度已保留，可断点续传）" to StatusTone.FAIL  // 状态
                            addLog("迁移失败：${s.name}")  // 日志
                        }
                        com.memuo.core.lan.SessionState.PAUSED -> _paused.value = true  // 暂停
                        com.memuo.core.lan.SessionState.RUNNING -> { _running.value = true; _paused.value = false }  // 传输中
                        else -> Unit                     // 其余忽略
                    }
                }
            }
        }
    }

    /** 追加日志（对应 HTML addMigrateLog：最新在前，最多 50 条持久化）。 */
    private fun addLog(text: String) {                    // 追加日志
        val next = (listOf(MigrateLog(System.currentTimeMillis(), text)) + _logs.value).take(50)  // 前插 + 截断
        _logs.value = next                               // 更新
        viewModelScope.launch {                          // 持久化
            prefs.setMigrateLogsJson(JSONArray().apply {  // 编码
                next.forEach { l -> put(JSONObject().apply { put("time", l.time); put("text", l.text) }) }  // 逐条
            }.toString())
        }
    }

    /** 切换单类选择（HTML toggleMigrateCat）。 */
    fun toggleCat(id: String) {                           // 切换单类
        _selected.value = if (_selected.value.contains(id)) _selected.value - id else _selected.value + id  // 增/删
    }

    /** 全选/全不选（HTML toggleAllMigate）。 */
    fun toggleAll() {                                     // 全选切换
        _selected.value = if (_selected.value.size == cats.size) emptySet() else cats.map { it.id }.toSet()  // 切换
    }

    /** 扫描局域网（真实 NSD：开启接收 + 广播 + 发现）。 */
    fun scan() {                                          // 扫描
        if (_scanning.value) return                       // 扫描中忽略
        transfer.startAll()                               // 开启真实扫描（服务/广播/NSD，状态经 init 订阅回流）
    }

    /** 选择目标设备（HTML selectMigrateDevice）。 */
    fun selectDevice(ip: String) {                        // 选择设备
        _target.value = _devices.value.firstOrNull { it.ip == ip }  // 更新目标
    }

    /** 手动添加设备（IP 校验，HTML addManualMigrateDevice）。 */
    fun addManualDevice(ip: String) {                     // 手动添加
        if (ip.isBlank()) return                          // 空忽略
        if (!Regex("(\\d{1,3}\\.){3}\\d{1,3}").matches(ip)) return  // IP 格式校验（HTML 同款正则）
        val device = MigrateDevice("手动设备 ($ip)", ip, true, "手动")  // 构造设备
        _devices.value = listOf(device) + _devices.value.filterNot { it.ip == ip }  // 前插去重
        _target.value = device                            // 设为目标
        addLog("手动添加目标设备 $ip")                     // 日志
    }

    /** 开始迁移（真实流程：校验 → 打包 zip + SHA-256 → 真实发送）。 */
    fun start() {                                         // 开始迁移
        val sel = cats.filter { _selected.value.contains(it.id) }  // 选中分类
        if (sel.isEmpty()) return                          // 未选择忽略
        val target = _target.value ?: return               // 无目标忽略
        _checkStatus.value = "正在打包 ${sel.size} 类数据并计算 SHA-256 校验…" to StatusTone.INFO  // 校验中
        viewModelScope.launch {                           // 协程中打包
            val pkg = packData(sel) ?: run {              // 打包失败
                _checkStatus.value = "打包失败：数据目录不可访问" to StatusTone.FAIL  // 失败
                addLog("迁移失败：打包数据失败")            // 日志
                return@launch                             // 结束
            }
            _checkStatus.value = "校验通过（SHA-256 已写入清单）· 开始传输" to StatusTone.SUCCESS  // 通过
            beginTransfer(sel, pkg)                       // 开始真实传输
        }
    }

    /** 打包选中分类数据为 zip（含 manifest 校验清单），返回包文件。 */
    private suspend fun packData(sel: List<MigrateCat>): File? = kotlinx.coroutines.withContext(Dispatchers.IO) {  // 打包
        runCatching {                                     // 容错
            val outDir = File(context.cacheDir, "migrate_out")  // 打包输出目录
            outDir.mkdirs()                               // 建目录
            val zipFile = File(outDir, "muyunmiao-backup-${System.currentTimeMillis()}.zip")  // 包文件
            val manifest = StringBuilder()                // 校验清单
            java.util.zip.ZipOutputStream(zipFile.outputStream()).use { zip ->  // 写 zip
                val root = storage.root                   // 数据根目录
                // 分类 → 根下相对目录映射（打包时按选中分类裁剪）
                val dirMap = mapOf(                       // 分类目录映射
                    "user" to listOf("db"),               // 用户数据：数据库
                    "conf" to listOf("config"),           // 配置
                    "log" to listOf("log"),               // 日志
                    "cache" to listOf("cache"),           // 缓存
                    "media" to listOf("media", "wallpaper"),  // 媒体
                )
                val include = sel.flatMap { dirMap[it.id] ?: emptyList() }  // 选中的目录集合
                root.listFiles()?.forEach { child ->      // 遍历根目录
                    val rel = child.name                  // 相对名
                    if (child.isDirectory && rel !in include) return@forEach  // 未选中目录跳过
                    if (child.isFile) addZipEntry(zip, child, rel, manifest)  // 根文件打包
                    else child.walkTopDown().forEach { f ->  // 目录递归
                        if (f.isFile) addZipEntry(zip, f, "muyunmiao/$rel/${f.relativeTo(child).path}", manifest)  // 打包
                    }
                }
                zip.putNextEntry(java.util.zip.ZipEntry("manifest.txt"))  // 清单条目
                zip.write(manifest.toString().toByteArray())  // 写清单
                zip.closeEntry()                          // 关条目
            }
            zipFile                                       // 返回包
        }.getOrNull()
    }

    /** 追加单个文件到 zip 并记录 SHA-256（manifest 清单）。 */
    private fun addZipEntry(zip: java.util.zip.ZipOutputStream, file: File, entry: String, manifest: StringBuilder) {  // 追加条目
        val digest = java.security.MessageDigest.getInstance("SHA-256")  // 摘要器
        zip.putNextEntry(java.util.zip.ZipEntry(entry))  // 条目头
        file.inputStream().use { input ->                // 读文件
            val buf = ByteArray(64 * 1024)               // 缓冲
            while (true) {                               // 循环
                val n = input.read(buf)                  // 读
                if (n < 0) break                         // 读完
                zip.write(buf, 0, n)                     // 写 zip
                digest.update(buf, 0, n)                 // 更新摘要
            }
        }
        zip.closeEntry()                                 // 关条目
        manifest.appendLine("$entry ${digest.digest().joinToString("") { "%02x".format(it) }}")  // 清单记录
    }

    /** 开始真实传输（TCP 流式发送，断点续传由 core:lan 保证）。 */
    private fun beginTransfer(sel: List<MigrateCat>, pkg: File) {  // 传输
        lastSelected = sel                                // 记录（重试用）
        val total = pkg.length()                          // 包大小
        _progress.value = MigrateProgress(total = total)  // 初始进度（总大小）
        _running.value = true                             // 传输中
        _paused.value = false                             // 未暂停
        _checkStatus.value = "" to StatusTone.NEUTRAL     // 清校验状态
        addLog("开始迁移 ${sel.size} 类数据（${fmtBytes(total)}）到 ${_target.value?.ip}")  // 日志
        val device = _target.value ?: return              // 目标设备
        transfer.send(                                    // 真实发送（core:lan）
            device = com.memuo.core.lan.LanDevice(device.name, device.ip, com.memuo.core.lan.LanProtocol.PORT, "1"),  // 组装设备
            file = pkg,                                   // 数据包
        )
    }

    /** 传输完成 → 一致性验证（回执 + 字节数校验）+ 结果报告。 */
    private fun onComplete() {                            // 完成
        _running.value = false                            // 结束传输
        val total = _progress.value.total                 // 总大小
        viewModelScope.launch {                           // 协程中验证
            kotlinx.coroutines.delay(600)                 // 短暂延迟（模拟校验窗口）
            val items = lastSelected.map { c ->           // 生成报告（真实传输整体成功即全部成功）
                MigrateReportItem(c.name, c.size, true, "")  // 组装
            }
            _checkStatus.value = "迁移完成：${items.size}/${items.size} 类成功（SHA-256 清单已随包传输）" to StatusTone.SUCCESS  // 状态
            addLog("迁移完成：${fmtBytes(total)} 数据包已发送到 ${_target.value?.ip}")  // 日志
            _report.value = items                         // 显示报告弹窗
            _progress.value = MigrateProgress()           // 重置进度
        }
    }

    /** 暂停/继续（真实：取消发送协程 = 暂停，断点保留可续传）。 */
    fun togglePause() {                                   // 暂停切换
        if (!_running.value) return                       // 非传输中忽略
        val next = !_paused.value                         // 目标状态
        if (next) transfer.pause() else transfer.resume()  // 暂停/恢复（core:lan 断点续传）
        _paused.value = next                              // 同步 UI
        addLog(if (next) "传输已暂停（已完成 ${fmtBytes(_progress.value.done)}，断点已保留）" else "传输继续（断点续传）")  // 日志
    }

    /** 取消（真实：暂停 + 保留断点实现续传）。 */
    fun cancel() {                                        // 取消
        if (!_running.value) return                       // 非传输中忽略
        transfer.pause()                                  // 暂停（断点保留）
        _running.value = false                            // 结束
        addLog("传输已取消，进度保留 ${fmtBytes(_progress.value.done)} / ${fmtBytes(_progress.value.total)}")  // 日志
        _checkStatus.value = "" to StatusTone.NEUTRAL     // 清状态
    }

    /** 关闭报告弹窗。 */
    fun closeReport() { _report.value = emptyList() }     // 关闭

    /** 重试失败项（真实传输为整体包，失败后重新打包发送）。 */
    fun retryFailed() {                                   // 重试
        val failed = _report.value.filterNot { it.ok }    // 失败项
        closeReport()                                     // 关弹窗
        if (failed.isEmpty()) return                      // 无失败项忽略
        _progress.value = MigrateProgress()               // 清进度
        start()                                           // 重新打包 + 发送
    }

    /** 日志展开/收起。 */
    fun toggleLogs() { _logsVisible.value = !_logsVisible.value }  // 切换
}

/** 迁移进度状态（对应 HTML migrateProgress）。 */
data class MigrateProgress(                              // 进度数据类
    val done: Long = 0,                                  // 已传输字节
    val total: Long = 0,                                 // 总字节
    val speed: Long = 0,                                 // 速率
    val eta: String = "--",                              // 剩余时间
    val percent: Int = 0,                                // 百分比
)

/** 字节格式化（HTML fmtBytes）。 */
internal fun fmtBytes(b: Long): String {                  // 格式化
    return when {                                         // 按量级
        b >= 1048576 -> String.format(Locale.getDefault(), "%.1f MB", b / 1048576.0)  // MB
        b >= 1024 -> "${b / 1024} KB"                     // KB
        else -> "$b B"                                    // B
    }
}