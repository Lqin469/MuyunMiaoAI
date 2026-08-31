# 开发记录：引擎切换优化 — 长按配置 + 智能输入 + 本地模型选择（M-035）

- 日期：2026-08-28 ｜ 作者：MuyunMiao Dev ｜ 涉及模块：app（MainTopBar/MainActivity）、feature:settings（ApiManageScreen、新增 LocalModelSelectScreen）、core:models（ModelImporter）、feature:settings（ExtPrefs）｜ 关联需求：R1（API 自配）、R2/R3（模型选择与导入）

## 1. 目标与范围

优化右上角「本地/云端」引擎切换按钮的交互，补齐本地模型选择与 API 智能输入两个入口。

- **做**：胶囊按钮短按切换引擎、长按进对应配置页；新增本地模型选择页；API 添加「智能输入」自动解析粘贴文本。
- **不做**：本地模型切换后底层引擎的实际重载逻辑（登记为后续待办）。

## 2. 设计要点

- **手势区分**：切换胶囊 `clickable` → `combinedClickable(onClick=短按切换, onLongClick=长按配置)`；`MainActivity` 长按回调按 `engineType` 路由——本地 → `local-model-select`、云端 → `api`。
- **本地模型选择页**：`ModelImporter.listLocalModels()` 扫 `modelsDir()/llm`（MNN 目录）与 gguf 目录（.gguf 文件）；MNN `runnable=true`、GGUF `runnable=false`（需 llama.cpp 运行时，尚未集成）。选中模型 id 存 `ExtPrefs.localModelId`。
- **智能输入**：用正则提取 URL（`https?://...`）、密钥（`sk-...`/`key=xxx`/`Bearer token`）、模型（`model=xxx`）、名称（域名推断），降低手动填写门槛。

## 3. 接口契约

- `core:models` `ModelImporter.listLocalModels(): List<LocalModelInfo>`（新增）
- `LocalModelInfo` 数据类（新增：模型信息 + runnable 标记）
- `feature:settings` `ExtPrefs.localModelId: Flow<String>`（默认 `"mnn-llm"`）+ `setLocalModelId(id)`（新增）
- `ApiManageViewModel.parseSmart(text): ParsedApiConfig`（新增解析器）
- `ParsedApiConfig(url, key, model, name)` 数据类（新增）
- 路由 `local-model-select`（新增，MainActivity 注册）

## 4. 关键实现

- 手势区分（MainTopBar 切换胶囊）：
  ```kotlin
  Modifier.combinedClickable(
      onClick = { /* 短按切换引擎 */ },
      onLongClick = { /* 长按进配置 */ },
  )
  ```
- 智能输入解析（ApiManageViewModel）：
  ```kotlin
  fun parseSmart(text: String): ParsedApiConfig {
      val url = Regex("https?://[^\\s]+").find(text)?.value ?: ""
      val key = Regex("(sk-[A-Za-z0-9]+|key=\\S+|Bearer \\S+)").find(text)?.value ?: ""
      val model = Regex("model=\\S+").find(text)?.value?.removePrefix("model=") ?: ""
      val name = /* 域名推断 */
      return ParsedApiConfig(url, key, model, name)
  }
  ```

## 5. 测试与验证

- `compileDebugKotlin` + `assembleDebug` 通过；
- APK = `沐云杪AI-v0.4.0-引擎切换优化.apk`；
- 短按/长按手势、模型选择页、智能输入弹窗均按 HTML 原型验证。

## 6. 接手指引

- **待办**：本地模型切换后需实际重载引擎（`LocalChatEngine` 释放旧模型、加载新模型），当前仅更新偏好；
- **踩坑**：GGUF 模型 `runnable=false`，选中后需提示用户「需 llama.cpp 运行时」（关联 M-028）；
- 后续功能：智能输入可扩展为从真实配置文本（如 DeepSeek 控制台）一键导入。
