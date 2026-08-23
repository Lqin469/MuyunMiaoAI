# 项目初始化与安全规范 — 整合执行提示词

> 本文件将全部需求整合为一份**结构清晰、步骤明确、指令具体、逻辑完整**的提示词。
> 开发 AI（或开发者）拿到本文件后，应能**一次性完整执行**以下全部内容。
> 本仓库已按本提示词完成执行，历史可查：docs/devlog/M-001-security-gitignore.md。

---

## 任务目标

在现有 Android 多模块工程（MuyunMiaoAI / 沐云杪AI）中完成：**安全基线（.gitignore + 敏感信息防护）+ GitHub 仓库创建 + 全代码中文注释**，确保推送公开仓库前不泄露任何敏感信息，且无编程基础的人也能读懂代码。

## 一、创建完整的 `.gitignore` 文件

在仓库根目录创建/重写 `.gitignore`，必须满足：

1. **覆盖全面**，至少包含以下类别：
   - `.env` 环境文件及其变体（`.env.local`、`.env.production` 等）；
   - 各类配置文件（`config/secret`、`config/secrets/`、`credentials/`、`secrets/`、`*.properties`、`application.yml` 等）；
   - 密钥目录与文件；
   - 构建产物（`build/`、`.gradle/`、`.kotlin/`、`.cxx/`、`*.apk`、`*.aab`、`*.hprof`）；
   - 日志文件（`*.log`、`logs/`）；
   - 数据库与快照（`*.db`、`*.sqlite` 等）；
   - AI 模型文件（`models/*.mnn`、`*.gguf`、`*.bin`、`*.onnx` 等，防止大模型误提交）；
   - IDE 与系统文件（`.idea/`、`.DS_Store`、`Thumbs.db`、`*.iml`、`*.swp`）。
2. **明确列出密钥文件模式**：`*.key`、`*.pem`、`*.crt`、`*.cer`、`*.p12`、`*.pfx`、`*.jks`、`*.keystore`、`*.p8`、`*.asc`、`*.gpg`、`id_rsa`、`id_ed25519`、`*.ppk` 等；
3. **环境变量与配置变体**：`.env`、`.env.*`（保留 `!.env.example` 例外）、`config/secret` 等；
4. **每个忽略项必须有简短中文注释**，说明用途，便于维护；
5. **必须保留必要例外**：`!gradle.properties`、`!gradle-wrapper.properties`（否则会误伤本工程自己的构建配置）。

## 二、增加安全提示章节（文档）

新建 `docs/security-git-secrets.md`，包含：

1. **如何检查是否已误提交密钥**：
   - `git ls-files | grep -E '\.(key|pem|crt|p12|jks|keystore|env)$'`（已跟踪文件）；
   - `git log --all --oneline -- <file>`（某文件是否进过历史）；
   - `git grep -n -I -E '-----BEGIN [A-Z ]*PRIVATE KEY-----' $(git rev-list --all)`（全文扫描历史）；
   - 工具：`gitleaks`（自动化扫描）、GitHub 的 Secret Scanning（仓库 → Settings → Code security）。
2. **如何从 Git 历史彻底移除敏感文件**：
   - 停止跟踪但保留本地：`git rm --cached <file>`，加入 `.gitignore` 后提交；
   - 重写历史：`git filter-branch`（注意仓库大小、需 `--force` 推送）；
   - 推荐现代化工具：**BFG Repo-Cleaner**（`bfg --delete-files <file>`）与 **git-filter-repo**（`git filter-repo --invert-paths --path <file>`）；
   - 历史重写后必须 `git push --force --all`（或强制推送各分支/tag）；
   - **最重要的提示：已泄露的密钥必须立即轮换/吊销，删除历史不等于收回泄露**。
3. 注明：本项目 `.gitignore` 已覆盖上述全部模式，见仓库根目录。

## 三、强调上传前的自查流程（文档 + 脚本）

