# 开发记录：M4 知识库核心 — 解析/分块/嵌入/混合检索 + R7 笔记自动入库 + API Key 加密

- 日期：2026-08-24
- 涉及模块：core:db、core:ingest、core:ai:embed、feature:settings、app
- 关联需求：R7（笔记自动进知识库）、R8（文件投喂）、R9（压缩包，后续补）、R1（API Key 加密）
- 关联文档：docs/03-contracts.md、docs/04-database.md

## 1. 目标与范围

**目标**
- 知识库核心闭环：文档实体（KbDocument/KbChunk）+ 分块 + 解析 + 嵌入 + 混合检索 + 带引用作答；
- R7「笔记自动进知识库」：NoteBridge 上移到 core 层，KnowledgeRepository 订阅实现自动入库；
- API Key 改用 EncryptedSharedPreferences（Keystore AES256_GCM）加密存储；
- 确认 gradle wrapper jar 可提交（jar + gradlew + gradlew.bat 就位，未被 .gitignore 忽略）。

**不做（留后续）**
- 压缩包解析（ArchiveExtractor，M4 后续）、图片 OCR、PDF 解析（pdfbox）；
- 本地 bge 嵌入（M6，当前用 SimpleHashEmbeddingProvider 占位，保证闭环可运行）；
- 云端 embedding API。

## 2. 设计要点

- **依赖方向修正**：NoteBridge 从 feature:notes 上移到 core:ingest（core 不依赖 feature 的架构不变式），feature:notes 依赖 core:ingest；
- **嵌入双轨占位**：EmbeddingProvider 接口 + SimpleHashEmbeddingProvider（字符 n-gram 哈希 256 维，本地占位），M6 换 bge 时接口不变、检索层无感；
- **混合检索**：语义（余弦）+ 关键词（LIKE）+ RRF 融合，中文专名/编号用关键词兜底；
- **幂等入库**：入库前先 deleteChunksByDoc 再 insertChunks，docId 稳定（note_<id> / file_<hash>）；
- **API Key 分级存储**：baseUrl/model 用 DataStore（非敏感），apiKey 用 EncryptedSharedPreferences（敏感）。

## 3. 接口契约

```kotlin
// core:ai:embed
interface EmbeddingProvider { val dim: Int; suspend fun embed(texts): List<FloatArray> }
class SimpleHashEmbeddingProvider : EmbeddingProvider   // 占位实现
class HybridRetriever { suspend fun retrieve(folderId, query, topK): List<RetrievedChunk> }
fun FloatArray.toBytes(): ByteArray                      // 向量序列化

// core:ingest
object Chunker { fun split(text, maxLen=400, overlap=80): List<String> }
object DocumentParser { fun parse(file): ParsedText }    // TXT/MD/DOCX
class KnowledgeRepository { suspend fun ingestNote(noteId); fun observeNoteBridge(scope, bridge) }
class RagService { suspend fun ask(folderId, question): Flow<ChatEvent> }

// core:db
interface KbDao  // upsertDocument / chunksByFolder / deleteChunksByDoc / insertChunks / searchByKeyword
```

已同步 docs/03-contracts.md（EmbeddingProvider 转"已定"）。

## 4. 关键实现

新增/变更文件（逐行中文注释）：

```
core/db/...  entity/KbEntities.kt（KbDocument/KbChunk/IngestStatus）、dao/KbDao.kt
             AppDatabase（v2，注册 kb 表 + kbDao）、DatabaseModule（provideKbDao）
core/ingest/...  Chunker.kt / DocumentParser.kt / NoteBridge.kt（上移）/ KnowledgeRepository.kt / RagService.kt
core/ai/embed/...  EmbeddingProvider.kt / HybridRetriever.kt / EmbedModule.kt
feature/notes/...  NoteListViewModel（改 import core.ingest.NoteBridge）、删 NoteBridge.kt
feature/settings/...  CloudConfigRepository（apiKey 改 EncryptedSharedPreferences）、build.gradle（+security-crypto）
app/...  MemoApp.kt（启动时订阅 NoteBridge）、build.gradle.kts（+core:ingest +core:ai:embed）
gradle/wrapper/  gradle-wrapper.jar + gradlew + gradlew.bat（可提交）
```

可复现命令：`gradlew :app:assembleDebug`（wrapper jar 已就位）。

## 5. 测试与验证

- [ ] `gradlew :app:assembleDebug` 编译通过（本环境无 SDK，需用户 AS/CI 验证）；
- [ ] R7 闭环：新建/编辑笔记 → 自动入库 → RagService.ask 能检索到笔记内容；
- [ ] API Key 加密：保存后 apiKey 不落明文（DataStore 仅存 baseUrl/model）；
- [ ] wrapper jar 未被 .gitignore 忽略（`git check-ignore` 验证通过）。

**已知问题**：本环境无法编译 Android；SimpleHashEmbeddingProvider 语义精度有限（M6 换 bge）；PDF/压缩包/OCR 未实现。

## 6. 接手指引

下一步（M4 收尾 + M5/M6）：
1. ArchiveExtractor（ZIP/TAR/7z/RAR4，commons-compress + junrar）+ FileLocationIndex（R10）；
2. PDF（pdfbox-android）+ 图片 OCR（MNN-PaddleOCR，M6）；
3. M5 会话记忆提炼（MemoryExtractor）；
4. M6 本地引擎：bge 嵌入 + Qwen3 对话（MNN）+ 模型管理/硬件评估/本地导入。

踩坑点：
- Room 数据库 version 从 1 升到 2（新增 kb 表），已升级但未提供 Migration（开发期 destroy 重建即可，发布前补）；
- NoteBridge 上移后 feature:notes 必须 import `com.memuo.core.ingest.NoteBridge`；
- EncryptedSharedPreferences 的 MasterKey 依赖 Keystore，需设备正常支持（模拟器/真机均 OK）。
