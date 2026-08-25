# M-013 文件检索提权（M7）+ 打磨发布（M8）

> 日期：2026-08-25　提交：`a3fd7c8`（M7）

## 一、M7 文件检索提权（Shizuku + libsu 三档能力）

### 需求（#R11）
- 参考 Operit 的 adb/root 提权方案，AI 自动搜索本地文件；
- 三档能力：L0 无权限 / L1 Shizuku-adb / L2 Shizuku-root；
- 全盘文件索引（只存元数据）；AI 工具调用 `search_file`；
- 验收：**adb 授权后 AI 说出文件真实路径并可跳转**。

### 交付清单

| 模块 | 文件 | 说明 |
|---|---|---|
| core:search | `privilege/PrivilegeManager.kt` | 三档能力检测 + 授权请求 + libsu shell；`level`/`authorized` 状态流实时刷新 |
| core:search | `index/FileIndexerImpl.kt` | 范围与等级严格校验；walkTopDown 流式遍历；200 条/批落库；进度上报 + 取消响应 |
| core:search | `service/SearchServiceImpl.kt` | FileLocationDao 模糊查询（名称/路径 LIKE）+ 扩展名/目录过滤 |
| core:search | `di/SearchModule.kt` | Hilt 绑定：FileIndexer/SearchService/ConsentGate |
| core:ai:tools | `ToolCallingBus.kt` | 工具注册/分发 + `search_file` + `tell_location` 内置工具 + [[name:args]] 标记协议 |
| feature:filesearch | `FilesearchViewModel.kt` | 等级订阅 + 范围按等级解析 + 授权请求 + 索引查询 |
| feature:filesearch | `FilesearchScreen.kt` | 等级徽章（L0/L1/L2）+ Shizuku 引导（安装/授权）+ 进度条 + 结果列表（点击复制路径） |
| feature:chat | `ChatViewModel.kt` | 工具声明注入 system prompt + Done 后解析 [[tool]] 标记执行 + 结果追加消息 |
| app | `AndroidManifest.xml` | ShizukuProvider 注册（${applicationId}.shizuku） |
| app | `MainActivity.kt` | filesearch 路由 + 抽屉「文件检索」入口 |

### 关键设计决策
1. **等级判定**：Shizuku 13.1.5 无 `isSuiAvailable()`（14.x 才有），改用 libsu `Shell.isAppGrantedRoot()` 判定 L2；
2. **工具协议**：MVP 用「[[name:argsJson]] 文本标记」而非完整 function calling——LLM 在回复中输出标记，客户端解析执行，结果以独立消息追加。完整 function calling 留待后续；
3. **隐私红线全落地**：索引只存元数据；搜索必经 ConsentGate；范围与等级不匹配抛 `UnauthorizedSearchException`；UI 常驻等级徽章。

### 报错记录
| 报错 | 根因 | 修复 |
|---|---|---|
| `Unresolved reference 'isSuiAvailable'` | Shizuku 13.1.5 无此 API（14.x 才有） | 改用 libsu `Shell.isAppGrantedRoot()`（注意返回 `Boolean?` 需 `== true`） |
| `Condition type mismatch: Boolean?` | `Shell.isAppGrantedRoot()` 返回可空 | `== true` 判等 |
| FilesearchUiState Unresolved | 重写 ViewModel 时丢了 sealed interface | 补回定义 |
| CI 429 Too Many Requests | Maven Central 对 shizuku POM 下载限流 | 重跑 CI（临时限流，非代码问题） |

## 二、M8 打磨发布

### 隐私自检（`docs/security-audit-m8.md`，全部通过）
- 密钥：无硬编码；加密存储；check-secrets 钩子；.gitignore 规则生效；
- 索引：只存元数据；显式触发；后台默认关闭；进度可取消；云端只发文件名/路径；
- 提权：三档严格校验；UI 明示等级；可降级。

### 内存/省电
- 无后台常驻任务；索引 200 条/批；流式遍历；模型懒加载。

### 稳定性
- 无代码 TODO 残留；CI 全绿；数据库迁移兜底。

### 发布
- v0.2.0 Release（含 M7 文件检索提权 + M6 全部功能）。
