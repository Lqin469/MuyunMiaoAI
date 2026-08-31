package com.memuo.feature.chat                           // 声明包名：对话业务模块

import android.Manifest                                  // 导入 Manifest：权限常量
import android.content.Context                           // 导入 Context：文件跳转上下文
import android.content.Intent                            // 导入 Intent：语音识别意图
import android.content.pm.PackageManager                 // 导入 PackageManager：权限检查
import android.graphics.Bitmap                            // 导入 Bitmap：相机预览位图
import android.net.Uri                                   // 导入 Uri：内容标识
import android.speech.RecognitionListener                // 导入 RecognitionListener：识别回调
import android.speech.RecognizerIntent                   // 导入 RecognizerIntent：识别意图构造
import android.speech.SpeechRecognizer                   // 导入 SpeechRecognizer：系统语音识别
import androidx.activity.compose.rememberLauncherForActivityResult  // 导入 rememberLauncherForActivityResult：系统启动器
import androidx.activity.result.contract.ActivityResultContracts  // 导入 ActivityResultContracts：系统契约
import androidx.compose.animation.AnimatedVisibility      // 导入 AnimatedVisibility：显隐动画
import androidx.compose.animation.core.RepeatMode         // 导入 RepeatMode：循环模式
import androidx.compose.animation.core.animateFloat       // 导入 animateFloat：浮点动画
import androidx.compose.animation.core.animateFloatAsState  // 导入 animateFloatAsState：浮点状态动画
import androidx.compose.animation.core.infiniteRepeatable  // 导入 infiniteRepeatable：无限循环
import androidx.compose.animation.core.rememberInfiniteTransition  // 导入 rememberInfiniteTransition：无限动画状态
import androidx.compose.animation.core.tween              // 导入 tween：动画时长
import androidx.compose.animation.fadeIn                  // 导入 fadeIn：淡入
import androidx.compose.animation.fadeOut                 // 导入 fadeOut：淡出
import androidx.compose.animation.scaleIn                 // 导入 scaleIn：放大进入
import androidx.compose.animation.scaleOut                // 导入 scaleOut：缩小退出
import androidx.compose.foundation.Image                  // 导入 Image：位图渲染
import androidx.compose.foundation.background             // 导入 background：背景修饰
import androidx.compose.foundation.clickable              // 导入 clickable：点击修饰
import androidx.compose.foundation.horizontalScroll       // 导入 horizontalScroll：横向滚动
import androidx.compose.foundation.layout.Arrangement     // 导入 Arrangement：排列
import androidx.compose.foundation.layout.Box             // 导入 Box：盒式布局
import androidx.compose.foundation.layout.Column          // 导入 Column：纵向布局
import androidx.compose.foundation.layout.Row             // 导入 Row：横向布局
import androidx.compose.foundation.layout.Spacer          // 导入 Spacer：占位
import androidx.compose.foundation.layout.fillMaxSize     // 导入 fillMaxSize：铺满
import androidx.compose.foundation.layout.fillMaxWidth    // 导入 fillMaxWidth：占满宽度
import androidx.compose.foundation.layout.height          // 导入 height：固定高度
import androidx.compose.foundation.layout.imePadding      // 导入 imePadding：软键盘避让
import androidx.compose.foundation.layout.navigationBarsPadding  // 导入 navigationBarsPadding：底部手势条避让
import androidx.compose.foundation.layout.padding          // 导入 padding：内边距
import androidx.compose.foundation.layout.size            // 导入 size：固定尺寸
import androidx.compose.foundation.layout.width           // 导入 width：固定宽度
import androidx.compose.foundation.layout.widthIn         // 导入 widthIn：最大宽度约束
import androidx.compose.foundation.lazy.LazyColumn         // 导入 LazyColumn：消息列表
import androidx.compose.foundation.lazy.itemsIndexed       // 导入 itemsIndexed：带下标遍历
import androidx.compose.foundation.text.selection.SelectionContainer  // 导入 SelectionContainer：气泡文字长按复制
import androidx.compose.foundation.lazy.rememberLazyListState  // 导入 rememberLazyListState：列表状态（自动滚动）
import androidx.compose.foundation.shape.CircleShape       // 导入 CircleShape：圆形
import androidx.compose.foundation.shape.RoundedCornerShape  // 导入 RoundedCornerShape：圆角形状
import androidx.compose.material3.Icon                    // 导入 Icon：图标
import androidx.compose.material3.MaterialTheme           // 导入 MaterialTheme：主题
import androidx.compose.material3.Text                    // 导入 Text：文本
import androidx.compose.runtime.Composable                // 导入 Composable：可组合函数注解
import androidx.compose.runtime.DisposableEffect            // 导入 DisposableEffect：销毁副作用（释放识别器）
import androidx.compose.runtime.LaunchedEffect            // 导入 LaunchedEffect：副作用
import androidx.compose.runtime.collectAsState            // 导入 collectAsState：状态流→状态
import androidx.compose.runtime.getValue                  // 导入 getValue：by 委托
import androidx.compose.runtime.mutableStateOf            // 导入 mutableStateOf：可变状态
import androidx.compose.runtime.remember                  // 导入 remember：记住状态
import androidx.compose.runtime.setValue                  // 导入 setValue：by 委托写
import androidx.core.content.ContextCompat                 // 导入 ContextCompat：权限检查
import androidx.core.content.FileProvider                  // 导入 FileProvider：文件跳转生成 content URI
import androidx.compose.ui.Alignment                      // 导入 Alignment：对齐
import androidx.compose.ui.Modifier                       // 导入 Modifier：修饰
import androidx.compose.ui.draw.clip                      // 导入 clip：裁剪
import androidx.compose.ui.draw.rotate                     // 导入 rotate：旋转（加号变乘号）
import androidx.compose.ui.draw.scale                      // 导入 scale：缩放（麦克风脉冲）
import androidx.compose.ui.draw.shadow                    // 导入 shadow：投影
import androidx.compose.ui.graphics.Color                 // 导入 Color：颜色
import androidx.compose.ui.layout.ContentScale            // 导入 ContentScale：缩放模式
import androidx.compose.ui.platform.LocalContext          // 导入 LocalContext：上下文
import androidx.compose.ui.text.AnnotatedString           // 导入 AnnotatedString：富文本
import androidx.compose.ui.text.LinkAnnotation            // 导入 LinkAnnotation：可点击链接（文件路径）
import androidx.compose.ui.text.SpanStyle                 // 导入 SpanStyle：文本片段样式
import androidx.compose.ui.text.buildAnnotatedString      // 导入 buildAnnotatedString：构建富文本
import androidx.compose.ui.text.font.FontWeight           // 导入 FontWeight：字重
import androidx.compose.ui.text.style.TextDecoration      // 导入 TextDecoration：下划线
import androidx.compose.ui.text.style.TextOverflow        // 导入 TextOverflow：溢出省略
import androidx.compose.ui.text.withLink                  // 导入 withLink：添加链接
import androidx.compose.ui.text.withStyle                 // 导入 withStyle：添加样式
import androidx.compose.ui.unit.dp                        // 导入 dp：尺寸单位
import androidx.compose.ui.unit.sp                        // 导入 sp：字号单位
import androidx.hilt.navigation.compose.hiltViewModel     // 导入 hiltViewModel：Hilt 提供 ViewModel
import com.memuo.core.db.entity.ChatMessage                // 导入消息实体
import com.memuo.core.ui.AppIcons                          // 导入应用图标集
import com.memuo.core.ui.components.LocalToast             // 导入 Toast 状态
import com.memuo.core.ui.rememberBitmap                    // 导入位图加载
import com.memuo.core.ui.theme.MuyunAccentLight            // 导入浅灰底
import com.memuo.core.ui.theme.MuyunBar                    // 导入顶栏半透明背景
import com.memuo.core.ui.theme.MuyunBrand                  // 导入品牌色
import com.memuo.core.ui.theme.MuyunBrandGradient          // 导入品牌渐变
import com.memuo.core.ui.theme.MuyunBrandSoft              // 导入品牌浅底
import com.memuo.core.ui.theme.MuyunCard                   // 导入卡片白
import com.memuo.core.ui.theme.MuyunDanger                 // 导入危险红
import com.memuo.core.ui.theme.MuyunDisabled               // 导入禁用色（发送按钮禁用态）
import com.memuo.core.ui.theme.MuyunText                   // 导入主文字色
import com.memuo.core.ui.theme.MuyunText2                  // 导入次级文字色
import com.memuo.core.ui.theme.MuyunText3                  // 导入三级文字色
import com.memuo.core.ui.theme.MuyunTitleGradient          // 导入标题渐变
import com.memuo.core.ui.theme.MuyunUserBubbleGradient     // 导入用户气泡渐变
import java.io.File                                        // 导入 File：相机临时文件
import java.text.SimpleDateFormat                          // 导入 SimpleDateFormat：时间格式化
import java.util.Calendar                                 // 导入 Calendar：日期
import java.util.Date                                     // 导入 Date：日期
import java.util.Locale                                   // 导入 Locale：区域

