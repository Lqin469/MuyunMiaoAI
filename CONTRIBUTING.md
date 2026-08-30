# 贡献指南

欢迎参与 MuyunMiaoAI（沐云杪AI）开发。本仓库采用**文档驱动开发**（R12）：文档与代码同等重要，缺失文档的改动不会被合并。

> 🤖 **AI 协作者（Claude / GPT / 其他 Agent）请先阅读 [AGENTS.md](AGENTS.md)** —— 那里有面向 AI 的强制红线，其中**第一条就是「严禁把 API Key、密钥、.env 等敏感信息上传到 GitHub」**。

## 快速上手

1. 阅读 [docs/00-overview.md](docs/00-overview.md)（总纲）与 [docs/03-contracts.md](docs/03-contracts.md)（接口契约）；
2. 从「Good First Issue」或 devlog 中的「待办」开始；
3. 开发前先看 [docs/devlog/README.md](docs/devlog/README.md) 的模板与规则。

## 硬性规则

1. **每个功能模块完成时，当回合归档 `docs/devlog/M-xxx-*.md`**，否则不算完成；
2. **接口变更先写 ADR**（docs/adl/），再改代码；ADR 记录：为什么变、影响面、迁移方式；
3. 提交信息引用文档：`feat(notes): 笔记自动同步知识库 #R7 docs:M-002`；
4. 数据表变更同步更新 `docs/04-database.md`；接口签名变更同步更新 `docs/03-contracts.md`；
5. 目标状态：新成员（人或 AI）**只读 docs/ 即可无痛接手**——文档中的命令必须可直接复制运行。

## 安全红线（不可协商）

### 搜索类功能（R11）

- 所有搜索/文件索引**必须由用户显式触发**，禁止后台静默运行（默认）；
- 执行期间必须实时上报进度（`SearchProgressListener`）并在 UI 可见，支持随时停止；
- 所有搜索调用必须经 `SearchConsentGate`，禁止绕过；
- 详见 [docs/privacy-search-consent.md](docs/privacy-search-consent.md)。

### 敏感信息（推送 GitHub 前）

- **任何 AI / 开发者严禁把密钥与凭据上传到 GitHub**：API Key、Token、`.env*`、私钥/证书（`*.key/*.pem/*.jks/*.keystore` 等）、`local.properties`、日志、堆转储一律禁止提交；
- **禁止在代码、注释、文档、提交信息、PR 描述中硬编码真实密钥**；需要时用占位符（`YOUR_API_KEY`），真实值只存本机；
- 推送前必须运行 `bash scripts/check-secrets.sh`（或安装钩子：`git config core.hooksPath scripts/hooks`）；
- 若误提交，按 [docs/security-git-secrets.md](docs/security-git-secrets.md) 处理（git rm --cached + 历史重写 + **密钥轮换**）；
- 自查全流程见 [docs/security-pre-push.md](docs/security-pre-push.md)。

## 代码风格

- Kotlin + Jetpack Compose；MVVM + Clean Architecture（模块只依赖接口）；
- 模块依赖方向：`:feature:* → :core:* → 基础设施`，禁止反向；
- 所有写文件操作必须经 `StorageProvider`，禁止硬编码路径；
- **所有代码必须带逐行中文注释**，让没有编程基础的人也能看懂。

## 代码审查（新增，R12 配套）

- **每次改动必须经过三道闸**：本地自检（阶段0）→ CI（阶段2）→ 同行审查（阶段3），详见 [docs/code-review-guide.md](docs/code-review-guide.md)；
- **审查维度**：正确性 / 安全与隐私（本项目红线）/ 架构一致性（不变式）/ 可维护性 / 性能 / 测试验证，六维清单见上文文档 §2；
- **严重级别**：🔴 Blocker（一票否决，合并前必解）/ 🟡 Suggestion（应修或登记）/ 💭 Nit（可选）；
- **审查评论格式**：`级别｜维度｜文件:行号｜问题 → 为什么 → 建议`；
- **AI 协作者**：生成代码不豁免审查；作为审查者只评不改、必须引用行号、不得把接触到的密钥写入任何文档；
- 合并条件：全部 🔴 解决 + CI 全绿 + PR 模板勾选项完成 + devlog 已归档。
