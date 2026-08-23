# 搜索隐私约束（用户强制 · 必读）

> 本文件是所有搜索类功能开发、评审、测试的**强制依据**。违反即不予合并。

## 一、核心约束（不可协商）

1. **显式触发**：所有搜索类功能（全盘搜索 / 文件搜索 / 文件索引）必须在用户于当前界面**明确发起**后才执行；
2. **禁止后台静默**：绝不未经许可在后台偷偷运行任何搜索/索引；
3. **进度可见**：执行搜索时必须提供**清晰的进度条**实时提示进度，用户能看到当前状态；
4. **随时可停**：搜索执行期间用户可随时停止。

## 二、代码落地

| 约束 | 实现 | 位置 |
|---|---|---|
| 显式触发唯一入口 | `SearchConsentGate`（`beginUserInitiated` 放行 / `beginScheduled` 需显式开关） | core:search/consent |
| 无会话禁止执行 | `FileIndexer.index()` 无 `SearchSession` 抛 `UnauthorizedSearchException` | core:search/index |
| 进度实时上报 | `SearchProgress` + `SearchProgressListener`（每 200 文件或 1% 必报） | core:search/progress |
| UI 进度条 + 停止 | `SearchProgressBar`（百分比 / 计数 / 当前路径 / 停止按钮） | feature:filesearch |
| 审计留痕 | `consent_audit` 表记录每次触发与结果 | core:search/audit（M1 落库） |
| 默认关闭后台 | `SearchSettings.backgroundIndexingEnabled` 默认 **false** | feature:settings（M1 实现） |

## 三、开发时的检查清单

- [ ] 新增的搜索/索引代码是否**都**经过 `SearchConsentGate`？有没有绕过？
- [ ] 是否引入了定时器 / WorkManager 周期任务 / 启动时自动扫描？→ 一律不允许（除非用户显式开启且伴随可见进度）；
- [ ] 进度上报是否满足"每 200 文件或 1%"？阶段切换/结束是否必报？
- [ ] UI 是否展示进度条与停止按钮？
- [ ] 是否写审计日志（granted/reason）？

## 四、其他隐私红线

- 文件索引只存元数据（路径/名称/大小/时间），**不索引文件内容**；
- 云端问答默认只发送文件名/路径，不发送文件内容；
- 记忆提炼默认本地执行，可一键清空记忆库；
- 审计日志可查看：设置 → 隐私与安全 → 搜索审计。

## 五、关联文档

- ADR-001（显式触发）、ADR-002（进度上报）
- docs/03-contracts.md（契约）、docs/01-architecture.md（不变式 #5/#6）
