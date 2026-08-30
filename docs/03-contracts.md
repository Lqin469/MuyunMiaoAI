# 03 · 接口契约（单一事实源）

> 本文是模块接口的权威文档。**改接口先写 ADR，再改代码，然后同步本文**。
> 状态：M0-M8 全部实现完毕，以下契约均与当前代码一致（2026-08-31 核对）。

## 搜索契约（core:search · M0/M7 已实现）

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

## 提权契约（core:search · M7 已实现）

```kotlin
enum class PrivilegeLevel { NONE, SHIZUKU_ADB, SHIZUKU_ROOT }        // L0 / L1 / L2
class PrivilegeManager(context: Context) {
    val level: StateFlow<PrivilegeLevel>                              // binder 监听实时刷新
    val authorized: StateFlow<Boolean>
    fun currentLevel(): PrivilegeLevel                                // NONE / SHIZUKU_ADB / SHIZUKU_ROOT
    fun requestAdbPermission(onResult: (Boolean) -> Unit)             // 弹 Shizuku 授权页
    fun shell(command: String): String                                // libsu root 执行（L2）
}
// 判定规则：Shizuku.pingBinder() && Shell.isAppGrantedRoot()==true → SHIZUKU_ROOT
```

## 数据库契约（core:db · 当前 version = 4）

```kotlin
@Database(
    entities = [Note, TodoItem, Conversation, ChatMessage, KbDocument, KbChunk, KbMemory, FileLocation, ConsentAuditEntity],
    version = 4, exportSchema = false,
)   // 路径走 StorageProvider.dbDir()/muyunmiao.db；升级用 fallbackToDestructiveMigration（发布前改显式 Migration）
interface NoteDao          // observeActive / observeTrashed / observeById / upsert / softDelete / restore / purge / purgeTrashed / observeTodos / upsertTodo / setTodoDone / deleteTodo
interface ChatDao          // observeConversations / upsertConversation / observeMessages / insertMessage / touch / deleteConversation / deleteMessage
interface KbDao            // observeDocuments / observeChunksByFolder / upsertDocument / upsertChunk / countChunks / searchChunksLike
interface MemoryDao        // observeRecent / insert / delete / search
interface FileLocationDao  // upsertAll / fuzzySearch（name/path LIKE）
interface ConsentAuditDao  // insert / recent
```

## 对话引擎契约（core:ai:engine · M3/M6/M-027 已实现）

```kotlin
sealed interface ChatEvent { data class Delta(text: String): ChatEvent; data class Done(reason: String): ChatEvent }
interface ChatEngine { val type: EngineType; fun streamChat(messages, system?): Flow<ChatEvent> }
enum class EngineType { CLOUD, LOCAL }

// 引擎设置与路由（M-010）
interface EngineSettings { val engineType: StateFlow<EngineType>; suspend fun setEngineType(type: EngineType) }
class EngineRouter(settings, localEngine, cloudEngine) : ChatEngine   // 按设置路由；切本地前检查模型就绪

// 云端配置（R1）
data class CloudConfig(baseUrl, apiKey, model)
interface CloudConfigProvider { suspend fun current(): CloudConfig? }  // feature:settings 实现，apiKey 加密存储
class CloudApiClient          // OpenAI 兼容 SSE；超时 10s/120s；4xx 不重试、5xx/网络指数退避 3 次（1s→2s→4s+抖动）

// 本地引擎（M6/M-014/M-033）
class LocalChatEngine : ChatEngine       // MNN-LLM JNI；加载前内存预检（权重×1.5+256MB）；isModelLoaded()

// 运行状态监控（M-027）与诊断（M-031）
data class EngineRuntimeState(idle/loading/running/error, loadMs, firstTokenMs, tokenCount, error?)
class EngineRuntimeMonitor { val state: StateFlow<EngineRuntimeState> }
class ModelLoadDiagnostics { suspend fun runDiagnostics(): String }   // 7 段诊断日志；已加载则跳过 nativeInit（防 OOM）
```

## 设备契约（core:device · M-027 新增）

```kotlin
data class DeviceSnapshot(manufacturer, model, androidVersion, sdkInt, cpu: CpuInfo, memory: MemoryInfo, storage: StorageInfo)
enum class CheckLevel { PASS, WARN, FAIL }
data class CheckResult(id, label, value, level, hint)
class DeviceInfoProvider { suspend fun snapshot(): DeviceSnapshot }   // 型号/系统/CPU 主频(sysfs)/内存/存储
class CapabilityChecker { fun checkAll(info: DeviceSnapshot): List<CheckResult> }  // 64 位必需/RAM≥3GB/存储≥512MB
```

## 局域网契约（core:lan · M-027 新增）

