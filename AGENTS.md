# AGENTS.md — 面向 AI 协作者（含其他 AI 开发者）的强制规则

> 任何 AI（Claude / GPT / Gemini / 其他 Agent）或人类开发者在参与本仓库（MuyunMiaoAI 沐云杪AI）开发前，
> **必须**先阅读并遵守本文件。本文件是"开发过程红线"，违反即不予合并、不予接手。

---

## 一、安全红线（最高优先级，不可协商）

### 1. 严禁把任何密钥/凭据上传到 GitHub

- **禁止提交**以下任何内容：
  - API Key / Token / 访问密钥（云厂商、AI 服务商等一切密钥字面量）；
  - 环境变量文件 `.env`、`.env.local`、`.env.production` 等全部变体；
  - 私钥与证书：`*.key`、`*.pem`、`*.crt`、`*.cer`、`*.p12`、`*.pfx`、`*.jks`、`*.keystore`、`*.p8`、`*.asc`、`*.gpg`、`id_rsa` 等；
  - `local.properties`（含本机 SDK 绝对路径）、日志（`*.log`）、堆转储（`*.hprof`）、数据库文件（`*.db/*.sqlite`）。
- **禁止在代码、注释、文档、提交信息、PR 描述中硬编码真实密钥/Token**；需要密钥时一律用占位符（如 `YOUR_API_KEY` / `sk-xxxx` 打码），真实值只存本机、绝不落库。
- **提交前必须运行自查**：`bash scripts/check-secrets.sh`（建议安装 pre-push 钩子：`git config core.hooksPath scripts/hooks`，每次 push 自动拦截）。
- **一旦误提交**：立即按 `docs/security-git-secrets.md` 处理——`git rm --cached` + 历史重写（`git filter-repo`/BFG）+ **立即轮换/吊销泄露的密钥**（删除历史 ≠ 收回泄露）。
- **给自己/协作 AI 写任务提示词时**，必须显式声明："严禁将 API Key、密钥、令牌、.env 等敏感信息提交到 GitHub"。

### 2. 搜索类功能红线（R11）

- 所有搜索/文件索引**必须由用户显式触发**，禁止后台静默运行；
- 执行期间必须实时上报进度（`SearchProgressListener`）并在 UI 可见、可停止；
- 一切搜索调用必须经 `SearchConsentGate`，禁止绕过。详见 `docs/privacy-search-consent.md`。

---

## 二、开发过程规则（R12 文档驱动）

1. 每个功能模块完成时，**当回合**归档 `docs/devlog/M-xxx-*.md`，否则不算完成；
2. **接口变更先写 ADR**（docs/adl/），再改代码；
3. 提交信息引用文档：`feat(notes): 说明 #R7 docs:M-002`；
4. 数据表变更同步 `docs/04-database.md`；接口签名变更同步 `docs/03-contracts.md`；
5. 目标：**新成员只读 docs/ 即可无痛接手**——文档里的命令必须可直接复制运行。

---

## 三、代码风格（强制）

- Kotlin + Jetpack Compose；MVVM + Clean Architecture（模块只依赖接口）；
- 模块依赖方向 `:feature:* → :core:* → 基础设施`，禁止反向；
- 所有写文件操作必须经 `StorageProvider`，禁止硬编码路径；
- **所有代码逐行中文注释**，让没有编程基础的人也能看懂。

---

## 四、入口文档索引

- 总纲：`docs/00-overview.md` ｜ 架构：`docs/01-architecture.md` ｜ 契约：`docs/03-contracts.md`
- 安全：`docs/security-git-secrets.md`、`docs/security-pre-push.md`、`docs/privacy-search-consent.md`
- 开发记录模板：`docs/devlog/README.md` ｜ 整合执行提示词：`PROMPT.md`
