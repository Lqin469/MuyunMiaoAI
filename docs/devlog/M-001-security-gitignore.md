# 开发记录：安全基线 — .gitignore / 密钥防护 / 推送自查 / 全代码注释

- 日期：2026-08-24
- 涉及模块：全仓库（安全基线）
- 关联需求：R12（文档驱动）、R11（搜索隐私）
- 关联文档：docs/security-git-secrets.md、docs/security-pre-push.md、PROMPT.md
- 交付物提示词：PROMPT.md（整合执行提示词，本记录即其执行结果）

## 1. 目标与范围

**目标**
- 建立完整 `.gitignore`（覆盖敏感文件/密钥/构建产物，每项中文注释）；
- 提供"检查误提交密钥 + 历史清理"的安全文档；
- 提供"推送前自查"清单与自动化脚本/钩子；
- 完成 GitHub 仓库 `MuyunMiaoAI` 的创建指引（本环境无 gh CLI）；
- 全部代码逐行中文注释。

**不做**
- 不创建真实密钥/凭据（项目本身无任何密钥文件）；
- 不实际执行 GitHub 推送（需用户授权 gh 或手动创建仓库后执行，命令已备好）。

## 2. 设计要点

- `.gitignore` 按十大类组织（系统/IDE、环境变量、密钥证书、构建产物、本地环境、日志、数据库、模型、测试、其他），**每条规则带中文注释**，并保留 `!gradle.properties` / `!gradle-wrapper.properties` 例外避免误伤；
- 密钥防护文档区分"检查"（git ls-files/git grep/gitleaks）与"清除"（git rm --cached → filter-branch/BFG/filter-repo → 强制推送），**强调密钥轮换优先于删历史**；
- 自查流程文档化 10 步清单 + 可执行脚本 `check-secrets.sh` + git pre-push 钩子（`git config core.hooksPath scripts/hooks` 一键启用）；
- 全代码注释：Kotlin/Gradle/TOML/XML/属性文件逐行中文注释（含 import、配置键、逻辑），面向零基础读者。

## 3. 接口契约

无接口变更（纯工程安全基建）。新增文件：

```
PROMPT.md                              # 整合执行提示词（需求→任务→验收）
.gitignore                             # 全注释敏感文件忽略清单
docs/security-git-secrets.md           # 密钥检查与清除指南
docs/security-pre-push.md              # 推送前自查流程（10 步）
scripts/check-secrets.sh               # 敏感信息自查脚本（可执行）
scripts/hooks/pre-push                 # git pre-push 钩子（自动调用自查脚本）
docs/devlog/M-001-security-gitignore.md # 本文件
```

## 4. 关键实现

```bash
# 自查脚本（核心逻辑）：扫描已跟踪文件的敏感扩展名 + 工作区密钥字面量
git ls-files | grep -E '\.(env|key|pem|crt|p12|jks|keystore|p8|asc|gpg)$|(^|/)\.env'
grep -rInE '-----BEGIN [A-Z ]*PRIVATE KEY-----|AKIA[0-9A-Z]{16}|sk-[A-Za-z0-9]{20,}' . \
  --exclude-dir=.git --exclude-dir=build

# 安装推送前钩子（一条命令，团队共享）
git config core.hooksPath scripts/hooks
```

## 5. 测试与验证

- [x] `bash scripts/check-secrets.sh` 对本仓库运行 → 输出 OK（无敏感文件、无密钥字面量）；
- [ ] 首次 commit 前已确认 `git ls-files` 无敏感文件（见 §6 遗留）；
- [ ] `gradle-wrapper.jar` 未提交（无法本环境生成），README「常见问题」已说明两种解决方案。

## 6. 接手指引

下一步：
1. **创建 GitHub 仓库**（本环境无 gh CLI，二选一）：
   ```bash
   # A：GitHub CLI（先 gh auth login）
   gh repo create MuyunMiaoAI --public \
     --description "沐云杪AI｜本地端侧AI备忘录，采用 MNN‑LLM 本地大模型与 RAG 知识库，面向安卓客户端。" \
     --source . --push
   # B：网页 new 仓库后
   git remote add origin https://github.com/<用户名>/MuyunMiaoAI.git && git branch -M main && git push -u origin main
   ```
2. 执行首次提交：`git add . && git commit -m "chore(m0): 项目骨架与安全基线 #R12"`（提交前先跑自查脚本）；
3. 安装推送钩子：`git config core.hooksPath scripts/hooks`；
4. 继续 M1（core:storage / core:db / feature:settings）。

踩坑点：
- `.gitignore` 的 `*.properties` 会误伤 `gradle.properties`，已用 `!` 例外解决（勿删）；
- 历史重写（filter-repo/BFG）会改变所有提交 hash，需强制推送并通知协作者重新 clone；
- 密钥一旦泄露过，删除历史**不等于**收回，必须轮换。
