# 03 · 接口契约（单一事实源）

> 本文是模块接口的权威文档。**改接口先写 ADR，再改代码，然后同步本文**。
> M0 状态：仅 core:search 的搜索契约已定义；其余模块接口在各自阶段实现时补录。

## 搜索契约（core:search · M0 已定）

### SearchConsentGate（许可闸门）

```kotlin
class SearchConsentGate(
    private val settings: SearchSettings,
    private val now: () -> Long = System::currentTimeMillis,
) {
    /** 用户显式触发（UI 点击）——始终放行。 */
    fun beginUserInitiated(requestId: String): SearchSession

    /** 后台/计划触发——仅当 backgroundIndexingEnabled=true 才放行，否则返回 null。 */
    fun beginScheduled(requestId: String): SearchSession?

    fun isAllowed(trigger: SearchTrigger): Boolean
}
```

### SearchSession

```kotlin
class SearchSession private constructor(
    val requestId: String,
    val trigger: SearchTrigger,   // USER_ACTION / SCHEDULED_BACKGROUND
    val startedAt: Long,
) {
    @Volatile var cancelled: Boolean  // UI「停止」置位，索引器每批轮询
    fun cancel()
}
```

### SearchSettings（由 feature:settings 用 DataStore 实现）

```kotlin
interface SearchSettings {
    val backgroundIndexingEnabled: StateFlow<Boolean>  // 默认必须为 false
}
```

### FileIndexer

```kotlin
interface FileIndexer {
    /** 必须携带 SearchSession；无会话抛 UnauthorizedSearchException。 */
    suspend fun index(session: SearchSession, scope: SearchScope, listener: SearchProgressListener): IndexResult
    fun cancel(requestId: String)
}

sealed interface SearchScope {
    data class AppScoped(val roots: List<File>, val safTrees: List<Uri> = emptyList()) : SearchScope   // L0
    data class UserStorage(val allowedTopDirs: List<String>) : SearchScope                             // L1
    data class FullDisk(val allowedTopDirs: List<String>, val includeData: Boolean = false) : SearchScope // L2
}
```

### SearchProgress（进度契约，上报规则见 ADR-002）

```kotlin
enum class SearchPhase { INITIALIZING, SCANNING_DIRS, HASHING, INDEXING_DB, QUERYING, DONE, CANCELLED, FAILED }

data class SearchProgress(
    val requestId: String,
    val phase: SearchPhase,
    val scannedItems: Long, totalItems: Long,
    val currentPath: String?, message: String,
    val startedAt: Long, updatedAt: Long,
) { val percent: Float }   // 0f..1f

fun interface SearchProgressListener { fun onProgress(progress: SearchProgress) }
```

### SearchService（纯查询，无副作用）

```kotlin
interface SearchService {
    suspend fun search(query: FileQuery): List<FileHit>   // 只读 FTS5 索引
}
```

### 审计

```kotlin
data class ConsentAuditEntry(
    val requestId: String, val trigger: String, val scope: String,
    val granted: Boolean, val reason: String, val startedAt: Long,
)   // 落库表 consent_audit（M1 实现）
```

## 存储契约（core:storage · M1 已定）

```kotlin
interface StorageProvider {
    val root: File
    fun dbDir(): File; fun modelsDir(): File; fun knowledgeDir(): File; fun indexDir(): File
    fun ensureDirs()
}
class DefaultStorageProvider(context: Context): StorageProvider       // 应用私有目录（默认）
class CustomStorageProvider(customRoot: File): StorageProvider        // 用户指定目录（需 MANAGE_EXTERNAL_STORAGE / root，M7 开放）
object StorageMigrator { fun migrate(from: File, to: File): Int }     // 复制 + 逐文件大小校验
```

## 数据库契约（core:db · M1 已定）

```kotlin
@Database(entities=[Note,TodoItem,Conversation,ChatMessage,ConsentAuditEntity], version=1)
abstract class AppDatabase : RoomDatabase()          // 路径走 StorageProvider.dbDir()/muyunmiao.db
interface NoteDao          // observeActive / observeById / upsert / softDelete / observeTodos / upsertTodo
interface ChatDao          // observeConversations / upsertConversation / observeMessages / insertMessage / touch
interface ConsentAuditDao  // insert / recent（consent_audit 落库）
```

## 对话引擎契约（core:ai:engine · M3 已定）

```kotlin
sealed interface ChatEvent { data class Delta(text: String): ChatEvent; data class Done(reason: String): ChatEvent }
interface ChatEngine { val type: EngineType; fun streamChat(messages, system?): Flow<ChatEvent> }
data class CloudConfig(baseUrl, apiKey, model)
interface CloudConfigProvider { suspend fun current(): CloudConfig? }   // 由 feature:settings 实现
class CloudChatEngine : ChatEngine    // OpenAI 兼容 SSE（本地 MNN 引擎 M6 补）
```

## 预留接口（后续阶段补录）

| 接口 | 归属 | 阶段 |
|---|---|---|
| `EmbeddingProvider` | core:ai:embed | M4 |
| `PrivilegeManager` | core:search | M7 |
| `ToolCallingBus` / `AiTool` | core:ai:tools | M7 |
| `DocumentParser` / `ArchiveExtractor` | core:ingest | M4 |
| `OcrEngine` | core:ingest | M4 |
| `ModelRepository` / `ModelImporter` / `HardwareProfile` | core:models | M6 |
| `MemoryExtractor` / `MemoryStore` | core:ai:memory | M5 |
