# ADR-002 · 搜索/索引进度必须实时上报并可见

- 状态：**Accepted（M0 定稿）**
- 日期：2026-08-24
- 关联需求：R11
- 关联文档：docs/privacy-search-consent.md、docs/03-contracts.md

## 背景

用户强制约束：**执行搜索操作时，需要提供清晰的进度条来实时提示搜索进度，让用户能够看到当前进行的状态。**

全盘索引可能耗时数分钟，用户需要知道：当前在做什么阶段、已处理多少、还剩多少、能否停止。

## 决策

1. 定义进度契约 `SearchProgress`（core:search），字段：`phase / scannedItems / totalItems / currentPath / percent / message / startedAt / updatedAt`；
2. `FileIndexer.index()` 必须通过 `SearchProgressListener` 上报，**上报规则（强制）**：
   - 阶段切换必报；
   - 扫描/索引期间**每处理 200 个文件或每前进 1% 至少上报一次**（含 currentPath）；
   - 结束（DONE / CANCELLED / FAILED）必报；
3. UI 层（feature:filesearch 的 `SearchProgressBar`）将进度渲染为可见进度条 + 已处理数 + 当前路径 + **「停止」按钮**；
4. 后台索引（若用户开启）同样必须通过前台服务通知展示进度，并支持从通知停止；
5. `SearchSession.cancelled` 供索引器每批轮询，实现"随时停止"。

## 被否决的备选

- **"跑完再一次性展示结果"**：否决。用户无法感知进行中状态，违反约束；
- **"仅展示不确定进度条"**：否决。应尽量给出可量化进度（百分比、计数），不确定进度仅用于初始化阶段。

## 后果

- 正面：搜索全程可见、可量化、可中断；
- 成本：索引器需维护计数与节流上报（少量性能开销，可忽略）；UI 增加一个进度组件。

## 验证

- 单测：伪造监听器，断言上报次数满足"每 200 文件或 1%"；
- UI 验收：真机索引 1 万+ 文件，进度条平滑推进、停止即时生效。
