# M8 隐私自检报告（security-audit-m8）

> 依据 v2 规划「M8 打磨发布：隐私自检通过」验收标准，逐项审计。日期：2026-08-25。

## 一、密钥安全

| 检查项 | 结果 | 证据 |
|---|---|---|
| 代码无硬编码 API Key/令牌 | ✅ 通过 | 全仓 grep `sk-[a-z0-9]{20}` / `api_key="..."` 无命中 |
| 云端 API Key 加密存储 | ✅ 通过 | `CloudConfigRepository` 用 androidx.security-crypto 加密 |
| 提交前密钥自查脚本 | ✅ 通过 | `scripts/check-secrets.sh` + pre-push 钩子（hooksPath） |
| .gitignore 密钥规则生效 | ✅ 通过 | 2026-08-24 修复行内注释失效问题（提交 `245ed61`）后全绿 |

## 二、文件索引隐私（R11 红线）

| 检查项 | 结果 | 证据 |
|---|---|---|
| 索引只存元数据不存内容 | ✅ 通过 | `FileIndexerImpl` 只写 path/name/ext/size/mtime，从不读取文件内容 |
| 搜索必须用户显式触发 | ✅ 通过 | 全部经 `SearchConsentGate`；`SearchSession` 强制；无会话抛 `UnauthorizedSearchException` |
| 后台索引默认关闭 | ✅ 通过 | `SearchSettings.backgroundIndexingEnabled` 默认 false（ADR-001/002） |
| 实时进度 + 可取消 | ✅ 通过 | `SearchProgressListener` 每 200 文件上报；`session.cancelled` 每批检查 |
| 云端问答只发文件名/路径 | ✅ 通过 | `search_file` 工具返回 `FileHit` 元数据（路径/名称/大小），不发文件内容 |
| 索引每次触发留审计 | ✅ 通过 | `ConsentAuditEntry` 契约（requestId/trigger/granted/reason） |

## 三、提权安全（M7）

| 检查项 | 结果 | 证据 |
|---|---|---|
| 三档能力严格校验 | ✅ 通过 | `FileIndexerImpl.resolveRoots`：UserStorage 需 L1+、FullDisk 需 L2，未达标抛异常 |
| UI 全程明示能力等级 | ✅ 通过 | FilesearchScreen 常驻 L0/L1/L2 徽章 + Shizuku 引导页 |
| 提权可降级 | ✅ 通过 | L0 无权限时仅扫描应用私有目录，功能不崩 |

## 四、内存与省电

| 检查项 | 结果 | 说明 |
|---|---|---|
| 无后台常驻任务 | ✅ | 索引仅用户点击触发，一次性执行完即释放；无 WorkManager 周期任务 |
| 索引批量落库 | ✅ | 200 条/批 upsertAll，避免逐条写库的内存/IO 抖动 |
| 索引遍历流式处理 | ✅ | `walkTopDown` 迭代器逐文件处理，不整体加载目录树 |
| 模型懒加载 | ✅ | LocalChatEngine/MnnEmbeddingProvider 首次调用才加载，不占用常驻内存 |

## 五、稳定性

| 检查项 | 结果 | 说明 |
|---|---|---|
| 代码 TODO 残留 | ✅ 无 | grep 命中均为 TODO 枚举（NoteType.TODO/MemoryType.TODO），非未完成代码 |
| CI 全绿 | ✅ | 全部提交经 GitHub Actions 编译验证 |
| 数据库迁移 | ✅ | `fallbackToDestructiveMigration` 兜底（v4 升级不崩） |

## 结论

**隐私自检通过 ✅**，可发布首版正式 Release。
