# 开发记录：M1 存储与数据层 — StorageProvider / Room 全库 / SearchSettings

- 日期：2026-08-24
- 涉及模块：core:storage、core:db、feature:settings
- 关联需求：R4（单一数据库目录）、R5（自定义存储目录）、R11（搜索设置开关）
- 关联文档：docs/03-contracts.md、docs/04-database.md

## 1. 目标与范围

**目标**
- 实现 `core:storage`：StorageProvider 接口 + 默认/自定义实现 + 目录迁移（R4/R5）；
- 实现 `core:db`：Room 全库骨架（实体 + DAO + AppDatabase + Hilt 装配），数据库路径走 StorageProvider；
- 实现 `feature:settings`：SettingsRepository（DataStore），落地 `SearchSettings` 接口，`backgroundIndexingEnabled` **默认 false**（用户强制约束）。

**不做**
- 不实现 UI 界面（设置页 UI、笔记页 UI 在后续阶段）；
- 不实现 RAG/文件索引等业务逻辑（M4/M7）；
- 不实现数据库 Migration（version=1，后续改表再加）。

## 2. 设计要点

- **存储收口**：所有目录（db/models/knowledge/index）经 `StorageProvider` 获取，禁止散落硬编码路径；切换自定义目录只需替换实现；
- **迁移三步走**：复制（保留结构）→ 校验（逐文件比对大小）→ 切换（旧目录标记待清理，不立即删除）；
- **依赖方向**：`core:db → core:storage`，`feature:settings → core:search`，`app → 全部`；`core:search` 的 SearchSettings 接口由 `feature:settings` 通过 Hilt `@Binds` 实现（依赖倒置，core 不依赖 feature）；
- **数据库路径**：`Room.databaseBuilder(context, AppDatabase, storage.dbDir().resolve("muyunmiao.db").absolutePath)`，支持自定义目录。

## 3. 接口契约

```kotlin
// core:storage
interface StorageProvider {
    val root: File
    fun dbDir(): File; fun modelsDir(): File; fun knowledgeDir(): File; fun indexDir(): File
    fun ensureDirs()
}
class DefaultStorageProvider(context): StorageProvider      // 应用私有目录
class CustomStorageProvider(customRoot: File): StorageProvider // 用户指定目录（需权限）
object StorageMigrator { fun migrate(from, to): Int }       // 复制+校验

// core:db
@Database(entities=[Note,TodoItem,Conversation,ChatMessage,ConsentAuditEntity], version=1)
abstract class AppDatabase : RoomDatabase()
interface NoteDao         // observeActive / upsert / softDelete / observeTodos / upsertTodo
interface ConsentAuditDao // insert / recent

// feature:settings
class SettingsRepository : SearchSettings   // backgroundIndexingEnabled: StateFlow<Boolean>（默认 false）
```

已同步更新 docs/03-contracts.md（StorageProvider 从"预留"转"已定"）与 docs/04-database.md。

## 4. 关键实现

新增文件（全部逐行中文注释）：

```
core/storage/...  StorageProvider.kt / StorageProviders.kt / StorageMigrator.kt / StorageModule.kt
core/db/...       entity/Entities.kt（Note/TodoItem/Conversation/ChatMessage/ConsentAuditEntity）
                  AppDatabase.kt / dao/Daos.kt（NoteDao/ConsentAuditDao）/ DatabaseModule.kt
feature/settings/... SettingsRepository.kt / SettingsModule.kt
```

关键依赖新增：`libs.versions.toml` 加 `room-compiler`；三个模块 build.gradle 启用 hilt+kapt；app 装配全部新模块。

可复现命令：`gradle :app:assembleDebug`（在 Android Studio / 已装 Gradle 环境）。

## 5. 测试与验证

- [ ] `gradle :app:assembleDebug` 编译通过（需本机 Gradle/AS 环境，本环境无）；
- [ ] 人工审查：数据库路径确认走 `StorageProvider.dbDir()`，无硬编码；
- [ ] `backgroundIndexingEnabled` 默认值确认为 false（`?: false`）；
- [ ] 依赖方向正确（无反向依赖）。

**已知问题**：本开发环境无法编译 Android（无 SDK/Gradle），编译验证需在用户真机/AS 环境执行；CI 会兜底构建。

## 6. 接手指引

下一步（M2）：常规备忘录 + NoteBridge：
1. `feature:notes`：笔记列表/编辑/待办 UI（用 NoteDao）；
2. NoteBridge：笔记保存 → 发布变更事件 → 触发知识库增量入库（M4 接入 IngestWorker）。

踩坑点：
- Room 动态路径需在 `databaseBuilder` 传入 File 的绝对路径；建库前务必 `ensureDirs()`；
- 自定义目录（CustomStorageProvider）需 MANAGE_EXTERNAL_STORAGE 或 root/Shizuku 权限（M7 开放），M1 默认仅 DefaultStorageProvider；
- DataStore 的 StateFlow 转换需作用域，用 `CoroutineScope(SupervisorJob()+Default)` 避免泄漏。
