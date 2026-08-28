# 核心功能实现方案（设备自检 / 高级权限 / 局域网迁移 / 云端 API / 本地大模型）

> 文档编号：docs/core-features-implementation-plan.md
> 日期：2026-08-28
> 对应需求：设备自检、高级权限、局域网数据迁移、云端 API 集成、本地大模型、开源方案调研
> 状态：设计已定稿，实现进行中

## 1. 现状差距分析

| 功能 | 现状 | 差距 |
|---|---|---|
| 1. 设备自检 | DeviceCheckScreen 有 4 项真实检测（内存/ABI/CPU 描述/存储） | 缺设备型号、系统版本、CPU 核心数主频、权限状态；无「不满足项」判定（任何设备都显示满足） |
| 2. 高级权限 | core:search PrivilegeManager 已有 Shizuku + libsu 三档真检测 | 界面 ADB/ROOT 按钮只写偏好字符串，未接入 PrivilegeManager；无无线 ADB 调试引导 |
| 3. 局域网迁移 | LanTransferDialog / MigrateScreen 均为原型级模拟（随机设备、delay 进度） | 无真实设备发现（NSD）、无 Socket 传输、无断点续传、无真实进度 |
| 4. 云端 API | CloudApiClient 已实现 OpenAI 兼容 SSE 流式对话 | 无重试机制、无超时控制、错误分类缺失；无数据同步/备份接口 |
| 5. 本地大模型 | MNN 导入/加载已实现（LocalChatEngine 真推理） | GGUF 仅识别元信息不能运行；无运行状态监控 |
| 6. 开源调研 | 无 | 本次完成（见第 4 节选型表） |

## 2. 模块划分与项目结构

```
MuyunMiaoAI/
├── app/                                # 应用壳（装配）
├── core/
│   ├── ui/                             # 共享 UI（已有）
│   ├── storage/                        # 存储抽象（已有）
│   ├── db/                             # Room 数据库（已有）
│   ├── device/                         # ★ 新增：设备信息检测 + 满足度判定
│   ├── lan/                            # ★ 新增：局域网设备发现 + 传输（断点续传）
│   ├── search/                         # 已有：PrivilegeManager（Shizuku/libsu 提权）
│   ├── ai/
│   │   ├── engine/                     # 修改：CloudApiClient 加重试/超时
│   │   ├── embed/ memory/ tools/       # 已有
│   ├── models/                         # 修改：GGUF 导入 + 运行状态监控
│   └── ingest/                         # 已有
├── feature/
│   ├── settings/                       # 修改：自检页/权限页/迁移页接入真实能力
│   ├── notes/ chat/ knowledge/         # 已有
└── docs/                               # 本文档 + devlog 归档
```

### 2.1 core:device（新模块）

职责：纯硬件/系统信息检测与满足度判定，无 UI、无外部依赖（仅 Android Framework API）。

| 组件 | 职责 |
|---|---|
| `DeviceInfoProvider` | 检测：型号（Build.MANUFACTURER+MODEL）、系统版本（RELEASE/SDK_INT）、CPU（核心数 availableProcessors、主频读 /sys/devices/system/cpu、ABI 列表）、内存（MemoryInfo）、存储（StatFs） |
| `CheckResult` / `CheckLevel` | 单项目检测结果：PASS（满足）/ WARN（可用但受限）/ FAIL（不满足）+ 实测值 + 阈值 + 说明 |
| `CapabilityChecker` | 满足度判定规则：64 位架构必需（MNN 库仅 arm64-v8a）；RAM ≥ 3GB 可运行端侧模型（<3GB 仅云端）；空闲存储 ≥ 1GB（模型导入需 1GB+） |

### 2.2 core:lan（新模块）

职责：局域网设备发现与文件传输（含断点续传），全部走接口 + 状态流，UI 在 feature:settings。

| 组件 | 职责 |
|---|---|
| `LanAdvertiser` | NsdManager 注册本机服务 `_muyunmiao._tcp`（含设备名/端口/IP） |
| `LanScanner` | NsdManager 发现同网段设备，解析 IP/端口/设备名，StateFlow 暴露在线列表 |
| `TransferServer` | ServerSocket 监听：接收 `SEND`/`QUERY` 帧；QUERY 返回已有字节数（断点续传锚点）；接收数据 RandomAccessFile 写入，进度 StateFlow |
| `TransferClient` | 连接目标：先 QUERY 断点 offset，再 SEND 数据；逐块发送，实时进度/速度回调 |
| `TransferSession` | 会话状态模型：IDLE/RUNNING/PAUSED/SUCCESS/FAILED + 字节进度 + 速度 + 文件信息 |
| `TransferRepository` | 断点持久化（DataStore 存 fileId → 已传字节），暂停/恢复/取消控制 |

