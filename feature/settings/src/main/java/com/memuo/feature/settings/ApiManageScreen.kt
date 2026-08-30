package com.memuo.feature.settings                         // 声明包名：设置业务模块

import androidx.compose.foundation.background             // 导入 background：背景修饰
import androidx.compose.foundation.clickable              // 导入 clickable：点击修饰
import androidx.compose.foundation.layout.Arrangement     // 导入 Arrangement：按钮排列
import androidx.compose.foundation.layout.Box             // 导入 Box：盒式布局
import androidx.compose.foundation.layout.Column          // 导入 Column：纵向布局
import androidx.compose.foundation.layout.Row             // 导入 Row：横向布局
import androidx.compose.foundation.layout.fillMaxHeight   // 导入 fillMaxHeight：占满高度
import androidx.compose.foundation.layout.fillMaxSize     // 导入 fillMaxSize：铺满
import androidx.compose.foundation.layout.fillMaxWidth    // 导入 fillMaxWidth：占满宽度
import androidx.compose.foundation.layout.height          // 导入 height：固定高度
import androidx.compose.foundation.layout.padding          // 导入 padding：内边距
import androidx.compose.foundation.layout.size            // 导入 size：固定尺寸
import androidx.compose.foundation.layout.width           // 导入 width：固定宽度
import androidx.compose.foundation.lazy.LazyColumn         // 导入 LazyColumn：懒加载列表
import androidx.compose.foundation.lazy.items              // 导入 items：列表项
import androidx.compose.foundation.shape.RoundedCornerShape  // 导入 RoundedCornerShape：圆角形状
import androidx.compose.foundation.text.KeyboardOptions     // 导入 KeyboardOptions：键盘选项
import androidx.compose.material3.Icon                    // 导入 Icon：图标
import androidx.compose.material3.MaterialTheme           // 导入 MaterialTheme：主题
import androidx.compose.material3.OutlinedButton           // 导入 OutlinedButton：描边按钮（智能输入）
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
import androidx.compose.ui.text.input.PasswordVisualTransformation  // 导入 PasswordVisualTransformation：密码掩码
import androidx.compose.ui.text.input.VisualTransformation  // 导入 VisualTransformation：明文
import androidx.compose.ui.text.style.TextOverflow        // 导入 TextOverflow：溢出省略
import androidx.compose.ui.unit.dp                        // 导入 dp：尺寸单位
import androidx.compose.ui.unit.sp                        // 导入 sp：字号单位
import androidx.hilt.navigation.compose.hiltViewModel     // 导入 hiltViewModel：Hilt 提供 ViewModel
import androidx.lifecycle.ViewModel                       // 导入 ViewModel：UI 数据持有者
import androidx.lifecycle.viewModelScope                  // 导入 viewModelScope：协程作用域
import com.memuo.core.ui.AppIcons                          // 导入应用图标集
import com.memuo.core.ui.components.BrandButton            // 导入品牌主按钮
import com.memuo.core.ui.components.EmptyState            // 导入空态组件
import com.memuo.core.ui.components.LocalToast            // 导入 Toast 状态
import com.memuo.core.ui.components.ModalCloseButton      // 导入弹窗关闭按钮
import com.memuo.core.ui.components.MuyunModal            // 导入弹窗容器
import com.memuo.core.ui.components.MuyunSegmented        // 导入分段胶囊
import com.memuo.core.ui.components.StatusBar             // 导入状态条
import com.memuo.core.ui.components.StatusTone            // 导入状态色调
import com.memuo.core.ui.components.SubBody               // 导入子页内容容器
import com.memuo.core.ui.components.SubHeader             // 导入子页顶栏
import com.memuo.core.ui.theme.MuyunBorder                // 导入分割线色
import com.memuo.core.ui.theme.MuyunCard                  // 导入卡片白
import com.memuo.core.ui.theme.MuyunDanger                // 导入危险红
import com.memuo.core.ui.theme.MuyunGreen                 // 导入成功绿
import com.memuo.core.ui.theme.MuyunGreenBg               // 导入成功绿底
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
import org.json.JSONObject                                 // 导入 JSONObject：JSON 对象
import javax.inject.Inject                                // 导入 Inject：构造函数注入

/** 云端 API 条目（对应 HTML apiList 的元素）。 */
data class ApiEntry(                                     // API 数据类
    val id: Long,                                        // 唯一 ID
    val name: String,                                    // 名称
    val type: String,                                    // 类型：openai/custom
    val url: String,                                     // 地址
    val key: String,                                     // 密钥
    val model: String,                                   // 模型名
)

/** 智能输入解析结果（M-035）：从粘贴的配置文本中提取的字段。 */
data class ParsedApiConfig(                              // 解析结果
    val url: String,                                     // API 地址
    val key: String,                                     // 密钥
    val model: String,                                   // 模型名
    val name: String,                                    // 推断的名称
)