/**
 * 对话页 —— AI 对话界面（HTML 主对话页「AI 模式」完整迁移）。
 * 对应 HTML 交互：渐变问候 + 日期 + 快捷提问、气泡（用户深色渐变/助手白卡、非对称圆角）、
 * 时间戳合并（>5 分钟才显示）、✓✓ 发送状态、流式打字光标、重新生成、
 * 图片/文件暂存预览、加号菜单（拍照/相册/文件）、麦克风、壁纸背景。
 * 顶栏（菜单 + 会话胶囊 + 新建/云本地切换）由 MainActivity 全局渲染。
 */
@Composable                                               // 可组合 UI 函数
fun ChatScreen(                                           // 对话页
    conversationId: Long,                                 // 当前会话 ID
    viewModel: ChatViewModel = hiltViewModel(),           // 用 Hilt 获取 ViewModel
) {
    LaunchedEffect(conversationId) {                      // 会话 ID 变化时
        viewModel.openConversation(conversationId)        // 加载该会话的消息
    }
    val messages by viewModel.messages.collectAsState()   // 订阅消息列表
    val streaming by viewModel.streaming.collectAsState() // 订阅流式状态
    val streamText by viewModel.streamText.collectAsState()  // 订阅流式文本
    val attachments by viewModel.attachments.collectAsState()  // 订阅附件映射
    val engineMessage by viewModel.engineMessage.collectAsState()  // 订阅切换提示
    val toast = LocalToast.current                        // 取全局 Toast
    val context = LocalContext.current                    // 取上下文
    val listState = rememberLazyListState()               // 列表滚动状态

    var input by remember { mutableStateOf("") }          // 输入框内容
    var previews by remember { mutableStateOf(listOf<Attachment>()) }  // 暂存附件（HTML previewImages）
    var attachOpen by remember { mutableStateOf(false) }  // 加号菜单开关
    var recording by remember { mutableStateOf(false) }   // 麦克风录音态

    // 引擎切换提示（HTML showToast 行为）
    LaunchedEffect(engineMessage) {                       // 提示变化
        engineMessage?.let { toast.show(it); viewModel.consumeEngineMessage() }  // 弹 Toast 并消费
    }

    // 新消息/流式文本变化时自动滚到底部（HTML 每条消息后 scrollTop = scrollHeight）
    // 流式中改用 scrollToItem 即时滚动，避免逐字 animate 造成的动画抖动/卡顿
    LaunchedEffect(messages.size, streamText) {           // 消息数/流式文本变化
        if (messages.isNotEmpty() || streaming) {         // 有消息或流式中
            val target = (messages.size + if (streaming) 1 else 0) - 1  // 目标下标（最后一条）
            if (target >= 0) listState.scrollToItem(target)  // 直接滚到底（无动画，保证流畅）
        }
    }

    // 语音识别真实现：麦克风 → 权限检查 → 系统 SpeechRecognizer 识别 → 结果填入输入框
    val speechRecognizer = remember {                     // 懒创建识别器（设备不支持则 null）
        if (SpeechRecognizer.isRecognitionAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context) else null  // 系统支持则创建
    }
    fun startVoiceListening() {                          // 启动真实语音识别（局部函数，供权限回调/录音态调用）
        val sr = speechRecognizer ?: return               // 无识别器直接返回
        sr.setRecognitionListener(object : RecognitionListener {  // 识别回调
            override fun onResults(results: android.os.Bundle?) {  // 最终识别结果
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()  // 取第一个候选
                if (text.isNotBlank()) {                 // 有识别内容
                    input = text                         // 填入输入框
                    toast.show("已识别，请点击发送")       // 提示
                } else toast.show("未识别到内容，请重试")  // 空结果提示
                recording = false                        // 复位录音态
            }
            override fun onError(error: Int) {           // 识别出错
                recording = false                        // 复位
                toast.show("语音识别失败，请重试")         // 提示
            }
            override fun onReadyForSpeech(params: android.os.Bundle?) {}  // 就绪
            override fun onBeginningOfSpeech() {}        // 开始说话
            override fun onRmsChanged(rmsdB: Float) {}   // 音量变化
            override fun onBufferReceived(buffer: ByteArray?) {}  // 音频缓冲
            override fun onEndOfSpeech() {}              // 结束说话
            override fun onPartialResults(partialResults: android.os.Bundle?) {}  // 部分结果
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}  // 事件
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {  // 识别意图
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)  // 自由模式
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")  // 中文识别
        }
        sr.startListening(intent)                        // 开始监听
    }
    val voicePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->  // 权限结果
        if (granted) startVoiceListening()               // 授权后启动识别
        else {                                           // 拒绝
            recording = false                            // 复位录音态
            toast.show("未授予录音权限，无法语音输入")      // 提示
        }
    }
    DisposableEffect(Unit) {                             // 离开页面时释放识别器
        onDispose { speechRecognizer?.destroy() }        // 释放系统资源
    }
    LaunchedEffect(recording) {                          // 录音态变化
        if (recording) {                                 // 开始语音输入
            when {                                       // 分支处理
                speechRecognizer == null -> {            // 设备不支持
                    recording = false                    // 复位
                    toast.show("当前设备不支持语音识别")    // 提示
                }
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ->  // 无录音权限
                    voicePermission.launch(Manifest.permission.RECORD_AUDIO)  // 请求权限
                else -> startVoiceListening()            // 已授权，直接启动识别
            }
        }
    }

    // —— 系统选择器：拍照（缩略图直返，无需 FileProvider）/ 相册多选 / 文件多选 ——
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->  // 拍照结果
        bmp?.let {                                        // 有结果
            val file = File(context.cacheDir, "cam_${System.currentTimeMillis()}.jpg")  // 临时文件
            file.outputStream().use { it.write(bitmapToBytes(bmp)) }  // 写 JPEG
            previews = previews + Attachment(             // 加入暂存
                kind = AttachmentKind.IMAGE,              // 图片
                name = "拍照 ${previews.size + 1}",       // 名称
                uri = Uri.fromFile(file).toString(),      // Uri
                sizeText = fmtBytes(file.length()),       // 大小
            )
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(9)) { uris ->  // 相册结果
        uris.forEach { uri ->                             // 逐张处理
            val size = runCatching { context.contentResolver.openFileDescriptor(uri, "r")?.statSize ?: 0L }.getOrDefault(0L)  // 文件大小
            previews = previews + Attachment(             // 加入暂存
                kind = AttachmentKind.IMAGE,              // 图片
                name = uri.lastPathSegment ?: "图片",     // 名称
                uri = uri.toString(),                     // Uri
                sizeText = fmtBytes(size),                // 大小
            )
        }
    }
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->  // 文件结果
        uris.forEach { uri ->                             // 逐个处理
            val size = runCatching { context.contentResolver.openFileDescriptor(uri, "r")?.statSize ?: 0L }.getOrDefault(0L)  // 文件大小
            previews = previews + Attachment(             // 加入暂存
                kind = AttachmentKind.FILE,               // 文件
                name = uri.lastPathSegment ?: "文件",     // 名称
                uri = uri.toString(),                     // Uri
                sizeText = fmtBytes(size),                // 大小
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {              // 根容器
        Column(modifier = Modifier.fillMaxSize()) {       // 内容层（壁纸由 MainActivity 全局背景统一渲染）
            // —— 消息区（空态显示欢迎页，对应 HTML chat-welcome / chat-msgs）——
            Box(                                          // 消息区容器
                modifier = Modifier                        // 修饰
                    .weight(1f)                           // 占满剩余
                    .fillMaxWidth(),                      // 占满宽度
            ) {
                if (messages.isEmpty() && !streaming) {   // 空会话 → 欢迎页
                    WelcomeArea(                          // 欢迎区
                        onQuickAsk = { viewModel.send(it) },  // 快捷提问直接发送
                        modifier = Modifier.align(Alignment.Center),  // 居中
                    )
                } else {                                  // 有消息 → 列表
                    LazyColumn(                           // 懒加载列表
                        state = listState,                // 绑定滚动状态
                        modifier = Modifier              // 修饰
                            .fillMaxSize()               // 铺满
                            .padding(horizontal = 24.dp, vertical = 20.dp),  // HTML .chat-body padding 28/24 近似
                    ) {
                        itemsIndexed(messages, key = { _, m -> m.id }) { index, msg ->  // 遍历消息
                            MessageRow(                   // 单条消息行
                                msg = msg,                // 消息
                                showTime = shouldShowTime(messages, index),  // 时间戳合并规则
                                attachments = attachments[msg.id].orEmpty(),  // 附件
                                isLast = index == messages.lastIndex,  // 是否最后一条
                                streaming = streaming,    // 流式状态
                                onRegenerate = { viewModel.regenerate() },  // 重新生成
                            )
                        }
                        if (streaming) {                  // 流式中追加占位气泡
                            item {                        // 单个列表项
                                MessageRow(               // 渲染流式中的助手气泡
                                    msg = ChatMessage(convId = conversationId, role = "assistant", content = streamText, ts = System.currentTimeMillis()),  // 临时消息
                                    showTime = false,     // 流式气泡不显示时间
                                    attachments = emptyList(),  // 无附件
                                    isLast = true,        // 视为最后一条
                                    streaming = true,     // 流式（显示光标）
                                    onRegenerate = {},    // 流式中不可重新生成
                                )
                            }
                        }
                    }
                }
            }

            // —— 附件暂存预览区（HTML .chat-preview-wrap，横向滚动缩略图）——
            AnimatedVisibility(                           // 显隐动画
                visible = previews.isNotEmpty(),          // 有附件才显示
                enter = fadeIn() + scaleIn(initialScale = 0.95f),  // 淡入放大
                exit = fadeOut() + scaleOut(targetScale = 0.95f),  // 淡出缩小
            ) {
                Column(                                   // 预览区
                    modifier = Modifier                  // 修饰
                        .fillMaxWidth()                  // 占满宽度
                        .background(MuyunBar)            // 半透明浅灰底（透出壁纸，暗色自适应）
                        .padding(horizontal = 16.dp, vertical = 8.dp),  // 内边距
                ) {
                    Row(                                  // 预览区头部（计数 + 清空）
                        modifier = Modifier.fillMaxWidth(),  // 占满宽度
                    ) {
                        Text(                             // 计数（HTML chat-preview-hint）
                            text = "已选择 ${previews.size} 项",  // 内容
                            style = MaterialTheme.typography.labelSmall,  // 小字（HTML 11px）
                            color = MuyunText3,           // 三级灰
                        )
                        Spacer(Modifier.weight(1f))       // 占位
                        Text(                             // 清空按钮（HTML chat-preview-clear）
                            text = "清空",                // 内容
                            style = MaterialTheme.typography.labelSmall,  // 小字
                            color = MuyunText3,           // 三级灰
                            modifier = Modifier.clickable { previews = emptyList() },  // 点击清空
                        )
                    }
                    Row(                                  // 横向缩略图列表
                        modifier = Modifier              // 修饰
                            .fillMaxWidth()              // 占满宽度
                            .horizontalScroll(androidx.compose.foundation.rememberScrollState())  // 横向滚动
                            .padding(top = 6.dp),        // 上留白
                        horizontalArrangement = Arrangement.spacedBy(8.dp),  // 间距 8（HTML gap 8）
                    ) {
                        previews.forEach { att ->         // 遍历附件
                            PreviewItem(                  // 单条预览
                                attachment = att,        // 附件
                                onRemove = { previews = previews - att },  // 移除
                            )
                        }
                    }
                }
            }

            // —— 加号菜单（HTML .attach-menu：输入栏上方展开，选项交错动画）——
            AttachMenu(                                   // 加号菜单
                open = attachOpen,                        // 展开状态
                onCamera = { attachOpen = false; cameraLauncher.launch(null) },  // 拍照
                onGallery = { attachOpen = false; galleryLauncher.launch(PickVisualMediaRequestCompat) },  // 相册
                onFile = { attachOpen = false; fileLauncher.launch(arrayOf("*/*")) },  // 文件
                modifier = Modifier.padding(start = 20.dp),  // 左对齐（HTML left 20px）
            )

            // —— 底部输入栏（HTML .chat-input-bar）——
            Row(                                          // 输入栏
                modifier = Modifier                      // 修饰
                    .fillMaxWidth()                      // 占满宽度
                    .background(MuyunBar)                // 半透明浅灰底（透出壁纸，暗色自适应）
                    .padding(horizontal = 16.dp, vertical = 10.dp)  // 内边距（HTML padding 10px 16px）
                    .navigationBarsPadding()             // 避让底部手势条
                    .imePadding(),                       // 键盘弹出整体上移（HTML v21 键盘适配）
                verticalAlignment = Alignment.CenterVertically,  // 垂直居中
            ) {
                // 加号按钮（展开时旋转 45° 变乘号，HTML .chat-plus-btn）
                val plusRotation by animateFloatAsState(if (attachOpen) 45f else 0f, label = "plusRot")  // 旋转动画
                Box(                                      // 圆形按钮
                    modifier = Modifier                  // 修饰
                        .size(38.dp)                     // 38dp（HTML .icon-btn 38）
                        .clip(CircleShape)               // 圆形
                        .background(MuyunAccentLight)    // 浅灰底
                        .clickable { attachOpen = !attachOpen },  // 点击开关菜单
                    contentAlignment = Alignment.Center,  // 居中
                ) {
                    Icon(                                // 加号图标
                        imageVector = AppIcons.Plus,      // 图标
                        contentDescription = "添加附件",   // 描述
                        tint = MuyunText2,                // 次级灰
                        modifier = Modifier              // 修饰
                            .size(18.dp)                 // 18dp
                            .rotate(plusRotation),       // 旋转（动画）
                    )
                }
                // 输入框（HTML .chat-input-wrap：圆角 22 浅灰底，内含麦克风）
                Box(                                      // 输入框容器
                    modifier = Modifier                  // 修饰
                        .weight(1f)                      // 占满剩余
                        .padding(horizontal = 10.dp)     // 两侧留白（HTML gap 10）
                        .height(40.dp)                   // 高度 40（HTML .chat-input-wrap）
                        .clip(RoundedCornerShape(22.dp)) // 圆角 22
                        .background(MuyunAccentLight),   // 浅灰底
                ) {
                    Row(                                  // 输入框内部
                        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),  // 内边距（HTML padding 0 18px）
                        verticalAlignment = Alignment.CenterVertically,  // 垂直居中
                    ) {
                        androidx.compose.foundation.text.BasicTextField(  // 无边框输入
                            value = input,                // 绑定输入
                            onValueChange = { input = it },  // 更新输入
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MuyunText),  // 字体（HTML 15px）
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(MuyunBrand),  // 光标品牌色
                            modifier = Modifier.weight(1f),  // 占满剩余
                            decorationBox = { inner ->     // 占位符
                                if (input.isEmpty()) {    // 空输入
                                    Text("输入消息", color = MuyunText3, style = MaterialTheme.typography.bodyLarge)  // 占位（主题自适应三级灰）
                                }
                                inner()                   // 输入区
                            },
                        )
                        // 麦克风（录音态红底脉冲，HTML .mic-btn.recording）
                        val micScale by animateFloatAsState(if (recording) 1.15f else 1f, label = "mic")  // 脉冲动画
                        Box(                              // 麦克风按钮
                            modifier = Modifier          // 修饰
                                .size(28.dp)             // 28dp（HTML .mic-btn）
                                .clip(CircleShape)       // 圆形
                                .background(if (recording) MuyunDanger else Color.Transparent)  // 录音红底
                                .clickable { recording = !recording },  // 点击开关录音
                            contentAlignment = Alignment.Center,  // 居中
                        ) {
                            Icon(                        // 麦克风图标
                                imageVector = AppIcons.Mic,  // 图标
                                contentDescription = "语音输入",  // 描述
                                tint = if (recording) Color.White else MuyunText3,  // 录音白/常态灰
                                modifier = Modifier      // 修饰
                                    .size(15.dp)         // 15dp（HTML svg 15）
                                    .scale(micScale),    // 脉冲缩放
                            )
                        }
                    }
                }
                // 发送按钮（品牌渐变圆钮，HTML v19 .chat-input-bar > .icon-btn）
                val canSend = input.isNotBlank() || previews.isNotEmpty()  // 是否可发送（文字或附件至少一项）
                Box(                                      // 发送按钮
                    modifier = Modifier                  // 修饰
                        .size(38.dp)                     // 38dp
                        .shadow(if (canSend) 6.dp else 0.dp, CircleShape)  // 投影（禁用无投影）
                        .clip(CircleShape)               // 圆形
                        .background(                     // 背景：可发送品牌渐变 / 禁用灰
                            if (canSend) MuyunBrandGradient
                            else androidx.compose.ui.graphics.SolidColor(MuyunDisabled)
                        )
                        .clickable(enabled = canSend) {   // 点击发送（禁用时不响应）
                            viewModel.send(input, previews)  // 发送
                            input = ""                    // 清空输入
                            previews = emptyList()        // 清空暂存
                        },
                    contentAlignment = Alignment.Center,  // 居中
                ) {
                    Icon(                                // 发送图标
                        imageVector = AppIcons.Send,      // 纸飞机
                        contentDescription = "发送",       // 描述
                        tint = if (canSend) Color.White else MuyunText3,  // 可发送白 / 禁用灰
                        modifier = Modifier.size(18.dp),  // 18dp
                    )
                }
            }
        }
    }
}

