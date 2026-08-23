## 变更说明

请简要描述本次改动（修复 / 特性 / 重构），并**引用需求编号**（R1-R12）。

## 检查清单（合并前必须全部勾选）

- [ ] 代码通过 CI（`gradle :app:assembleDebug :app:lintDebug`）
- [ ] 功能模块开发记录已归档：`docs/devlog/`（R12，缺失则不算完成）
- [ ] 若接口有变更：已新增/更新 `docs/adl/ADR-xxx-*.md`（先写 ADR 再改代码）
- [ ] 若涉及数据表：已同步更新 `docs/04-database.md`
- [ ] 若涉及接口签名：已同步更新 `docs/03-contracts.md`

## 搜索类改动专属检查（R11 强制）

- [ ] 本次改动未引入任何"后台/静默自动搜索或索引"路径
- [ ] 所有搜索调用均经 `SearchConsentGate` 获取 `SearchSession`
- [ ] 所有搜索/索引进度均通过 `SearchProgressListener` 实时上报并可见
- [ ] 相关日志已写入 `consent_audit` 审计（或注明后续落库）

## 关联

- Closes #（Issue 编号）
- 需求：R（编号）
