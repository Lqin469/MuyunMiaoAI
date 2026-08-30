# 02 · 技术选型与理由（含否决记录）

> 本文是**技术决策事实源**：为什么选 A 不选 B；换过什么；否决了什么。
> 变更规则：任何选型变化 / 否决新方案，**当回合**在本文追加一条记录（含日期、理由、替代方案否决原因）。

## 1. 技术选型总表（当前生效）

| 类别 | 选型 | 版本 | 用途 | 理由 |
|---|---|---|---|---|
| 语言/UI | Kotlin + Jetpack Compose（Material3） | Kotlin 2.1.20 / BOM 2026.05.00 | 全部 UI | 现代声明式 UI；官方维护 |
| DI | Hilt | 2.56 | 依赖注入 | 官方推荐；编译期校验 |
| 本地库 | Room（FTS5 预留）+ DataStore + EncryptedSharedPreferences | Room 2.7.x | 数据/偏好/密钥 | 官方；FTS5 全文检索 |
| 网络 | OkHttp | 5.1+ | SSE 流式对话 | 支持 SSE；超时/重试可控 |
| 后台 | WorkManager | 2.10.x | 入库/索引/记忆 | 系统级调度（当前未启用后台索引，受 R11 约束） |
| 本地推理 | **MNN 3.6.x 源码构建 AAR**（LLM/Vision） | 自编译 so | 本地引擎 | 支持 qwen3_5 新架构（linear attention）；Apache-2.0 |
| 提权 | Shizuku `dev.rikka.shizuku:api` + `:provider`；libsu | 13.1.5 / 5.2.2 | 文件检索 R11 | 三档能力 L0/L1/L2；参考 Operit 方案 |
| 压缩 | Apache Commons-Compress + junrar | 1.27.x / 7.x | R9 压缩包 | ZIP/TAR/7z + RAR4 覆盖 |
| 文档解析 | pdfbox-android（com.tom-roush）+ 自研 DOCX 提取 | 2.0.27.x | R8 文档投喂 | 纯本地解析 |
| 云端协议 | OpenAI 兼容（用户自配 baseUrl/Key/模型） | — | R1 | 不内置服务商；通用 |
| 开源协议 | 仓库 GPL-3.0；自研文件头 Apache-2.0 双注明 | — | 合规 | 引用 Operit 修改版 GPLv3 |

## 2. 关键决策记录（按时间）

### D-001 本地推理引擎选 MNN（2026-08）
- **选中**：MNN（alibaba）3.6.x，源码构建 LLM so + 自写 JNI 桥 `mnnllm_jni.cpp`；
- **理由**：支持 LLM 推理（含 `qwen3_5` 新架构 linear attention）；Apache-2.0 许可；安卓端性能好；
- **否决**：llama.cpp —— GGUF 生态强但需独立运行时（登记 M-028 后续）；TensorFlow Lite —— LLM 支持弱；ONNX Runtime —— 移动端 LLM 生态不成熟。

### D-002 提权方案参考 Operit（2026-08）
- **选中**：Shizuku（ADB 通道）+ libsu（root 通道）双栈，`PrivilegeManager` 三档能力；
- **理由**：用户强制要求参考 Operit 的 adb/root 提权方案；两库均为 Apache-2.0；
- **合规**：仓库整体 GPL-3.0，README 署名 Operit 来源；
- **坑**：Shizuku 13.1.5 无 `isSuiAvailable()`（14.x 才有）→ root 判定用 `Shell.isAppGrantedRoot()==true`。

### D-003 Markdown 渲染自研（2026-08，M-013）
- **选中**：自研 `MarkdownText.kt`（后随 UI 迁移删除，改纯文本气泡）；
- **否决**：mikepenz 0.43 需 compileSdk 37、0.39 需 Kotlin 2.3，均与项目冲突 → 自研；
- **后续**：M8.5 UI 迁移后按 HTML 原型改纯文本，自研组件删除。

### D-004 API Key 存储分层（2026-08）
- **选中**：`CloudConfigRepository` 用 `EncryptedSharedPreferences`（Keystore AES256_GCM）存当前引擎 apiKey；
- **遗留**：ApiManageScreen 多 API 列表经 DataStore JSON 存 key（明文）——**登记 🟡 待加固**（见 code-review-guide §2.2），方案 A 加密或方案 B 拆分。

### D-005 局域网传输协议自研（2026-08，M-027）
- **选中**：自研轻量协议（`QUERY/HAVE` 断点锚点 + `SEND <JSON>` + 二进制流，端口 21066）；
- **参考**：LocalSend（89.6k⭐）协议思想，但 Flutter 不可直接依赖 → 协议自研；
- **边界**：明文 TCP 仅限可信局域网（加密/TLS 登记后续）。

### D-006 会话记忆提炼（2026-08，M5）
- **选中**：`MemoryExtractor`（LLM 提炼 JSON：facts/preferences/todos）+ `MemoryStore` 落库，每 4 轮触发；
- **否决**：规则关键词提取（精度低）→ 用 LLM；本地无模型时降级。

### D-007 构建链固定 Gradle 8.13 + JDK 17（2026-08-30，环境重建）
- **选中**：Gradle 8.13（与 wrapper 一致）+ Temurin JDK 17.0.20.1（清华镜像）；
- **理由**：AGP 8.9.2 要求 JDK 17；wrapper 下载 SSL 失败 → 本机用 gradle.bat 直连；
- **坑**：官方源极慢 → 一律清华/腾讯镜像；`.lock` 跨进程在 WorkBuddy 会话内被拒 → 构建走外部终端（详见总流程日志 §2.6/§2.7）。

### D-008 正式签名 keystore（2026-08-31，M-036）
- **选中**：`D:\LQYMYH\keystores\yzqy.jks`（alias/password 均 `yzqy`，RSA 2048，有效期 999999 天）；
- **理由**：旧 debug keystore 随 admin 用户目录丢失 → 一次性卸载重装换正式签名，此后永不变；
- **配置**：`keystore.properties`（已被 .gitignore 忽略，**严禁提交 GitHub**）。

## 3. 已否决方案汇总

| 方案 | 否决原因 | 记录 |
|---|---|---|
| llama.cpp 运行时（GGUF 直跑） | 需集成第三方运行时，先登记后续（M-028） | D-001 |
| mikepenz Markdown 库 | compileSdk/Kotlin 版本冲突 | D-003 |
| material-icons-extended 图标库 | 体积大；改用 SVG path 自绘（AppIcons.kt，42 个图标） | M8.5 |
| Coil 图片加载库 | 免依赖；手写 `rememberBitmap`（采样解码） | M8.5 |
| LocalSend 直接集成 | Flutter 技术栈不可依赖 | D-005 |
| BottomNavigation（M2 命名） | Material3 用 `NavigationBar` | 总流程日志 §4.2 |
| ModelScope 应用内下载器 | 302 跳转/大文件易断 → 改为"复制下载地址" | 总流程日志 §4.2 |

## 4. 依赖版本锁定

见 `gradle/libs.versions.toml`（唯一事实源）。升级依赖后需在本文追加记录（理由 + 验证结果）。

---

*本文与 00-overview、01-architecture、03-contracts 共同构成参考文档层；决策背景看这里，接口细节看 03。*
