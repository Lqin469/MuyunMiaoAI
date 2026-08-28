# 01 · 架构与模块边界

## 分层

```
Presentation（Compose · 单 Activity · 抽屉双模式）
  常规备忘录 │ AI 对话(流式MD) │ 知识库 │ 模型管理 │ 文件检索 │ 设置
Application 用例层（UseCase / ViewModel）
Domain 接口层（纯 Kotlin：ChatEngine / EmbeddingProvider / StorageProvider /
              OcrEngine / PrivilegeManager / ToolCallingBus / SearchConsentGate）
Data 层（Room·FTS5 / DataStore / SAF·绝对路径 / WorkManager / OkHttp(SSE·下载) /
         ArchiveExtractor / MemoryExtractor / FileIndexer / ModelImporter）
基础设施（MNN-LLM·JNI / MNN-PaddleOCR / Shizuku / libsu / 压缩库）
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
| core:storage | 存储根目录抽象、目录规划、迁移 | — |
| core:db | Room 全库 + FTS5 + consent_audit | storage |
| core:ai:engine | ChatEngine 双实现（MNN 本地 / OpenAI 兼容云端）、SSE | db |
| core:ai:embed | 双轨 Embedding、向量存取、混合检索 | db |
| core:ai:tools | 工具调用总线与内置工具（search_file/开路径/读文件/查记忆） | db, search |
| core:ai:memory | 会话关键信息提炼与记忆库 | db, engine |
| core:ingest | 文档/图片/压缩包解析、分块、增量入库 | db, embed, storage |
| core:search | 文件索引、许可闸门、进度契约、检索服务 | db, storage |
| core:models | 模型目录、下载/校验/删除、本地导入、硬件评估 | storage |
| feature:notes / chat / knowledge / settings | 各业务页 | 对应 core |

## M0 已落地的接口（详见 03-contracts.md）

- `SearchConsentGate` / `SearchSession` / `SearchTrigger` / `SearchSettings`（core:search）
- `FileIndexer` / `SearchScope` / `IndexResult` / `UnauthorizedSearchException`（core:search）
- `SearchProgress` / `SearchPhase` / `SearchProgressListener`（core:search）
- `SearchService` / `FileQuery` / `FileHit`（core:search）
- `ConsentAuditEntry`（core:search，落库在 M1）
