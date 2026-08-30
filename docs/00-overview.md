# 00 · 项目总纲与需求索引

> 本文是仓库文档入口。完整的架构、选型、契约、数据表、模型清单见各分册（索引见 [README.md](README.md)）。
> 上游规划文档（仓库外）：`AI备忘录App实现方案.md`（v1）、`AI备忘录-开发规划与架构设计-v2.md`（v2 总纲，R1-R12 定义 + M0-M8 计划）。

## 需求编号索引（状态 = 代码实际，2026-08-31 核对）

| 编号 | 需求 | 状态 | 落地模块 | 关键实现 |
|---|---|---|---|---|
| R1 | 云端 API 用户自配（OpenAI 兼容，不内置服务商） | ✅ | core:ai:engine + feature:settings | `CloudConfigRepository`（apiKey 加密）+ `ApiManageScreen`（多 API + 智能输入）+ `CloudApiClient`（SSE + 指数退避） |
| R2 | 模型下载 + 硬件评估 | ✅ | core:models | `ModelRepository.probeHardware()` 真实内存/存储检测；`ModelManageScreen.check()` 红黄绿档位 |
| R3 | 本地模型导入（MNN / GGUF） | ✅ | core:models | `ModelImporter`（SAF 目录检测 + 复制 + 完整性校验）；GGUF 预览支持（运行待 M-028） |
| R4 | 单一数据库目录 | ✅ | core:storage + core:db | `StorageProvider` + Room 走 `dbDir()/muyunmiao.db` |
| R5 | 自定义存储目录 | ⏳ 部分 | core:storage | `CustomStorageProvider`/`StorageMigrator` 已建**未接线**（StorageModule 仅绑 Default） |
| R6 | 会话自动记忆 | ✅ | core:ai:memory | `MemoryExtractor` + `MemoryStore`；ChatViewModel 每 4 轮提炼；记忆页可查/删 |
| R7 | 笔记自动进知识库 | ✅ | core:ingest + feature:notes | `NoteBridge` 事件桥 + `KnowledgeRepository` 订阅；自动入库开关（默认开） |
| R8 | 文件夹/文件/图片投喂 | ✅ | core:ingest | SAF 投喂 + `DocumentParser`（TXT/MD/PDF/DOCX）+ OCR（桩，待模型） |
| R9 | 压缩包 + 常见格式 | ✅ | core:ingest | `ArchiveExtractor`（ZIP/TAR/7z + junrar RAR4；zip-slip/炸弹防护） |
| R10 | 不可解析文件记录位置 | ✅ | core:db | `FileLocation` 表 + `tell_location` 工具 |
| R11 | 文件检索提权（Shizuku/adb/root） | ✅ | core:search + core:ai:tools | `PrivilegeManager` 三档 + `FileIndexerImpl` + `search_file` 工具；UI 页 M8.6 移除，工具保留 |
| R12 | 文档驱动开发 | ✅ 进行中 | docs/ | 本文档体系 + devlog 强制归档 + code-review-guide |

## 隐私承诺（用户强制约束）

> **搜索类功能必须由用户显式触发后才执行，绝不未经许可在后台偷偷运行；执行时必须提供实时进度条。**
> 已通过 `SearchConsentGate`（core:search）落地为代码强制，ADR-001/ADR-002 固化，任何 PR 不得绕过。
> 全文见 [privacy-search-consent.md](privacy-search-consent.md)。

## 安全红线（AI 协作者必读）

> 🤖 任何 AI / 开发者**严禁把 API Key、密钥、Token、`.env*`、证书等敏感信息上传到 GitHub**；禁止在代码/注释/文档/提交信息中硬编码真实密钥。规则与处置见 [AGENTS.md](../AGENTS.md)、[security-git-secrets.md](security-git-secrets.md)、[security-pre-push.md](security-pre-push.md)。提交前必须运行 `bash scripts/check-secrets.sh`。