/**
 * 云端 API 管理页 —— 多 API 列表 + 添加弹窗（HTML 云端API管理页迁移）。
 * 对应 HTML：卡片列表（类型标签/使用中标记/点击切换当前/删除）、添加弹窗
 * （名称/类型分段/地址/密钥带眼睛/模型名/测试连接/校验）、底部「添加 API」按钮。
 * 切换当前 API 时同步写入 CloudConfigRepository，保证真实引擎立即生效。
 */
@Composable                                               // 可组合 UI 函数
fun ApiManageScreen(                                     // 云端 API 管理页
    onBack: () -> Unit,                                  // 返回回调
    viewModel: ApiManageViewModel = hiltViewModel(),     // Hilt 提供 ViewModel
) {
    val apis by viewModel.apis.collectAsState()          // 订阅 API 列表
    val currentId by viewModel.currentId.collectAsState()  // 订阅当前 id
    val addVisible by viewModel.addVisible.collectAsState()  // 订阅添加弹窗
    val smartVisible by viewModel.smartVisible.collectAsState()  // 订阅智能输入弹窗
    val smartText by viewModel.smartText.collectAsState()  // 订阅智能输入文本
    val smartPreview by viewModel.smartPreview.collectAsState()  // 订阅解析预览
    val toast = LocalToast.current                       // 取全局 Toast

    Column(modifier = Modifier.fillMaxSize()) {           // 纵向布局
        SubHeader(                                       // 顶栏
            title = "云端API管理",                       // 标题（HTML .sub-title）
            onBack = onBack,                             // 返回
            actions = {                                  // 右上角 + 添加（HTML .sub-header-plus）
                Box(                                     // 加号按钮
                    modifier = Modifier                 // 修饰
                        .size(36.dp)                    // 36dp
                        .clip(RoundedCornerShape(10.dp))  // 圆角 10
                        .clickable { viewModel.openAdd() },  // 打开添加弹窗
                    contentAlignment = Alignment.Center, // 居中
                ) {
                    Icon(                               // 加号图标
                        imageVector = AppIcons.Plus,     // 图标
                        contentDescription = "添加 API",  // 描述
                        tint = MuyunText2,               // 次级灰
                        modifier = Modifier.size(18.dp), // 18dp
                    )
                }
            },
        )
        SubBody(modifier = Modifier.fillMaxSize()) {      // 内容容器
            Column(modifier = Modifier.fillMaxSize()) {   // 纵向内容
                Text(                                    // 头部提示（HTML set-hint）
                    text = "点击 API 卡片可将其设为当前使用；已选中的卡片显示「使用中」。",  // 文案
                    style = MaterialTheme.typography.labelSmall,  // 小字（HTML 11px）
                    color = MuyunText3,                  // 三级灰
                    modifier = Modifier.padding(bottom = 14.dp),  // 下留白
                )
                if (apis.isEmpty()) {                    // 空态
                    EmptyState(                          // 空态组件
                        icon = AppIcons.Cloud,           // 云朵插图
                        text = "还没有 API，点击下方按钮或右上角 + 添加",  // HTML api-empty 文案
                    )
                } else {                                 // 列表
                    LazyColumn(                          // 懒加载列表
                        modifier = Modifier.weight(1f),  // 占满剩余
                    ) {
                        items(apis, key = { it.id }) { api ->  // 遍历 API
                            ApiCard(                     // API 卡片
                                api = api,               // 数据
                                isCurrent = api.id == currentId,  // 是否使用中
                                onClick = { viewModel.setCurrent(api.id) },  // 点击切换当前
                                onDelete = { viewModel.delete(api.id) },  // 删除
                            )
                        }
                    }
                }
                Column(modifier = Modifier.padding(top = 10.dp)) {  // 底部按钮
                    BrandButton(text = "添加 API", onClick = { viewModel.openAdd() })  // HTML 底部 check-btn
                }
            }
        }
    }

    // —— 添加 API 弹窗（HTML #api-add-modal）——
    MuyunModal(                                          // 弹窗容器
        visible = addVisible,                            // 绑定状态
        onDismiss = { viewModel.closeAdd() },            // 点遮罩关闭
        title = "添加 API",                              // 标题
        headerActions = { ModalCloseButton { viewModel.closeAdd() } },  // 右上关闭
        body = {                                        // 弹窗主体
            ApiAddForm(viewModel = viewModel, toast = toast)  // 表单
        },
    )

    // —— 智能输入弹窗（M-035）——
    MuyunModal(                                          // 弹窗容器
        visible = smartVisible,                          // 绑定状态
        onDismiss = { viewModel.closeSmartInput() },     // 点遮罩关闭
        title = "智能输入",                              // 标题
        headerActions = { ModalCloseButton { viewModel.closeSmartInput() } },  // 右上关闭
        body = {                                        // 弹窗主体
            SmartInputBody(                             // 智能输入主体
                viewModel = viewModel,                   // ViewModel
            )
        },
    )
}

