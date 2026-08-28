package com.memuo.feature.notes                         // 声明包名：笔记业务模块

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
import androidx.compose.foundation.lazy.items              // 导入 items：列表项扩展
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
import androidx.compose.ui.text.font.FontWeight           // 导入 FontWeight：字重
import androidx.compose.ui.text.style.TextOverflow        // 导入 TextOverflow：溢出省略
import androidx.compose.ui.unit.dp                        // 导入 dp：尺寸单位
import androidx.hilt.navigation.compose.hiltViewModel     // 导入 hiltViewModel：Hilt 提供 ViewModel
import com.memuo.core.db.entity.Note                       // 导入笔记实体
import com.memuo.core.ui.AppIcons                         // 导入应用图标集
import com.memuo.core.ui.components.EmptyState            // 导入空态组件
import com.memuo.core.ui.components.LocalToast            // 导入 Toast 状态
import com.memuo.core.ui.components.SubBody               // 导入子页内容容器
import com.memuo.core.ui.components.SubHeader             // 导入子页顶栏
import com.memuo.core.ui.theme.MuyunBrand                 // 导入品牌色
import com.memuo.core.ui.theme.MuyunBrandSoft             // 导入品牌浅底
import com.memuo.core.ui.theme.MuyunCard                  // 导入卡片白
import com.memuo.core.ui.theme.MuyunDanger                // 导入危险红
import com.memuo.core.ui.theme.MuyunDangerBg              // 导入危险红底
import com.memuo.core.ui.theme.MuyunText2                 // 导入次级文字色
import com.memuo.core.ui.theme.MuyunText3                 // 导入三级文字色
import java.text.SimpleDateFormat                          // 导入 SimpleDateFormat：时间格式化
import java.util.Date                                     // 导入 Date：日期
import java.util.Locale                                   // 导入 Locale：区域

/**
 * 回收站页 —— 软删除笔记的管理。
 * 行为：保留天数提示（可配置）、恢复、彻底删除、清空回收站、空态。
 */
