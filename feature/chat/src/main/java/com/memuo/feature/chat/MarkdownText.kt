package com.memuo.feature.chat                           // 声明包名：对话业务模块

import androidx.compose.foundation.background             // 导入 background：背景
import androidx.compose.foundation.layout.Column          // 导入 Column：纵向布局
import androidx.compose.foundation.layout.Spacer          // 导入 Spacer：占位
import androidx.compose.foundation.layout.fillMaxWidth    // 导入 fillMaxWidth：占满宽度
import androidx.compose.foundation.layout.height          // 导入 height：高度
import androidx.compose.foundation.layout.padding          // 导入 padding：内边距
import androidx.compose.foundation.shape.RoundedCornerShape  // 导入 RoundedCornerShape：圆角
import androidx.compose.material3.MaterialTheme           // 导入 MaterialTheme：主题
import androidx.compose.material3.Surface                 // 导入 Surface：代码块底板
import androidx.compose.material3.Text                    // 导入 Text：文本
import androidx.compose.runtime.Composable                // 导入 Composable
import androidx.compose.ui.Modifier                       // 导入 Modifier
import androidx.compose.ui.text.SpanStyle                 // 导入 SpanStyle：文本片段样式
import androidx.compose.ui.text.buildAnnotatedString      // 导入 buildAnnotatedString：构建富文本
import androidx.compose.ui.text.font.FontFamily           // 导入 FontFamily：等宽字体
import androidx.compose.ui.text.font.FontWeight           // 导入 FontWeight：加粗
import androidx.compose.ui.text.withStyle                 // 导入 withStyle：设置片段样式
import androidx.compose.ui.unit.dp                        // 导入 dp
import androidx.compose.ui.unit.sp                        // 导入 sp

/**
 * 轻量 Markdown 渲染器（MarkdownText）—— 自研，无第三方依赖。
 * 支持：代码块、标题、列表、引用、行内加粗、行内代码。
 * 用于对话气泡 / 笔记正文渲染（M-013）。
 */
@Composable                                               // 可组合 UI 函数
fun MarkdownText(                                         // Markdown 渲染
    content: String,                                      // Markdown 文本
    modifier: Modifier = Modifier,                        // 修饰
) {
    val lines = content.split("\n")                       // 按行分割
    Column(modifier = modifier) {                         // 纵向布局
        var inCode = false                                // 是否在代码块内
        val code = StringBuilder()                        // 代码块累积缓冲

        for (raw in lines) {                              // 遍历每行
            val line = raw                                // 当前行
            if (line.trimStart().startsWith("```")) {     // 代码块标记
                if (inCode) {                             // 代码块结束
                    CodeBlock(code.toString().trimEnd('\n'))  // 渲染代码块
                    code.clear()                          // 清空缓冲
                }
                inCode = !inCode                          // 切换状态
                continue                                  // 跳过本行
            }
            if (inCode) {                                 // 代码块内
                code.append(line).append('\n')            // 累积代码
                continue                                  // 继续
            }
            when {                                        // 按行类型分发
                line.isBlank() -> Spacer(Modifier.height(4.dp))  // 空行：小间距
                line.trimStart().startsWith("### ") -> Heading(line.removePrefix("###").trim(), 3)  // 三级标题
                line.trimStart().startsWith("## ") -> Heading(line.removePrefix("##").trim(), 2)      // 二级标题
                line.trimStart().startsWith("# ") -> Heading(line.removePrefix("#").trim(), 1)        // 一级标题
                line.trimStart().startsWith("> ") -> Quote(line.removePrefix(">").trim())             // 引用
                line.trimStart().startsWith("- ") -> ListItem(line.trimStart().removePrefix("- "))    // 无序列表
                line.trimStart().startsWith("* ") -> ListItem(line.trimStart().removePrefix("* "))    // 无序列表（*）
                else -> Paragraph(line)                   // 普通段落（含行内语法）
            }
        }
        if (inCode && code.isNotEmpty()) {                // 未闭合代码块
            CodeBlock(code.toString().trimEnd('\n'))      // 渲染剩余代码
        }
    }
}

