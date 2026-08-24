package com.memuo.core.ingest                            // 声明包名：内容入库模块

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream  // 导入 TAR 输入流
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream  // 导入 BZip2 解压流
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream    // 导入 GZip 解压流
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream        // 导入 XZ 解压流
import java.io.BufferedInputStream                      // 导入缓冲输入流
import java.io.File                                     // 导入 File：本地文件
import java.io.FileInputStream                          // 导入文件输入流
import java.io.FileOutputStream                         // 导入文件输出流
import java.util.zip.ZipFile                            // 导入 ZipFile：ZIP 解压（内置）

/**
 * 压缩包解析器（ArchiveExtractor）—— 解压 ZIP / TAR(.gz/.bz2/.xz)（R9）。
 * 安全防护（强制）：
 *  1) zip-slip：条目路径规范化后必须位于目标目录内，防止路径穿越攻击；
 *  2) 解压炸弹：总量 > 2GB、单文件 > 500MB、压缩比 > 200 时中止。
 * 7z / RAR 由上层 catch 异常后走 FileLocationIndex 记录位置（R10 兜底）。
 */
object ArchiveExtractor {                               // 单例对象：压缩包解析

    /** 解压总量上限（2GB）。 */
    private const val MAX_TOTAL = 2L * 1024 * 1024 * 1024  // 常量：总大小上限

    /** 单文件大小上限（500MB）。 */
    private const val MAX_FILE = 500L * 1024 * 1024       // 常量：单文件上限

    /** 压缩比上限（防止 1KB 解出 1GB）。 */
    private const val MAX_RATIO = 200L                    // 常量：压缩比上限

    /** 不支持的压缩格式异常。 */
    class UnsupportedArchiveException(ext: String) :      // 自定义异常
        IllegalArgumentException("不支持的压缩格式：$ext")  // 带中文提示

    /**
     * 解压到目标目录，返回解压出的文件列表。
     */
    fun extract(archive: File, targetDir: File): List<File> {  // 解压方法
        targetDir.mkdirs()                                // 确保目标目录存在
        val files = when (archive.extension.lowercase()) {  // 按扩展名分派
            "zip" -> extractZip(archive, targetDir)       // ZIP
            "tar", "gz", "tgz", "bz2", "tbz2", "xz", "txz" -> extractTar(archive, targetDir)  // TAR 系
            else -> throw UnsupportedArchiveException(archive.extension)  // 7z/rar：上层记录位置
        }
        // 压缩比检查（解压后）：防止极小压缩包解出极大内容
        val total = files.sumOf { it.length() }           // 解压后总大小
        if (archive.length() > 0 && total.toDouble() / archive.length() > MAX_RATIO) {  // 压缩比超限
            files.forEach { it.delete() }                 // 清理已解压文件
            throw IllegalArgumentException("解压炸弹：压缩比超限")  // 抛异常
        }
        return files                                        // 返回
    }

    /** 解压 ZIP（java.util.zip 内置）。 */
    private fun extractZip(archive: File, targetDir: File): List<File> {  // ZIP 解压
        val out = mutableListOf<File>()                   // 结果文件列表
        var total = 0L                                    // 已解压总量
        ZipFile(archive).use { zip ->                     // 打开 zip
            val entries = zip.entries()                   // 取条目枚举
            while (entries.hasMoreElements()) {           // 遍历条目
                val entry = entries.nextElement()         // 取一个条目
                if (entry.isDirectory) continue           // 跳过目录
                check(entry.size <= MAX_FILE) { "单文件超限：${entry.name}" }  // 单文件上限
                total += entry.size                        // 累计总量
                check(total <= MAX_TOTAL) { "解压总量超限" }  // 总量上限
                val dest = safePath(targetDir, entry.name)  // zip-slip 防护的路径
                dest.parentFile?.mkdirs()                 // 建父目录
                zip.getInputStream(entry).use { it.copyTo(FileOutputStream(dest)) }  // 解压写文件
                out += dest                               // 记录结果
            }
        }
        return out                                        // 返回
    }

    /** 解压 TAR / TAR.GZ / TAR.BZ2 / TAR.XZ（commons-compress）。 */
    private fun extractTar(archive: File, targetDir: File): List<File> {  // TAR 解压
        val raw = FileInputStream(archive)                // 原始文件流
        val base: java.io.InputStream = when (archive.extension.lowercase()) {  // 按压缩层包装
            "gz", "tgz" -> GzipCompressorInputStream(raw) // GZip 层
            "bz2", "tbz2" -> BZip2CompressorInputStream(raw)  // BZip2 层
            "xz", "txz" -> XZCompressorInputStream(raw)   // XZ 层
            else -> raw                                   // 纯 tar 无压缩层
        }
        val out = mutableListOf<File>()                   // 结果列表
        TarArchiveInputStream(BufferedInputStream(base)).use { tar ->  // 打开 tar
            var total = 0L                                // 总量
            while (true) {                                // 遍历条目
                val entry = tar.nextEntry ?: break        // 取条目，空则结束
                if (entry.isDirectory) continue           // 跳过目录
                check(entry.size <= MAX_FILE) { "单文件超限：${entry.name}" }  // 单文件上限
                total += entry.size                        // 累计
                check(total <= MAX_TOTAL) { "解压总量超限" }  // 总量上限
                val dest = safePath(targetDir, entry.name)  // zip-slip 防护
                dest.parentFile?.mkdirs()                 // 建父目录
                FileOutputStream(dest).use { outStream ->  // 写文件
                    tar.copyTo(outStream)                 // 拷贝当前条目
                }
                out += dest                               // 记录
            }
        }
        return out                                        // 返回
    }

    /** zip-slip 防护：规范化路径，确保落在目标目录内。 */
    private fun safePath(targetDir: File, entryName: String): File {  // 路径防护
        val dest = File(targetDir, entryName).canonicalFile  // 拼接并规范化
        val base = targetDir.canonicalFile                // 目标目录规范化
        check(dest.path.startsWith(base.path + File.separator) || dest == base) {  // 必须在目录内
            "非法路径（zip-slip）：$entryName"             // 越界则抛异常
        }
        return dest                                        // 返回安全路径
    }
}
