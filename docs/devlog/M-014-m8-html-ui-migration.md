# M-014 · HTML 界面原型 → Jetpack Compose 原生迁移

> 状态：✅ 已完成（2026-08-28）
> 来源：`沐云杪-界面原型.html`（4391 行，v19~v24 设计）
> 关联：R1-R12 全量页面；本迁移把原型 UI 落地为符合工程规范的原生实现

## 一、目标与范围

把单文件 HTML 原型（CSS + JS + localStorage 模拟数据）迁移为 Android 原生
Jetpack Compose 实现，遵循工程不变式：

- **模块化**：新增 `:core:ui` 共享 UI 模块（主题/组件/图标/Toast），依赖方向 `feature → core` 不破坏；
- **路由接入**：全部页面接入 `NavHost`（navigation-compose），顶栏按路由定制；
- **状态管理**：Hilt + ViewModel + StateFlow；持久化沿用 Room / DataStore；
- **主题统一**：HTML v19 设计令牌（品牌靛蓝→青渐变、浅灰底、14dp 圆角、绿/紫/红语义色）
  映射为 Material3 ColorScheme（`MuyunTheme`）；
- **交互一致**：逐条对照 HTML 的 JS 逻辑（时间戳合并、左滑删除、流式光标、交错菜单、
  断点续传模拟、选中即生效等）。

## 二、新增/变更文件清单

| 文件 | 说明 |
|---|---|
| `core/ui/build.gradle.kts` | 新增共享 UI 模块（Compose only） |
| `core/ui/.../theme/Color.kt` | HTML v19 设计令牌 → Compose 颜色常量 |
| `core/ui/.../theme/Theme.kt` | MuyunTheme（lightColorScheme 映射） |
| `core/ui/.../AppIcons.kt` | HTML 内联 SVG path 原样翻译为 ImageVector（feather 描边风格，tint 着色） |
| `core/ui/.../WallpaperPresets.kt` | 6 个预设渐变壁纸（聊天页/壁纸页共用） |
| `core/ui/.../RememberBitmap.kt` | Uri → ImageBitmap 解码（无图片库，手写采样压缩） |
| `core/ui/.../components/BrandButton.kt` | 品牌渐变主按钮（.check-btn） |
| `core/ui/.../components/Segmented.kt` | 分段胶囊（会话模式/tabs/壁纸方式等） |
| `core/ui/.../components/SubHeader.kt` | 子页面顶栏（返回+居中标题+右侧槽位） |
| `core/ui/.../components/Toggle.kt` | iOS 风格开关（.toggle-switch） |
| `core/ui/.../components/Modal.kt` | 弹窗容器（遮罩+缩放卡片，.modal） |
| `core/ui/.../components/Toast.kt` | 全局 Toast（居中深色胶囊，1.8s 自动消失） |
| `core/ui/.../components/SwipeReveal.kt` | 左滑删除容器（88dp 红色删除层，过半展开/回弹） |
| `core/ui/.../components/Common.kt` | 状态徽章/分组卡片/菜单行/空态/状态条 |
| `core/ui/.../components/WallpaperBackground.kt` | 壁纸背景渲染（预设/上传 × 4 模式 + 模糊 + 白渐变遮罩） |
| `core/storage/.../AppPrefs.kt` | 首次启动标记（DataStore） |
| `core/storage/.../WallpaperPrefs.kt` | 壁纸配置持久化（DataStore，双页面共享） |
| `core/db/.../Daos.kt` | NoteDao 增加：observeTrashed/restore/purge/purgeTrashed |
| `core/db/.../ChatDao.kt` | 增加 deleteMessage（重新生成用） |
| `core/db/.../MemoryDao.kt` | 增加 observeRecent 响应式流 |
| `feature/notes/NoteListScreen.kt` | 重写：计数头+左滑删除卡片+空态+时间格式化 |
| `feature/notes/TrashScreen.kt` | 新增：回收站（恢复/彻底删除/清空/30 天提示） |
| `feature/notes/NoteListViewModel.kt` | 增加回收站流与恢复/彻底删除/清空 |
| `feature/notes/NoteEditScreen.kt` | 对齐原型：元信息行+「完成」按钮+分隔线 |
| `feature/notes/TodoListScreen.kt` | 重写：tabs 过滤+圆形勾选行+输入行（语音+渐变添加钮） |
| `feature/chat/ChatScreen.kt` | 重写：欢迎区/快捷提问/气泡时间戳/流式光标/重新生成/附件暂存预览/加号菜单/麦克风/壁纸背景 |
| `feature/chat/ChatViewModel.kt` | 增加：云本地切换/重新生成/附件随消息展示 |
| `feature/settings/ExtPrefs.kt` | 扩展偏好 DataStore（权限模式/API 列表/模型列表/知识库/迁移日志/局域网设置） |
| `feature/settings/SettingsHomeScreen.kt` | 新增：设置主页（分组卡片+菜单行+状态描述） |
| `feature/settings/ApiManageScreen.kt` | 新增：多 API 列表+添加弹窗（校验/测试连接/密钥眼睛），当前 API 同步引擎 |
| `feature/settings/WallpaperScreen.kt` | 新增：壁纸页（预览/4 模式/上传/预设库/应用/恢复默认） |
| `feature/settings/ModelManageScreen.kt` | 新增：模型列表+兼容性检测+导入（真实 SAF 导入） |
| `feature/settings/PermissionScreen.kt` | 新增：三档权限单选（选中即保存，与自检页联动） |
| `feature/settings/MigrateScreen.kt` | 新增：数据迁移（多选/扫描/校验/进度/暂停/取消/日志/报告弹窗） |
| `feature/settings/LanTransferDialog.kt` | 新增：局域网传输弹窗+传输设置弹窗 |
| `feature/settings/DeviceCheckScreen.kt` | 新增：设备自检（真实硬件数据，替代 HTML 硬编码） |
| `feature/settings/MemoryScreen.kt` | 升级：画像卡/分类 tabs/手动添加删除 |
| `feature/settings/KnowledgeScreen.kt` | 升级：文件夹列表/隐私开关/新建；保留投喂能力 |
| `app/.../MainActivity.kt` | 重写：品牌主题/按路由顶栏/抽屉重做/新路由接线/Toast 宿主/首次启动自检门 |
| `settings.gradle.kts` | 注册 `:core:ui` |

