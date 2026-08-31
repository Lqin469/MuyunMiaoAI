# 04 · 数据库设计（Room）

> 状态：当前 `AppDatabase` version=4，9 张表（2026-08-31 与实体核对一致）；**变更必须同步更新本文并升级 version**。
> 注意：部分规划表实际以 DataStore / 查询替代，未在 Room 中（见「未建表」节）。

## 表清单（= AppDatabase.kt 实体清单）

| 表 | 用途 | 关键字段 | 状态 |
|---|---|---|---|
| `notes` | 常规备忘录（含待办清单载体） | id, title, content, type(TEXT/RICH/TODO), pinned, createdAt, updatedAt, deletedAt | ✅ 建表 |
| `todo_items` | 待办条目 | id, noteId, text, done, order | ✅ 建表 |
| `conversations` | AI 会话 | id, title, engine(LOCAL/CLOUD), kbFolderId, createdAt, updatedAt | ✅ 建表 |
| `messages` | 聊天消息 | id, convId, role, content, citations(JSON), ts | ✅ 建表 |
| `kb_documents` | 已投喂文档 | docId(hash), folderId, fileName, fileUri, fileHash, status, chunkCount, indexedAt | ✅ 建表 |
| `kb_chunks` | 分块 + 向量 | id, docId, folderId, seq, text, embedding(BLOB: FloatArray) | ✅ 建表 |
| `kb_memory` | 会话记忆条目（R6） | id, type(FACT/PREFERENCE/TODO), topic, text, source(chat/memo/import), ts, embedding | ✅ 建表 |
| `file_locations` | 不可解析文件位置记录（R10） | path, archivePath?, name, ext, sizeBytes, mtime, indexedAt | ✅ 建表 |
| `consent_audit` | 搜索/索引审计日志 | requestId, trigger, scope, granted, reason, startedAt | ✅ 建表 |

## 未建表（以 DataStore / 查询替代，勿按表设计误解）

| 计划表 | 实际实现 | 说明 |
|---|---|---|
| `kb_folders` | DataStore JSON（`ExtPrefs.kbFoldersJson`） | 知识库文件夹列表（含 folderId），非 Room 表 |
| `kb_chunks_fts` | 暂用 `LIKE` 关键词检索（`KbDao.searchChunksLike`） | 未建 FTS5 外表；后续可升级 |
| `file_index` | 复用 `file_locations`（`FileLocationDao.fuzzySearch`） | M7 索引写入文件位置表，未建独立索引表 |

## DataStore 偏好键（分散存储，勿混淆）

| DataStore 文件 | 键 | 用途 |
|---|---|---|
| `settings` | engine_type / background_indexing_enabled | 引擎类型 / 后台索引开关（默认 false） |
| `cloud_settings` | cloud_base_url / cloud_model（DataStore）+ cloud_api_key（EncryptedSharedPreferences） | 云端 API 配置 |
| `muyun_ext` | perm_mode / api_list_json / api_current_id / model_list_json / local_model_id / kb_folders_json / kb_privacy / migrate_logs_json / lan_receive_mode / lan_save_path | 各页面扩展偏好 |
| `wallpaper` | source(DEFAULT/PRESET/UPLOAD) / presetId / imageUri / mode(TILE/STRETCH/CENTER/BLUR) | 主题与壁纸配置 |
| `app_prefs` | first_run_done / dark_mode | 首启自检门 / 深色模式 |

## 索引建议

- `notes(updatedAt)`：NoteBridge 增量同步用；
- `kb_documents(folderId, fileHash)`：增量去重；
- `file_locations(path/name)`：模糊检索（当前 LIKE 实现）；
- `consent_audit(startedAt)`：隐私审计查询。

## 迁移策略（重要）

- 已改为**显式 Migration**：`core/db/Migrations.kt` 定义 `MIGRATION_1_2`（+kb_documents/kb_chunks）、`MIGRATION_2_3`（+file_locations）、`MIGRATION_3_4`（+kb_memory），`DatabaseModule` 用 `addMigrations(*Migrations.ALL)` 替换原 `fallbackToDestructiveMigration`，版本升级**保留数据不再清库**；
- 已开启 `exportSchema = true`，schema 历史提交到 `core/db/schemas/com.memuo.core.db.AppDatabase/`（当前 `4.json`），供编译期与运行时迁移校验；
- **后续变更规则**：改表结构必须 `version +1` 并新增对应 Migration，同时回写本文「表清单」与「迁移策略」两节。
