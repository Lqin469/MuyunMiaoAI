# M-027 核心功能真实现（设备自检 / 高级权限 / 局域网迁移 / 云端 API / 本地模型监控）

> 日期：2026-08-28
> 关联：docs/core-features-implementation-plan.md（实现方案）
> 状态：完成，待提交

## 背景

HTML 原型迁移（M8.5）后，设备自检、局域网传输、数据迁移等界面为「原型级模拟」：
随机设备列表、假进度条、权限按钮只写偏好不检测。本里程碑按实现方案补齐真实能力，
对应用户 6 项核心功能需求。

## 变更清单

### 新增模块

| 模块 | 文件 | 职责 |
|---|---|---|
| core:device | DeviceInfoProvider.kt | 真实检测：型号/系统/CPU（核心数+主频读 sysfs）/内存/存储（StatFs） |
| core:device | CapabilityChecker.kt | 满足度判定（PASS/WARN/FAIL 规则与阈值集中管理） |
| core:lan | LanProtocol.kt | 协议常量（`_muyunmiao._tcp` 端口 21066）+ 数据模型 + fileId 生成 |
| core:lan | LanAdvertiser.kt | NSD 注册本机服务（TXT 携带协议版本） |
| core:lan | LanScanner.kt | NSD 发现设备（多播锁 + 逐台解析 IP/端口） |
| core:lan | TransferServer.kt | 接收服务：QUERY 断点锚点 + SEND 续写 .part + 完整性校验改名 |
| core:lan | TransferClient.kt | 发送客户端：QUERY→断点续传→流式发送→速度采样 |
| core:lan | TransferRepository.kt | 编排层：设备流/会话流/暂停恢复（cancel=.part 保留） |

### 修改模块

| 模块 | 变更 |
|---|---|
| feature:settings DeviceCheckScreen | 8 项检测（硬件 6 + 权限 2）；等级徽章（满足/受限/不满足 + 原因说明）；ADB/ROOT 按钮接 PrivilegeManager 真授权；无线 ADB 引导弹窗（5 步骤 + 开发者选项/官网跳转）；Sui 引导弹窗 |
| feature:settings LanTransferDialog | NSD 真实设备列表；SAF 真实文件选择；TCP 真实传输 + 进度/速度/暂停恢复；文本经临时文件发送 |
| feature:settings MigrateScreen | 真实 NSD 扫描；选中分类打包 zip + SHA-256 manifest；真实 TCP 发送；进度/暂停/断点续传接 core:lan |
| core:ai:engine CloudApiClient | 错误分类（4xx 不重试/5xx+网络重试）；指数退避重试 3 次（1s→2s→4s+抖动）；SSE 流保护（已产出不重发防重复）；错误文案归一化 |
| core:ai:engine EngineModule | OkHttp 超时（连接 10s/读 120s/写 30s）；关闭底层自动重试（由 CloudApiClient 显式控制） |
| core:ai:engine EngineRuntimeMonitor（新增） | 本地模型运行状态中枢（LOADING/RUNNING/ERROR + 加载耗时/首 token/token 计数） |
| core:ai:engine LocalChatEngine | 挂接监控器：加载/推理/错误/释放全链路状态上报 |
| core:models ModelImporter | GGUF 导入落地（SAF + 本地路径两通道，复制到 modelsDir()/gguf/ + 大小校验） |
| feature:settings ModelManageScreen | 运行状态监控卡（阶段徽章 + 模型名 + 统计 + 错误） |

### 开源选型（调研结论，详见方案文档第 4 节）

Shizuku 29.4k⭐ + libsu 2.1k⭐（已集成）；MNN 16k⭐（已构建 AAR）；
LocalSend 89.6k⭐ 协议参考（Flutter 不可直接依赖）；llama.cpp 126k⭐（GGUF 运行时，登记后续里程碑）；
seedvault 1.8k⭐ 备份思想参考；openai-kotlin 1.8k⭐ 为自研 SSE 客户端的参考实现。

## 风险与边界

- GGUF 推理需 llama.cpp 运行时（NDK 编译），登记为 M-025，本轮完成导入+管理+监控。
- 局域网明文 TCP 仅限可信局域网；后续里程碑加预共享密钥/TLS（参考 LocalSend HTTPS 模式）。
- 「手动接收」模式 UI 已留，接收确认交互后续版本提供（当前自动接收）。
- 无线 ADB 无法由第三方应用直接开关，引导弹窗提供步骤说明与跳转。
