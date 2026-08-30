package com.memuo.core.ingest                            // 声明包名：内容入库模块

import com.memuo.core.ai.embed.EmbeddingProvider          // 导入嵌入提供者
import com.memuo.core.ai.embed.toBytes                    // 导入向量序列化扩展
import com.memuo.core.db.dao.FileLocationDao              // 导入文件位置 DAO（R10）
import com.memuo.core.db.dao.KbDao                        // 导入知识库 DAO
import com.memuo.core.db.dao.NoteDao                      // 导入笔记 DAO
import com.memuo.core.db.entity.FileLocation              // 导入文件位置实体
import com.memuo.core.db.entity.IngestStatus              // 导入入库状态枚举
import com.memuo.core.db.entity.KbChunk                   // 导入分块实体
import com.memuo.core.db.entity.KbDocument                // 导入文档实体
import com.memuo.core.storage.NotePrefs                    // 导入笔记偏好（自动入库开关）
import com.memuo.core.storage.StorageProvider             // 导入存储提供者（解压临时目录）
import kotlinx.coroutines.CoroutineScope                  // 导入 CoroutineScope：协程作用域
import kotlinx.coroutines.flow.first                       // 导入 first：取流首值（读开关）
import kotlinx.coroutines.launch                          // 导入 launch：启动协程
import java.io.File                                       // 导入 File：本地文件
import javax.inject.Inject                                // 导入 Inject：构造函数注入
import javax.inject.Singleton                             // 导入 Singleton：单例作用域

/**
 * 知识库仓库（KnowledgeRepository）—— 投喂内容的入库编排（M4 起，M6 补全路由）。
 * 流程：路由分派（文本/图片/压缩包）→ 解析 → 分块 → 嵌入 → 幂等入库。
 * 并订阅 NoteBridge 实现 R7「笔记自动进知识库」。
 */
