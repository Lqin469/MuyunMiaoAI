package com.memuo.core.ingest                            // 声明包名：内容入库模块

import android.content.Context                            // 导入 Context：应用上下文
import android.net.Uri                                    // 导入 Uri：文件统一标识
import java.io.File                                       // 导入 File：本地文件
import java.util.zip.ZipFile                              // 导入 ZipFile：DOCX 是 zip 包，用它读 word/document.xml

/**
 * 文档解析器（DocumentParser）—— 把各类文件解析为纯文本（M4 使用）。
 * 当前支持：TXT / MD / DOCX（解 zip 读 document.xml）。
 * PDF 用 pdfbox-android（后续补）；图片走 OCR（后续补）；压缩包走 ArchiveExtractor（后续补）。
 */
object DocumentParser {                                   // 单例对象：解析逻辑

    /** 解析结果：文本 + 来源文件名。 */
    data class ParsedText(val text: String, val source: String)  // 解析结果数据类

    /** 不支持的格式异常。 */
    class UnsupportedFormatException(ext: String) :        // 自定义异常
        IllegalArgumentException("不支持的格式：$ext")     // 带中文提示

    /** 按文件路径解析（根据扩展名分派）。 */
    fun parse(file: File): ParsedText {                   // 解析文件方法
        val ext = file.extension.lowercase()              // 取扩展名并小写
        return when (ext) {                               // 按扩展名分派
            "txt", "md", "json", "log" -> ParsedText(file.readText(), file.name)  // 文本类：直接读
            "docx" -> ParsedText(parseDocx(file), file.name)  // DOCX：解 zip
            else -> throw UnsupportedFormatException(ext) // 其他：抛异常（PDF/图片/压缩包后续补）
        }
    }

    /** 按 Uri 解析（SAF 文件用）。 */
    fun parse(context: Context, uri: Uri, fileName: String): ParsedText {  // 解析 Uri 方法
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""  // 读流为文本
        return ParsedText(text, fileName)                 // 返回结果
    }

    /** 解析 DOCX：解 zip 读 word/document.xml，去掉 XML 标签。 */
    private fun parseDocx(file: File): String {           // 解析 DOCX 方法
        ZipFile(file).use { zip ->                        // 打开 zip（DOCX 本质是 zip）
            val entry = zip.getEntry("word/document.xml") // 取正文 XML 条目
            val xml = zip.getInputStream(entry).bufferedReader().readText()  // 读 XML 文本
            return xml.replace(Regex("<w:p[ >]"), "\n")   // 段落标签换行
                .replace(Regex("<[^>]+>"), "")            // 去掉所有 XML 标签
                .trim()                                   // 去首尾空白
        }
    }
}