/** 相册选择契约别名（PickMultipleVisualMedia 需指定最大张数，HTML 允许多选）。 */
private val PickVisualMediaRequestCompat = androidx.activity.result.PickVisualMediaRequest(  // 构造请求
    androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly  // 仅图片
)

/** 用户气泡深色渐变画刷（随主题自适应：暗色下更亮，避免深气泡消失在深背景中）。 */
private val MuyunUserBubbleBrush: androidx.compose.ui.graphics.Brush get() = MuyunUserBubbleGradient  // 画刷（getter）

/** 欢迎区 —— 渐变问候 + 日期 + 快捷提问（对应 HTML chat-welcome/chat-greeting/quick-btn）。 */
@Composable                                               // 可组合函数
private fun WelcomeArea(                                  // 欢迎区
    onQuickAsk: (String) -> Unit,                         // 快捷提问回调
    modifier: Modifier = Modifier,                        // 外部修饰
) {
    Column(modifier = modifier) {                         // 纵向布局
        // 渐变问候（HTML .chat-greeting：品牌渐变文字）
        Text(                                             // 问候文字
            text = greeting(),                            // 按时段问候（HTML updateGreeting）
            fontSize = 32.sp,                             // 32px（HTML 32px）
            fontWeight = FontWeight.Bold,                 // 粗体（HTML 700）
            color = MuyunBrand,                           // 品牌色
            style = MaterialTheme.typography.headlineLarge.copy(  // 大标题样式
                brush = MuyunTitleGradient,               // 渐变画刷（HTML background-clip: text）
            ),
        )
        Text(                                             // 日期（HTML .chat-date）
            text = todayText(),                           // 今天日期
            fontSize = 13.sp,                             // 13px
            color = MuyunText3,                           // 三级灰
            modifier = Modifier.padding(top = 6.dp, bottom = 32.dp),  // 上下留白（HTML margin-bottom 32px）
        )
        listOf("今天有什么安排？", "帮我记住一件事", "设个明天的提醒").forEach { q ->  // 三个快捷提问（HTML 固定文案）
            Box(                                          // 快捷按钮（HTML .quick-btn）
                modifier = Modifier                      // 修饰
                    .padding(bottom = 12.dp)             // 下留白（HTML margin-bottom 12px）
                    .clip(RoundedCornerShape(22.dp))     // 圆角 22
                    .background(MuyunCard)               // 白底
                    .shadow(1.dp, RoundedCornerShape(22.dp))  // 轻投影
                    .clickable { onQuickAsk(q) }         // 点击发送
                    .padding(horizontal = 20.dp, vertical = 12.dp),  // 内边距（HTML padding 12px 20px）
            ) {
                Text(                                     // 按钮文字
                    text = q,                             // 提问内容
                    fontSize = 14.sp,                     // 14px
                    color = MuyunText,                    // 主文字色
                )
            }
        }
    }
}