@Composable                                               // 可组合 UI 函数
fun TrashScreen(                                          // 回收站页
    onBack: () -> Unit,                                   // 返回回调
    viewModel: NoteListViewModel = hiltViewModel(),       // Hilt 提供 ViewModel
) {
    val trashed by viewModel.trashed.collectAsState()     // 订阅回收站列表
    val trashDays by viewModel.trashDays.collectAsState() // 订阅回收站保留天数（设置面板可调）
    val toast = LocalToast.current                        // 取全局 Toast

    Column(modifier = Modifier.fillMaxSize()) {           // 纵向布局
        SubHeader(title = "回收站", onBack = onBack)       // 顶栏
        SubBody(modifier = Modifier.fillMaxSize()) {       // 内容容器
            Column(modifier = Modifier.fillMaxSize()) {    // 纵向内容
                Text(                                     // 头部提示（HTML .trash-head-hint）
                    text = "删除的备忘录保留 $trashDays 天，超期自动永久删除；可随时恢复。",  // 提示文案
                    style = MaterialTheme.typography.labelSmall,  // 小字（HTML 12px）
                    color = MuyunText3,                   // 三级灰
                    lineHeight = MaterialTheme.typography.labelSmall.lineHeight * 1.4f,  // 行距
                    modifier = Modifier.padding(bottom = 14.dp),  // 下留白
                )
                if (trashed.isEmpty()) {                  // 空态
                    EmptyState(                           // 空态组件
                        icon = AppIcons.Trash,            // 垃圾桶插图
                        text = "回收站是空的",             // HTML 空态文案
                    )
                } else {                                  // 列表
                    LazyColumn(                           // 懒加载列表
                        modifier = Modifier.weight(1f),   // 占满剩余
                    ) {
                        items(trashed, key = { it.id }) { note ->  // 遍历软删除笔记
                            TrashItem(                    // 单条回收站项
                                note = note,              // 笔记
                                trashDays = trashDays,    // 保留天数
                                onRestore = {             // 恢复
                                    viewModel.restoreNote(note.id)  // 清空软删除时间
                                    toast.show("已恢复")  // Toast
                                },
                                onPurge = {               // 彻底删除
                                    viewModel.purgeNote(note.id)  // 物理删除
                                    toast.show("已彻底删除")  // Toast
                                },
                            )
                        }
                        item {                            // 清空按钮
                            Box(                          // 全宽按钮容器
                                modifier = Modifier     // 修饰
                                    .fillMaxWidth()     // 占满宽度
                                    .padding(top = 10.dp)  // 上留白
                                    .clip(RoundedCornerShape(14.dp))  // 圆角
                                    .background(MuyunCard)  // 白底
                                    .clickable {          // 点击清空
                                        viewModel.emptyTrash()  // 批量物理删除
                                        toast.show("回收站已清空")  // Toast
                                    }
                                    .padding(vertical = 13.dp),  // 内边距
                                contentAlignment = Alignment.Center,  // 居中
                            ) {
                                Text(                    // 清空文字
                                    text = "清空回收站",  // 内容
                                    style = MaterialTheme.typography.bodyMedium,  // 字号（HTML 14px）
                                    fontWeight = FontWeight.Medium,  // 中粗
                                    color = MuyunDanger,  // 危险红
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 单条回收站项：标题 + 剩余天数 + 恢复按钮 + 彻底删除按钮（对应 HTML .trash-item）。 */
@Composable                                               // 可组合 UI 函数
private fun TrashItem(                                    // 回收站项
    note: Note,                                           // 笔记数据
    trashDays: Int,                                       // 保留天数（设置面板可调）
    onRestore: () -> Unit,                                // 恢复回调
    onPurge: () -> Unit,                                  // 彻底删除回调
) {
    val remainDays = ((trashDays * 24L * 60 * 60 * 1000 - (System.currentTimeMillis() - note.deletedAt!!)) / 86400000).coerceAtLeast(0).toInt()  // 剩余天数
    Row(                                                  // 横向布局
        modifier = Modifier                               // 修饰
            .fillMaxWidth()                               // 占满宽度
            .padding(bottom = 10.dp)                      // 下留白
            .shadow(1.dp, RoundedCornerShape(14.dp))      // 轻投影
            .clip(RoundedCornerShape(14.dp))              // 圆角
            .background(MuyunCard)                        // 白底
            .padding(horizontal = 16.dp, vertical = 14.dp),  // 内边距
        verticalAlignment = Alignment.CenterVertically,   // 垂直居中
    ) {
        Column(modifier = Modifier.weight(1f)) {           // 信息区（占满剩余）
            Text(                                         // 标题
                text = note.title.ifBlank { "无标题" },    // 空标题占位
                style = MaterialTheme.typography.titleSmall,  // 字号（HTML 14px）
                fontWeight = FontWeight.SemiBold,         // 半粗
                color = MuyunText2,                       // 次级灰
                maxLines = 1,                             // 单行
                overflow = TextOverflow.Ellipsis,         // 溢出省略
            )
            Text(                                         // 副信息
                text = "$remainDays 天后永久删除 · ${SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault()).format(Date(note.deletedAt!!))}",  // 剩余天数 + 删除时间
                style = MaterialTheme.typography.labelSmall,  // 小字（HTML 11px）
                color = MuyunText3,                       // 三级灰
                modifier = Modifier.padding(top = 4.dp),  // 上留白
            )
        }
        Box(                                              // 恢复按钮
            modifier = Modifier                          // 修饰
                .padding(end = 8.dp)                     // 右留白
                .clip(RoundedCornerShape(16.dp))         // 胶囊圆角
                .background(MuyunBrandSoft)              // 品牌浅底
                .clickable { onRestore() }               // 点击恢复
                .padding(horizontal = 14.dp, vertical = 7.dp),  // 内边距（HTML padding 7px 14px）
        ) {
            Text(                                         // 恢复文字
                text = "恢复",                            // 内容
                style = MaterialTheme.typography.labelMedium,  // 小字（HTML 12px）
                fontWeight = FontWeight.Medium,           // 中粗
                color = MuyunBrand,                       // 品牌色
            )
        }
        Box(                                              // 彻底删除按钮
            modifier = Modifier                          // 修饰
                .size(34.dp)                             // 34dp（HTML 34×34）
                .clip(RoundedCornerShape(10.dp))         // 圆角 10
                .clickable { onPurge() }                 // 点击彻底删除
                .padding(9.dp),                          // 内边距
        ) {
            Icon(                                         // 垃圾桶图标
                imageVector = AppIcons.Trash,             // 图标
                contentDescription = "彻底删除",           // 描述
                tint = MuyunDanger,                       // 危险红（HTML 按压态）
                modifier = Modifier.size(16.dp),          // 16dp
            )
        }
    }
}