/** API 卡片：图标 + 名称/类型标签/使用中 + 地址 + 删除（对应 HTML .api-item）。 */
@Composable                                               // 可组合函数
private fun ApiCard(                                     // API 卡片
    api: ApiEntry,                                       // 数据
    isCurrent: Boolean,                                  // 是否使用中
    onClick: () -> Unit,                                 // 点击
    onDelete: () -> Unit,                                // 删除
) {
    Row(                                                 // 横向布局
        modifier = Modifier                              // 修饰
            .fillMaxWidth()                              // 占满宽度
            .padding(bottom = 10.dp)                     // 下留白
            .shadow(1.dp, RoundedCornerShape(14.dp))     // 轻投影
            .clip(RoundedCornerShape(14.dp))             // 圆角 14
            .background(MuyunCard)                       // 白底
            .clickable { onClick() }                     // 点击切换
            .padding(horizontal = 16.dp, vertical = 14.dp),  // 内边距
        verticalAlignment = Alignment.CenterVertically,  // 垂直居中
    ) {
        Box(                                             // 图标底（紫色，HTML .api-item-icon）
            modifier = Modifier                         // 修饰
                .size(36.dp)                            // 36dp
                .clip(RoundedCornerShape(10.dp))        // 圆角 10
                .background(MuyunPurpleBg),             // 紫浅底
            contentAlignment = Alignment.Center,         // 居中
        ) {
            Icon(                                        // 云朵图标
                imageVector = AppIcons.Cloud,            // 图标
                contentDescription = null,               // 装饰
                tint = MuyunPurple,                      // 品牌紫
                modifier = Modifier.size(16.dp),         // 16dp
            )
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {  // 信息区
            Row(verticalAlignment = Alignment.CenterVertically) {  // 名称行
                Text(                                    // 名称
                    text = api.name,                     // 内容
                    style = MaterialTheme.typography.titleSmall,  // 字号（HTML 14px）
                    fontWeight = FontWeight.SemiBold,    // 半粗
                    color = MuyunText,                   // 主文字色
                )
                Text(                                    // 类型标签（HTML .api-item-type）
                    text = if (api.type == "custom") "自定义" else "OpenAI 兼容",  // 标签文案
                    style = MaterialTheme.typography.labelSmall.copy(  // 小字
                        color = MuyunPurple,             // 紫
                        background = MuyunPurpleBg,      // 紫浅底
                    ),
                    modifier = Modifier.padding(start = 6.dp, top = 0.dp),  // 留白
                )
                if (isCurrent) {                         // 使用中
                    Text(                                // 使用中标签（HTML .api-current-tag）
                        text = "使用中",                  // 文案
                        style = MaterialTheme.typography.labelSmall.copy(  // 小字
                            color = MuyunGreen,          // 绿
                            background = MuyunGreenBg,   // 绿浅底
                        ),
                        modifier = Modifier.padding(start = 6.dp),  // 留白
                    )
                }
            }
            Text(                                        // 地址行（HTML .api-item-sub）
                text = api.url + if (api.model.isNotBlank()) " · " + api.model else "",  // 地址 + 模型
                style = MaterialTheme.typography.labelSmall,  // 小字（HTML 11px）
                color = MuyunText3,                      // 三级灰
                maxLines = 1,                            // 单行
                overflow = TextOverflow.Ellipsis,        // 省略
                modifier = Modifier.padding(top = 4.dp),  // 上留白
            )
        }
        Box(                                             // 删除按钮（HTML .api-item-delete）
            modifier = Modifier                         // 修饰
                .size(32.dp)                            // 32dp
                .clip(RoundedCornerShape(8.dp))         // 圆角 8
                .clickable { onDelete() },              // 点击删除
            contentAlignment = Alignment.Center,         // 居中
        ) {
            Icon(                                        // 垃圾桶图标
                imageVector = AppIcons.Trash,            // 图标
                contentDescription = "删除",              // 描述
                tint = MuyunText3,                       // 三级灰
                modifier = Modifier.size(14.dp),         // 14dp
            )
        }
    }
}

/** 添加 API 表单：字段 + 校验 + 测试连接 + 保存（对应 HTML #api-add-modal 内容）。 */
@Composable                                               // 可组合函数
private fun ApiAddForm(                                  // 添加表单
    viewModel: ApiManageViewModel,                       // ViewModel
    toast: com.memuo.core.ui.components.ToastState,      // Toast 状态
) {
    val name by viewModel.formName.collectAsState()      // 名称
    val url by viewModel.formUrl.collectAsState()        // 地址
    val key by viewModel.formKey.collectAsState()        // 密钥
    val model by viewModel.formModel.collectAsState()    // 模型名
    val typeIndex by viewModel.formTypeIndex.collectAsState()  // 类型分段
    val keyVisible by viewModel.keyVisible.collectAsState()  // 密钥可见
    val errors by viewModel.formErrors.collectAsState()  // 校验错误
    val testState by viewModel.testState.collectAsState()  // 测试状态

    Column {                                           // 纵向表单
        // 智能输入入口（M-035）
        Row(                                          // 智能输入入口行
            modifier = Modifier                       // 修饰
                .fillMaxWidth()                       // 占满宽度
                .padding(bottom = 12.dp)              // 下留白
                .clip(RoundedCornerShape(10.dp))      // 圆角
                .background(MuyunPurpleBg)            // 紫浅底
                .clickable { viewModel.openSmartInput() }  // 打开智能输入
                .padding(horizontal = 12.dp, vertical = 11.dp),  // 内边距
            verticalAlignment = Alignment.CenterVertically,  // 垂直居中
        ) {
            Icon(                                     // 闪电/魔法图标（用 Regenerate 近似）
                imageVector = AppIcons.Refresh,        // 图标
                contentDescription = null,             // 装饰
                tint = MuyunPurple,                    // 品牌紫
                modifier = Modifier.size(15.dp),       // 15dp
            )
            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {  // 文案
                Text(                                  // 标题
                    text = "智能输入",                 // 内容
                    style = MaterialTheme.typography.bodySmall,  // 字号
                    fontWeight = FontWeight.SemiBold,  // 半粗
                    color = MuyunPurple,               // 紫
                )
                Text(                                  // 副标题
                    text = "粘贴含地址/密钥/模型名的文本，自动识别填充",  // 文案
                    style = MaterialTheme.typography.labelSmall,  // 小字
                    color = MuyunText3,                // 三级灰
                    modifier = Modifier.padding(top = 1.dp),  // 上留白
                )
            }
            Text(                                      // 右箭头提示
                text = "›",                            // 箭头
                style = MaterialTheme.typography.bodyMedium,  // 字号
                color = MuyunPurple,                   // 紫
            )
        }
        ApiField("API 名称 *", errors["name"]) {        // 名称字段
            ApiTextField(name, { viewModel.setName(it) }, "如：OpenAI 主服务")  // 输入框
        }
        ApiField("服务类型", null) {                    // 类型分段
            MuyunSegmented(                             // 分段（HTML .lan-seg）
                labels = listOf("OpenAI 兼容", "自定义"),  // 两段
                selectedIndex = typeIndex,              // 当前
                onSelect = { viewModel.setType(it) },   // 切换
            )
        }
        ApiField("API 地址 *", errors["url"]) {         // 地址字段
            ApiTextField(url, { viewModel.setUrl(it) }, "https://api.openai.com/v1")  // 输入框
        }
        ApiField("API 密钥 *", errors["key"]) {         // 密钥字段
            Row(verticalAlignment = Alignment.CenterVertically) {  // 密钥 + 眼睛
                Box(modifier = Modifier.weight(1f)) {   // 输入框容器
                    ApiTextField(                       // 密钥输入
                        key, { viewModel.setKey(it) }, "sk-...",  // 占位
                        visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),  // 明文/掩码
                    )
                }
                Box(                                    // 眼睛按钮（HTML .set-eye）
                    modifier = Modifier                // 修饰
                        .padding(start = 6.dp)         // 留白
                        .clickable { viewModel.toggleKeyVisible() }  // 切换可见
                        .padding(6.dp),                 // 热区
                ) {
                    Icon(                               // 眼睛图标
                        imageVector = AppIcons.Eye,      // 图标
                        contentDescription = "显示/隐藏密钥",  // 描述
                        tint = MuyunText3,               // 三级灰
                        modifier = Modifier.size(16.dp), // 16dp
                    )
                }
            }
        }
        ApiField("模型名称", null) {                    // 模型字段
            ApiTextField(model, { viewModel.setModel(it) }, "gpt-4o-mini")  // 输入框
        }
        // 测试结果条（HTML .set-test-result）
        if (testState.first.isNotBlank()) {              // 有测试结果
            StatusBar(                                   // 状态条
                text = testState.first,                  // 结果文案
                tone = testState.second,                 // 色调
                modifier = Modifier.padding(top = 10.dp),  // 上留白
            )
        }
        Row(modifier = Modifier.padding(top = 14.dp)) {  // 底部按钮行（HTML modal-footer）
            Box(                                         // 测试连接按钮
                modifier = Modifier                     // 修饰
                    .weight(1f)                         // 占半
                    .clip(RoundedCornerShape(10.dp))    // 圆角
                    .background(MuyunCard)              // 白底
                    .clickable { viewModel.testConnection() }  // 点击测试
                    .padding(vertical = 12.dp),         // 内边距
                contentAlignment = Alignment.Center,     // 居中
            ) {
                Text(                                    // 文字
                    text = "测试连接",                   // 内容
                    style = MaterialTheme.typography.bodyMedium,  // 字号（HTML 14px）
                    fontWeight = FontWeight.Medium,      // 中粗
                    color = MuyunText,                   // 主文字色
                )
            }
            Box(modifier = Modifier.weight(1f).padding(start = 10.dp)) {  // 保存按钮容器
                BrandButton(                             // 保存按钮
                    text = "保存",                       // 文字
                    onClick = { viewModel.save() },      // 保存
                    height = 48.dp,                      // 略矮
                )
            }
        }
    }
}