/** 单条消息行 —— 附件网格 + 气泡 + 元信息（时间/状态）+ AI 操作（重新生成）。 */
@Composable                                               // 可组合函数
private fun MessageRow(                                   // 消息行
    msg: ChatMessage,                                     // 消息数据
    showTime: Boolean,                                    // 是否显示时间戳
    attachments: List<Attachment>,                        // 附件列表
    isLast: Boolean,                                      // 是否最后一条
    streaming: Boolean,                                   // 是否流式中
    onRegenerate: () -> Unit,                             // 重新生成回调
) {
    val isUser = msg.role == "user"                       // 是否用户消息
    val context = LocalContext.current                    // 上下文（文件跳转打开文件）
    val toast = LocalToast.current                        // Toast（文件跳转提示）
    val showCursor = streaming && !isUser && isLast && msg.content.isNotBlank()  // 流式光标（最后一条助手消息）
    Column(                                               // 纵向（气泡 + 元信息）
        modifier = Modifier                              // 修饰
            .fillMaxWidth()                              // 占满宽度
            .padding(vertical = 5.dp),                   // 上下留白
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,  // 用户靠右/助手靠左
    ) {
        // 附件区（图片网格 + 文件列表，对应 HTML msg-img-wrap / msg-files）
        if (attachments.isNotEmpty()) {                   // 有附件
            Column(                                       // 附件列
                modifier = Modifier.padding(bottom = 4.dp).width(260.dp),  // 最大宽 260（HTML max-width）
                horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,  // 对齐跟随气泡
            ) {
                val imgs = attachments.filter { it.kind == AttachmentKind.IMAGE }  // 图片附件
                val files = attachments.filter { it.kind == AttachmentKind.FILE }  // 文件附件
                if (imgs.isNotEmpty()) {                  // 有图片
                    Row(                                  // 图片网格（自动换行简化：横向排）
                        modifier = Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()),  // 横向滚动
                        horizontalArrangement = Arrangement.spacedBy(4.dp),  // 间距 4
                    ) {
                        imgs.forEach { img ->             // 遍历图片
                            val bmp = rememberBitmap(Uri.parse(img.uri), maxSize = 320)  // 加载缩略图（HTML 压缩 320px）
                            Box(                          // 图片容器
                                modifier = Modifier      // 修饰
                                    .size(96.dp)         // 96dp（HTML .msg-img 96×96）
                                    .clip(RoundedCornerShape(10.dp))  // 圆角 10
                                    .background(MuyunAccentLight),  // 占位底
                            ) {
                                bmp?.let {               // 有位图
                                    Image(               // 渲染图片
                                        bitmap = it,     // 位图
                                        contentDescription = img.name,  // 描述
                                        contentScale = ContentScale.Crop,  // 裁切
                                        modifier = Modifier.fillMaxSize(),  // 铺满
                                    )
                                }
                            }
                        }
                    }
                }
                if (files.isNotEmpty()) {                 // 有文件
                    files.forEach { f ->                  // 遍历文件
                        Row(                              // 文件行（HTML .msg-file-item）
                            modifier = Modifier         // 修饰
                                .fillMaxWidth()         // 占满宽度
                                .padding(top = 4.dp)    // 上留白
                                .clip(RoundedCornerShape(10.dp))  // 圆角 10
                                .background(if (isUser) Color(0x26FFFFFF) else MuyunAccentLight)  // 用户半透白/助手浅灰
                                .padding(horizontal = 10.dp, vertical = 8.dp),  // 内边距
                            verticalAlignment = Alignment.CenterVertically,  // 垂直居中
                        ) {
                            Box(                         // 文件图标底
                                modifier = Modifier     // 修饰
                                    .size(30.dp)        // 30dp（HTML .att-icon）
                                    .clip(RoundedCornerShape(7.dp))  // 圆角 7
                                    .background(MuyunBrandSoft),  // 品牌浅底
                                contentAlignment = Alignment.Center,  // 居中
                            ) {
                                Icon(                   // 文件图标
                                    imageVector = AppIcons.File,  // 图标
                                    contentDescription = null,  // 装饰
                                    tint = MuyunBrand,  // 品牌色
                                    modifier = Modifier.size(15.dp),  // 15dp
                                )
                            }
                            Column(modifier = Modifier.padding(start = 8.dp)) {  // 文件名+大小
                                Text(                   // 文件名
                                    text = f.name,      // 内容
                                    fontSize = 11.sp,   // 11px（HTML .att-name）
                                    fontWeight = FontWeight.Medium,  // 中粗
                                    color = if (isUser) Color.White else MuyunText,  // 用户白/助手主色
                                    maxLines = 1,       // 单行
                                    overflow = TextOverflow.Ellipsis,  // 省略
                                )
                                Text(                   // 文件大小
                                    text = f.sizeText,  // 内容
                                    fontSize = 9.sp,    // 9px（HTML .att-size）
                                    color = if (isUser) Color.White.copy(alpha = 0.6f) else MuyunText3,  // 弱化色
                                )
                            }
                        }
                    }
                }
            }
        }
        // 气泡（HTML .chat-bubble）
        val bubbleShape = if (isUser) RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp) else RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)  // 非对称圆角
        Box(                                              // 气泡容器
            modifier = Modifier                          // 修饰
                .widthIn(max = 300.dp)                   // 最大宽（HTML max-width 78% 近似）
                .clip(bubbleShape)                       // 圆角
                .background(                             // 气泡背景
                    if (isUser) MuyunUserBubbleBrush     // 用户深色渐变（画刷）
                    else androidx.compose.ui.graphics.SolidColor(MuyunCard),  // 助手白色纯色画刷
                )
                .then(if (!isUser) Modifier.shadow(1.dp, bubbleShape) else Modifier)  // 助手气泡投影
                .padding(horizontal = 14.dp, vertical = 11.dp),  // 内边距（HTML padding 11px 14px）
        ) {
            SelectionContainer {                          // 长按选中复制（报错文字可复制）
                Text(                                     // 气泡文字（文件路径渲染为可点击链接）
                    text = buildPathAnnotated(            // 识别路径 → 可点击
                        content = msg.content.ifBlank { "…" },  // 空内容占位
                        linkColor = if (isUser) Color.White else MuyunBrand,  // 链接色（用户白/助手品牌色）
                        onOpenPath = { path -> openPath(context, path) { toast.show(it) } },  // 点击路径打开文件
                    ),
                    fontSize = 14.sp,                     // 14px（HTML .chat-bubble）
                    lineHeight = 22.sp,                   // 行距（HTML 1.6 × 14 ≈ 22）
                    color = if (isUser) Color.White else MuyunText,  // 用户白/助手主色
                )
            }
            if (showCursor) {                             // 流式光标（HTML .typing::after）
                val cursorAlpha by rememberInfiniteTransition(label = "cursor").animateFloat(  // 闪烁动画
                    initialValue = 1f,                    // 起始不透明
                    targetValue = 0f,                     // 目标透明
                    animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),  // 无限往复
                    label = "cursorAlpha",                // 标签
                )
                Text(                                     // 光标字符
                    text = "▌",                           // 光标
                    fontSize = 14.sp,                     // 字号
                    color = if (isUser) Color.White else MuyunText,  // 跟随文字色
                    modifier = Modifier.padding(start = 2.dp),  // 微留白
                )
            }
        }
        // 元信息行（时间 + 状态，对应 HTML .msg-meta）
        Row(                                              // 元信息
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),  // 内边距
            verticalAlignment = Alignment.CenterVertically,  // 垂直居中
        ) {
            if (showTime) {                               // 需要显示时间
                Text(                                     // 时间
                    text = fmtTime(msg.ts),               // HH:mm
                    fontSize = 10.sp,                     // 10px（HTML .msg-meta）
                    color = MuyunText3,                   // 三级灰
                )
            }
            Text(                                         // 发送状态（✓✓）
                text = if (isUser) "✓✓" else "✓✓",        // 双勾（HTML msg-status）
                fontSize = 11.sp,                         // 11px
                color = if (isUser) MuyunBrand else MuyunText3,  // 用户已读品牌色/助手灰（HTML .read/.sent）
                modifier = Modifier.padding(start = 4.dp),  // 留白
            )
        }
        // AI 操作：重新生成（仅最后一条助手消息且非流式中，HTML .bubble-actions 移动端常显简化）
        if (!isUser && isLast && !streaming && msg.content.isNotBlank()) {  // 可重新生成
            Box(                                          // 重新生成按钮
                modifier = Modifier                      // 修饰
                    .padding(top = 4.dp)                 // 上留白
                    .clip(RoundedCornerShape(8.dp))      // 圆角
                    .clickable { onRegenerate() }        // 点击
                    .padding(horizontal = 6.dp, vertical = 2.dp),  // 内边距
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {  // 图标+文字
                    Icon(                                // 重新生成图标
                        imageVector = AppIcons.Regenerate,  // 图标
                        contentDescription = null,       // 装饰
                        tint = MuyunText3,               // 三级灰
                        modifier = Modifier.size(12.dp), // 12dp
                    )
                    Text(                                // 文字
                        text = "重新生成",                // 内容
                        fontSize = 11.sp,                // 11px（HTML .bubble-actions）
                        color = MuyunText3,              // 三级灰
                        modifier = Modifier.padding(start = 3.dp),  // 留白
                    )
                }
            }
        }
    }
}