## 三、关键设计决策

1. **图标 100% 保真**：项目只有 material-icons-core（无相机/麦克风等），
   新增 AppIcons 把 HTML 里的 SVG path 数据用 `PathParser` 直接翻译成 ImageVector
   （描边 2、圆头圆角），与原型视觉完全一致，且不引入 icons-extended 大依赖。
2. **顶栏归属**：HTML 的「菜单+胶囊+操作按钮」顶栏由 MainActivity 按路由渲染，
   子页面自绘 SubHeader——与 HTML 的单页顶栏/子页头结构一一对应。
3. **壁纸共享**：聊天页背景与壁纸设置页共用 `WallpaperPrefs`（core:storage）+
   `WallpaperBackground`（core:ui），选中即生效与 HTML 一致。
4. **回收站真实化**：HTML 用 localStorage 软删 + 30 天 TTL；原生用 Note.deletedAt
   字段（Room 已有）+ 新增 DAO 查询，恢复/彻底删除/清空全部落地。
5. **云端 API 多配置**：原型 apiList + currentApiId 用 DataStore JSON 持久化；
   切换当前时同步写入 CloudConfigRepository，真实引擎立即生效。
6. **设备自检真实化**：HTML 硬编码 11GB/arm64 等值；原生改用 ActivityManager /
   StatFs / Build.SUPPORTED_ABIS 实时检测。
7. **首次启动门**：`AppPrefs.firstRunDone`（DataStore）控制 startDestination，
   自检完成写标记并清栈跳主页。

## 四、与原 HTML 的已知差异（有意为之）