/** 标题：按级别放大字号 + 加粗。 */
@Composable                                               // 可组合 UI 函数
private fun Heading(text: String, level: Int) {           // 标题渲染
    val size = when (level) {                             // 按级别定字号
        1 -> 20.sp                                        // 一级
        2 -> 17.sp                                        // 二级
        else -> 15.sp                                     // 三级
    }
    Text(                                                 // 标题文本
        text = text,
        fontSize = size,                                  // 字号
        fontWeight = FontWeight.Bold,                     // 加粗
        modifier = Modifier.padding(vertical = 4.dp),     // 上下留白
    )
}

/** 列表项：圆点 + 缩进。 */
@Composable                                               // 可组合 UI 函数
private fun ListItem(text: String) {                      // 列表项渲染
    Text(                                                 // 列表项文本
        text = "•  $text",                               // 圆点前缀
        style = MaterialTheme.typography.bodyMedium,      // 正文样式
        modifier = Modifier.padding(vertical = 2.dp, start = 4.dp),  // 缩进
    )
}

/** 引用：左侧竖线 + 灰色文字。 */
@Composable                                               // 可组合 UI 函数
private fun Quote(text: String) {                         // 引用渲染
    Text(                                                 // 引用文本
        text = "▎ $text",                                // 竖线前缀
        style = MaterialTheme.typography.bodyMedium,      // 正文样式
        color = MaterialTheme.colorScheme.onSurfaceVariant,  // 灰色
        modifier = Modifier.padding(vertical = 2.dp),     // 上下留白
    )
}

/** 代码块：等宽字体 + 灰底圆角。 */
@Composable                                               // 可组合 UI 函数
private fun CodeBlock(code: String) {                     // 代码块渲染
    Surface(                                              // 底板
        shape = RoundedCornerShape(6.dp),                 // 圆角
        color = MaterialTheme.colorScheme.surfaceVariant, // 灰底
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),  // 占满 + 留白
    ) {
        Text(                                             // 代码文本
            text = code,
            fontFamily = FontFamily.Monospace,            // 等宽字体
            fontSize = 13.sp,                             // 小字号
            modifier = Modifier.padding(10.dp),           // 内边距
        )
    }
}

/** 普通段落：处理行内 **加粗** 与 `代码`。 */
@Composable                                               // 可组合 UI 函数
private fun Paragraph(text: String) {                     // 段落渲染
    Text(                                                 // 富文本
        text = buildInline(text),                         // 构建行内富文本
        style = MaterialTheme.typography.bodyMedium,      // 正文样式
        modifier = Modifier.padding(vertical = 2.dp),     // 上下留白
    )
}

/** 构建行内富文本：识别 **加粗** 与 `行内代码`。 */
private fun buildInline(text: String) = buildAnnotatedString {  // 行内富文本
    val regex = Regex("(\\*\\*[^*]+\\*\\*|`[^`]+`)")      // 匹配加粗 / 行内代码
    var last = 0                                          // 上次匹配结束位置
    for (m in regex.findAll(text)) {                      // 遍历匹配
        append(text.substring(last, m.range.first))       // 追加普通文本
        val token = m.value                               // 匹配到的 token
        when {                                            // 按类型设置样式
            token.startsWith("**") -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {  // 加粗
                append(token.removeSurrounding("**"))     // 追加去掉 ** 的文本
            }
            token.startsWith("`") -> withStyle(SpanStyle(  // 行内代码
                background = MaterialTheme.colorScheme.surfaceVariant,  // 灰底
                fontFamily = FontFamily.Monospace,        // 等宽字体
            )) {
                append(token.removeSurrounding("`"))      // 追加去掉 ` 的文本
            }
        }
        last = m.range.last + 1                           // 更新位置
    }
    append(text.substring(last))                          // 追加剩余文本
}
