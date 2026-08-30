# 代码审查标准与流程（Code Review Guide）

> 文档编号：docs/code-review-guide.md
> 配套：CONTRIBUTING.md ｜ .github/PULL_REQUEST_TEMPLATE.md ｜ docs/adl/ADR-001/002 ｜ docs/privacy-search-consent.md
> 目标：让每一次改动经过「**本地自检 → 自动化检查 → 同行审查**」三道闸，质量与安全不依赖个人状态。
> 原则：**审的是问题，不是人** —— 每条评论解释「为什么」，给出「怎么做」，标注「多严重」。

---

## 1. 审查原则

| # | 原则 | 说明 |
|---|---|---|
| 1 | 具体到行 | 必须指出文件与行号（如 `CloudApiClient.kt:47`），禁止「这里有安全问题」式空泛评论 |
| 2 | 先解释再建议 | 每条评论 = 问题 + 为什么 + 建议改法；不丢结论不给方案 |
| 3 | 分级标注 | 🔴 阻断 / 🟡 应修 / 💭 可改，见 §3；不混为一谈 |
| 4 | 一次审完 | 单轮审查给出全部发现，禁止挤牙膏式分批评论 |
| 5 | 肯定好的代码 | 遇到清晰的实现、正确的并发处理、好的命名，明确表扬 |
| 6 | 意图不明先问 | 不臆断「写错了」；先问清需求再下结论 |
| 7 | 只评不改 | 审查者只评论，由作者修改；避免双人同时改同一文件造成冲突 |

---

## 2. 审查维度与检查清单（本项目标准）

### 2.1 正确性（Correctness）

- [ ] 行为符合需求编号（R1-R12）与 PR 描述；「不做什么」是否与 devlog 一致
- [ ] 边界条件：空输入 / 空列表 / null / 0 长度 / 超大值 / 非法参数（如 `Chunker` 空文本、`NoteDao` 空库）
- [ ] 错误处理：IO / 网络 / 解析异常是否捕获并降级（参考 `MnnEmbeddingProvider` 降级 SimpleHash 的模式）；失败是否静默吞掉
- [ ] 并发与协程：`viewModelScope` 作用域是否正确；`Dispatchers.IO` 是否用于阻塞操作；共享状态是否线程安全（`MutableStateFlow` 单线程写）；`Mutex`/`withLock` 是否覆盖竞态
- [ ] 生命周期：`DisposableEffect`/`LaunchedEffect` 是否泄漏；`BackHandler`/`ON_STOP` 保存路径是否齐全（参考 M-026 三管齐下）
- [ ] 异步 ID 竞态：DAO 返回 ID 是否用回调式传递（参考 M-010 `ensureConversation`）
- [ ] 时间与格式：时间戳单位、时区、`Locale` 格式化是否正确

### 2.2 安全与隐私（本项目红线，Blocker 级）

- [ ] **搜索类（R11 强制）**：未引入任何后台/静默搜索路径；调用经 `SearchConsentGate`；进度经 `SearchProgressListener` 可见；审计写 `consent_audit`（违反 = 一票否决）
- [ ] **密钥**：无硬编码（API Key/Token/证书/`.env*`）；敏感项走 `EncryptedSharedPreferences`（Keystore）；占位符 `YOUR_API_KEY`；提交前 `bash scripts/check-secrets.sh`
- [ ] **隐私红线**：文件索引只存元数据（路径/名/大小/时间），不存内容；云端问答只发文件名/路径
- [ ] **压缩包安全**：`ArchiveExtractor` 的 zip-slip 路径校验、解压总量/单文件/压缩比上限未被削弱
- [ ] **网络**：HTTPS；连接/读写超时；4xx 不重试、5xx 指数退避（参考 `CloudApiClient`）；SSE 不重发已产出
- [ ] **权限**：`PrivilegeManager` 等级校验未绕过；提权能力可降级 L0/L1/L2
- [ ] **文件操作**：写文件经 `StorageProvider`，禁止散落硬编码路径

### 2.3 架构一致性（本项目不变式）

- [ ] 所有 AI 能力走 `ChatEngine`/`EmbeddingProvider` 接口（本地/云端可互换），禁止绕过接口直连实现
- [ ] 模块依赖方向 `:feature:* → :core:* → 基础设施`，禁止反向（如 feature 不依赖 app）
- [ ] AI 工具统一走 `ToolCallingBus`，禁止引擎直接调用具体工具
- [ ] 接口变更先写 ADR（docs/adl/）再改代码；数据表变更同步 docs/04-database.md；签名变更同步 docs/03-contracts.md
- [ ] 新功能模块当回合归档 docs/devlog/M-xxx（R12，缺失 = 不算完成）

### 2.4 可维护性（Maintainability）