## 许可证

- 仓库主许可证：**GPL-3.0**（LICENSE，因引用 Operit 修改版 GPLv3 代码/方案）；
- 自研源码文件头可附加 Apache-2.0 双注明；
- 致谢：alibaba/MNN（Apache-2.0）、AAswordman/Operit（修改版 GPLv3，README 已标注原始地址）。

## 路线图（M0-M8 + 里程碑）

| 阶段 | 内容 | 状态 |
|---|---|---|
| M0 | 基建：仓库/CI/docs 骨架/许可证/多模块/搜索契约 | ✅ 完成（devlog/M-000） |
| M1 | 存储与数据：StorageProvider + Room 全库 + SearchSettings | ✅ 完成 |
| M2 | 常规备忘录 + NoteBridge | ✅ 完成 |
| M3 | 云端对话（用户自配 API + SSE 流式） | ✅ 完成 |
| M4 | 知识库核心（分块/解析/混合检索/压缩包/位置记录） | ✅ 完成 |
| M5 | 会话记忆提炼（MemoryExtractor + 记忆库） | ✅ 完成 |
| M6 | 本地引擎（MNN-LLM 源码构建 + JNI 桥 + 模型管理） | ✅ 完成 |
| M7 | 文件检索提权（Shizuku 三档 + FileIndexerImpl + search_file 工具） | ✅ 完成 |
| M8 | 打磨发布（隐私自检 security-audit-m8 + Release v0.1~v0.2） | ✅ 基本完成 |
| M8.5 | HTML 界面原型 → Compose 原生迁移（:core:ui + 15 页面 + 首启自检门） | ✅ 完成（未提交） |
| M8.6 | 旧版 UI 彻底清除（filesearch 模块等删除，入口迁移） | ✅ 完成（未提交） |
| M8.7~M8.10 | 体验优化 / 黑白主题 / 主题系统 / 六大核心功能真实现 | ✅ 完成（未提交） |
| M-014~M-036 | 模型适配/诊断/修复/签名等里程碑 | ✅ 完成（未提交） |
| M-037 | 代码审查机制建立 | ✅ 完成（未提交） |

> 远程 main 停在 `2898825`（累计 45+ 提交，CI 全绿）；工作区含 M8.5 至今全部未提交改动，待评审后分批提交。
> Release 已发布：v0.1.0 / v0.2.0 / v0.2.2 / v0.2.3；交付目录最新 APK：`沐云杪AI-v0.4.1-正式签名.apk`。

## 文档索引

| 想找什么 | 去哪看 |
|---|---|
| 从哪开始读 | [docs/README.md](README.md) |
| 需求/状态/路线图 | 本文（00） |
| 架构分层与不变式 | [01-architecture.md](01-architecture.md) |
| 技术选型与否决记录 | [02-technical-choices.md](02-technical-choices.md) |
| 接口契约（单一事实源） | [03-contracts.md](03-contracts.md) |
| 数据表 / DataStore 键 | [04-database.md](04-database.md) |
| 模型清单与硬件评估 | [05-model-hardware.md](05-model-hardware.md) |
| 架构决策记录 | [adl/](adl/)（ADR-001/002） |
| 模块开发记录 | [devlog/](devlog/) |
| 搜索隐私红线 | [privacy-search-consent.md](privacy-search-consent.md) |
| 代码审查标准与流程 | [code-review-guide.md](code-review-guide.md) |
| 安全审计/密钥/推送前自查 | [security-audit-m8.md](security-audit-m8.md) 等 security-* |
| UI 迁移说明 | [ui-migration-guide.md](ui-migration-guide.md) |
| 核心功能实现方案 | [core-features-implementation-plan.md](core-features-implementation-plan.md) |
| 开发总流程日志（仓库外） | `D:\LQYMYH\ai备忘录\开发流程\总流程日志.md`（唯一总日志） |
