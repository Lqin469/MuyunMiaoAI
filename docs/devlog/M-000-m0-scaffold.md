# 开发记录：M0 项目基建 — 仓库 / CI / docs 骨架 / 搜索约束契约

- 日期：2026-08-24
- 涉及模块：全部（骨架）
- 关联需求：R11（搜索契约）、R12（文档驱动开发）
- 关联文档：ADR-001、ADR-002、docs/00-overview.md、docs/03-contracts.md、docs/privacy-search-consent.md

## 1. 目标与范围

**目标**

- 建立 Gradle 多模块 Android 工程骨架（:app + 9 个 :core + 5 个 :feature），可被 Android Studio 直接打开构建；
- 建立 docs/ 文档体系与 devlog 规范（R12）；
- **把用户强制约束「搜索必须显式触发 + 进度实时可见」落地为 core:search 的代码契约**（ADR-001/002）。

**不做**

- 不实现任何业务功能（存储、笔记、对话、索引实现均在后缀阶段）；
- 不内置任何模型/服务商；
- 不注册任何后台扫描任务（默认禁止，符合约束）。

## 2. 设计要点

- **模块只依赖接口**：`:feature:* → :core:* → 基础设施`，禁止反向依赖；
- **搜索约束代码化**：`SearchConsentGate` 作为搜索唯一入口 + `UnauthorizedSearchException` 兜底 + `consent_audit` 审计 + `SearchProgress` 上报契约，四层保障；
- **后台默认关闭**：`SearchSettings.backgroundIndexingEnabled` 契约默认 false；即使开启也必须伴随可见进度；
- **文档驱动**：PR 模板强制勾选 devlog / ADR / contracts 同步；
- **许可证**：仓库 GPL-3.0（因引用 Operit 修改版 GPLv3），自研头 Apache-2.0 双注明，README 署名 Operit。

## 3. 接口契约

见 docs/03-contracts.md「搜索契约」一节（M0 唯一已定义契约）。要点：

```
SearchConsentGate.beginUserInitiated(id): SearchSession   // UI 点击，放行
SearchConsentGate.beginScheduled(id): SearchSession?      // 需开关，默认 null
FileIndexer.index(session, scope, listener): IndexResult // 无会话抛异常
SearchProgress { phase, scanned, total, currentPath, percent }
```

## 4. 关键实现

文件清单（新增）：

```
settings.gradle.kts / build.gradle.kts / gradle.properties
gradle/libs.versions.toml                # AGP 8.9.2 · Kotlin 2.1.20 · Compose BOM 2026.05.00
gradle/wrapper/gradle-wrapper.properties # Gradle 8.13
LICENSE                                  # GPL-3.0 全文（674 行，取自 gnu.org）
.gitignore / README.md / CONTRIBUTING.md
.github/workflows/ci.yml                 # build + lint + docs 存在性检查
.github/PULL_REQUEST_TEMPLATE.md         # 含搜索类专属检查项
core/search/...                          # consent / progress / index / service / audit 契约
feature/filesearch/...                   # FilesearchViewModel + SearchProgressBar（进度条 UI 落地）
app/...                                  # MainActivity 占位 + Hilt 装配
docs/{00-overview,01-architecture,03-contracts,04-database,05-model-hardware}.md
docs/{privacy-search-consent}.md
docs/adl/ADR-001-search-consent.md · ADR-002-search-progress.md
docs/devlog/README.md · M-000-m0-scaffold.md（本文件）
```

可复现命令：

```bash
# 构建（需 Android SDK；wrapper jar 由 Android Studio 同步或 `gradle wrapper` 生成）
gradle :app:assembleDebug :app:lintDebug testDebugUnitTest --stacktrace
```

## 5. 测试与验证

- [ ] CI 绿：assembleDebug + lintDebug + 单测 + docs 存在性检查；
- [ ] Android Studio 打开可同步、可运行到模拟器/真机（minSdk 26）；
- [ ] 人工审查：搜索相关代码无绕过 `SearchConsentGate` 的路径；
- [ ] 后续补单测：`beginScheduled` 开关关闭返回 null；`index()` 无会话抛 `UnauthorizedSearchException`。

**已知问题**：gradle wrapper jar 未提交（二进制无法在本环境生成），需在首次构建时由 AS 同步或执行 `gradle wrapper --gradle-version 8.13` 生成；CI 已用 `gradle/actions/setup-gradle` 规避。

## 6. 接手指引

下一步（M1）：

1. 实现 `core:storage`：`StorageProvider` + 目录规划 + `StorageMigrator`（自定义目录 R4/R5）；
2. 实现 `core:db`：Room 全库 + FTS5 + `consent_audit` 表（docs/04-database.md 为准）；
3. 实现 `feature:settings` 的 `SearchSettings`（DataStore，`backgroundIndexingEnabled` 默认 false）。

踩坑点：

- Room 动态路径需要 `Room.databaseBuilder(ctx, ..., dbFile)` 支持自定义 File（M1 验证）；
- FTS5 在 Room 中建议用外部内容表（`@Fts4(contentEntity=...)`）；
- 搜索契约是硬约束，任何新搜索路径先过 `SearchConsentGate` 再写实现。
