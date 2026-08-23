# 00 · 项目总纲与需求索引

> 本文是仓库文档入口。完整的架构决策、接口契约、数据表、模型清单见各分册。
> 上游规划文档（本仓库之外的演进过程）：`AI备忘录App实现方案.md`（v1）、`AI备忘录-开发规划与架构设计-v2.md`（v2 总纲，含 R1-R12 需求定义、M0-M8 计划、模块边界表）。

## 需求编号索引

| 编号 | 需求 | 状态 | 落地模块 | 关联文档 |
|---|---|---|---|---|
| R1 | 云端 API 用户自配（OpenAI 兼容） | 规划 | core:ai:engine | 03-contracts |
| R2 | 模型下载 + 硬件评估 | 规划 | core:models | 05-model-hardware |
| R3 | 本地模型导入 | 规划 | core:models | 05-model-hardware |
| R4 | 单一数据库目录 | 规划 | core:storage | 03-contracts |
| R5 | 自定义存储目录 | 规划 | core:storage | 03-contracts |
| R6 | 会话自动记忆 | 规划 | core:ai:memory | 03-contracts |
| R7 | 笔记自动进知识库 | 规划 | core:ingest | 04-database |
| R8 | 文件夹/文件/图片投喂 | 规划 | core:ingest | 04-database |
| R9 | 压缩包 + 常见格式 | 规划 | core:ingest | 03-contracts |
| R10 | 不可解析文件记录位置 | 规划 | core:search | 04-database |
| R11 | 文件检索（Shizuku/adb/root 提权） | **M0 已定契约** | core:search | privacy-search-consent, ADR-001/002 |
| R12 | 文档驱动开发 | **进行中** | docs/ | devlog/README |

## 隐私承诺（用户强制约束）

> **搜索类功能必须由用户显式触发后才执行，绝不未经许可在后台偷偷运行；执行时必须提供实时进度条。**
> 该约束已通过 `SearchConsentGate`（core:search）落地为代码强制，并通过 ADR-001/ADR-002 固化，任何 PR 不得绕过。
> 全文见 [privacy-search-consent.md](privacy-search-consent.md)。

## 安全红线（AI 协作者必读）

> 🤖 任何 AI / 开发者**严禁把 API Key、密钥、Token、`.env*`、证书等敏感信息上传到 GitHub**；禁止在代码/注释/文档/提交信息中硬编码真实密钥。规则与处置见 [AGENTS.md](../AGENTS.md)、[security-git-secrets.md](security-git-secrets.md)、[security-pre-push.md](security-pre-push.md)。提交前必须运行 `bash scripts/check-secrets.sh`。

## 许可证

- 仓库主许可证：**GPL-3.0**（LICENSE，因引用 Operit 修改版 GPLv3 代码/方案）；
- 自研源码文件头可附加 Apache-2.0 双注明；
- 致谢：alibaba/MNN（Apache-2.0）、AAswordman/Operit（修改版 GPLv3，README 已标注原始地址）。

## 路线图（M0-M8）

| 阶段 | 目标 | 状态 |
|---|---|---|
| M0 | 基建：仓库/CI/docs 骨架/许可证/多模块 | ✅ 已完成（devlog/M-000） |
| M1 | 存储与数据：StorageProvider + 自定义目录 + Room 全库 | ⏳ 下一个 |
| M2 | 常规备忘录 + NoteBridge | 待排期 |
| M3 | 云端对话（用户自配 API + SSE） | 待排期 |
| M4 | 知识库投喂（压缩包/位置索引/混合检索） | 待排期 |
| M5 | 会话记忆提炼 | 待排期 |
| M6 | 本地引擎（模型管理/硬件评估/导入 + MNN） | 待排期 |
| M7 | 文件检索提权（Shizuku 三档 + search_file） | 待排期 |
| M8 | 打磨发布 | 待排期 |
