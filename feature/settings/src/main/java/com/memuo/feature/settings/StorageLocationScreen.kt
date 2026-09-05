package com.memuo.feature.settings                         // 声明包名：设置业务模块

import androidx.compose.foundation.background             // 导入 background：背景修饰
import androidx.compose.foundation.layout.Column          // 导入 Column：纵向布局
import androidx.compose.foundation.layout.fillMaxSize     // 导入 fillMaxSize：铺满
import androidx.compose.foundation.layout.fillMaxWidth    // 导入 fillMaxWidth：占满宽度
import androidx.compose.foundation.layout.padding          // 导入 padding：内边距
import androidx.compose.foundation.shape.RoundedCornerShape  // 导入 RoundedCornerShape：圆角形状
import androidx.compose.material3.MaterialTheme           // 导入 MaterialTheme：主题
import androidx.compose.material3.Text                    // 导入 Text：文本
import androidx.compose.runtime.Composable                // 导入 Composable：可组合函数注解
import androidx.compose.runtime.LaunchedEffect            // 导入 LaunchedEffect：副作用（提示→Toast）
import androidx.compose.runtime.collectAsState            // 导入 collectAsState：状态流→状态
import androidx.compose.runtime.getValue                  // 导入 getValue：by 委托
import androidx.compose.ui.Modifier                       // 导入 Modifier：修饰
import androidx.compose.ui.draw.clip                      // 导入 clip：裁剪
import androidx.compose.ui.unit.dp                        // 导入 dp：尺寸单位
import androidx.hilt.navigation.compose.hiltViewModel     // 导入 hiltViewModel：Hilt 提供 ViewModel
import androidx.lifecycle.ViewModel                       // 导入 ViewModel：UI 数据持有者
import androidx.lifecycle.viewModelScope                  // 导入 viewModelScope：协程作用域
import com.memuo.core.storage.AppPrefs                     // 导入应用偏好（自定义路径）
import com.memuo.core.storage.StorageMigrator              // 导入存储迁移器
import com.memuo.core.storage.StorageProvider              // 导入存储提供者（当前根目录）
import com.memuo.core.ui.components.BrandButton            // 导入品牌按钮
import com.memuo.core.ui.components.LocalToast            // 导入 Toast 状态
import com.memuo.core.ui.components.SectionCard            // 导入分组卡片
import com.memuo.core.ui.components.SectionCardTitle       // 导入分组标题
import com.memuo.core.ui.components.SubBody               // 导入子页内容容器
import com.memuo.core.ui.components.SubHeader             // 导入子页顶栏
import com.memuo.core.ui.theme.MuyunAccentLight           // 导入浅灰底（输入框背景）
import com.memuo.core.ui.theme.MuyunText                  // 导入主文字色
import com.memuo.core.ui.theme.MuyunText2                 // 导入次级文字色
import com.memuo.core.ui.theme.MuyunText3                 // 导入三级文字色
import dagger.hilt.android.lifecycle.HiltViewModel        // 导入 HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow            // 导入可变状态流
import kotlinx.coroutines.flow.StateFlow                  // 导入只读状态流
import kotlinx.coroutines.flow.asStateFlow                // 导入 asStateFlow
import kotlinx.coroutines.launch                           // 导入 launch：协程
import java.io.File                                        // 导入 File：目标目录
import javax.inject.Inject                                // 导入 Inject：构造函数注入

/**
 * 存储位置设置页 —— 查看当前存储根目录 + 自定义到新目录（R5）。
 * 迁移流程：复制现有数据 → 更新偏好 → 提示重启生效。
 */
