# 04 · 数据库设计（Room）

> M1 已建核心实体（notes / todo_items / conversations / messages / consent_audit），其余表按阶段补录；变更必须同步更新本文。

## 表清单

| 表 | 用途 | 关键字段 | 阶段 |
|---|---|---|---|
| `notes` | 常规备忘录（Markdown 富文本） | id, title, content, type(TEXT/RICH/TODO), pinned, createdAt, updatedAt, deletedAt | ✅ M1 建表 |
| `todo_items` | 待办条目 | id, noteId, text, done, order | ✅ M1 建表 |
| `conversations` | AI 会话 | id, title, engine(LOCAL/CLOUD), kbFolderId, createdAt, updatedAt | ✅ M1 建表 |
| `messages` | 聊天消息 | id, convId, role, content, citations(JSON), ts | ✅ M1 建表 |
| `kb_folders` | 知识库文件夹（SAF） | folderId, displayName, treeUri, createdAt | M4 后续 |
| `kb_documents` | 已投喂文档 | docId(hash), folderId, fileName, fileUri, fileHash, status, chunkCount, indexedAt | ✅ M4 建表 |
| `kb_chunks` | 分块 + 向量 | id, docId, folderId, seq, text, embedding(BLOB: FloatArray) | ✅ M4 建表 |
| `kb_chunks_fts` | 分块全文索引（FTS5 外表） | text, docId, folderId | M4 后续（暂用 LIKE） |
| `kb_memory` | 会话记忆条目（R6） | id, type(FACT/PREFERENCE/TODO), topic, text, source(chat/memo/import), ts, embedding | M5 |
| `file_locations` | 不可解析文件位置记录（R10） | path, archivePath?, name, ext, sizeBytes, mtime, indexedAt | M4 后续 |
| `file_index` | 文件索引（R11，FTS5） | path, name, ext, sizeBytes, mtime, parent | M7 |
| `consent_audit` | 搜索/索引审计日志 | requestId, trigger, scope, granted, reason, startedAt | ✅ M1 建表 |

## FTS5 说明

- `kb_chunks_fts` 与 `file_index` 使用 SQLite FTS5（Room 通过 `@Fts4`/`Fts3` 或外部 FTS5 支持）；
- 混合检索：语义向量（余弦）+ 关键词（FTS5），RRF 融合（core:ai:embed 实现，M4）。

## 索引建议

- `notes(updatedAt)`：NoteBridge 增量同步用；
- `kb_documents(folderId, fileHash)`：增量去重；
- `file_index(ext, parent)`：按目录/扩展名过滤；
- `consent_audit(startedAt)`：隐私审计查询。
