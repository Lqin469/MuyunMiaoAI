#!/usr/bin/env bash
# ============================================================
# check-secrets.sh — 敏感信息自查脚本（推送前运行）
# 用法：bash scripts/check-secrets.sh
# 作用：扫描"已跟踪文件"中的敏感扩展名，以及"工作区文本"中的密钥字面量；
#       命中任一 → 打印详情并以非零退出（可阻断 git push）。
# ============================================================

# 开启严格模式：命令失败/变量未定义/管道失败都会立即退出
set -euo pipefail

# 项目根目录（本脚本所在目录的上一级）
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

# 标记最终是否发现敏感信息（默认 0=安全）
FAILED=0

echo "==> [1/2] 检查已跟踪文件中的敏感扩展名（.env / 密钥 / 证书）..."

# 列出 git 跟踪的文件，过滤出敏感扩展名或 .env 变体；|| true 防止 grep 无匹配时中断
SENSITIVE_FILES=$(git ls-files | grep -E '\.(env|key|pem|crt|cer|p12|pfx|jks|keystore|p8|asc|gpg)$|(^|/)\.env' || true)

if [ -n "$SENSITIVE_FILES" ]; then
  # 命中：打印文件列表并置 FAILED=1
  echo "!!! 发现已跟踪的敏感文件（必须 git rm --cached 后按 security-git-secrets.md 处理）："
  echo "$SENSITIVE_FILES"
  FAILED=1
else
  # 未命中：提示通过
  echo "    OK：没有已跟踪的敏感文件。"
fi

echo "==> [2/2] 扫描工作区文本中的密钥字面量（PRIVATE KEY / AKIA / sk- ...）..."

# 在指定代码/配置文本文件中搜索密钥字面量；跳过 .git、build 目录
HITS=$(grep -rInE --include='*.{java,kt,kts,properties,toml,xml,yml,yaml,json,md,sh}' \
  '-----BEGIN [A-Z ]*PRIVATE KEY-----|AKIA[0-9A-Z]{16}|sk-[A-Za-z0-9]{20,}|AIza[0-9A-Za-z_-]{35}' \
  . \
  --exclude-dir=.git --exclude-dir=build --exclude-dir=.gradle 2>/dev/null || true)

if [ -n "$HITS" ]; then
  # 命中：打印位置并置 FAILED=1
  echo "!!! 发现疑似密钥字面量（请核实，若为真密钥立即轮换）："
  echo "$HITS"
  FAILED=1
else
  # 未命中：提示通过
  echo "    OK：未发现密钥字面量。"
fi

echo ""
if [ "$FAILED" -eq 0 ]; then
  # 全部通过：提示安全
  echo "==> 结论：未发现敏感信息，可以推送。"
else
  # 发现敏感信息：给出处理指引并以退出码 1 结束（阻断 push）
  echo "==> 结论：发现敏感信息！禁止推送。"
  echo "    处理指引：docs/security-git-secrets.md（git rm --cached + 历史清理 + 密钥轮换）"
  exit 1
fi