协议（文本头 + 二进制流，参考 LocalSend 的轻量设计）：

```
请求帧: QUERY <fileId>\n                     → 响应: HAVE <bytes>\n | NONE\n
请求帧: SEND <fileId> <name> <size> <offset>\n + <二进制数据流>
响应:   OK <received>\n                      （服务端校验后回执）
```

### 2.3 模块依赖关系

```
app
 └─ feature:settings ──┬─→ core:device（新增，硬件检测）
                        ├─→ core:lan（新增，局域网传输）
                        ├─→ core:search（已有，PrivilegeManager 提权检测/授权）
                        ├─→ core:ai:engine（增强，云端 API + 重试）
                        ├─→ core:models（增强，GGUF 导入 + 运行监控）
                        └─→ core:storage / core:ui / core:db / core:ingest（已有）
core:lan ──→ core:storage（目标目录提供）
core:device ──→（无依赖，纯 Framework API）
```

依赖方向保持「feature → core → 基础设施」，不反向。

## 3. 功能实现设计

### 3.1 设备自检（需求 1）

- 检测项扩展为 8 项：设备型号、系统版本、64 位架构、CPU 核心/指令、运行内存、可用存储、Shizuku 权限状态、ROOT 状态。
- 每项展示：实测值 + PASS/WARN/FAIL 徽章；FAIL 项红底 + 不满足原因说明。
- 「可运行」提示文案根据判定动态生成：端侧可用 / 仅云端可用 / 存储不足需清理。
- 权限状态项实时订阅 PrivilegeManager.level（服务上下线自动刷新）。

### 3.2 高级权限（需求 2）

- ADB 行：接入 `PrivilegeManager.requestAdbPermission()`（弹 Shizuku 授权页）；未装 Shizuku 时弹引导弹窗（下载链接 + 无线调试开启步骤：设置 → 开发者选项 → 无线调试 → 配对码）。
- ROOT 行：接入 `PrivilegeManager.currentLevel()`（Shizuku 在线 + `Shell.isAppGrantedRoot()==true` 判定）；无 root 时给出 Sui 安装引导。
- 撤销：ADB 可关 Shizuku 服务（引导），root 撤销走 Sui 应用管理；应用侧提供一键清除授权偏好。

### 3.3 局域网数据传输迁移（需求 3）

- 真实设备发现：NSD 注册 + 扫描（替代随机设备池）。
- 真实传输：文件/文本经 TransferServer/TransferClient 走 TCP；迁移数据先打包为应用数据包（zip + SHA-256 清单）再传输。
- 断点续传：发送前 QUERY 已传字节 → 从 offset 续传；接收端持久化进度，断网/取消后恢复。
- 进度展示：字节进度 + 实时速度（KB/s）+ 暂停/恢复/取消；完成后 SHA-256 校验 + 一致性报告。

### 3.4 云端 API 集成（需求 4）

- CloudApiClient 增加：连接/读写超时（10s/120s）、错误分类（401/403/404/429 不重试；5xx/网络异常指数退避重试最多 3 次：1s→2s→4s + 抖动）。
- 错误文案归一化：超时 / 认证失败 / 限流 / 服务端错误 / 网络不可用。
- 预留数据同步接口（`SyncApi` 骨架：push/pull 快照、备份上传，后续里程碑实现，保持隐私红线「仅发文件名/路径」）。

### 3.5 本地大模型（需求 5）

- MNN：已有导入/加载/推理，本次增加「运行状态监控」：`ModelRuntimeState`（IDLE/LOADING/RUNNING/ERROR + 耗时 + 首 token 延迟），LocalChatEngine 加载与推理过程写入状态流，ModelManageScreen 实时展示。
- GGUF：导入完整落地（复制到 modelsDir()/gguf/ + SHA-256 + 大小校验 + 去重）；运行依赖 llama.cpp 运行时（本次登记为后续里程碑，界面标注「预览支持，运行需 llama.cpp 运行时」）。