/** 智能输入主体（M-035）：多行粘贴框 + 解析预览 + 确认填充。 */
@Composable                                               // 可组合函数
private fun SmartInputBody(                               // 智能输入主体
    viewModel: ApiManageViewModel,                       // ViewModel
) {
    val text by viewModel.smartText.collectAsState()     // 输入文本
    val preview by viewModel.smartPreview.collectAsState()  // 解析预览

    Column {                                           // 纵向
        Text(                                          // 说明
            text = "粘贴一段包含 API 地址、密钥、模型名的文本（如服务商提供的接入信息），点击「解析」自动识别。",  // 文案
            style = MaterialTheme.typography.labelSmall,  // 小字
            color = MuyunText3,                        // 三级灰
            lineHeight = MaterialTheme.typography.labelSmall.lineHeight * 1.5f,  // 行距
            modifier = Modifier.padding(bottom = 10.dp),  // 下留白
        )
        // 多行输入框
        Box(                                           // 输入容器
            modifier = Modifier                       // 修饰
                .fillMaxWidth()                       // 占满
                .height(120.dp)                       // 高 120
                .clip(RoundedCornerShape(10.dp))      // 圆角
                .background(MuyunCard)                // 白底
                .padding(horizontal = 12.dp, vertical = 10.dp),  // 内边距
        ) {
            androidx.compose.foundation.text.BasicTextField(  // 无边框多行输入
                value = text,                          // 绑定
                onValueChange = { viewModel.setSmartText(it) },  // 更新
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MuyunText),  // 字体
                cursorBrush = androidx.compose.ui.graphics.SolidColor(MuyunPurple),  // 光标
                modifier = Modifier.fillMaxWidth().fillMaxHeight(),  // 占满
                decorationBox = { inner ->              // 占位
                    if (text.isEmpty()) {              // 空
                        Text("例如：https://api.deepseek.com/v1\nsk-xxxxxxxxxxxx\nmodel: deepseek-chat", color = MuyunText3, style = MaterialTheme.typography.bodyMedium, lineHeight = 20.sp)  // 占位示例
                    }
                    inner()                            // 输入区
                },
            )
        }
        // 解析结果预览
        if (preview != null) {                         // 已解析
            Column(                                    // 预览卡片
                modifier = Modifier                   // 修饰
                    .fillMaxWidth()                   // 占满
                    .padding(top = 12.dp)             // 上留白
                    .clip(RoundedCornerShape(10.dp))  // 圆角
                    .background(MuyunGreenBg)         // 绿浅底（识别成功）
                    .padding(horizontal = 12.dp, vertical = 10.dp),  // 内边距
            ) {
                Text(                                  // 预览标题
                    text = "已识别，请确认后填充",      // 文案
                    style = MaterialTheme.typography.labelSmall,  // 小字
                    fontWeight = FontWeight.SemiBold,  // 半粗
                    color = MuyunGreen,               // 绿
                    modifier = Modifier.padding(bottom = 6.dp),  // 下留白
                )
                SmartPreviewRow("地址", preview?.url)   // 地址
                SmartPreviewRow("密钥", preview?.key)   // 密钥
                SmartPreviewRow("模型", preview?.model)  // 模型
                SmartPreviewRow("名称", preview?.name)   // 名称
            }
        }
        // 底部按钮
        Row(                                           // 按钮行
            modifier = Modifier.padding(top = 14.dp),  // 上留白
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),  // 间距
        ) {
            androidx.compose.material3.OutlinedButton(  // 解析按钮
                onClick = { viewModel.parseAndPreview() },  // 解析
                modifier = Modifier.weight(1f),         // 平分
            ) { Text("解析") }                          // 文案
            BrandButton(                               // 确认填充
                text = "确认填充",                      // 文案
                onClick = { viewModel.applySmart() },   // 填充并关闭
                modifier = Modifier.weight(1f),         // 平分
            )
        }
    }
}