@Singleton                                               // 单例：全应用共享
class KnowledgeRepository @Inject constructor(           // 构造函数注入
    private val kbDao: KbDao,                            // 注入知识库 DAO
    private val noteDao: NoteDao,                        // 注入笔记 DAO（读笔记内容）
    private val embedder: EmbeddingProvider,             // 注入嵌入提供者
    private val ocr: OcrEngine,                          // 注入 OCR 引擎（图片识别，M6）
    private val locationDao: FileLocationDao,            // 注入文件位置 DAO（不可解析兜底，R10）
    private val storage: StorageProvider,                // 注入存储提供者（解压临时目录）
    private val notePrefs: NotePrefs,                    // 注入笔记偏好（自动入库开关）
) {
    /** 笔记自动入库使用的固定知识库 ID。 */
    companion object { const val NOTES_FOLDER = "notes" } // 笔记知识库标识

    /** 图片扩展名集合（走 OCR）。 */
    private val imageExts = setOf("png", "jpg", "jpeg", "webp", "bmp", "gif")  // 常见位图格式

    /** 压缩包扩展名集合（走 ArchiveExtractor，R9）。 */
    private val archiveExts = setOf("zip", "tar", "gz", "tgz", "bz2", "tbz2", "xz", "txz")  // 支持的压缩格式

    /**
     * 订阅笔记变更事件（R7）：笔记新建/更新时入库，删除时移除对应分块。
     * 由 app 层在启动时用应用级作用域调用一次。
     */
    fun observeNoteBridge(scope: CoroutineScope, bridge: NoteBridge) {  // 订阅笔记事件
        scope.launch {                                    // 在作用域内启动
            bridge.changes.collect { change ->            // 收集事件流
                when (change.action) {                    // 按动作分发
                    NoteChanged.Action.CREATED, NoteChanged.Action.UPDATED -> {  // 新建/更新
                        if (notePrefs.autoIngest.first()) ingestNote(change.noteId)  // 开关开启才自动入库
                    }
                    NoteChanged.Action.DELETED -> {        // 删除
                        val docId = docIdOf(change.noteId)  // 文档 ID
                        kbDao.deleteChunksByDoc(docId)     // 删除→移除分块
                        kbDao.deleteDocument(docId)         // 删除移除文档记录（避免孤儿文档）
                    }
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

    /**
     * 把一份文件解析后入库（文件投喂 R8/R9/R10 完整路由）。
     * 路由规则：压缩包→解压逐文件；图片→OCR；文本/PDF/DOCX→直接解析；
     * 7z/RAR 等不可解析→记录位置（R10），AI 可回答"文件在哪"。
     */
    suspend fun ingestFile(file: File, folderId: String) {  // 文件入库
        val docId = fileDocId(file)                       // 文档 ID（路径 hash）
        val ext = file.extension.lowercase()              // 扩展名小写
        when {
            ext in archiveExts -> ingestArchive(file, folderId)  // R9：压缩包
            ext in imageExts -> try {                     // M6：图片走 OCR
                ingestText(docId, folderId, file.name, ocr.recognize(file))  // OCR 提取后入库
            } catch (e: Exception) {                      // OCR 失败（如 AAR 未集成）
                recordLocation(file.absolutePath, null, file)  // R10：记录位置兜底
            }
            else -> try {                                 // 文本/PDF/DOCX：直接解析
                val parsed = DocumentParser.parse(file)   // 解析为文本
                ingestText(docId, folderId, parsed.source, parsed.text)  // 入库
            } catch (e: Exception) {                      // 不支持的格式（7z/RAR 等）
                recordLocation(file.absolutePath, null, file)  // R10：记录位置兜底
            }
        }
    }

    /**
     * 压缩包入库（R9）：解压到 indexDir/extract/ 临时目录后逐文件路由入库。
     * 嵌套压缩包不递归（防解压炸弹连环），不可解析条目走位置记录（R10）。
     * 入库完成后清理临时解压产物（分块已在数据库，无需保留）。
     */
    private suspend fun ingestArchive(archive: File, folderId: String) {  // 压缩包入库
        val targetDir = File(storage.indexDir(), "extract/" + archive.name + "-" + archive.length())  // 临时目录
        try {
            val files = ArchiveExtractor.extract(archive, targetDir)  // 解压（含 zip-slip/炸弹防护）
            files.forEach { f ->                          // 逐文件路由
                val ext = f.extension.lowercase()         // 条目扩展名
                val innerPath = archive.absolutePath + "!/" + f.relativeTo(targetDir).path  // 稳定内路径（记录用）
                when {
                    ext in imageExts -> try {             // 图片条目：OCR
                        ingestText(fileDocId(f), folderId, f.name, ocr.recognize(f))  // OCR 后入库
                    } catch (e: Exception) {              // OCR 失败
                        recordLocation(innerPath, archive.absolutePath, f)  // 位置记录
                    }
                    ext in archiveExts -> recordLocation(innerPath, archive.absolutePath, f)  // 嵌套压缩包：不递归，仅记录
                    else -> try {                         // 文本类条目：直接解析
                        val parsed = DocumentParser.parse(f)  // 解析
                        ingestText(fileDocId(f), folderId, parsed.source, parsed.text)  // 入库
                    } catch (e: Exception) {              // 不可解析条目
                        recordLocation(innerPath, archive.absolutePath, f)  // 位置记录
                    }
                }
            }
        } catch (e: Exception) {                          // 解压整体失败（炸弹/不支持格式）
            recordLocation(archive.absolutePath, null, archive)  // 记录压缩包本身位置
        } finally {
            targetDir.deleteRecursively()                 // 清理临时解压产物（派生数据）
        }
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
                    embedding = vectors.getOrNull(i)?.toBytes(),  // 向量（序列化为 BLOB；数量不匹配时降级无向量）
                )
            },
        )
    }

    /** 记录不可解析文件的位置（R10 兜底）：仅存元数据，不存内容（隐私红线）。 */
    private suspend fun recordLocation(path: String, archivePath: String?, file: File) {  // 位置记录
        locationDao.upsert(                               // 幂等写入
            FileLocation(                                 // 构造位置记录
                path = path,                              // 主键：绝对路径 或 "压缩包!/内路径"
                archivePath = archivePath,                // 所属压缩包（无则 null）
                name = file.name,                         // 文件名
                ext = file.extension.lowercase(),         // 扩展名
                sizeBytes = file.length(),                // 大小
                mtime = file.lastModified(),              // 修改时间
                indexedAt = System.currentTimeMillis(),   // 记录时间
            ),
        )
    }

    /** 笔记 → 文档 ID 的约定映射。 */
    private fun docIdOf(noteId: Long): String = "note_$noteId"  // 映射方法

    /** 文件 → 文档 ID 的约定映射（绝对路径 hash，稳定可复现）。 */
    private fun fileDocId(file: File): String = "file_" + file.absolutePath.hashCode().toString()  // 映射方法
}