```kotlin
object LanProtocol { const val SERVICE_TYPE = "_muyunmiao._tcp"; const val PORT = 21066; fun fileIdOf(file): String }
data class LanDevice(name, ip, port, version)
enum class SessionState { IDLE, RUNNING, PAUSED, SUCCESS, FAILED }
data class SendSession / ReceiveSession(fileId, name, size, sent/received, speedBps, state)
class TransferRepository {
    val devices: StateFlow<List<LanDevice>>; val sendSession: StateFlow<SendSession?>; val receiveSessions: StateFlow<List<ReceiveSession>>
    fun startAll() / stopAll() / send(device, file) / pause() / resume() / clearSend()
}   // 协议：QUERY fileId → HAVE bytes（断点锚点）；SEND {id,name,size,offset} + 二进制流 → OK/ERR
```

## 嵌入与检索契约（core:ai:embed / core:ingest · M4 已实现）

```kotlin
// core:ai:embed
interface EmbeddingProvider { val dim: Int; suspend fun embed(texts: List<String>): List<FloatArray> }
class SimpleHashEmbeddingProvider : EmbeddingProvider   // 本地占位（M6 换 bge）
class HybridRetriever { suspend fun retrieve(folderId, query, topK): List<RetrievedChunk> }  // 余弦+关键词 RRF

// core:ingest
object Chunker { fun split(text, maxLen=400, overlap=80): List<String> }
object DocumentParser { fun parse(file): ParsedText }    // TXT/MD/DOCX（PDF/压缩包/OCR 后续）
class KnowledgeRepository { suspend fun ingestNote(noteId); fun observeNoteBridge(scope, bridge) }  // R7
class RagService { suspend fun ask(folderId, question): Flow<ChatEvent> }
```

## 记忆契约（core:ai:memory · M5 已定）

```kotlin
data class MemoryEntry(type: MemoryType, topic: String, text: String)
class MemoryExtractor { suspend fun extract(messages: List<ChatMessage>): List<MemoryEntry> }  // LLM 提炼 JSON
class MemoryStore { suspend fun remember(messages): Int; suspend fun search(keyword, limit): List<KbMemory> }  // 嵌入落库 + 关键词检索
```

## 模型契约（core:models · M6/M-027/M-035 已实现）

```kotlin
// core:models
enum class ModelKind { LLM, EMBEDDING, OCR, VISION_LLM }
enum class ModelSource { CATALOG, LOCAL_IMPORT }
data class ModelItem(id, name, format, sizeGb, arch, needFp16, time)          // 模型列表条目（DataStore JSON 持久化）
data class HardwareProfile(totalRamMb, availRamMb, totalStorageMb, cpuCores, abi, gpu)  // 真物理内存（非 JVM 堆）
class ModelRepository { fun probeHardware(): HardwareProfile }                // ActivityManager.totalMem + StatFs
class ModelImporter {
    fun hasLocalModel(): Boolean
    suspend fun importMnnToAppDir(dir: File): Boolean                          // 复制到 modelsDir()/llm/
    suspend fun importFromUri(context, uri): Boolean                           // SAF 目录检测+复制
    suspend fun importGguf(context, uri): Boolean                              // 复制到 modelsDir()/gguf/
    fun listLocalModels(): List<LocalModelInfo>                                // 扫 llm/ + gguf/
    suspend fun deleteLocalModel(format, name): Boolean                        // 先 release 再删
    fun verifyModel(dir): Boolean                                              // 大小完整性校验（visual>30MB/llm>100KB）
}
data class LocalModelInfo(id, name, format, sizeBytes, runnable)               // MNN runnable=true / GGUF=false

// core:ingest
interface OcrEngine { suspend fun recognize(file: File): String }    // 桩实现（返回提示文本），待 MNN-PaddleOCR
```

## 工具调用契约（core:ai:tools · M7 已实现）

```kotlin
interface AiTool { val name: String; val desc: String; suspend fun invoke(args: Map<String, String>): String }
class ToolCallingBus { fun register(tool: AiTool); suspend fun dispatch(call: ToolCall): String; fun extractCalls(text: String): List<ToolCall> }
// 内置工具：search_file（搜索本地文件返回路径）/ tell_location（告知文件位置）
// 标记协议：[[name:args]]（本地/云端引擎共用；升级完整 function calling 登记后续）
```

## 预留接口（登记后续）

| 接口 | 归属 | 说明 |
|---|---|---|
| `SyncApi` | core:ai:engine | 云端数据同步骨架（push/pull 快照、备份上传）——仅设计，未实现 |
| GGUF 推理运行时 | core:models | 依赖 llama.cpp，登记 M-028 |
| bge 嵌入 / MNN-PaddleOCR | core:ai:embed / ingest | 模型未导出，当前降级 SimpleHash / 桩 |
