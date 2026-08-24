# 开发记录：M5 会话记忆 — MemoryExtractor 提炼 + RAG 并入长期记忆

- 日期：2026-08-24
- 涉及模块：core:db、core:ai:memory、core:ingest、feature:chat
- 关联需求：R6（会话自动记忆）
- 关联文档：docs/03-contracts.md、docs/04-database.md

## 1. 目标与范围

**目标**
- 实现会话自动记忆：MemoryExtractor（LLM 提炼事实/偏好/待办）+ MemoryStore（嵌入落库 + 检索）；
- RagService 问答时并入长期记忆；
- ChatViewModel 每 4 轮对话触发一次提炼。

**不做**
- 语义检索记忆（当前关键词兜底，语义检索后续并入 HybridRetriever）；
- 记忆的聚类/去重/冲突合并（后续）。

## 2. 设计要点

- **结构化提炼**：用 ChatEngine 让 LLM 输出 `{"facts":[],"preferences":[],"todos":[]}` JSON，解析容错（截取 `{...}` 片段，解析失败返回空不中断）；
- **触发策略**：每 4 轮（user 消息数 % 4 == 0）取最近 8 条消息提炼一次，避免每轮都调用 LLM；
- **双源问答**：RagService 同时检索知识库（HybridRetriever）+ 长期记忆（MemoryStore.search），记忆不标角标、知识库标 [n]；
- **隐私**：提炼走 ChatEngine（本地引擎时全本地，云端时仅发送最近对话）。

## 3. 接口契约

```kotlin
// core:ai:memory
data class MemoryEntry(type: MemoryType, topic: String, text: String)
class MemoryExtractor { suspend fun extract(messages): List<MemoryEntry> }   // LLM 提炼
class MemoryStore { suspend fun remember(messages): Int; suspend fun search(keyword, limit): List<KbMemory> }

// core:db
@Entity(tableName = "kb_memory") data class KbMemory(id, type, topic, text, source, ts, embedding)
interface MemoryDao  // upsert / searchByKeyword / recent / delete
```

AppDatabase version 4（新增 kb_memory 表）。已同步 docs/03-contracts.md 与 docs/04-database.md。

## 4. 关键实现

新增/变更文件（逐行中文注释）：

```
core/db/...  entity/KbMemory.kt、dao/MemoryDao.kt；AppDatabase(v4) + DatabaseModule(provideMemoryDao)
core/ai/memory/...  MemoryExtractor.kt、MemoryStore.kt；build.gradle（+db +engine +embed）
core/ingest/...  RagService.kt（并入记忆段）；build.gradle（+core:ai:memory）
feature/chat/...  ChatViewModel.kt（+memoryStore +maybeRemember 每 4 轮）；build.gradle（+core:ai:memory）
app/build.gradle.kts（+core:ai:memory）
```

可复现命令：`gradlew :app:assembleDebug`。

## 5. 测试与验证

- [ ] `gradlew :app:assembleDebug` 编译通过（需用户 AS/CI 验证）；
- [ ] 对话 4 轮后，kb_memory 表新增提炼出的记忆；
- [ ] RagService.ask 能结合记忆回答（记忆相关时命中）。

**已知问题**：本环境无法编译 Android；记忆检索为关键词兜底（语义检索后续）；记忆无去重（后续）。

## 6. 接手指引

下一步（M6 本地引擎）：
1. core:models：ModelRepository/ModelImporter/HardwareProfile（模型下载/硬件评估/本地导入）；
2. core:ai:engine 的 LocalChatEngine（MNN-LLM JNI 桥）+ 把 EmbeddingProvider 换成 bge（MNN）；
3. core:ingest 的 OcrEngine（MNN-PaddleOCR）。

踩坑点：
- MemoryExtractor 提炼时只调一次引擎（避免重复调用 LLM 浪费 token，已修复）；
- JSON 解析要容错（LLM 可能输出 ```json 包裹或前后杂文），用 indexOf('{')/lastIndexOf('}') 截取；
- 每 N 轮触发的 N 不能太小（否则频繁调 LLM），也不能太大（记忆滞后），4 轮是经验值。
