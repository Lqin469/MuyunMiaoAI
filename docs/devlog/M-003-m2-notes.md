# 开发记录：M2 常规备忘录 — 笔记 CRUD + NoteBridge

- 日期：2026-08-24
- 涉及模块：feature:notes、core:db（补 observeById/getById）
- 关联需求：R7（笔记自动进知识库的桥接层）、R12
- 关联文档：docs/03-contracts.md、docs/04-database.md

## 1. 目标与范围

**目标**
- 实现常规备忘录的增删改查（列表 / 编辑 / 新建 / 软删除）；
- 实现 NoteBridge 事件总线：笔记变更时发布事件，为 R7「备忘录内容自动被 AI 读取」预留订阅点（实际入库在 M4）。

**不做**
- 不实现富文本编辑器（Markdown 文本即可，渲染后续）；
- 不实现待办清单 UI（NoteDao 已有 TodoItem 表，M2 仅文本笔记）；
- 不实现知识库实际入库（M4 的 IngestWorker 才消费 NoteBridge 事件）。

## 2. 设计要点

- **事件桥解耦**：NoteBridge 用 `MutableSharedFlow<NoteChanged>`（缓冲 64 条），笔记 CRUD 只发事件、不关心消费者；M4 的知识库模块订阅它实现 R7，避免 feature:notes 反向依赖 core:ingest；
- **软删除**：删除只写 `deletedAt`，列表查询过滤 `deletedAt IS NULL`，支持回收站（后续）；
- **编辑页本地状态**：标题/正文用 `mutableStateOf` 本地缓存，保存时才写库 + 发事件（避免每键入一次就写库 + 触发知识库同步）；
- **单 Activity 导航**：MainActivity 用 NavHost 串 `note/list` ⇄ `note/edit/{id}`，替代 M0 占位页。

## 3. 接口契约

```kotlin
// feature:notes
data class NoteChanged(val noteId: Long, val action: Action)  // Action: CREATED/UPDATED/DELETED
@Singleton class NoteBridge { val changes: SharedFlow<NoteChanged>; suspend fun emitChanged(...) }
@HiltViewModel class NoteListViewModel {
    val notes: StateFlow<List<Note>>       // observeActive + stateIn
    fun createNote(): Long; fun deleteNote(id); fun updateContent(id, title, content)
    fun observeNote(id): Flow<Note?>       // 编辑页加载单条
}
// core:db（补）
NoteDao.observeById(id): Flow<Note?>; NoteDao.getById(id): Note?
```

已同步 docs/03-contracts.md（NoteBridge/NoteListViewModel）与 docs/04-database.md（notes 表 M2 启用）。

## 4. 关键实现

新增/变更文件（逐行中文注释）：

```
feature/notes/...  NoteBridge.kt / NoteListViewModel.kt / NoteListScreen.kt / NoteEditScreen.kt
core/db/dao/Daos.kt（NoteDao 补 observeById/getById）
app/...  MainActivity.kt（NavHost 导航替换占位页）、app/build.gradle.kts（+navigation +feature:notes）
gradle/libs.versions.toml（+hilt-navigation-compose 1.2.0）
```

可复现命令：`gradle :app:assembleDebug`（AS / 已装 Gradle 环境）。

## 5. 测试与验证

- [ ] `gradle :app:assembleDebug` 编译通过（本环境无 SDK，需用户真机/AS 验证）；
- [ ] 手工验收：新建笔记 → 编辑保存 → 返回列表可见 → 删除后从列表消失；
- [ ] NoteBridge 事件：保存/删除时事件被 emit（M4 接入后可验证入库联动）。

**已知问题**：本环境无法编译 Android，编译验证依赖用户 AS/CI；`createNote()` 返回的 ID 是异步赋值，仅用于导航跳转（跳转前协程已启动，ID 会被赋值，但极端情况下存在竞态，M2 可接受，后续优化为回调式）。

## 6. 接手指引

下一步（M3）：云端对话（用户自配 API + SSE 流式）：
1. `core:ai:engine`：ChatEngine 接口 + CloudChatEngine（OpenAI 兼容 SSE）+ 会话/消息写库（Conversation/ChatMessage 表已建）；
2. `feature:chat`：会话列表 + 对话页（流式 Markdown 渲染）；
3. `feature:settings`：云端 API 配置页（baseUrl/APIKey/模型，Key 用 EncryptedSharedPreferences）。

踩坑点：
- NoteBridge 的 SharedFlow 需缓冲（replay=0 + extraBufferCapacity），否则慢消费者丢事件；
- 软删除的列表查询务必带 `deletedAt IS NULL`，否则"已删除"笔记会重新出现；
- 编辑页本地状态用 `LaunchedEffect(note?.id)` 同步，切换笔记时重置。
