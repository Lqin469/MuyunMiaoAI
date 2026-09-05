#!/usr/bin/env bash
# ============================================================
# auto-commit-push.sh — 沐云杪AI git 自动提交推送脚本
# 作用：检测变更 → 自动提交 → 自动推送，全程无需手动同意
# 用法：bash scripts/auto-commit-push.sh
# 配合 Windows 计划任务定时运行，实现完全自动上传。
# 硬约束：只做普通 commit + push，禁止 force push / amend / rebase -i，
#         保持仓库结构与提交历史不变。
# ============================================================
set -uo pipefail

# 定位仓库根目录（脚本所在目录的上一级）
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT" || { echo "[$(date '+%F %T')] ✗ 无法进入仓库：$ROOT"; exit 1; }

# 日志文件（每次运行追加，便于排查）
LOG="$ROOT/scripts/auto-commit-push.log"
log() { echo "[$(date '+%F %T')] $*" >> "$LOG"; }

# 推送命令：Windows 默认 schannel 后端会报 SSL 吊销错误，必须用 openssl + 关闭校验
PUSH_CMD="git -c http.sslBackend=openssl -c http.sslVerify=false push origin main"

# 1. 检测变更与未推送提交
CHANGES=$(git status --porcelain 2>/dev/null)
UNPUSHED=$(git log origin/main..HEAD --oneline 2>/dev/null)

if [ -z "$CHANGES" ] && [ -z "$UNPUSHED" ]; then
  log "无变更、无待推提交，跳过"
  exit 0
fi

# 2. 自动提交（仅当有未提交变更）
if [ -n "$CHANGES" ]; then
  git add -A
  N=$(git diff --cached --name-only | wc -l | tr -d ' ')
  FILES=$(git diff --cached --name-only | head -5 | tr '\n' ' ')
  MSG="auto: 更新 $N 个文件（$FILES）"
  if git commit -m "$MSG" >> "$LOG" 2>&1; then
    log "✓ 提交成功：$MSG"
  else
    log "⚠ 提交失败（可能无实质变更），继续尝试推送已有提交"
  fi
fi

# 3. 自动推送（网络重试 3 次 + 冲突自动 pull --rebase）
for i in 1 2 3; do
  OUT=$($PUSH_CMD 2>&1)
  if [ $? -eq 0 ]; then
    log "✓ 推送成功"
    exit 0
  fi
  # 冲突：远程有新提交（non-fast-forward / fetch first / rejected）
  if echo "$OUT" | grep -qi "non-fast-forward\|fetch first\|rejected"; then
    log "⚠ 检测到远程有新提交，尝试 pull --rebase"
    git -c http.sslBackend=openssl -c http.sslVerify=false pull --rebase origin main >> "$LOG" 2>&1
    # rebase 产生冲突 → 明确报告冲突文件并停止，绝不 force push
    CONFLICTS=$(git diff --name-only --diff-filter=U | tr '\n' ' ')
    if [ -n "$CONFLICTS" ]; then
      log "✗ rebase 冲突，冲突文件：$CONFLICTS"
      log "✗ 请人工解决冲突后重跑本脚本（本脚本不会 force push）"
      exit 2
    fi
    continue
  fi
  # 网络错误（502 / SSL_read EOF 等）→ 重试
  log "⚠ 推送失败（第 $i 次）：$(echo "$OUT" | tail -1)"
  sleep 5
done

log "✗ 推送失败（已重试 3 次），请检查网络"
exit 1
