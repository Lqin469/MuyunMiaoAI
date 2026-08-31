# 01 · 架构与模块边界

## 分层

```
Presentation（Compose · 单 Activity · 顶栏胶囊 + 抽屉 + NavHost 路由）
  常规备忘 │ AI 对话(流式) │ 知识库 │ 记忆 │ 待办 │ 设置(自检/权限/迁移/模型/主题/API) │ 回收站
Application 用例层（ViewModel）
Domain 接口层（纯 Kotlin：ChatEngine / EmbeddingProvider / StorageProvider /
              OcrEngine / PrivilegeManager / ToolCallingBus / SearchConsentGate）
Data 层（Room·FTS5 / DataStore / SAF / OkHttp(SSE) / ArchiveExtractor /
         MemoryExtractor / FileIndexer / ModelImporter / LanProtocol）
基础设施（MNN-LLM·JNI / Shizuku / libsu / 压缩库）
```

## 架构不变式（不可破坏）

1. **双向引擎**：所有 AI 能力只通过 `ChatEngine` / `EmbeddingProvider` 接口，本地 MNN 与云端 API 可互换；
2. **存储唯一入口**：任何写文件/建目录必须经 `StorageProvider`，禁止散落硬编码路径（R4/R5）；
3. **提权可降级**：`PrivilegeManager` 暴露能力等级 L0/L1/L2，文件检索在无权限时优雅降级，UI 明示；
4. **工具统一总线**：AI 想"做事"（搜文件/开路径）都走 `ToolCallingBus`，本地/云端共用同一套工具协议；
5. **搜索必须显式触发**（用户强制约束，ADR-001）：一切搜索/索引必须先过 `SearchConsentGate`；
6. **搜索进度必须可见**（ADR-002）：一切搜索/索引必须实时上报 `SearchProgress`。

## 模块依赖方向（禁止反向）

```
:feature:* ──► :core:* ──► 基础设施
```

| 模块 | 职责 | 依赖 |
|---|---|---|
| core:storage | 存储根目录抽象、目录规划、迁移、偏好（AppPrefs/WallpaperPrefs/NotePrefs） | — |
| core:db | Room 全库（9 表，version 4）+ DataStore 偏好 | storage |
| core:ai:engine | ChatEngine 双实现（MNN 本地 / OpenAI 兼容云端）、引擎路由/设置、运行监控、诊断 | db |
| core:ai:embed | 双轨 Embedding、向量存取、混合检索 | db |
| core:ai:tools | 工具调用总线与内置工具（search_file / tell_location） | db, search |
| core:ai:memory | 会话关键信息提炼（MemoryExtractor）与记忆库 | db, engine |
| core:ingest | 文档/图片/压缩包解析、分块、入库（KnowledgeRepository/RagService/NoteBridge） | db, embed, storage |
| core:search | 文件索引、许可闸门、进度契约、提权（PrivilegeManager）、检索服务 | db, storage |
| core:models | 模型导入/删除/硬件评估/列表、运行监控 | storage |
| core:device | 设备信息检测 + 满足度判定（M-027 新增） | — |
| core:lan | 局域网发现 + TCP 传输（NSD + 断点续传，M-027 新增） | — |
| core:ui | 共享 UI：主题（亮暗 + 17 套）、42 图标、通用组件、壁纸渲染（M8.5 新增） | — |
| feature:notes / chat / settings | 业务页（知识库 UI 在 settings 内实现；原 feature:knowledge 空壳已删除） | 对应 core |

## 已落地的核心接口（详见 03-contracts.md）

- 搜索/提权：`SearchConsentGate` / `SearchSession` / `SearchSettings` / `FileIndexer` / `SearchScope` / `SearchProgress` / `SearchService` / `PrivilegeManager`（core:search）
- 引擎：`ChatEngine` / `EngineSettings` / `EngineRouter` / `CloudConfigProvider` / `CloudApiClient` / `LocalChatEngine` / `EngineRuntimeMonitor` / `ModelLoadDiagnostics`（core:ai:engine）
- 设备：`DeviceInfoProvider` / `CapabilityChecker` / `CheckResult`（core:device）
- 局域网：`LanProtocol` / `TransferRepository` / `LanDevice` / `SendSession`（core:lan）
- 工具：`ToolCallingBus` / `AiTool` / `search_file` / `tell_location`（core:ai:tools）
- 模型：`ModelImporter` / `ModelRepository` / `LocalModelInfo`（core:models）
- 存储：`StorageProvider` / `StorageMigrator`（core:storage）
