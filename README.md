# MuyunMiaoAI 沐云杪AI · 开源 AI 备忘助手

<div align="center"><img src="assets/banner.svg" alt="沐云杪AI MuyunMiaoAI" width="100%"/></div>

> **沐云杪AI｜本地端侧AI备忘录，采用 MNN‑LLM 本地大模型与 RAG 知识库，面向安卓客户端。**
>
> 把「随手记」和「长期 AI 记忆」合二为一：日常笔记/待办，加上一个**只属于你的 AI 记忆库**——本地 MNN 离线推理 + 云端 API 双引擎，全部数据留在本机。

![license](https://img.shields.io/badge/license-GPL--3.0-blue)

## 特性（需求编号 R1-R12，详见 docs/00-overview.md）

| 编号 | 能力 |
|---|---|
| R1 | 云端 API 用户自行配置接入（OpenAI 兼容），不内置任何服务商 |
| R2 | 大模型按需下载，附**硬件配置评估**（内存/存储/CPU 档位红黄绿） |
| R3 | 支持**本地导入已下载模型**（MNN 目录 / GGUF 转换指引） |
| R4/R5 | 单一数据库目录；**存储目录可自定义**并支持迁移 |
| R6 | 对话中 AI **自动记忆关键信息**（事实/偏好/待办），问答时可召回 |
| R7 | 常规备忘录内容**自动同步**进 AI 知识库 |
| R8 | 上传文件夹 / 文件 / 图片供 AI 读取（PDF/DOCX/TXT/MD/OCR） |
| R9 | 支持**压缩包**（ZIP/TAR/GZ/BZ2/XZ/7z/RAR4）及常见格式 |
| R10 | 无法解析的文件**记录位置**，AI 可告知"文件在哪" |
| R11 | **文件检索**：Shizuku/adb/root 三档提权（参考 Operit），AI 工具调用搜索本地文件 |
| R12 | **文档驱动开发**：每模块开发记录归档 docs/devlog/，他人/AI 仅凭文档可无缝接手 |

## 隐私承诺（用户强制约束，务必遵守）

> ⚠️ 所有**搜索类功能（全盘搜索 / 文件搜索 / 文件索引）必须由用户显式发起后才执行，绝不未经许可在后台偷偷运行**；搜索执行期间必须提供**实时进度条**展示当前状态，并支持随时停止。
> 实现：`core:search` 的 `SearchConsentGate` 许可闸门 + `SearchProgress` 进度契约（ADR-001 / ADR-002，见 docs/privacy-search-consent.md）。

其他隐私红线：
- 文件索引只存元数据（路径/名称/大小/时间），**不索引文件内容**；
- 云端问答默认只发送文件名/路径，不发送文件内容；
- 记忆提炼默认本地执行，可一键清空记忆库。

## 快速开始

1. 用 **Android Studio**（JDK 17）打开本目录，等待 Gradle Sync（首次需下载 Gradle 8.13 与依赖）；
2. 选择 `app` 运行到设备（minSdk 26 / Android 8.0+）；本地构建见「常见问题」节。

```bash
# 命令行构建（本机环境重建后：JAVA_HOME 已持久化到用户级）
cd /d D:\LQYMYH\ai备忘录\MuyunMiaoAI
gradlew.bat :app:assembleDebug
# 正式签名产物（keystore 见 docs/02-technical-choices.md D-008）
gradlew.bat :app:assembleRelease
```

> ⚠️ 已知环境坑：在 WorkBuddy 会话内运行 Gradle 会因 `.lock` 跨进程被系统拦截而失败（详见 docs/README.md 或总流程日志 §2.7）；**请在普通终端（cmd/PowerShell）执行上述命令**。

## 常见问题

### gradle wrapper 为什么有时报 SSL 错误？
- 首次运行 `gradlew` 需从 services.gradle.org 下载发行包，本机网络拦截会导致 `PKIX path building failed`；
- 解决：用本机已装的 Gradle 直连（`C:\Users\Administrator\.workbuddy\binaries\gradle\gradle-8.13\bin\gradle.bat`），或预置发行包到 `~/.gradle/wrapper/dists/gradle-8.13-bin/`（见总流程日志 §2.6）。

### 为什么「签名不一致」无法覆盖安装？
- v0.4.1 起改用正式签名 keystore（`D:\LQYMYH\keystores\yzqy.jks`，alias/password=yzqy）；
- 旧版（≤v0.4.0）为 debug 签名，**需卸载重装一次**；此后升级签名永不变（详见 docs/02-technical-choices.md D-008）。

## 安全规范（推送 GitHub 前必读）

> 🤖 **AI 协作者先读 [AGENTS.md](AGENTS.md)** —— 第一条红线就是「严禁把 API Key、密钥、`.env` 等敏感信息上传到 GitHub」。

- [docs/security-git-secrets.md](docs/security-git-secrets.md) — 检查并清除误提交的密钥（git rm --cached / filter-branch / BFG / filter-repo）
- [docs/security-pre-push.md](docs/security-pre-push.md) — 推送前自查流程（10 步清单）
- 根目录 `.gitignore` — 已覆盖环境变量/密钥/证书/构建产物/日志/模型文件，每项带中文注释
- **安装推送前自动检查钩子**（推荐）：

```bash
git config core.hooksPath scripts/hooks
# 之后每次 git push 都会自动运行 scripts/check-secrets.sh，发现敏感信息即拦截推送
```

## 项目结构

```
app/                        # 应用壳（单 Activity + Compose + 路由/抽屉）
core/                       # 能力层（只依赖接口）
  ui/                       # 共享 UI：主题（亮暗 + 17 套）/42 图标/通用组件/壁纸（M8.5 新增）
  storage/                  # 存储抽象与自定义目录（R4/R5）+ 偏好（AppPrefs/WallpaperPrefs/NotePrefs）
  db/                       # Room 全库（9 表，version 4）
  device/                   # 设备信息检测 + 满足度判定（M-027 新增）
  lan/                      # 局域网发现 + TCP 传输（NSD + 断点续传，M-027 新增）
  ai/engine/                # ChatEngine 双实现：MNN 本地 / OpenAI 兼容云端 + 引擎路由/监控/诊断
  ai/embed/                 # 双轨 Embedding + 混合检索
  ai/tools/                 # 工具调用总线（search_file 等）
  ai/memory/                # 会话自动记忆（R6）
  ingest/                   # 文档/图片/压缩包解析入库（R8/R9）
  search/                   # 文件索引 + 许可闸门 + 提权（PrivilegeManager）
  models/                   # 模型导入/删除/硬件评估（R2/R3）
feature/                    # 业务层
  notes/  chat/  settings/  # 知识库 UI 在 settings 内实现；feature:knowledge 为空壳占位
scripts/                    # 安全自查脚本与 git 钩子
docs/                       # 文档驱动开发（入口见 docs/README.md）
```

## 文档

- [docs/README.md](docs/README.md) — **文档体系总纲（从这开始读）**
- [docs/00-overview.md](docs/00-overview.md) — 项目总纲与需求索引（R1-R12 状态）
- [docs/01-architecture.md](docs/01-architecture.md) — 架构与模块边界
- [docs/02-technical-choices.md](docs/02-technical-choices.md) — 技术选型与否决记录
- [docs/03-contracts.md](docs/03-contracts.md) — 接口契约（单一事实源）
- [docs/04-database.md](docs/04-database.md) — 数据表设计
- [docs/05-model-hardware.md](docs/05-model-hardware.md) — 模型与硬件评估
- [docs/adl/](docs/adl/) — 架构决策记录（ADR-001/002）
- [docs/devlog/](docs/devlog/) — 模块开发记录（R12 强制归档）
- [docs/privacy-search-consent.md](docs/privacy-search-consent.md) — 搜索隐私约束（必读）
- [docs/code-review-guide.md](docs/code-review-guide.md) — 代码审查标准与流程
- [docs/security-git-secrets.md](docs/security-git-secrets.md) — 密钥检查与清除
- [docs/security-pre-push.md](docs/security-pre-push.md) — 推送前自查流程
- [AGENTS.md](AGENTS.md) — **AI 协作者强制红线（严禁上传密钥等）**

## 致谢与合规

- 本地推理引擎：[alibaba/MNN](https://github.com/alibaba/MNN)（Apache-2.0），含 MNN-LLM / PaddleOCR / Embedding
- 云端对接与提权方案、UI 设计参考：[AAswordman/Operit](https://github.com/AAswordman/Operit)（修改版 GPLv3）——本项目为开源衍生，README 按要求标注原始地址
- 本项目仓库主许可证：**GPL-3.0**（自研文件头同时注明 Apache-2.0，详见 LICENSE）

## 路线图

M0 基建 ✅ → M1 存储/数据 ✅ → M2 备忘录 ✅ → M3 云端对话 ✅ → M4 知识库 ✅ → M5 记忆 ✅ → M6 本地引擎 ✅ → M7 文件检索 ✅ → M8 发布 ✅ → M8.5~M8.10 迁移/优化/核心功能 ✅（详见 docs/00-overview.md「路线图」节）