/** 加号菜单 —— 拍照/相册/文件 三选项，交错淡入动画（对应 HTML .attach-menu）。 */
@Composable                                               // 可组合函数
private fun AttachMenu(                                   // 加号菜单
    open: Boolean,                                        // 展开状态
    onCamera: () -> Unit,                                 // 拍照
    onGallery: () -> Unit,                                // 相册
    onFile: () -> Unit,                                   // 文件
    modifier: Modifier = Modifier,                        // 外部修饰
) {
    Column(                                               // 纵向选项
        modifier = modifier                             // 外部修饰
            .padding(bottom = 10.dp),                   // 与输入栏留白
        verticalArrangement = Arrangement.spacedBy(14.dp),  // 间距 14（HTML gap 14px）
    ) {
        listOf(                                           // 三个选项
            Triple(AppIcons.Camera, "拍照", onCamera),     // 拍照
            Triple(AppIcons.Gallery, "从相册选择", onGallery),  // 相册
            Triple(AppIcons.File, "选择文件", onFile),    // 文件
        ).forEachIndexed { index, (icon, label, action) ->  // 遍历选项
            // 展开延迟自上而下 0.05/0.11/0.17s；收起自下而上（HTML 同款交错动画）
            val enterDelay = index * 60                   // 展开延迟
            val exitDelay = (2 - index) * 60              // 收起延迟
            AnimatedVisibility(                           // 单选项动画
                visible = open,                           // 绑定展开状态
                enter = fadeIn(tween(300, delayMillis = enterDelay)) + scaleIn(initialScale = 0.6f, animationSpec = tween(300, delayMillis = enterDelay)),  // 淡入放大
                exit = fadeOut(tween(300, delayMillis = exitDelay)) + scaleOut(targetScale = 0.6f, animationSpec = tween(300, delayMillis = exitDelay)),  // 淡出缩小
            ) {
                Row(                                      // 图标 + 标签
                    modifier = Modifier                  // 修饰
                        .clip(RoundedCornerShape(20.dp))  // 圆角（扩大点击热区视觉）
                        .clickable(enabled = open) { action() },  // 整行可点（收起态不拦截，HTML pointer-events: none）
                    verticalAlignment = Alignment.CenterVertically,  // 垂直居中
                ) {
                    Box(                                  // 圆形图标底（HTML .attach-option-icon）
                        modifier = Modifier              // 修饰
                            .size(40.dp)                 // 40dp
                            .clip(CircleShape)           // 圆形
                            .background(MuyunCard)       // 白底
                            .shadow(8.dp, CircleShape)   // 大投影（HTML --shadow-lg）
                            .clickable(enabled = open) { action() },  // 点击
                        contentAlignment = Alignment.Center,  // 居中
                    ) {
                        Icon(                            // 图标
                            imageVector = icon,          // 矢量
                            contentDescription = label,  // 描述
                            tint = MuyunText,            // 主文字色
                            modifier = Modifier.size(17.dp),  // 17dp
                        )
                    }
                    Box(                                  // 深色标签（HTML .attach-option-label）
                        modifier = Modifier              // 修饰
                            .padding(start = 8.dp)       // 留白
                            .clip(RoundedCornerShape(13.dp))  // 胶囊圆角
                            .background(Color(0xBF1E1E1E))  // 深色半透明（HTML rgba(30,30,30,0.75)）
                            .padding(horizontal = 12.dp, vertical = 5.dp),  // 内边距
                    ) {
                        Text(                            // 标签文字
                            text = label,                // 内容
                            fontSize = 12.sp,            // 12px
                            fontWeight = FontWeight.Medium,  // 中粗
                            color = Color.White,         // 白
                        )
                    }
                }
            }
        }
    }
}