| 项目 | HTML 原型 | 原生实现 | 原因 |
|---|---|---|---|
| 壁纸「平铺」 | CSS repeat 背景 | 近似为等比缩放 | Compose 无原生平铺背景，差异已记录 |
| 毛玻璃 blur(20px) | 顶栏/输入栏 backdrop-filter | 半透明纯色近似 | Android 无跨版本 backdrop-filter |
| 聊天图片发送 | dataURL 存 localStorage | Uri + 内存附件映射 | 大图不入库；附件重启后不显示（原型级能力） |
| 迁移/局域网传输 | 纯模拟 | 状态机同款模拟 | 真实传输属后续里程碑 |
| 语音输入 | 1.5s 模拟文本 | 同款模拟 | 真实 STT 依赖系统能力，后续接入 |
| 记忆「人际/项目」tab | 前端任意分类 | 暂映射 TODO/空态 | 数据库仅 FACT/PREFERENCE/TODO 三类 |
| 拍照 | input capture 相机 | TakePicturePreview（缩略图） | 免 FileProvider 配置，后续可换完整相机 |

## 五、验证

- `./gradlew :app:compileDebugKotlin` 通过（含新模块 :core:ui 全量编译）。
- 迁移说明文档：`docs/ui-migration-guide.md`（初学者向）。

## 五·补、旧版 UI 彻底清除（2026-08-28 第二轮）

按「HTML 设计稿为唯一视觉依据、旧 UI 零残留」要求，本轮清除：

| 清除对象 | 类型 | 说明 |
|---|---|---|
| `feature/filesearch/` 整个模块 | 旧 UI 页面 | HTML 无文件检索页；已从 settings.gradle / app 依赖 / 路由 / 抽屉项全链移除。M7 能力不丢：AI 对话中的 `search_file` 工具仍经 core:search 正常工作 |
| `DatabaseConfigScreen.kt` + "db" 路由 | 旧 UI 页面 | HTML 无数据库配置页；StorageProvider 默认目录不受影响 |
| `CloudConfigScreen.kt` + "cloud" 路由 + 抽屉「云端 API 配置」 | 旧 UI 页面 | 被新 ApiManageScreen 完全替代（设置→云端API管理） |
| `ChatListScreen.kt` | 旧 UI 页面 | HTML 无会话列表页，会话区已内嵌侧边栏（今天分组） |
| `MarkdownText.kt` | 旧样式组件 | 新聊天气泡为纯文本（HTML 同款），无 Markdown 渲染 |
| 抽屉「对话引擎」区（RadioButton 单选/导入模型/复制下载地址/模型状态/云端API配置） | 旧 UI 残留 | HTML 抽屉无此区；引擎切换入口=聊天顶栏胶囊，模型导入入口=设置→模型管理 |
| 抽屉「文件检索」菜单项 | 旧 UI 入口 | HTML 抽屉仅 任务/局域网传输/数据迁移 |
| README/01-architecture/core:search 注释中的 filesearch 引用 | 文档引用 | 全部同步更新 |

**补交互（HTML 有、首轮遗漏）**：会话胶囊滑动切换（`detectHorizontalDragGestures`，>40dp 阈值，左滑→AI、右滑→常规，对应 HTML initCapsuleSwipe）。

**功能入口变更对照（防丢功能）**：

| 原入口 | 新入口（HTML 对应） |
|---|---|
| 抽屉·引擎单选 | 聊天顶栏「本地/云端」胶囊（HTML .chat-switch） |
| 抽屉·导入模型 | 设置 → 模型管理 → 导入模型（HTML 模型管理页） |
| 抽屉·云端 API 配置 | 设置 → 云端API管理（HTML 云端API管理页） |
| 文件检索人工页 | 已移除（HTML 无）；AI 对话内 search_file 工具不受影响 |

## 六、遗留项（下一里程碑）

- [ ] 真实局域网传输 / 数据迁移后端
- [ ] 语音识别真实接入
- [ ] 壁纸平铺真实实现（自定义 draw）
- [ ] 深色模式（darkColorScheme）
