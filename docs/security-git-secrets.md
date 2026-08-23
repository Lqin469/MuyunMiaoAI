# 安全提示：检查并清除误提交的密钥文件

> 本文件指导：①检查仓库是否已存在被误提交的密钥；②从 Git 历史中彻底移除。
> 配套：仓库根 `.gitignore`（已覆盖全部密钥/环境变量模式）｜推送前自查：`docs/security-pre-push.md`。

## 一、如何检查是否已误提交密钥

### 1.1 检查「已跟踪」的文件里有没有敏感文件

```bash
# 列出所有被 git 跟踪的文件中，命中的敏感扩展名（.env / 密钥 / 证书）
git ls-files | grep -E '\.(env|key|pem|crt|cer|p12|pfx|jks|keystore|p8|asc|gpg)$|(^|/)\.env'
```

### 1.2 检查某个文件是否进过提交历史

```bash
# 查看指定文件在历史中的所有提交记录（确认它是否曾被提交过）
git log --all --oneline -- path/to/secret.file
```

### 1.3 全文扫描「整个提交历史」中的密钥字面量

```bash
# 在全部历史提交里搜索 PEM 私钥块（效率低但最彻底）
git grep -n -I -E '-----BEGIN [A-Z ]*PRIVATE KEY-----' $(git rev-list --all)

# 在全部历史提交里搜索常见云厂商密钥前缀（AWS AKIA、OpenAI sk- 等）
git grep -n -I -E 'AKIA[0-9A-Z]{16}|sk-[A-Za-z0-9]{20,}' $(git rev-list --all)
```

### 1.4 使用自动化工具（推荐）

```bash
# gitleaks：业界标准密钥扫描工具（支持 CI 集成）
gitleaks detect --source . --log-opts="--all"

# GitHub 自带 Secret Scanning：仓库 → Settings → Code security and analysis → Secret scanning
# 命中后 GitHub 会发邮件告警，即使历史已被清理也可能仍在告警列表（说明确实泄露过）
```

## 二、如何从 Git 中彻底移除敏感文件

> ⚠️ **最重要的提示**：只要密钥曾进入公开仓库（哪怕后来删了），就必须**立即轮换/吊销该密钥**。
> 攻击者可能早已抓取历史。删除历史 ≠ 收回泄露。

### 2.1 第一步：停止跟踪（保留本地文件）

```bash
git rm --cached .env                  # 从版本库移除跟踪，但本地文件保留
git rm --cached local.properties      # 同理处理其他敏感文件
# 然后把对应规则加入 .gitignore（本项目已覆盖全部常见模式）
git add .gitignore
git commit -m "chore(security): 停止跟踪敏感文件并加入 .gitignore #SEC"
git push                              # 推送后，新提交不再包含该文件
```

### 2.2 第二步：清理提交历史（三选一）

```bash
# 方式 A：git filter-branch（内置，但慢、且官方已建议改用 filter-repo）
git filter-branch --force --index-filter \
  "git rm --cached --ignore-unmatch .env" \
  --prune-empty --tag-name-filter cat -- --all

# 方式 B：BFG Repo-Cleaner（快，需 java）
#   1) 下载 bfg.jar；2) 把要删的文件写进 delete-files 参数
java -jar bfg.jar --delete-files .env
git reflog expire --expire=now --all && git gc --prune=now --aggressive

# 方式 C：git-filter-repo（官方推荐，最干净）
git filter-repo --invert-paths --path .env --path local.properties
```

### 2.3 第三步：强制推送与收尾

```bash
# 历史被重写，必须强制推送所有分支与标签（通知协作者重新 clone）
git push --force --all
git push --force --tags
# 本地清理悬空对象
git reflog expire --expire=now --all && git gc --prune=now --aggressive
# 到 GitHub → Settings → 将旧提交标记的密钥从"已泄露"改为"已修复/已轮换"（如适用）
```

### 2.4 后续防护

1. 提交前运行 `scripts/check-secrets.sh`（或安装 pre-push 钩子，见 security-pre-push.md）；
2. 配置 GitHub 规则：禁止把 `.env`、`*.pem` 等加入仓库（`.gitignore` + 团队约定双保险）；
3. CI 中加入 `gitleaks` 扫描（任何包含密钥的 PR 自动失败）。

## 三、本项目现状

- 仓库根 `.gitignore` 已覆盖：环境变量（.env 全变体）、密钥/证书（.key/.pem/.crt/.p12/.jks/.keystore 等）、构建产物、日志、数据库、模型文件、`local.properties`；
- 提交前自查脚本 `scripts/check-secrets.sh` 已就位；
- 首次提交前执行 `git ls-files` 人工核对，确保无敏感文件入库。