/** 暂存预览项 —— 图片缩略图 / 文件卡片 + 右上角移除按钮（对应 HTML .chat-preview-item）。 */
@Composable                                               // 可组合函数
private fun PreviewItem(                                  // 预览项
    attachment: Attachment,                               // 附件
    onRemove: () -> Unit,                                 // 移除回调
) {
    Box(                                                  // 容器
        modifier = Modifier                              // 修饰
            .size(64.dp)                                 // 64dp（HTML 64×64）
            .clip(RoundedCornerShape(10.dp))             // 圆角 10
            .background(MuyunCard)                       // 白底
            .shadow(1.dp, RoundedCornerShape(10.dp)),    // 轻投影
    ) {
        if (attachment.kind == AttachmentKind.IMAGE) {    // 图片附件
            val bmp = rememberBitmap(Uri.parse(attachment.uri), maxSize = 320)  // 加载缩略图
            bmp?.let {                                    // 有位图
                Image(                                    // 渲染
                    bitmap = it,                          // 位图
                    contentDescription = attachment.name,  // 描述
                    contentScale = ContentScale.Crop,     // 裁切
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(9.dp)),  // 内圆角 9
                )
            }
        } else {                                          // 文件附件
            Column(                                       // 图标 + 文件名
                modifier = Modifier.fillMaxSize().padding(4.dp),  // 内边距
                horizontalAlignment = Alignment.CenterHorizontally,  // 居中
                verticalArrangement = Arrangement.Center,  // 垂直居中
            ) {
                Icon(                                     // 文件图标
                    imageVector = AppIcons.File,          // 图标
                    contentDescription = null,            // 装饰
                    tint = MuyunBrand,                    // 品牌色
                    modifier = Modifier.size(20.dp),      // 20dp（HTML svg 20）
                )
                Text(                                     // 文件名
                    text = attachment.name,               // 内容
                    fontSize = 9.sp,                      // 9px（HTML .file-name）
                    color = MuyunText2,                   // 次级灰
                    maxLines = 1,                         // 单行
                    overflow = TextOverflow.Ellipsis,     // 省略
                    modifier = Modifier.padding(top = 4.dp).width(56.dp),  // 最大宽 56（HTML max-width）
                )
            }
        }
        // 右上角移除按钮（视觉 18dp，热区扩大到 44dp，对应 HTML .chat-preview-remove 的 ::before 热区）
        Box(                                              // 移除按钮
            modifier = Modifier                          // 修饰
                .align(Alignment.TopEnd)                 // 右上角
                .size(18.dp)                             // 18dp 视觉
                .clip(CircleShape)                       // 圆形
                .background(Color(0x8C000000))           // 半透明黑（HTML rgba(0,0,0,0.55)）
                .clickable { onRemove() },               // 点击移除
            contentAlignment = Alignment.Center,          // 居中
        ) {
            Icon(                                         // × 图标
                imageVector = AppIcons.Close,             // 图标
                contentDescription = "移除",               // 描述
                tint = Color.White,                       // 白
                modifier = Modifier.size(10.dp),          // 10dp
            )
        }
    }
}

