# 开发记录：M6 侧边抽屉菜单 + SAF 模型导入（M-011）

- 日期：2026-08-24
- 涉及模块：app（MainActivity 抽屉）、core:models（SAF 导入/检测）、feature:settings（三个页面）
- 关联需求：R3（本地模型导入）、R4/R5（存储目录）、R6（记忆库）
- 关联文档：docs/03-contracts.md

## 1. 目标与成果

**按用户要求重构 UI 为侧边抽屉式，并把模型导入改为「系统文件夹选择器 + 自动检测」。**

| 交付 | 内容 |
|---|---|
| 侧边抽屉 | MainActivity 用 `ModalNavigationDrawer`：左上角汉堡图标（`Icons.Menu`）点击左滑出菜单 |
| 菜单项 | 数据库配置 / 记忆库 / 云端·本地引擎切换（RadioButton）/ 导入模型（仅本地）/ 云端 API 配置（仅云端） |
| 条件显示 | 切「本地 AI」→ 显示「导入模型」；切「云端 AI」→ 显示「云端 API 配置」 |
| SAF 导入 | `OpenDocumentTree` 文件夹选择器 → `ModelImporter.importFromUri`（DocumentFile 递归复制到 modelsDir()/llm/） |
| 模型检测 | `detectMnnModel`：检测 config.json + llm.mnn + llm.mnn.weight 三项齐全才导入 |
| 页面 | DatabaseConfigScreen（目录布局）、MemoryScreen（记忆列表）、CloudConfigScreen（云端配置） |

## 2. 设计要点

- **SAF 文件夹选择**：`rememberLauncherForActivityResult(OpenDocumentTree)` 在 MainActivity 层创建，选中 Uri 交给 SettingsViewModel.importModelFromUri；
- **导入前检测**：`DocumentFile` 遍历前先 `listFiles().map { it.name }` 收集文件名，校验 `config.json`/`llm.mnn`/`llm.mnn.weight` 三项齐全，不通过直接拒绝（避免复制无效目录）；
- **DocumentFile 递归复制**：目录→递归，文件→`contentResolver.openInputStream` → `FileOutputStream`，走 `Dispatchers.IO`；
- **抽屉菜单引擎切换**：AppDrawer 复用 SettingsViewModel（Activity 作用域），切换逻辑走 EngineRouter（未下载就切本地仍被拦截）；
- **页面拆分**：原 SettingsScreen 拆为 CloudConfigScreen（云端配置）+ AppDrawer（引擎/导入），新增 DatabaseConfigScreen + MemoryScreen。

## 3. 接口契约

```kotlin
// core:models
class ModelImporter {
    fun detectMnnModel(dir: File): Boolean          // 检测 config.json + llm.mnn + llm.mnn.weight
    suspend fun importFromUri(context, uri): Boolean // SAF 目录检测 + 复制
}

// feature:settings
class SettingsViewModel { fun importModelFromUri(uri: Uri) }   // SAF 导入入口
```

## 4. 关键实现

```
app/...                 MainActivity.kt（ModalNavigationDrawer + AppDrawer + SAF 选择器 + 6 路由）
core/models/...         ModelImporter.kt（+detectMnnModel/importFromUri/copyDocumentTree）、build.gradle（+documentfile）
feature/settings/...    CloudConfigScreen.kt、DatabaseConfigScreen.kt、MemoryScreen.kt（新增）
                        SettingsViewModel.kt（+importModelFromUri + @ApplicationContext）、删 SettingsScreen.kt
gradle/libs.versions.toml（+documentfile 1.0.1）
```

## 5. 测试与验证

- [ ] CI 编译通过；
- [ ] 真机：点左上角汉堡 → 抽屉左滑出 → 菜单项齐全；
- [ ] 真机：切「本地 AI」→ 显示「导入模型」→ 点它跳系统文件夹选择器 → 选 Qwen 模型目录 → 自动检测并复制 → 状态变「已就绪」；
- [ ] 真机：切「云端 AI」→ 显示「云端 API 配置」→ 进页填配置保存；
- [ ] 真机：数据库配置页显示五个目录；记忆库页展示提炼出的记忆。

## 6. 遗留

- SAF 导入未加持久化权限（`takePersistableUriPermission`），当前"复制后即用"，若需跨会话访问需补；
- 自定义存储目录（R5）仍为只读展示，未接入 StorageMigrator；
- 记忆库页仅列表展示，未做删除/检索交互。