1. 新建 `docs/security-pre-push.md`：**推送 GitHub 前的自查步骤清单**（不少于 10 步），包括：
   - 运行敏感信息自查脚本；
   - `git status` / `git diff --cached` 人工核对暂存内容；
   - 检查 `.env*`、密钥文件是否出现在工作区；
   - 检查 `local.properties`（含本机 SDK 路径）是否被跟踪；
   - 检查日志/堆转储（`*.log`、`*.hprof`）是否入库；
   - 检查模型/大文件是否误入（`git ls-files | xargs du -h` 找超大文件）；
   - 使用 `gitleaks detect`（可选）；
   - 最终确认：`git log --oneline -3` + 远程仓库预览页核对。
2. 新建 `scripts/check-secrets.sh`（可执行）：
   - 扫描已跟踪文件中的敏感模式（.env/密钥扩展名）；
   - 扫描工作区文本中的密钥字面量（`PRIVATE KEY`、`AKIA`、`sk-` 等）；
   - 命中即非零退出，阻断提交/推送。
3. 新建 `scripts/hooks/pre-push`：git pre-push 钩子，推送前自动运行上述脚本；
4. 提供钩子安装说明：`git config core.hooksPath scripts/hooks`（项目级，免手动复制）。

## 四、创建 GitHub 仓库

1. 仓库名：**`MuyunMiaoAI`**
2. 描述（一字不改）：**`沐云杪AI｜本地端侧AI备忘录，采用 MNN‑LLM 本地大模型与 RAG 知识库，面向安卓客户端。`**
3. 将本地工程（目录已重命名为 `MuyunMiaoAI`，`rootProject.name = "MuyunMiaoAI"`）初始化为 git 仓库并提交全部文件；
4. **创建命令**（本环境未安装 gh CLI，二者选一）：
   ```bash
   # 方式 A：GitHub CLI（需先 gh auth login）
   gh repo create MuyunMiaoAI --public \
     --description "沐云杪AI｜本地端侧AI备忘录，采用 MNN‑LLM 本地大模型与 RAG 知识库，面向安卓客户端。" \
     --source . --push

   # 方式 B：网页手动创建（github.com/new 填同名与同描述），然后：
   git remote add origin https://github.com/<你的用户名>/MuyunMiaoAI.git
   git branch -M main
   git push -u origin main
   ```
5. **解释 gradle wrapper jar 无法在当前环境生成二进制的问题**（写入 README「常见问题」）：
   - 原因：`gradle-wrapper.jar` 是二进制文件，需要本机已安装 Gradle 或由 IDE 生成；当前构建环境未安装 Gradle，无法生成该二进制，故未提交；
   - 解决方案：首次用 **Android Studio 打开工程并同步**（AS 会自动生成 wrapper），或在已装 Gradle 的机器执行 `gradle wrapper --gradle-version 8.13`；
   - CI 已通过 `gradle/actions/setup-gradle` 指定版本规避，不影响自动化构建。

## 五、代码注释要求

1. 对**每个 Kotlin 源文件、Gradle 脚本（.kts）、版本目录（libs.versions.toml）、XML（Manifest/资源）、属性文件（gradle.properties、gradle-wrapper.properties）**的每一行添加中文注释；
2. 注释需说明该行/该段的作用与原因，**使没有编程基础的人也能看懂**（import 行、配置键、关键逻辑均需注释）；
3. 注释风格：行内 `//` 或上方行注释，中文；
4. 完成后自查：随机抽查 3 个文件，确认不存在无注释的逻辑行。

## 六、验收标准（全部满足才算完成）

- [ ] `.gitignore` 覆盖 §一 全部类别，每项有中文注释，且未误伤 `gradle.properties` / `gradle-wrapper.properties`；
- [ ] `docs/security-git-secrets.md` 与 `docs/security-pre-push.md` 存在且内容完整；
- [ ] `scripts/check-secrets.sh` 可执行，对本仓库运行返回 OK；
- [ ] `scripts/hooks/pre-push` 已就位，README 写明 `git config core.hooksPath scripts/hooks`；
- [ ] GitHub 仓库 `MuyunMiaoAI` 已创建（或已给出可一键执行的创建命令）；
- [ ] README 包含：仓库名/描述、gradle wrapper 问题说明、安全文档链接、钩子安装说明；
- [ ] 全部代码文件逐行中文注释，无逻辑行遗漏；
- [ ] 按 R12 规范归档 `docs/devlog/M-001-security-gitignore.md` 开发记录。