// ====== 工具函数（对应 HTML 的同名 JS 函数）======

/** 时间戳合并规则：与上一条消息间隔 >5 分钟才显示时间（HTML shouldShowTime）。 */
private fun shouldShowTime(list: List<ChatMessage>, index: Int): Boolean {  // 时间合并
    if (index <= 0) return true                          // 首条必显示
    return (list[index].ts - list[index - 1].ts) > 5 * 60 * 1000  // 间隔超 5 分钟
}

/** 消息时间格式化 HH:mm（HTML fmtTime）。 */
private fun fmtTime(ts: Long): String =                  // 时间格式化
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))  // 时分

/** 今天日期文案（HTML updateDate：M月d日 周X）。 */
private fun todayText(): String {                         // 日期文案
    val now = Calendar.getInstance()                      // 当前时间
    val weekdays = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")  // 星期表
    return "${now.get(Calendar.MONTH) + 1}月${now.get(Calendar.DAY_OF_MONTH)}日 ${weekdays[now.get(Calendar.DAY_OF_WEEK) - 1]}"  // 拼文案
}

/** 按时段问候（HTML updateGreeting）。 */
private fun greeting(): String {                          // 时段问候
    return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {  // 按小时
        in 0..5 -> "夜深了。"                             // 深夜
        in 6..8 -> "早上好。"                             // 早晨
        in 9..11 -> "上午好。"                            // 上午
        in 12..13 -> "中午好。"                           // 中午
        in 14..17 -> "下午好。"                           // 下午
        else -> "晚上好。"                                // 晚上
    }
}