## 4. 开源方案调研（需求 6）

调研日期 2026-08-28，数据来自 GitHub Search API（gh CLI）。

| 功能 | 选型 | Stars | License | 说明 |
|---|---|---|---|---|
| ADB/Root 提权 | [RikkaApps/Shizuku](https://github.com/RikkaApps/Shizuku) + [RikkaApps/Sui](https://github.com/RikkaApps/Sui) + [topjohnwu/libsu](https://github.com/topjohnwu/libsu) | 29.4k / 4.2k / 2.1k | Apache-2.0 / GPL-3.0 / Apache-2.0 | 已集成（core:search），本次接入界面 |
| 局域网传输 | [localsend/localsend](https://github.com/localsend/localsend)（协议参考）+ Android 原生 NsdManager/Socket 自研 | 89.6k | MIT | LocalSend 为 Flutter 不可直接依赖；其 [protocol](https://github.com/localsend/protocol)（568⭐）为轻量协议设计参考 |
| 局域网传输备选 | [KDE/kdeconnect-android](https://github.com/KDE/kdeconnect-android) | 1.4k | GPL-2.0 | 参考设备互联交互，协议复杂不引入 |
| 本地推理（MNN） | [alibaba/MNN](https://github.com/alibaba/MNN) + [wangzhaode/mnn-llm](https://github.com/wangzhaode/mnn-llm) | 16.0k / 1.6k | Apache-2.0 | 已源码构建 AAR 并集成，维护活跃（MNN 今日仍有更新） |
| 本地推理（GGUF） | [ggml-org/llama.cpp](https://github.com/ggml-org/llama.cpp)（官方 llama.android 示例）、[nerve-sparks/iris_android](https://github.com/nerve-sparks/iris_android)（290⭐）、[jegly/OfflineLLM](https://github.com/jegly/OfflineLLM)（223⭐） | 126k | MIT | llama.cpp 运行时为 GGUF 唯一成熟方案，本次登记后续里程碑 |
| 云端 API | 现有自研 SSE 客户端增强；[aallam/openai-kotlin](https://github.com/aallam/openai-kotlin) 为参考实现 | 1.8k | MIT | 自研已满足（SSE 流式 + 隐私红线），仅补重试/超时 |
| 数据备份 | [seedvault-app/seedvault](https://github.com/seedvault-app/seedvault) | 1.8k | Apache-2.0 | Android 官方开源备份方案，备份设计思想参考（zip + 加密 + 校验） |
| 设备信息参考 | [sidAndroid01/DeviceInfoLibrary](https://github.com/sidAndroid01/DeviceInfoLibrary) | 48 | MIT | 字段组织参考，实现用原生 API（零依赖） |

选型原则：优先已集成项目（Shizuku/MNN）> 原生 API 自研（NSD/Socket/设备信息）> 引入轻依赖（无新增）；协议设计参考大社区方案（LocalSend/seedvault）避免自创协议踩坑。

## 5. 实现计划

| 步骤 | 内容 | 验证 |
|---|---|---|
| S1 | core:device 模块（检测 + 判定） | compileDebugKotlin 通过 |
| S2 | core:lan 模块（发现 + 传输 + 断点续传） | compileDebugKotlin 通过 |
| S3 | DeviceCheckScreen/PermissionScreen 接入真检测 + 引导弹窗 | 编译 + 界面逻辑走查 |
| S4 | LanTransferDialog/MigrateScreen 接入 core:lan | 编译通过 |
| S5 | CloudApiClient 重试/超时/错误分类 | 编译通过 |
| S6 | core:models GGUF 导入落地 + 运行状态监控 | 编译通过 |
| S7 | 全量 assembleDebug + 归档 devlog + 总流程日志 | APK 产出 |

## 6. 风险与边界

- 无线 ADB 引导仅提供步骤说明（Android 限制第三方应用无法直接开关无线调试）。
- GGUF 推理需要 llama.cpp 运行时（NDK 编译 + JNI），工作量大，登记为下一里程碑（M-025），本次完成「导入 + 管理 + 监控」。
- 局域网传输安全：明文 TCP 仅限可信局域网；后续可加预共享密钥 + TLS（参考 LocalSend HTTPS 模式）。
- 断点续传元数据存应用私有目录（DataStore），卸载即失，符合「迁移不跨设备重置」场景。
