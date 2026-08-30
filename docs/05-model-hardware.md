# 05 · 模型清单与硬件评估（R2/R3）

> 模型一律**按需下载**（ModelScope 为主）或**本地导入**，APK 不内置大模型。
> 硬件评估公式：`权重 ≈ 参数量 × bits/8`；`KV Cache ≈ 2 × layers × hidden × ctxLen × 2B`；`总需求 ≈ 权重 + KV + 1.5GB 系统开销`。
> UI 在模型详情页展示「本机内存 vs 建议内存」红黄绿档位（运行时用 ActivityManager.memoryClass + MemInfo 探测）。

## 推荐模型档位表

| 用途 | 模型 | 量化 | 权重 | 建议内存 | 存储 | 说明 |
|---|---|---|---|---|---|---|
| 轻量对话 | Qwen3-0.6B | Q4_0 | ~0.5GB | ≥4GB | 1GB | 低配机兜底 |
| 通用对话（默认档） | Qwen3-1.7B | Q4_0 | ~1.1GB | ≥6GB | 2GB | 推荐 |
| 推理型 | DeepSeek-R1-Distill-Qwen-1.5B | Q4_0 | ~1.0GB | ≥6GB | 2GB | MNN Chat 官方支持 |
| 高性能对话 | Qwen3-4B | Q4_0 | ~2.5GB | ≥8GB | 4GB | 需旗舰机 |
| 视觉问答 | Qwen2.5-VL-3B | Q4_0 | ~2.2GB | ≥8GB | 4GB | 需 LLM_SUPPORT_VISION |
| 本地嵌入（RAG） | bge-small-zh-v1.5 | fp16 | ~0.13GB | ≥2GB | 0.2GB | 默认本地嵌入 |
| 本地 OCR | ch_PP-OCRv4_mobile | fp16 | ~0.01GB | ≥2GB | 0.05GB | MNN-PaddleOCR |

## 实际已验证模型（本机，2026-08 起）

| 模型 | 来源 | 规格 | 状态 |
|---|---|---|---|
| **Qwen3.5-0.8B-MNN** | ModelScope `MNN/Qwen3.5-0.8B-MNN`（HF 缓存目录 `models--MNN--Qwen3.5-0.8B-MNN/snapshots/_no_sha_/`） | 权重 449MB / 总约 523MB；`is_visual=true`（多模态，需 visual.mnn） | ✅ 可被 LocalChatEngine 加载 |
| **Qwen3.5-2B-MNN** | ModelScope `MNN/Qwen3.5-2B-MNN`（平铺目录） | 权重 1.1GB；`qwen3_5` 架构（linear attention，**需新版 libMNN/libllm**）；`is_visual=true` | ✅ 可加载（内存约 2.5GB，速度约慢 2.5 倍） |

- 通用结论：MNN 模型目录 = `config.json` 所在目录；`is_visual=true` 时 createLLM 走 Omni 额外加载 `visual.mnn`，缺则 load 失败；
- 模型存放地址（用户指定）：`D:\LQYMYH\ai模型`；
- 兼容性判定：`ModelRepository.probeHardware()` 用真实物理内存（`ActivityManager.totalMem`），模型体积 > 0.7×RAM 判红；非 arm64 才提示 fp16 不支持。

## 推荐模型档位表（规划值，供参考）

| 用途 | 模型 | 量化 | 权重 | 建议内存 | 存储 | 说明 |
|---|---|---|---|---|---|---|
| 轻量对话 | Qwen3-0.6B | Q4_0 | ~0.5GB | ≥4GB | 1GB | 低配机兜底 |
| 通用对话（默认档） | Qwen3-1.7B | Q4_0 | ~1.1GB | ≥6GB | 2GB | 推荐 |
| 推理型 | DeepSeek-R1-Distill-Qwen-1.5B | Q4_0 | ~1.0GB | ≥6GB | 2GB | MNN Chat 官方支持 |
| 高性能对话 | Qwen3-4B | Q4_0 | ~2.5GB | ≥8GB | 4GB | 需旗舰机 |
| 视觉问答 | Qwen2.5-VL-3B | Q4_0 | ~2.2GB | ≥8GB | 4GB | 需 LLM_SUPPORT_VISION |
| 本地嵌入（RAG） | bge-small-zh-v1.5 | fp16 | ~0.13GB | ≥2GB | 0.2GB | 默认本地嵌入（当前降级 SimpleHash，待导出） |
| 本地 OCR | ch_PP-OCRv4_mobile | fp16 | ~0.01GB | ≥2GB | 0.05GB | MNN-PaddleOCR（当前为桩） |

## 模型来源

- 一律**按需下载**（ModelScope 为主）或**本地导入**，APK 不内置大模型；
- 应用内不再做下载器（302 跳转/大文件易断），改为「复制下载地址」引导用户浏览器下载（M-013 决策）；
- GGUF 导入已落地（modelsDir()/gguf/），运行需 llama.cpp 运行时（登记 M-028）。

| 来源 | 用途 | 说明 |
|---|---|---|
| ModelScope（默认） | 官方目录下载 | 国内直连，支持断点续传 + sha256 |
| 本地导入（R3） | 用户已有模型 | MNN 目录直接注册；GGUF 给转换指引（可选二期接 llama.cpp） |
| 远端目录 JSON | 模型列表 | 内置 + 可更新 |

## 硬件评估契约（M6 实现）

```kotlin
data class HardwareProfile(
    val minRamMb: Int,
    val minStorageMb: Int,
    val cpuNote: String,     // 如 "8 核以上，支持 fp16"
    val gpuNote: String,     // 如 "Vulkan/OpenCL 加速（可选）"
)
// ModelItem 内嵌；UI 显示 绿(满足)/黄(临界)/红(不满足)
```