@Composable                                               // 可组合 UI 函数
fun StorageLocationScreen(                                // 存储位置页
    onBack: () -> Unit,                                  // 返回回调
    viewModel: StorageLocationViewModel = hiltViewModel(),  // Hilt 提供 ViewModel
) {
    val currentRoot by viewModel.currentRoot.collectAsState()  // 订阅当前根目录
    val input by viewModel.input.collectAsState()         // 订阅输入
    val migrating by viewModel.migrating.collectAsState() // 订阅迁移中
    val message by viewModel.message.collectAsState()     // 订阅提示
    val toast = LocalToast.current                       // 取全局 Toast

    LaunchedEffect(message) {                            // 提示 → Toast
        message?.let { toast.show(it); viewModel.consumeMessage() }  // 弹提示并消费
    }

    Column(modifier = Modifier.fillMaxSize()) {           // 纵向布局
        SubHeader(title = "存储位置", onBack = onBack)     // 顶栏
        SubBody(modifier = Modifier.fillMaxSize()) {       // 内容容器
            Column(modifier = Modifier.fillMaxSize()) {    // 纵向内容
                SectionCard {                              // 当前目录分组
                    SectionCardTitle("当前存储目录")        // 分组标题
                    Text(                                 // 当前路径
                        text = currentRoot,               // 路径文本
                        style = MaterialTheme.typography.bodySmall,  // 小字
                        color = MuyunText2,               // 次级色
                    )
                }
                Column(modifier = Modifier.padding(top = 14.dp)) {  // 卡片间距
                    SectionCard {                          // 自定义目录分组
                        SectionCardTitle("自定义存储目录")  // 分组标题
                        androidx.compose.foundation.text.BasicTextField(  // 路径输入框
                            value = input,                // 输入值
                            onValueChange = { viewModel.setInput(it) },  // 输入变化
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MuyunText),  // 文字样式
                            singleLine = true,            // 单行
                            modifier = Modifier           // 修饰
                                .fillMaxWidth()           // 占满宽度
                                .clip(RoundedCornerShape(12.dp))  // 圆角 12
                                .background(MuyunAccentLight)  // 浅灰底
                                .padding(horizontal = 12.dp, vertical = 12.dp),  // 内边距
                        )
                        BrandButton(                      // 迁移按钮
                            text = if (migrating) "迁移中…" else "迁移到此目录",  // 按钮文字
                            onClick = { viewModel.migrateTo(input) },  // 触发迁移
                            enabled = !migrating,         // 迁移中禁用
                            modifier = Modifier.padding(top = 12.dp),  // 上边距
                        )
                    }
                }
                Text(                                     // 底部说明
                    text = "迁移会把现有数据（模型/知识库/索引等）复制到新目录并切换存储位置，重启应用后生效；建议迁移前先退出进行中的对话。",  // 文案
                    style = MaterialTheme.typography.labelSmall,  // 小字
                    color = MuyunText3,                   // 三级灰
                    lineHeight = MaterialTheme.typography.labelSmall.lineHeight * 1.5f,  // 行距
                    modifier = Modifier.padding(vertical = 14.dp, horizontal = 4.dp),  // 内边距
                )
            }
        }
    }
}

/** 存储位置 ViewModel —— 查看当前根目录 + 迁移到自定义目录（R5）。 */
@HiltViewModel                                           // 注解：由 Hilt 创建
class StorageLocationViewModel @Inject constructor(      // 构造函数注入
    private val appPrefs: AppPrefs,                     // 注入应用偏好（自定义路径）
    private val storage: StorageProvider,                // 注入存储提供者（当前根目录）
) : ViewModel() {                                        // 继承 ViewModel

    private val _currentRoot = MutableStateFlow(storage.root.absolutePath)  // 当前根目录
    val currentRoot: StateFlow<String> = _currentRoot.asStateFlow()  // 只读暴露

    private val _input = MutableStateFlow("")            // 输入路径
    val input: StateFlow<String> = _input.asStateFlow()  // 只读暴露

    private val _migrating = MutableStateFlow(false)     // 迁移中
    val migrating: StateFlow<Boolean> = _migrating.asStateFlow()  // 只读暴露

    private val _message = MutableStateFlow<String?>(null)  // 提示消息
    val message: StateFlow<String?> = _message.asStateFlow()  // 只读暴露

    /** 输入变化。 */
    fun setInput(v: String) { _input.value = v }          // 更新输入

    /** 消费提示消息。 */
    fun consumeMessage() { _message.value = null }        // 清空消息

    /** 迁移数据到新目录并更新偏好（重启后生效）。 */
    fun migrateTo(newPath: String) {                      // 迁移方法
        val path = newPath.trim()                         // 去空白
        if (path.isBlank()) {                             // 空路径
            _message.value = "请输入目录路径"               // 提示
            return
        }
        val newRoot = File(path)                          // 目标目录
        if (newRoot.absolutePath == storage.root.absolutePath) {  // 与当前相同
            _message.value = "已是当前目录"                 // 提示
            return
        }
        viewModelScope.launch {                          // 协程中迁移
            _migrating.value = true                       // 迁移中
            val copied = runCatching {                    // 容错复制
                StorageMigrator.migrate(storage.root, newRoot)  // 复制 + 校验
            }.getOrNull()                                 // 失败返回 null
            if (copied != null) {                         // 迁移成功
                appPrefs.setCustomStoragePath(newRoot.absolutePath)  // 更新偏好（重启后 StorageModule 选自定义）
                _message.value = "已迁移 $copied 个文件，重启应用后生效"  // 提示
            } else {                                     // 迁移失败
                _message.value = "迁移失败：目标目录不可写或数据被占用"  // 提示
            }
            _migrating.value = false                      // 结束
        }
    }
}
