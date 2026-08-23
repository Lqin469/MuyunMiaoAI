# 开发记录：M3 云端对话 — ChatEngine 接口 + OpenAI 兼容 SSE + 会话落库

- 日期：2026-08-24
- 涉及模块：core:ai:engine、core:db（ChatDao）、feature:chat、feature:settings（CloudConfigRepository）
- 关联需求：R1（云端 API 用户自配）、R12
- 关联文档：docs/03-contracts.md、docs/04-database.md

## 1. 目标与范围

**目标**
- 定义 `ChatEngine` 接口（架构不变式：所有 AI 对话能力只通过本接口，本地/云端可互换）；
- 实现云端引擎：`CloudApiClient`（OkHttp SSE 流式，OpenAI 兼容）+ `CloudChatEngine`；
- 会话/消息落库（`ChatDao`）；
- 对话 UI（`ChatScreen` 流式渲染）+ 云端配置仓库（`CloudConfigRepository`）。

**不做**
- 不实现本地 MNN 引擎（M6）；
- 不实现 API Key 加密存储（当前 DataStore 明文，M3+ 改用 EncryptedSharedPreferences）；
- 不实现会话侧栏/标题自动生成（后续）。

## 2. 设计要点

- **依赖倒置**：`CloudConfigProvider` 接口定义在 core:ai:engine，由 feature:settings 实现并 `@Binds` 绑定，core 不依赖 feature；
- **SSE 流式**：OkHttp 逐行读 `data:` 行，解析 `choices[0].delta.content`，`[DONE]` 结束；用 `callbackFlow` 把回调转 `Flow<ChatEvent>`；
- **流式落库**：用户消息先落库 → 引擎流式累积增量（实时刷新 UI）→ 结束后 AI 回复一次性落库；
- **隐私**：云端只发送对话内容，绝不发送本地文件内容。

## 3. 接口契约

```kotlin
// core:ai:engine
sealed interface ChatEvent { data class Delta(text); data class Done(reason) }
interface ChatEngine { val type: EngineType; fun streamChat(messages, system?): Flow<ChatEvent> }
data class CloudConfig(baseUrl, apiKey, model)
interface CloudConfigProvider { suspend fun current(): CloudConfig? }
class CloudApiClient { fun streamChat(config, messages, system?, onDelta, onDone) }
class CloudChatEngine : ChatEngine

// core:db
interface ChatDao  // observeConversations / upsertConversation / observeMessages / insertMessage / updateTitle / touch
```

已同步 docs/03-contracts.md（ChatEngine 从"预留"转"已定"）。

## 4. 关键实现

新增/变更文件（逐行中文注释）：

```
core/ai/engine/...  ChatEngine.kt / CloudConfig.kt / CloudApiClient.kt / CloudChatEngine.kt / EngineModule.kt
core/db/...  dao/ChatDao.kt；AppDatabase +chatDao；DatabaseModule +provideChatDao
feature/settings/...  CloudConfigRepository.kt；SettingsModule +bindCloudConfigProvider
feature/chat/...  ChatViewModel.kt / ChatScreen.kt
app/build.gradle.kts（+feature:chat +core:ai:engine）
```

可复现命令：`gradle :app:assembleDebug`（AS / 已装 Gradle 环境）。

## 5. 测试与验证

- [ ] `gradle :app:assembleDebug` 编译通过（本环境无 SDK，需用户 AS/CI 验证）；
- [ ] 手工验收：设置页填 baseUrl/APIKey/model → 对话页发消息 → 流式逐字显示 → 结束后消息落库；
- [ ] 未配置时：发送返回"未配置云端 API"提示（CloudChatEngine 兜底）。

**已知问题**：本环境无法编译 Android；`CloudConfigRepository` 的 apiKey 目前 DataStore 明文存储（后续加密）。

## 6. 接手指引

下一步（M4）：知识库投喂（压缩包/文档解析/混合检索）：
1. `core:ingest`：DocumentParser（TXT/MD/PDF/DOCX）+ ArchiveExtractor（ZIP/TAR/7z/RAR）+ Chunker 分块 + IngestWorker；
2. `core:ai:embed`：EmbeddingProvider 双轨 + HybridRetriever（余弦+FTS5 RRF）；
3. 消费 M2 的 NoteBridge 事件，实现 R7「笔记自动进知识库」。

踩坑点：
- SSE 解析用 `line.startsWith("data:")` 过滤，`[DONE]` 结束；`data:` 后可能有空格需 trim；
- `callbackFlow` 里发 Done 后必须 `close()`，否则流不结束；
- 发送按钮在 `streaming` 时禁用，避免并发写库。
