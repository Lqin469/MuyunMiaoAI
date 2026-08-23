# ADR-001 · 搜索必须由用户显式触发（禁止后台静默搜索）

- 状态：**Accepted（M0 定稿）**
- 日期：2026-08-24
- 关联需求：R11
- 关联文档：docs/privacy-search-consent.md、docs/03-contracts.md

## 背景

用户强制约束：**所有涉及全盘搜索、文件搜索等搜索类功能，都必须由用户明确提出后才执行，绝不能未经许可在后台偷偷运行。**

搜索/索引天然具备"重、敏感、可观察性差"的特点：全盘遍历耗电耗资源、可能触及用户隐私路径，若在后台静默执行，用户完全无感知。

## 决策

1. 引入 `SearchConsentGate`（core:search）作为搜索类功能的**唯一入口**：
   - `beginUserInitiated()` —— 仅由用户在 UI 显式点击触发，始终放行；
   - `beginScheduled()` —— 后台/计划触发，**仅当用户显式开启 `backgroundIndexingEnabled`（默认 false）才放行**，否则返回 null；
2. `FileIndexer.index()` 必须携带 `SearchSession`，无会话直接抛 `UnauthorizedSearchException`；
3. 每次触发（含被拒的）写 `consent_audit` 审计日志，供隐私自检；
4. 默认**不注册**任何周期性的文件索引 Worker；即使开启后台索引，也必须伴随可见进度与可取消能力（ADR-002）。

## 被否决的备选

- **"首次启动静默建索引"**：否决。违反用户约束；
- **"仅靠权限弹窗一次授权"**：否决。授权 ≠ 每次执行许可，仍需逐次显式触发；
- **"仅文档约束不写代码"**：否决。约束必须落到代码强制，避免回归。

## 后果

- 正面：用户对搜索行为 100% 知情；审计可查；PR 评审有强制检查项（PULL_REQUEST_TEMPLATE）；
- 成本：每次搜索多一次会话创建与校验；后台索引能力默认不可用，需用户两步开启（总开关 + 单次触发）。

## 验证

- 单测：`beginScheduled()` 在开关关闭时返回 null；`index()` 无会话抛异常；
- Code Review：搜索路径必须出现 `SearchConsentGate` 调用；CI 的 PR 模板含专属检查项。
