# 开发记录：M4 收尾 — 压缩包解析 / PDF / 文件位置记录（R9/R10）

- 日期：2026-08-24
- 涉及模块：core:db、core:ingest
- 关联需求：R9（压缩包 + 常见格式）、R10（不可解析文件记录位置）
- 关联文档：docs/03-contracts.md、docs/04-database.md

## 1. 目标与范围

**目标**
- ArchiveExtractor：解压 ZIP / TAR(.gz/.bz2/.xz)，含 zip-slip + 解压炸弹防护（R9）；
- DocumentParser 增 PDF（pdfbox-android）解析；
- FileLocation 表 + DAO：记录不可解析文件的位置，AI 可告知"文件在哪"（R10）。

**不做**
- 7z / RAR 解压（由上层 catch 异常后走 FileLocation 记录位置，R10 兜底）；
- 图片 OCR（M6 用 MNN-PaddleOCR）。

## 2. 设计要点

- **安全优先**：ArchiveExtractor 强制 zip-slip 防护（canonicalFile 规范化后必须在目标目录内）+ 解压炸弹防护（总量 2GB / 单文件 500MB / 压缩比 200 三重上限）；
- **R10 兜底**：7z/RAR 等不支持格式抛 `UnsupportedArchiveException`，上层捕获后写 FileLocation 表（存压缩包路径 + 内部路径 + 大小），AI 问答时经 `FileLocationDao.search` 告知用户位置；
- **PDF**：pdfbox-android（`com.tom_roush.pdfbox` 命名空间）提取全文，扫描版 PDF 需 OCR（后续）。

## 3. 接口契约

```kotlin
// core:ingest
object ArchiveExtractor { fun extract(archive: File, targetDir: File): List<File> }  // ZIP/TAR，含防护
object DocumentParser { fun parse(file: File): ParsedText }   // + pdf 支持

// core:db
@Entity(tableName = "file_locations") data class FileLocation(path, archivePath?, name, ext, sizeBytes, mtime, indexedAt)
interface FileLocationDao { fun upsert(loc); fun search(keyword, limit): List<FileLocation> }
```

AppDatabase version 3（新增 file_locations 表）。已同步 docs/04-database.md。

## 4. 关键实现

新增/变更文件（逐行中文注释）：

```
core/db/...  entity/FileLocation.kt、dao/FileLocationDao.kt；AppDatabase(v3) + DatabaseModule(provideFileLocationDao)
core/ingest/...  ArchiveExtractor.kt、DocumentParser.kt（+parsePdf）；build.gradle（+commons-compress +pdfbox-android）
```

可复现命令：`gradlew :app:assembleDebug`。

## 5. 测试与验证

- [ ] `gradlew :app:assembleDebug` 编译通过（需用户 AS/CI 验证）；
- [ ] 投喂一个 zip：能解压出文本文件并入库；
- [ ] 投喂 7z/rar：不崩溃，位置记录写入 file_locations；
- [ ] zip-slip 样例（含 `../` 路径的恶意 zip）被拒绝。

**已知问题**：本环境无法编译 Android；7z/RAR 未实现解压（走位置记录）；扫描版 PDF 无 OCR。

## 6. 接手指引

下一步（M5 会话记忆）：core:ai:memory 的 MemoryExtractor（每 N 轮提炼事实/偏好/待办 → kb_memory 表）+ 问答时并入 RAG 上下文。

踩坑点：
- pdfbox-android 的包名是 `com.tom_roush.pdfbox`（不是 `org.apache.pdfbox`）；
- zip-slip 用 `canonicalFile.startsWith` 判断，不能用字符串 startsWith（易被 `..` 绕过）；
- 7z/RAR 解析应 catch 后降级为位置记录，而非让入库流程崩溃。
