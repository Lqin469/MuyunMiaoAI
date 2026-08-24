package com.memuo.core.ingest                            // 声明包名：内容入库模块

import com.memuo.core.ai.embed.EmbeddingProvider          // 导入嵌入提供者
import com.memuo.core.ai.embed.toBytes                    // 导入向量序列化扩展
import com.memuo.core.db.dao.KbDao                        // 导入知识库 DAO
import com.memuo.core.db.dao.NoteDao                      // 导入笔记 DAO
import com.memuo.core.db.entity.IngestStatus              // 导入入库状态枚举
import com.memuo.core.db.entity.KbChunk                   // 导入分块实体
import com.memuo.core.db.entity.KbDocument                // 导入文档实体
import kotlinx.coroutines.CoroutineScope                  // 导入 CoroutineScope：协程作用域
import kotlinx.coroutines.launch                          // 导入 launch：启动协程
import java.io.File                                       // 导入 File：本地文件
import javax.inject.Inject                                // 导入 Inject：构造函数注入
import javax.inject.Singleton                             // 导入 Singleton：单例作用域

/**
 * 知识库仓库（KnowledgeRepository）—— 投喂内容的入库编排（M4）。
 * 流程：解析 → 分块 → 嵌入 → 幂等入库。并订阅 NoteBridge 实现 R7「笔记自动进知识库」。
 */
@Singleton                                               // 单例：全应用共享
class KnowledgeRepository @Inject constructor(           // 构造函数注入
    private val kbDao: KbDao,                            // 注入知识库 DAO
    private val noteDao: NoteDao,                        // 注入笔记 DAO（读笔记内容）
    private val embedder: EmbeddingProvider,             // 注入嵌入提供者
) {
    /** 笔记自动入库使用的固定知识库 ID。 */
    companion object { const val NOTES_FOLDER = "notes" } // 笔记知识库标识

    /**
     * 订阅笔记变更事件（R7）：笔记新建/更新时入库，删除时移除对应分块。
     * 由 app 层在启动时用应用级作用域调用一次。
     */
    fun observeNoteBridge(scope: CoroutineScope, bridge: NoteBridge) {  // 订阅笔记事件
        scope.launch {                                    // 在作用域内启动
            bridge.changes.collect { change ->            // 收集事件流
                when (change.action) {                    // 按动作分发
                    NoteChanged.Action.CREATED, NoteChanged.Action.UPDATED -> ingestNote(change.noteId)  // 新建/更新→入库
                    NoteChanged.Action.DELETED -> kbDao.deleteChunksByDoc(docIdOf(change.noteId))  // 删除→移除分块
                }
            }
        }
    }

    /** 把一条笔记的标题+正文入库。 */
    suspend fun ingestNote(noteId: Long) {               // 笔记入库
        val note = noteDao.getById(noteId) ?: return      // 读笔记，不存在则返回
        val text = (note.title + "\n" + note.content).trim()  // 合并标题+正文
        if (text.isEmpty()) return                        // 空内容跳过
        ingestText(                                       // 入库文本
            docId = docIdOf(noteId),                      // 文档 ID = "note_<id>"
            folderId = NOTES_FOLDER,                      // 知识库 ID
            fileName = note.title.ifBlank { "笔记 #$noteId" },  // 来源名（标题或默认）
            text = text,                                  // 文本
        )
    }

    /** 把一份文件解析后入库（文件投喂 R8 的文本类）。 */
    suspend fun ingestFile(file: File, folderId: String) {  // 文件入库
        val parsed = DocumentParser.parse(file)           // 解析文件为文本
        ingestText(                                       // 入库文本
            docId = "file_" + file.absolutePath.hashCode().toString(),  // 文档 ID（路径 hash）
            folderId = folderId,                          // 知识库 ID
            fileName = parsed.source,                     // 来源文件名
            text = parsed.text,                           // 文本
        )
    }

    /** 核心入库：分块 → 嵌入 → 幂等写库。 */
    private suspend fun ingestText(docId: String, folderId: String, fileName: String, text: String) {  // 核心入库
        val chunks = Chunker.split(text)                  // 分块
        if (chunks.isEmpty()) return                       // 无有效块则跳过
        val vectors = embedder.embed(chunks)              // 批量嵌入（本地/云端）

        kbDao.upsertDocument(                             // 写入文档（幂等）
            KbDocument(                                   // 构造文档
                docId = docId,                            // 文档 ID
                folderId = folderId,                      // 知识库 ID
                fileName = fileName,                      // 文件名
                fileUri = "",                             // 笔记无 URI，留空
                fileHash = text.hashCode().toString(),    // 内容 hash（增量判断用）
                status = IngestStatus.INDEXED,            // 已入库
                chunkCount = chunks.size,                 // 分块数
                indexedAt = System.currentTimeMillis(),   // 入库时间
            ),
        )
        kbDao.deleteChunksByDoc(docId)                    // 清空旧分块（幂等）
        kbDao.insertChunks(                               // 写入新分块
            chunks.mapIndexed { i, t ->                   // 组装分块
                KbChunk(                                  // 构造分块
                    docId = docId,                        // 文档 ID
                    folderId = folderId,                  // 知识库 ID
                    seq = i,                              // 序号
                    text = t,                             // 文本
                    embedding = vectors[i].toBytes(),     // 向量（序列化为 BLOB）
                )
            },
        )
    }

    /** 笔记 → 文档 ID 的约定映射。 */
    private fun docIdOf(noteId: Long): String = "note_$noteId"  // 映射方法
}