/** 智能输入预览行：标签 + 值。 */
@Composable                                               // 可组合函数
private fun SmartPreviewRow(                              // 预览行
    label: String,                                       // 标签
    value: String?,                                      // 值
) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {  // 行
        Text(                                           // 标签
            text = label,                               // 内容
            style = MaterialTheme.typography.labelSmall,  // 小字
            color = MuyunText3,                         // 三级灰
            modifier = Modifier.width(48.dp),           // 固定标签宽
        )
        Text(                                           // 值
            text = if (value.isNullOrBlank()) "未识别" else value,  // 值或未识别
            style = MaterialTheme.typography.labelSmall,  // 小字
            color = if (value.isNullOrBlank()) MuyunDanger else MuyunText,  // 未识别红/正常主色
            maxLines = 2,                               // 最多两行
            overflow = TextOverflow.Ellipsis,           // 省略
            modifier = Modifier.weight(1f),             // 占满
        )
    }
}

/** 表单字段包装：标签 + 必填星 + 错误提示（对应 HTML .api-field）。 */
@Composable                                               // 可组合函数
private fun ApiField(                                    // 表单字段
    label: String,                                       // 标签
    error: String?,                                      // 错误文案（null 无错）

    content: @Composable () -> Unit,                     // 内容
) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {  // 字段间距（HTML margin-bottom 12px）
        Text(                                            // 标签
            text = label,                                // 内容
            style = MaterialTheme.typography.labelMedium,  // 小字（HTML 12px）
            fontWeight = FontWeight.Medium,              // 中粗
            color = MuyunText2,                          // 次级灰
            modifier = Modifier.padding(bottom = 6.dp),  // 下留白（HTML margin-bottom 6px）
        )
        content()                                        // 字段内容
        if (error != null) {                             // 有错误
            Text(                                        // 错误文案（HTML .api-field-error）
                text = error,                            // 内容
                style = MaterialTheme.typography.labelSmall,  // 小字（HTML 11px）
                color = MuyunDanger,                     // 危险红
                modifier = Modifier.padding(top = 5.dp),  // 上留白
            )
        }
    }
}

