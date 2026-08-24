package com.memuo.core.db.entity                          // 声明包名：数据库模块的实体子包

import androidx.room.ColumnInfo                            // 导入 ColumnInfo：自定义列名/类型
import androidx.room.Entity                                // 导入 Entity：声明数据库表
import androidx.room.PrimaryKey                            // 导入 PrimaryKey：主键

/** 入库状态枚举：PENDING=待解析 / PARSING=解析中 / INDEXED=已入库 / FAILED=失败。 */
enum class IngestStatus { PENDING, PARSING, INDEXED, FAILED }  // 四种入库状态

/**
 * 知识库文档表（kb_documents）—— 一份已投喂的文档（M4 使用）。
 * docId 用内容 hash 保证幂等；fileHash 用于增量判断（文件变化则重新入库）。
 */
@Entity(tableName = "kb_documents")                        // 声明表名 kb_documents
data class KbDocument(                                     // 文档数据类
    @PrimaryKey val docId: String,                         // 主键：文档唯一 ID（hash(uri+path)）
    val folderId: String,                                  // 所属知识库 ID
    val fileName: String,                                  // 文件名
    val fileUri: String,                                   // 文件 URI（原始位置）
    val fileHash: String,                                  // 文件内容 hash（增量判断用）
    val status: IngestStatus,                              // 入库状态
    val chunkCount: Int = 0,                               // 分块数量
    val indexedAt: Long? = null,                           // 入库完成时间
)

/**
 * 知识库分块表（kb_chunks）—— 文档切分后的一段文本 + 其向量（M4 使用）。
 * embedding 存 FloatArray 序列化后的字节（BLOB）。
 */
@Entity(tableName = "kb_chunks")                           // 声明表名 kb_chunks
data class KbChunk(                                        // 分块数据类
    @PrimaryKey(autoGenerate = true) val id: Long = 0,     // 主键：自增 ID
    val docId: String,                                     // 所属文档 ID
    val folderId: String,                                  // 所属知识库 ID
    val seq: Int,                                          // 块序号（保持文档内顺序）
    val text: String,                                      // 文本内容
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val embedding: ByteArray? = null,  // 向量（BLOB，FloatArray 序列化）
)