- [ ] 命名表达意图（方法名动词、布尔值 is/has、常量语义化）
- [ ] **逐行中文注释**（项目硬性要求），注释解释「为什么」而非复述代码
- [ ] 单一职责：函数/类是否只做一件事；过长方法是否应拆分
- [ ] 重复代码：是否应提取到 core/ui 或共享组件（如多个页面复制的 Row 样式）
- [ ] 魔法数/魔法字符串：是否提取为常量或枚举（如 `CapabilityChecker` 阈值集中管理）

### 2.5 性能（Performance）

- [ ] 主线程无阻塞：无 IO/网络/大计算在主线程；数据库查询是否挂起函数
- [ ] 大对象内存：Bitmap 是否采样解码（`rememberBitmap` 的 inJustDecodeBounds）；是否及时释放
- [ ] 数据库查询：是否 N+1；`chunksByFolder` 全量取回的规模是否可控
- [ ] 不必要的分配：循环内创建对象、大字符串拼接是否用 StringBuilder
- [ ] 流式/列表：LazyColumn 是否使用稳定 key；状态更新是否触发不必要重组

### 2.6 测试与验证（Testing）

- [ ] 核心逻辑是否有单元测试或可验证路径（当前项目以真机验证为主，需在 devlog 记录验证步骤与结果）
- [ ] 边界用例是否验证（空库、断网、模型缺失降级、权限拒绝）
- [ ] 构建验证：`gradle :app:assembleDebug :app:lintDebug` 通过
- [ ] 真机回归：涉及 UI/引擎/权限的改动需记录真机验证结果

---

## 3. 严重级别与处置

| 级别 | 含义 | 处置 |
|---|---|---|
| 🔴 **Blocker** | 安全漏洞、数据丢失、崩溃、破坏契约、违反隐私/搜索红线 | **合并前必须解决**；无法当场解决则 PR 打回 |
| 🟡 **Suggestion** | 输入校验缺失、命名混乱、缺测试、明显性能问题、重复代码 | 应修；可随本 PR 或明确登记为后续 Issue |
| 💭 **Nit** | 风格小瑕疵、文档措辞、替代方案 | 可选；不阻塞合并 |

> 规则：**一个 🔴 即可打回**；🟡 建议在合并前清零或登记跟踪；💭 不阻塞。
> 审查结论三选一：✅ 通过 / 🔁 修改后通过（列出必改项）/ ❌ 打回（列出 blocker）。

---

## 4. 审查流程（五阶段端到端）

```
[阶段0 本地自检]  →  [阶段1 提交]  →  [阶段2 CI]  →  [阶段3 PR审查]  →  [阶段4 合并]
   作者完成         规范提交           自动检查          人工+AI 同行       全部通过
```

### 阶段 0 — 本地自检（作者，提交前）
1. 本地构建通过：`gradle :app:assembleDebug`（本机：`C:\Users\Administrator\.workbuddy\binaries\gradle\gradle-8.13\bin\gradle.bat`）；
2. 安全自查：`bash scripts/check-secrets.sh`；有 pre-push 钩子则自动触发（`git config core.hooksPath scripts/hooks`）；
3. 对照 §2 清单自审一遍（尤其 2.1 正确性 / 2.2 安全隐私）；
4. 归档 devlog：`docs/devlog/M-xxx-*.md`（R12 强制，未归档 = 未完成）。

### 阶段 1 — 提交（规范）
- 提交信息格式：`feat(scope): 说明 #Rn docs:M-xxx`（如 `feat(notes): 笔记自动同步知识库 #R7 docs:M-002`）；
- 提交内容不含任何构建产物、`local.properties`、`keystore.properties`、密钥文件。

### 阶段 2 — CI 自动检查（无需人工）
- `.github/workflows/ci.yml`：build（`assembleDebug`）+ secret-scan 两个 job，全绿才允许合并；
- 建议后续增强：`lintDebug`（已有模板勾选）、单元测试 job、detekt（见 §7 可选）。

### 阶段 3 — PR 审查（人工 + AI）
1. **作者**：填写 PR 模板全部勾选项（文档/搜索/密钥三块）；
2. **审查者**（项目成员或 AI 审查者）：
   - 按 §2 六维度过清单，重点核查 2.2 安全隐私与 2.3 架构一致性；
   - 评论格式：`🔴 级别｜维度｜文件:行号｜问题 → 为什么 → 建议`（示例见 §8）；
   - 一次审完所有发现；如有疑问先用问题口吻澄清；
   - 结论：✅ / 🔁 / ❌；
3. **轮次**：最多 2 轮（作者修改 → 复审）；超 2 轮或 48h 无响应，升级给项目负责人；
4. **时限**：审查者 48h 内给出首轮意见；作者 48h 内响应。