/** 圆角输入框（对应 HTML .set-input）。 */
@Composable                                               // 可组合函数
private fun ApiTextField(                                // 圆角输入框
    value: String,                                       // 值
    onValueChange: (String) -> Unit,                     // 变化回调
    placeholder: String,                                 // 占位
    visualTransformation: VisualTransformation = VisualTransformation.None,  // 视觉变换（密码掩码）
) {
    Box(                                                 // 输入框容器
        modifier = Modifier                             // 修饰
            .fillMaxWidth()                             // 占满宽度
            .clip(RoundedCornerShape(10.dp))            // 圆角 10
            .background(MuyunCard)                      // 白底
            .padding(horizontal = 12.dp, vertical = 9.dp),  // 内边距（HTML padding 9px 12px）
    ) {
        androidx.compose.foundation.text.BasicTextField(  // 无边框输入
            value = value,                               // 绑定
            onValueChange = onValueChange,               // 更新
            singleLine = true,                           // 单行
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MuyunText),  // 字体（HTML 13px 近似）
            visualTransformation = visualTransformation,  // 掩码
            cursorBrush = androidx.compose.ui.graphics.SolidColor(MuyunPurple),  // 光标
            modifier = Modifier.fillMaxWidth(),          // 占满
            decorationBox = { inner ->                   // 占位符
                if (value.isEmpty()) {                   // 空值
                    Text(placeholder, color = MuyunText3, style = MaterialTheme.typography.bodyMedium)  // 占位
                }
                inner()                                  // 输入区
            },
        )
    }
}

