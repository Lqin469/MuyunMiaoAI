# 上传前自查流程（推送 GitHub 前必读）

> 目的：在 `git push` 到 GitHub（公开仓库）之前，主动排查并确认**敏感信息不会泄露**。
> 本流程由 `scripts/check-secrets.sh` 自动执行核心检查；手动步骤用于兜底。

## 一键自查（推荐）

```bash
# 安装 pre-push 钩子（项目级，一次配置，之后每次 push 自动检查）
git config core.hooksPath scripts/hooks

# 或手动执行
bash scripts/check-secrets.sh
```

## 完整自查步骤清单（10 步）

1. **运行自查脚本**：`bash scripts/check-secrets.sh` —— 扫描已跟踪文件中的 `.env`/密钥扩展名，以及工作区文本中的密钥字面量（`PRIVATE KEY` / `AKIA` / `sk-` 等）；命中即报错并退出（阻断推送）。
2. **核对暂存区内容**：`git status` 与 `git diff --cached`，逐条确认本次要推送的文件没有多余/可疑文件。
3. **确认环境变量文件未被跟踪**：`git ls-files | grep -E '(^|/)\.env'` 应为空；`.env*` 已被 .gitignore 忽略，但**确认没有用 `git add -f` 强加**。
4. **确认密钥/证书未被跟踪**：`git ls-files | grep -E '\.(key|pem|crt|p12|jks|keystore|p8|asc|gpg)$'` 应为空。
5. **确认 `local.properties` 未被跟踪**：该文件含本机 Android SDK 绝对路径（个人信息），必须被忽略；`git ls-files local.properties` 应为空。
6. **确认日志/堆转储未入库**：`git ls-files | grep -E '\.(log|hprof|dump)$'` 应为空（堆转储可能含内存中的敏感数据）。
7. **确认模型/大文件未误入**：`git ls-files | xargs -r du -h | sort -rh | head -20`，查看最大文件；AI 模型（.mnn/.gguf/.bin/.onnx）一律不入库。
8. **全文扫描当前工作区**（可选但推荐）：`gitleaks detect --source .`；无 gitleaks 时用 `git grep -n -I -E '-----BEGIN [A-Z ]*PRIVATE KEY-----|AKIA[0-9A-Z]{16}|sk-[A-Za-z0-9]{20,}'` 兜底。
9. **核对最近提交**：`git log --oneline -3`，确认提交信息与内容一致，且没有任何"临时调试文件"混入。
10. **推送后复查**：打开 GitHub 仓库页面，抽查文件列表与最近提交；开启 **Secret Scanning**（Settings → Code security and analysis），让平台持续兜底。

## 误提交后的紧急处理

- 立即停止推送；若已推送，按 `docs/security-git-secrets.md` §二 执行：`git rm --cached` + 历史重写（BFG / filter-repo）+ **强制推送**；
- **同时立即轮换/吊销已泄露的密钥**（比删历史更重要）；
- 更新 `.gitignore`（本项目已覆盖，无需重复添加）。

## 钩子安装说明

```bash
# 项目级钩子目录（提交到仓库，团队共享），一条命令启用
git config core.hooksPath scripts/hooks
# 之后每次 git push 都会自动运行 scripts/hooks/pre-push → check-secrets.sh
```