### 阶段 4 — 合并（Gatekeeper）
- 全部 🔴 解决；🟡 已处理或登记 Issue；CI 全绿；文档同步完成；
- 合并方式：Squash merge（保留单一清晰提交，便于回滚与追溯）；
- 合并后：删除已合并分支；在 devlog 记录「验证通过」结论（如需）。

---

## 5. AI 协作者审查协议（本项目 AI 协作高频）

- **AI 生成代码同样必须过阶段 0-4**，不豁免；
- **AI 作为审查者**：只评论不改码；必须引用文件:行号；不得自行「顺手修复」；
- **AI 作为作者**：提交前自审清单必须逐项自查；被审查打回时逐条回应，不重复犯错（把历史 review 意见当先验知识）；
- **AI 审查红线**：发现密钥/搜索绕过/架构破坏等 Blocker，立即标注 🔴 并说明违反的具体约束条款；
- **安全提示**：AI 在审查中接触到的密钥信息（如 keystore.properties、API Key）**不得写入任何文档、日志或评论**；审查敏感文件时只看结构不输出内容。

---

## 6. 本项目高频问题速查（从既往 devlog/报错表提炼）

| 高频问题 | 审查要点 | 出处 |
|---|---|---|
| `.lock` 跨进程被拒 | Windows/WorkBuddy 会话内 Gradle 无法运行；构建走外部终端 | 总流程日志 §2.7 |
| keytool 密码 <6 字符 | 用 Java KeyStore API 改写密码绕过限制 | 总流程日志 M-036 |
| so 中文字符串误判 | 验证须 UTF-8 字节匹配，勿用 latin1 | 总流程日志 M-030 |
| 强杀 daemon 留锁 | 构建失败勿 taskkill java，先 `gradlew --stop` | 总流程日志 §2.7 |
| KDoc 写 `*/` | 注释块提前终止 → 语法错误；改写避免 | 总流程日志 M-027 |
| `weight` 用在组件自身 | weight 只能由父级 Row/Column 作用域传入 | 总流程日志 M-020 |
| 中文路径编译 | 项目路径含中文需 `android.overridePathCheck=true`；MNN 编译走英文路径 | 总流程日志 §4.4 |
| 行内 `#` 注释 | .gitignore/gradle.properties/wrapper 均禁行内注释 | 总流程日志 §4.6 |

---

## 7. 后续增强建议（可选，登记不强制）

1. **静态分析**：接入 detekt（Kotlin 静态分析，规则文件 `config/detekt/detekt.yml`，CI 增加 `detekt` job）——建议基线先 `baseline.xml` 只拦新增问题，避免历史噪音；
2. **单测门禁**：为 core 层纯逻辑（Chunker/ArchiveExtractor/HybridRetriever/CloudApiClient 重试）补 JUnit 单测，CI 跑 `testDebugUnitTest`；
3. **审查模板化**：GitHub 增加 CODEOWNERS（指定核心模块 owner），自动指派审查者；
4. **本地 lint 钩子**：pre-commit 加 `gradle :app:lintDebug`（耗时较长，可选）。

---

## 8. 审查评论示例

```markdown
🔴 **安全｜搜索约束（R11）**
`core/search/index/FileIndexerImpl.kt:58`：新加的 `autoRefresh()` 会定时调用 `index()`，绕过了 `SearchConsentGate`。

**为什么**：用户强制「搜索必须显式触发」（ADR-001），定时器属于 SCHEDULED_BACKGROUND 触发，默认必须拒绝，且必须有可见进度。

**建议**：
- 删除 `autoRefresh()`，或在 `SearchConsentGate.beginScheduled()` 返回 null 时直接放弃执行；
- 触发必须写 `consent_audit` 审计；UI 必须有进度条与停止按钮。
```

```markdown
🟡 **正确性｜空输入**
`core/ingest/Chunker.kt:33`：`text.split(...)` 对空串返回 `[""]`，`filter { it.length >= 8 }` 会过滤掉，行为正确；但上游 `ingestText` 未判空时多一次嵌入调用。

**为什么**：空文本也会触发 `embedder.embed()`，浪费一次模型调用。

**建议**：`ingestText` 开头加 `if (text.isBlank()) return`。
```

```markdown
💭 **可维护性｜命名**
`feature/settings/ExtPrefs.kt:52`：`apiListJson` 名暗示 JSON 字符串，实际调用处还需自行 `JSONArray(json)` 解析。

**建议**：考虑封装为 `Flow<List<ApiConfig>>`，把解析收敛到一处。
```

---

*本文档为 R12 文档驱动开发的审查配套；与 PR 模板、CONTRIBUTING、ADR 共同构成「约定 + 自动化 + 审查」三层质量保障。*