/** 字节大小格式化（HTML fmtBytes）。 */
private fun fmtBytes(b: Long): String {                   // 大小格式化
    return when {                                         // 按量级
        b >= 1048576 -> String.format(Locale.getDefault(), "%.1f MB", b / 1048576.0)  // MB
        b >= 1024 -> "${b / 1024} KB"                     // KB
        else -> "$b B"                                    // B
    }
}

/** 相机预览位图转 JPEG 字节。 */
private fun bitmapToBytes(bmp: Bitmap): ByteArray =      // 位图转字节
    java.io.ByteArrayOutputStream().also { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }.toByteArray()  // 压缩输出

/** 文件路径识别正则：以 / 开头，直到空白或中英文标点（避免误匹配 URL 的 http://）。 */
private val PATH_REGEX = Regex("/[^\\s,，。；;:：、]+")  // 路径

/** 构建带可点击文件路径的消息富文本（路径段高亮 + 下划线，点击回调 onOpenPath）。 */
private fun buildPathAnnotated(                          // 构建富文本
    content: String,                                     // 原始消息
    linkColor: Color,                                    // 链接色
    onOpenPath: (String) -> Unit,                        // 点击路径回调
): AnnotatedString = buildAnnotatedString {              // 构建
    var last = 0                                         // 上次匹配结束位置
    for (m in PATH_REGEX.findAll(content)) {             // 遍历所有路径
        append(content.substring(last, m.range.first))   // 追加普通文本
        val path = m.value                               // 路径字符串
        withLink(LinkAnnotation.Clickable(tag = "path") { onOpenPath(path) }) {  // 可点击链接
            withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {  // 高亮 + 下划线
                append(path)                             // 追加路径
            }
        }
        last = m.range.last + 1                          // 更新位置
    }
    append(content.substring(last))                      // 追加剩余文本
}

/** 用系统应用打开文件路径（经 FileProvider 生成 content URI）。 */
private fun openPath(                                    // 打开文件
    context: Context,                                    // 上下文
    path: String,                                        // 文件路径
    toast: (String) -> Unit,                             // 提示回调
) {
    val file = File(path)                                // 文件对象
    if (!file.exists()) {                                // 文件不存在
        toast("文件不存在：$path")                        // 提示
        return
    }
    val uri = runCatching {                              // 生成 content URI（FileProvider）
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)  // FileProvider URI
    }.getOrElse { Uri.fromFile(file) }                   // 失败回退 file URI
    val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull() ?: "*/*"  // MIME 类型
    val intent = Intent(Intent.ACTION_VIEW).apply {      // 打开意图
        setDataAndType(uri, mime)                        // URI + 类型
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)  // 授予读取权限
    }
    val ok = runCatching { context.startActivity(intent) }.isSuccess  // 启动
    if (!ok) toast("无法打开该文件（未安装对应应用）")     // 失败提示
}
