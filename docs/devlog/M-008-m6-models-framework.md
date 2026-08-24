# 开发记录：M6 本地引擎框架 — core:models + LocalChatEngine/OcrEngine 桩

- 日期：2026-08-24
- 涉及模块：core:models（新建）、core:ai:engine（加 LocalChatEngine 桩）、core:ingest（加 OcrEngine 接口 + MnnOcrEngine 桩）
- 关联需求：R2（模型下载/评估）、R3（本地导入）
- 关联文档：docs/03-contracts.md、docs/05-model-hardware.md

## 1. 目标与范围

**目标**
- `core:models` 模块骨架：ModelItem / ModelKind / ModelSource / HardwareProfile 数据类 + ModelRepository（硬编码推荐目录 + 硬件画像探测 + 红黄绿评级）+ ModelImporter（识别 MNN 目录 / GGUF 文件）；
- `core:ai:engine` 加 `LocalChatEngine` 桩（type=LOCAL，调用即返回"未集成 MNN"提示，避免静默失败）；
- `core:ingest` 加 `OcrEngine` 接口 + `MnnOcrEngine` 桩。

**真实 MNN 集成（需用户本地操作，本沙箱环境无法完成）**
- 用 Android Studio 拉 MNN 源码 → 按 `project/android/apps/MnnLlmChat/README.md` 的 cmake 命令构建 `MNN-LLM` AAR；
- 把 AAR 放到 `app/libs/mnn-llm.aar`，在 app/build.gradle.kts 加 `implementation(files("libs/mnn-llm.aar"))` + `kapt` 启用；
- 替换 `LocalChatEngine.streamChat` 桩：用 Flow 流式接收 MNN 回调（参考 `CloudApiClient` 的 callbackFlow 模式）。

## 2. 设计要点

- **数据类先行**：ModelItem 字段覆盖 v1 规划（id/name/kind/quant/source/sizeBytes/minRamMb/...），不依赖 Room（运行时内存数据，不持久化）；
- **硬件画像 Runtime 探测**（简化版）：`Runtime.maxMemory / freeMemory / availableProcessors + Build.SUPPORTED_ABIS[0]`；M6 完善时用 ActivityManager.MemoryInfo；
- **红黄绿评级**：`canRun(item, hw)` —— 可用内存达最低一半 + 存储满足 + 4 核 → OK / WARN / BLOCKED（UI 染色）；
- **桩模式**：MNN 类桩不返回空字符串，而是返回明确"未集成"提示 + 集成步骤，**避免静默失败**（用户能立刻看到原因）；
- **OcrEngine 接口化**：core:ingest 不再写死 PaddleOCR，未来可换 ML Kit / Tesseract，调用方零改动。

## 3. 接口契约

```kotlin
// core:models
enum class ModelKind { LLM, EMBEDDING, OCR, VISION_LLM }
enum class ModelSource { CATALOG, LOCAL_IMPORT }
data class ModelItem(id, name, kind, quant, source, downloadUrl?, sha256?, sizeBytes, minRamMb, minStorageMb, cpuNote, gpuNote)
data class HardwareProfile(totalRamMb, availRamMb, totalStorageMb, cpuCores, abi, gpu)
class ModelRepository { val catalog: List<ModelItem>; fun probeHardware(): HardwareProfile; fun canRun(item, hw): RunStatus }
class ModelImporter { fun importFromPath(path: String): ModelItem? }   // 识别 MNN_DIR / GGUF

// core:ingest
interface OcrEngine { suspend fun recognize(file: File): String }
class MnnOcrEngine : OcrEngine  // 桩

// core:ai:engine
class LocalChatEngine : ChatEngine  // 桩，type=LOCAL
```

docs/03-contracts.md 的预留表 `ModelRepository / ModelImporter / HardwareProfile` 标注 M6 已定（已加 model 仓库契约段）。

## 4. 关键实现

新增文件（逐行中文注释）：

```
core/models/...        ModelItem.kt（数据类 + 枚举）、ModelRepository.kt（目录+硬件探测+评级）、ModelImporter.kt（MNN/GGUF 识别）
core/ai/engine/...      LocalChatEngine.kt（MNN 桩）
core/ingest/...         OcrEngine.kt（接口）、MnnOcrEngine.kt（MNN 桩）
core/models/build.gradle.kts（+hilt+kapt 插件）、app/build.gradle.kts（+core:models）
```

