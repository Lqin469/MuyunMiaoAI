# 开发记录：M6 引擎切换 + 模型导入 + 设置页（M-010）

- 日期：2026-08-24
- 涉及模块：core:ai:engine（EngineSettings/EngineRouter）、core:models（ModelImporter 复制）、feature:settings（SettingsViewModel/Screen）、feature:chat（ChatViewModel）、app（底部导航）
- 关联需求：R2（模型下载/评估）、R3（本地导入）、R1（云端/本地引擎切换）
- 关联文档：docs/03-contracts.md、docs/05-model-hardware.md

## 1. 目标与成果

**打通「模型导入 → 引擎切换 → 本地对话」闭环，让 M6 构建的 Qwen3.5-0.8B-MNN 模型真正可用。**

| 交付 | 内容 |
|---|---|
| EngineSettings | core:ai:engine 接口（engineType 状态流 + 写入），feature:settings 的 SettingsRepository 实现（DataStore 持久化，默认 CLOUD） |
| EngineRouter | 实现 ChatEngine，按设置路由 local/cloud；`switchTo` 切本地前检查模型就绪（未就绪拒绝）；`hasLocalModel` 检查 modelsDir/llm/config.json |
| ModelImporter | 注入 StorageProvider，新增 `importMnnToAppDir(sourceDir)` 把模型目录复制到 modelsDir()/llm/（IO 线程）+ `hasLocalModel` |
| SettingsScreen/ViewModel | 设置页：引擎切换（RadioButton）、模型导入（路径输入）、云端配置（baseUrl/apiKey/model） |
| 底部导航 | MainActivity 改为三栏（笔记/对话/设置），对话 tab 自动确保会话（ensureConversation） |

## 2. 设计要点

- **依赖倒置**：EngineSettings 接口定义在 core:ai:engine，由 feature:settings 实现 + @Binds（core 不依赖 feature，与 SearchSettings/CloudConfigProvider 同模式）；
- **引擎切换零侵入**：ChatViewModel 仍只依赖 ChatEngine 接口（现注入 EngineRouter），切换引擎无需改调用方；newConversation 的 engine 字段改用 `engine.type`（动态）；
- **切本地前拦截**：EngineRouter.switchTo(LOCAL) 检查 config.json 存在，不存在返回 false，UI 提示"请先导入模型"；
- **导入走 IO**：523M 模型复制用 withContext(Dispatchers.IO)，避免阻塞主线程；幂等（清旧再复制）；
- **对话 tab 会话管理**：ensureConversation 用 observeConversations().first() 取一次真实数据，避免异步竞态（有则复用最新，无则新建）。

## 3. 接口契约

```kotlin
// core:ai:engine
interface EngineSettings { val engineType: StateFlow<EngineType>; suspend fun setEngineType(type) }
class EngineRouter(cloud, local, settings, storage) : ChatEngine {
    fun hasLocalModel(): Boolean
    suspend fun switchTo(type): Boolean   // 切本地无模型返回 false
}

// core:models
class ModelImporter(storage) {
    suspend fun importMnnToAppDir(sourceDir: File): Boolean  // 复制到 modelsDir/llm/
    fun hasLocalModel(): Boolean
}
```

## 4. 关键实现

```
core/ai/engine/...      EngineSettings.kt（接口）、EngineRouter.kt（路由）、EngineModule.kt（改绑 EngineRouter）
core/models/...         ModelImporter.kt（+importMnnToAppDir/hasLocalModel）、build.gradle（+core:storage）
feature/settings/...    SettingsRepository.kt（+EngineSettings）、SettingsModule.kt（+bindEngineSettings）
                        SettingsViewModel.kt、SettingsScreen.kt（新增）、build.gradle（+Compose +core:models +core:db）
feature/chat/...        ChatViewModel.kt（+ensureConversation、engine 字段动态）
app/...                 MainActivity.kt（底部导航三栏 + ChatTab）
```

## 5. 测试与验证

- [x] CI 编译通过（run 32685986369 success）；
- [ ] 真机：设置页切"本地"→ 未导入时提示拦截；导入模型后切换成功；
- [ ] 真机：本地引擎对话（需先把 Qwen3.5-0.8B-MNN 模型推到设备并导入）；
- [ ] 真机：云端配置保存后切"云端"正常对话。

## 6. 报错与修复

- **`Unresolved reference 'BottomNavigation'`**：Material3 底部导航组件叫 `NavigationBar`/`NavigationBarItem`（`BottomNavigation` 是 Material2 命名）。修复：改用 material3 的 NavigationBar/NavigationBarItem（commit 19f11eb）。

## 7. 遗留

- **模型部署**：模型在 PC 的 `D:\LQYMYH\ai模型`，真机测试需 `adb push` 到设备可访问目录，再在设置页填路径导入（或后续做 SAF 目录选择器）；
- **SAF 导入**：当前为绝对路径输入，后续改用 SAF OpenDocumentTree（正规、免路径手填）；
- **仅 arm64-v8a**；**bge 嵌入真实现**（MNN Embedding::createEmbedding）；
- 引擎切换后已打开会话的 engine 字段不回改（历史会话仍记创建时引擎）。
