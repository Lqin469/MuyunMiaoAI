package com.memuo.core.db.entity                          // 声明包名：数据库模块的实体子包

import androidx.room.ColumnInfo                            // 导入 ColumnInfo：自定义列名
import androidx.room.Entity                                // 导入 Entity：声明数据库表
import androidx.room.PrimaryKey                            // 导入 PrimaryKey：主键

/**
 * 文件位置记录表（file_locations）—— 记录"无法直接解析"的文件的位置（R10）。
 * 场景：压缩包内无法解析的条目、超限文件、未知格式等，仅存元数据，
 * AI 问答时可告知用户"文件在 xxx"。
 */
@Entity(tableName = "file_locations")                      // 声明表名 file_locations
data class FileLocation(                                   // 文件位置数据类
    @PrimaryKey val path: String,                          // 主键：文件绝对路径 或 压缩包内路径
    val archivePath: String? = null,                       // 若文件在压缩包内：压缩包路径（否则 null）
    val name: String,                                      // 文件名
    val ext: String,                                       // 扩展名（小写）
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,  // 文件大小（字节）
    val mtime: Long,                                       // 最后修改时间戳
    @ColumnInfo(name = "indexed_at") val indexedAt: Long,  // 记录时间
)