可复现命令：`gradlew :app:assembleDebug`。

## 5. 测试与验证

- [ ] `gradlew :app:assembleDebug` 编译通过（需用户 AS/CI 验证）；
- [ ] ModelRepository.catalog 返回 5 个推荐模型（Qwen3-0.6B/1.7B、DeepSeek-R1-1.5B、bge-small-zh、PaddleOCR-VL）；
- [ ] ModelRepository.canRun 在 6GB 设备上对 Qwen3-0.6B 返回 OK、对 Qwen3-4B 返回 WARN；
- [ ] LocalChatEngine.streamChat 立即返回"未集成 MNN"提示事件（不崩溃）；
- [ ] OcrEngine.recognize 返回占位文本（不崩溃）。

**已知问题**：本环境无法编译/运行 MNN（需 NDK + cmake + MNN 源码）。桩实现保证架构正确，真实推理待用户本地 AS 构建。

## 6. 接手指引

下一步（仍是 M6）：
1. MNN AAR 本地构建（app/libs/mnn-llm.aar）+ 真实现 LocalChatEngine.streamChat（JNI 桥接）；
2. 换 SimpleHashEmbeddingProvider → BgeEmbeddingProvider（MNN bge 模型推理）；
3. 增强 ModelRepository：ModelItem 持久化（Room 增 model 表，记录已下载/已导入的本地路径）、下载断点续传 + sha256 校验、硬件画像用 ActivityManager.MemoryInfo 完善；
4. devlog M-009（GUI：模型管理页 UI + 模型选择持久化）。

踩坑点：
- 真实 MNN 集成在沙箱无法做，桩保证架构但无推理结果，CI 只验证编译不验证推理；
- 切本地引擎前必须保证模型已下载（UI 应阻止"未下载就切"），否则 MNN 加载失败；
- `@Inject constructor()`（无参）容易被遗漏，Hilt @Binds 才会要（Hilt 编译期检查）。

## 7. 增量（同日第二轮）：CI 修复 + 投喂路由接线

**CI #17 报错与修复**
- 报错：`LocalChatEngine.kt:22 'type' overrides nothing` / `:24 'streamChat' overrides nothing`；
- 根因：类声明漏写 `: ChatEngine`（`class LocalChatEngine @Inject constructor() {`），接口未实现导致 override 失效；
- 修复：补 `: ChatEngine`（commit `17157ba`，CI `32680341265` success）。

**R8/R9/R10 完整投喂路由（KnowledgeRepository.ingestFile）**
- 压缩包（zip/tar/gz/tgz/bz2/tbz2/xz/txz）→ `ArchiveExtractor.extract` 解压到 `indexDir/extract/<name>-<size>/`，逐文件路由入库；嵌套压缩包**不递归**（防连环炸弹），仅位置记录；
- 图片（png/jpg/jpeg/webp/bmp/gif）→ `OcrEngine.recognize`（M6 桩返回占位文本）；
- 文本/PDF/DOCX → `DocumentParser.parse`；
- 7z/RAR 及一切不可解析（含解压失败、条目不可解析）→ `FileLocationDao.upsert` 位置记录（R10 兜底，仅元数据不存内容）；
- 解压临时产物入库后 `deleteRecursively()` 清理（分块已在 DB）；
- 新增 `IngestModule`（@Binds：OcrEngine → MnnOcrEngine）。

**bge 嵌入桩（core:ai:embed/MnnEmbeddingProvider）**
- 实现 EmbeddingProvider，dim=512（bge-small-zh）；embed() 抛 IllegalStateException 快速失败；
- 未注册进 EmbedModule（默认仍是 SimpleHashEmbeddingProvider），MNN 落地后改 @Binds 即切换；
- 注意：换 bge 后维度 256→512，旧分块向量需重建（索引重建策略 M6 后续补）。

**依赖变更**：core:ingest 新增 `implementation(project(":core:storage"))`（解压临时目录用 StorageProvider.indexDir）。
