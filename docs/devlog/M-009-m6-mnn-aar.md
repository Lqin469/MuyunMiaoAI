# 开发记录：M6 本地引擎 — MNN-LLM AAR 构建 + LocalChatEngine 真实现

- 日期：2026-08-24
- 涉及模块：app（jniLibs + cpp JNI 桥）、core:ai:engine（MnnLlmNative + LocalChatEngine 真实现）
- 关联需求：R1（本地对话引擎）、R2（模型下载/评估）、R3（本地导入）
- 关联文档：docs/03-contracts.md、docs/05-model-hardware.md

## 1. 目标与成果

**本机真实构建出 MNN-LLM 本地推理库，并接入项目，LocalChatEngine 从桩升级为真实现。**

| 产物 | 位置 | 大小（strip 后） |
|---|---|---|
| libMNN.so | app/src/main/jniLibs/arm64-v8a/ | 2.8M |
| libMNN_Express.so | 同上 | 747K |
| libMNN_CL.so（OpenCL 后端） | 同上 | 2.3M |
| libllm.so（LLM 引擎） | 同上 | 1.8M |
| libmnnllm_jni.so（JNI 桥） | 同上 | 55K |

合计约 7.7M（未 strip 前 169M）。

## 2. 构建环境与命令（本机 Windows，可复现）

**工具链（已装齐）**
- JDK 17：`C:\Users\admin\.workbuddy\binaries\java\jdk17`（Temurin 17.0.20）
- NDK 27：`C:\Users\admin\AppData\Local\Android\Sdk\ndk\27.0.12077973`
- cmake 3.22.1 + ninja：`...\Android\Sdk\cmake\3.22.1\bin\`
- android-36 platform（项目 compileSdk=36）

**编译 MNN-LLM 库（arm64-v8a）**
```bash
git clone --depth 1 https://github.com/alibaba/MNN.git
cd MNN && mkdir build_android && cd build_android
cmake.exe .. -G Ninja \
  -DCMAKE_TOOLCHAIN_FILE="$NDK/build/cmake/android.toolchain.cmake" \
  -DCMAKE_BUILD_TYPE=Release -DANDROID_ABI=arm64-v8a \
  -DANDROID_PLATFORM=android-24 -DANDROID_STL=c++_static \
  -DMNN_BUILD_LLM=ON -DMNN_LOW_MEMORY=ON -DMNN_USE_LOGCAT=OFF \
  -DMNN_BUILD_BENCHMARK=OFF -DMNN_BUILD_TEST=OFF -DMNN_BUILD_DIFFUSION=OFF \
  -DMNN_BUILD_OPENCV=OFF -DMNN_OPENCL=ON -DMNN_ARM82=ON \
  -DMNN_SUPPORT_TRANSFORMER_FUSE=ON
ninja -j8
```
产物在 `build_android/`：`OFF/arm64-v8a/libMNN.so`、`OFF/arm64-v8a/libllm.so`、
`express/OFF/arm64-v8a/libMNN_Express.so`、`source/backend/opencl/OFF/arm64-v8a/libMNN_CL.so`。
（SEP_BUILD 官方默认 ON，故拆成多 so；configure 会自动下载 ARM KleidiAI 矩阵加速依赖。）

**剥离符号**（169M → 7.7M）
```bash
llvm-strip.exe --strip-unneeded *.so
```

**编译 JNI 桥 libmnnllm_jni.so**（源码 app/src/main/cpp/mnnllm_jni.cpp）
```bash
aarch64-linux-android24-clang++ -shared -fPIC -std=c++17 -fexceptions -frtti -O2 \
  -I MNN/transformers/llm/engine/include -I MNN/include \
  mnnllm_jni.cpp -o libmnnllm_jni.so \
  libllm.so libMNN_CL.so libMNN_Express.so libMNN.so -llog -landroid
```

## 3. 关键实现

- **JNI 桥（app/src/main/cpp/mnnllm_jni.cpp）**：`nativeInit`（createLLM+load，返回原生指针）/ `nativeResponse`（response 流式生成，经自定义 `CallbackStreamBuf` 逐段回调 Java `onDelta`）/ `nativeRelease`。
- **Kotlin 桥（MnnLlmNative.kt）**：按依赖顺序 `System.loadLibrary`（MNN → MNN_Express → MNN_CL → llm → mnnllm_jni）+ `external` 声明。
- **LocalChatEngine.kt（真实现）**：注入 `StorageProvider`，懒加载 `modelsDir()/llm/`（含 config.json），`callbackFlow` 把 JNI 回调转成 `ChatEvent` 流；模型未就绪时返回明确提示（不崩溃）。

## 4. 依赖加载顺序（不可乱）

```
libMNN.so（核心，无依赖）
  → libMNN_Express.so（依赖 MNN）
  → libMNN_CL.so（OpenCL，依赖 MNN）
  → libllm.so（依赖 MNN/MNN_Express/MNN_CL）
  → libmnnllm_jni.so（本桥）
```

## 5. 集成与 CI 策略

- 5 个 .so 提交到 `app/src/main/jniLibs/arm64-v8a/`，AGP 自动打包进 APK，**CI 无需装 NDK**（so 为预编译产物）。
- JNI 桥 cpp 源码保留在 `app/src/main/cpp/` 作存档（不参与 CI 编译）。

## 6. 遗留与限制

1. **模型文件缺失**：本地引擎要跑起来，还需用 `llmexport`（MNN 的 transformers/llm/export）把 Qwen 等模型导出为 `.mnn` + `config.json` + `tokenizer`，放到 `modelsDir()/llm/`。导出需 python+torch+onnx+模型权重（数 GB），未在本机执行。
2. **仅 arm64-v8a**：当前只编了 arm64 ABI，32 位（armeabi-v7a）设备不支持。后续需补编。
3. **引擎切换 UI**：ChatEngine 默认仍绑定云端，LocalChatEngine 真实现已就绪但需模型管理页（M-010）提供"导入模型 + 切换本地引擎"入口。
4. **bge 嵌入**：MNN 自带 `Embedding::createEmbedding`，MnnEmbeddingProvider 可从桩升级为真实现（后续）。

## 7. 踩坑记录

- Windows 下 `curl` 下载需加 `--ssl-no-revoke`（schannel 吊销检查报 `CRYPT_E_NO_REVOCATION_CHECK`）。
- `sdkmanager.bat` 需 JDK 17 且 `JAVA_HOME` 必须是 Windows 路径格式（Git Bash 的 `/c/...` 不被 .bat 识别）。
- Git Bash 调 cmake.exe：用 Unix 路径调用 exe，但**传给 cmake 的参数路径要用 Windows 格式**（`C:/...`），否则 toolchain 找不到。
- NDK clang++ 在 Git Bash 可直接调无扩展名脚本（`aarch64-linux-android24-clang++`），但**源码/头文件路径必须用 Windows 格式**（`D:/...`）。
- `llm.hpp` 的 include 是 `<llm/llm.hpp>`（在 `include/llm/` 子目录），不是 `<llm.hpp>`。
- 链接 JNI 桥时 `-l`/`-L` 在 MSYS 下路径查找失败，**改用完整 .so 路径直接链接**最稳。