/** 云端 API 管理 ViewModel —— 列表 CRUD + 当前切换 + 表单校验 + 模拟测试连接。 */
@HiltViewModel                                           // 注解：由 Hilt 创建
class ApiManageViewModel @Inject constructor(            // 构造函数注入
    private val prefs: ExtPrefs,                         // 注入扩展偏好
    private val cloudRepo: CloudConfigRepository,        // 注入云端仓库（当前 API 同步引擎）
) : ViewModel() {                                        // 继承 ViewModel

    private val _apis = MutableStateFlow<List<ApiEntry>>(emptyList())  // API 列表
    val apis: StateFlow<List<ApiEntry>> = _apis.asStateFlow()  // 只读暴露
    private val _currentId = MutableStateFlow(-1L)       // 当前 id
    val currentId: StateFlow<Long> = _currentId.asStateFlow()  // 只读暴露
    private val _addVisible = MutableStateFlow(false)    // 添加弹窗
    val addVisible: StateFlow<Boolean> = _addVisible.asStateFlow()  // 只读暴露

    // —— 表单状态 ——
    private val _formName = MutableStateFlow("")         // 名称
    val formName: StateFlow<String> = _formName.asStateFlow()  // 只读
    private val _formUrl = MutableStateFlow("https://api.openai.com/v1")  // 地址（默认 OpenAI）
    val formUrl: StateFlow<String> = _formUrl.asStateFlow()  // 只读
    private val _formKey = MutableStateFlow("")          // 密钥
    val formKey: StateFlow<String> = _formKey.asStateFlow()  // 只读
    private val _formModel = MutableStateFlow("")        // 模型
    val formModel: StateFlow<String> = _formModel.asStateFlow()  // 只读
    private val _formTypeIndex = MutableStateFlow(0)     // 类型分段（0=openai）
    val formTypeIndex: StateFlow<Int> = _formTypeIndex.asStateFlow()  // 只读
    private val _keyVisible = MutableStateFlow(false)    // 密钥可见
    val keyVisible: StateFlow<Boolean> = _keyVisible.asStateFlow()  // 只读
    private val _formErrors = MutableStateFlow<Map<String, String>>(emptyMap())  // 校验错误
    val formErrors: StateFlow<Map<String, String>> = _formErrors.asStateFlow()  // 只读
    private val _testState = MutableStateFlow("" to StatusTone.NEUTRAL)  // 测试结果
    val testState: StateFlow<Pair<String, StatusTone>> = _testState.asStateFlow()  // 只读

    // —— 智能输入（M-035）——
    private val _smartVisible = MutableStateFlow(false)  // 智能输入弹窗
    val smartVisible: StateFlow<Boolean> = _smartVisible.asStateFlow()  // 只读
    private val _smartText = MutableStateFlow("")        // 智能输入文本
    val smartText: StateFlow<String> = _smartText.asStateFlow()  // 只读
    private val _smartPreview = MutableStateFlow<ParsedApiConfig?>(null)  // 解析预览（null=未解析）
    val smartPreview: StateFlow<ParsedApiConfig?> = _smartPreview.asStateFlow()  // 只读

    init {                                                // 初始化
        viewModelScope.launch {                          // 加载列表
            prefs.apiListJson.collect { json ->          // JSON 变化
                _apis.value = decode(json)               // 解码列表
            }
        }
        viewModelScope.launch {                          // 加载当前 id
            prefs.currentApiId.collect { id ->           // 当前 id 变化
                _currentId.value = id                     // 更新
            }
        }
    }

    /** JSON → 列表。 */
    private fun decode(json: String): List<ApiEntry> =   // 解码
        if (json.isBlank()) emptyList() else runCatching {  // 空/解析
            val arr = JSONArray(json)                    // 数组
            (0 until arr.length()).map { i ->            // 遍历
                val o = arr.getJSONObject(i)             // 对象
                ApiEntry(                                // 组装
                    id = o.optLong("id"),                // id
                    name = o.optString("name"),          // 名称
                    type = o.optString("type", "openai"),  // 类型
                    url = o.optString("url"),            // 地址
                    key = o.optString("key"),            // 密钥
                    model = o.optString("model"),        // 模型
                )
            }
        }.getOrDefault(emptyList())                      // 失败空列表

    /** 列表 → JSON。 */
    private fun encode(list: List<ApiEntry>): String =   // 编码
        JSONArray().apply {                              // 数组
            list.forEach { a ->                          // 遍历
                put(JSONObject().apply {                 // 对象
                    put("id", a.id); put("name", a.name); put("type", a.type)  // 基本字段
                    put("url", a.url); put("key", a.key); put("model", a.model)  // 其余字段
                })
            }
        }.toString()                                     // 转字符串

    /** 持久化列表。 */
    private fun persist() {                              // 持久化
        viewModelScope.launch { prefs.setApiListJson(encode(_apis.value)) }  // 写 DataStore
    }

    /** 打开添加弹窗（清空表单，HTML openApiAdd）。 */
    fun openAdd() {                                      // 打开弹窗
        _formName.value = ""                             // 清名称
        _formUrl.value = if (_formTypeIndex.value == 0) "https://api.openai.com/v1" else ""  // 默认地址
        _formKey.value = ""                              // 清密钥
        _formModel.value = ""                            // 清模型
        _formErrors.value = emptyMap()                   // 清错误
        _testState.value = "" to StatusTone.NEUTRAL      // 清测试
        _addVisible.value = true                         // 显示弹窗
    }

    /** 关闭添加弹窗。 */
    fun closeAdd() { _addVisible.value = false }         // 隐藏

    /** 切换服务类型分段（HTML setApiFormType）。 */
    fun setType(index: Int) {                            // 切类型
        _formTypeIndex.value = index                     // 更新
        _formUrl.value = if (index == 0) "https://api.openai.com/v1" else ""  // 切换时改地址
    }

    /** 切换密钥可见。 */
    fun toggleKeyVisible() { _keyVisible.value = !_keyVisible.value }  // 切换

    fun setName(v: String) { _formName.value = v }       // 名称输入
    fun setUrl(v: String) { _formUrl.value = v }         // 地址输入
    fun setKey(v: String) { _formKey.value = v }         // 密钥输入
    fun setModel(v: String) { _formModel.value = v }     // 模型输入

    // —— 智能输入（M-035）——
    /** 打开智能输入弹窗（清空旧文本与预览）。 */
    fun openSmartInput() {                               // 打开智能输入
        _smartText.value = ""                            // 清文本
        _smartPreview.value = null                       // 清预览
        _smartVisible.value = true                       // 显示弹窗
    }

    /** 关闭智能输入弹窗。 */
    fun closeSmartInput() { _smartVisible.value = false }  // 关闭

    /** 更新智能输入文本。 */
    fun setSmartText(v: String) { _smartText.value = v }  // 输入

    /** 解析粘贴的配置文本，生成预览。 */
    fun parseAndPreview() {                              // 解析预览
        _smartPreview.value = parseSmart(_smartText.value)  // 解析并预览
    }

    /** 确认填充：把预览结果写入表单字段并关闭弹窗。 */
    fun applySmart() {                                   // 确认填充
        val p = _smartPreview.value ?: return            // 无预览忽略
        if (p.url.isNotBlank()) _formUrl.value = p.url   // 填充地址
        if (p.key.isNotBlank()) _formKey.value = p.key   // 填充密钥
        if (p.model.isNotBlank()) _formModel.value = p.model  // 填充模型
        if (p.name.isNotBlank()) _formName.value = p.name  // 填充名称
        _smartVisible.value = false                      // 关闭弹窗
    }

    /**
     * 解析配置文本（M-035）：从一段含 API 地址/密钥/模型名的文本中提取字段。
     * 支持常见格式：
     *  - 地址：http(s)://... ；
     *  - 密钥：sk-... / api_key=... / Bearer <token> / key: xxx ；
     *  - 模型：model=... / model: xxx 。
     */
    private fun parseSmart(text: String): ParsedApiConfig {  // 解析
        val t = text.trim()                              // 去首尾空白
        if (t.isBlank()) return ParsedApiConfig("", "", "", "")  // 空文本
        // ① 地址
        val url = Regex("https?://[^\\s\"'，,；;]+").find(t)?.value?.trimEnd('.', ',', '，', '。', '；', ';') ?: ""  // URL
        // ② 密钥（多策略依次尝试）
        var key = Regex("sk-[A-Za-z0-9_\\-]{8,}").find(t)?.value ?: ""  // sk- 前缀
        if (key.isEmpty()) {                             // 无 sk- 前缀
            key = Regex("(?i)(?:api[-_]?key|key|token|authorization)\\s*[=:：]\\s*[\"']?([A-Za-z0-9_\\-.]{8,})").find(t)?.groupValues?.get(1) ?: ""  // key=xxx
        }
        if (key.isEmpty()) {                             // 仍无
            key = Regex("(?i)Bearer\\s+([A-Za-z0-9_\\-.]+)").find(t)?.groupValues?.get(1) ?: ""  // Bearer token
        }
        // ③ 模型名
        val model = Regex("(?i)model(?:name)?\\s*[=:：]\\s*[\"']?([A-Za-z0-9_\\-.]+)").find(t)?.groupValues?.get(1) ?: ""  // model=xxx
        // ④ 名称（从地址域名推断）
        val name = runCatching { Regex("https?://([^/]+)").find(url)?.groupValues?.get(1) ?: "" }.getOrDefault("")  // 域名
        return ParsedApiConfig(url = url, key = key, model = model, name = name)  // 返回结果
    }

    /** 测试连接（模拟，HTML testApiForm：75% 成功率）。 */
    fun testConnection() {                               // 测试连接
        val url = _formUrl.value.trim()                  // 地址
        val key = _formKey.value.trim()                  // 密钥
        if (url.isBlank() || key.isBlank()) {            // 缺字段
            _testState.value = "请先填写 API 地址与密钥" to StatusTone.FAIL  // 错误
            return
        }
        _testState.value = "正在测试连接…" to StatusTone.INFO  // 测试中
        viewModelScope.launch {                          // 协程中模拟延迟
            kotlinx.coroutines.delay(1200)               // 1.2 秒
            val ok = Math.random() > 0.25                // 75% 成功
            _testState.value = if (ok) {                 // 成功
                val model = _formModel.value.trim()      // 模型名
                (if (model.isNotBlank()) "连接成功 · 已识别模型 $model" else "连接成功") to StatusTone.SUCCESS  // 成功文案
            } else {                                     // 失败
                "连接失败，请检查地址与密钥" to StatusTone.FAIL  // 失败文案
            }
        }
    }

    /** 保存表单（HTML saveApiForm：必填校验 → 入库）。 */
    fun save() {                                         // 保存
        val name = _formName.value.trim()                // 名称
        val url = _formUrl.value.trim()                  // 地址
        val key = _formKey.value.trim()                  // 密钥
        val model = _formModel.value.trim()              // 模型
        val errors = mutableMapOf<String, String>()      // 错误集
        if (name.isBlank()) errors["name"] = "请输入 API 名称"  // 名校验
        if (url.isBlank()) errors["url"] = "请输入 API 地址"    // 地址空
        else if (!url.startsWith("http://") && !url.startsWith("https://")) errors["url"] = "API 地址需以 http:// 或 https:// 开头"  // 地址格式（HTML 同款正则）
        if (key.isBlank()) errors["key"] = "请输入 API 密钥"  // 密钥校验
        if (errors.isNotEmpty()) {                       // 有错
            _formErrors.value = errors                   // 显示错误
            return
        }
        val entry = ApiEntry(                            // 构造条目
            id = System.currentTimeMillis(),             // id 用时间戳（HTML Date.now()）
            name = name, type = if (_formTypeIndex.value == 0) "openai" else "custom",  // 类型
            url = url, key = key, model = model,         // 字段
        )
        _apis.value = _apis.value + entry                // 加入列表
        persist()                                        // 持久化
        _addVisible.value = false                        // 关闭弹窗
    }

    /** 切换当前 API（HTML setCurrentApi：选中即同步引擎配置）。 */
    fun setCurrent(id: Long) {                           // 切换当前
        val api = _apis.value.firstOrNull { it.id == id } ?: return  // 找不到返回
        _currentId.value = id                            // 更新当前
        viewModelScope.launch {                          // 协程中
            prefs.setCurrentApiId(id)                    // 持久化当前
            cloudRepo.save(api.url, api.key, api.model)  // 同步给引擎（CloudConfigRepository）
        }
    }

    /** 删除 API（HTML deleteApi：删当前则清空）。 */
    fun delete(id: Long) {                               // 删除
        _apis.value = _apis.value.filterNot { it.id == id }  // 移除
        persist()                                        // 持久化
        if (_currentId.value == id) {                    // 删的是当前
            _currentId.value = -1L                       // 清空当前
            viewModelScope.launch { prefs.setCurrentApiId(-1L) }  // 持久化
        }
    }
}