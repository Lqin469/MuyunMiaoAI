# UI 迁移说明（初学者版）：HTML 界面原型 → Android 原生实现

> 面向对象：不熟悉 Android 原生开发的读者
> 配套文档：`docs/devlog/M-014-m8-html-ui-migration.md`（工程师版，含完整文件清单）

---

## 0. 先认识几个名词（后面都会用到）

| 名词 | 一句话解释 |
|---|---|
| **HTML 原型** | 用网页技术（HTML+CSS+JS）画出来的"界面草图"，只能在浏览器里看，不能装进手机应用 |
| **Jetpack Compose** | Google 官方的 Android 原生 UI 框架：用 Kotlin 代码"描述"界面，是网页里 HTML+CSS 的替代品 |
| **原生实现** | 用 Android 官方技术重写一遍，能真正编译进 App 运行 |
| **Composable 组件** | Compose 里的"函数式组件"，类似网页里的自定义标签 `<MyCard/>`，可无限复用 |
| **NavHost / 路由** | 页面导航系统：给每个页面起个名字（如 `"settings"`），通过名字跳转，类似网页里的"网址跳转" |
| **ViewModel** | 每个页面的"大脑"：只管数据和逻辑，不管画面长什么样；页面销毁重建时数据不丢 |
| **StateFlow** | 一种"数据水管"：数据变了自动通知所有盯着的界面刷新，类似网页里的"响应式数据" |
| **Hilt（依赖注入）** | "自动装配工"：谁需要什么工具，Hilt 自动送到手上，不用自己 new |
| **Room** | Android 官方数据库工具，替代原型里的 localStorage（网页本地存储） |
| **DataStore** | Android 官方"小本本"（键值存储），专门存设置项这类零散配置 |
| **Gradle 模块** | 项目的"分区"：把代码按功能拆成多个文件夹包，各自独立编译，互不拖累 |

## 1. 迁移前的现状对比

| | HTML 原型 | Android 工程 |
|---|---|---|
| 技术 | HTML + CSS + JavaScript（一个 4391 行的大文件） | Kotlin + Jetpack Compose（多模块） |
| 页面切换 | JS 切换 CSS class | NavHost 路由跳转 |
| 数据保存 | localStorage（关浏览器就没了） | Room 数据库 + DataStore（真正持久化） |
| 模拟数据 | 全部硬编码 | 设备自检等改用**真实系统数据** |

## 2. 迁移后的文件结构（新增/大改的都在这里）

```
MuyunMiaoAI/
├── core/ui/                          ← 新增：共享 UI 模块（所有页面共用的"零件仓库"）
│   └── src/main/java/com/memuo/core/ui/
│       ├── AppIcons.kt               ← 图标库：把网页里的 SVG 图标翻译成 Compose 图标
│       ├── WallpaperPresets.kt       ← 6 个预设壁纸渐变
│       ├── RememberBitmap.kt         ← 图片加载器（项目没装图片库，自己手写）
│       ├── theme/                    ← 主题：网页 CSS 里的配色 → Kotlin 颜色常量
│       └── components/               ← 通用零件：
│           ├── BrandButton.kt        ← 品牌渐变大按钮（网页 .check-btn）
│           ├── Segmented.kt          ← 分段胶囊（"常规|AI"、各种 Tab）
│           ├── SubHeader.kt          ← 子页面顶栏（返回+标题）
│           ├── Toggle.kt             ← iOS 风格开关
│           ├── Modal.kt              ← 弹窗容器
│           ├── Toast.kt              ← 居中黑胶囊提示
│           ├── SwipeReveal.kt        ← 左滑删除容器
│           └── WallpaperBackground.kt← 聊天页壁纸背景
│
├── core/storage/                     ← 存储模块（加了两份"小本本"）
│   ├── AppPrefs.kt                   ← 记录"是否已完成首次自检"
│   └── WallpaperPrefs.kt             ← 壁纸配置（聊天页/壁纸页共用）
│
├── core/db/                          ← 数据库模块（补了几个查询）
│   └── dao/                          ← 增加：回收站查询、恢复、彻底删除、删除单条消息
│
├── feature/notes/                    ← 常规备忘录模块
│   ├── NoteListScreen.kt             ← 重写：计数 + 左滑删除 + 空态
│   ├── TrashScreen.kt                ← 新增：回收站页
│   ├── NoteEditScreen.kt             ← 微调：元信息 + "完成"按钮
│   └── TodoListScreen.kt             ← 重写：待办 + 三个 Tab 过滤
│
├── feature/chat/                     ← AI 对话模块
│   ├── ChatScreen.kt                 ← 大重写：问候区/快捷提问/气泡/流式光标/
│   │                                    重新生成/图片暂存/加号菜单/麦克风/壁纸
│   └── ChatViewModel.kt              ← 增加：云本地切换、重新生成、附件展示
│
├── feature/settings/                 ← 设置模块（新增 8 个页面）
│   ├── SettingsHomeScreen.kt         ← 设置主页（分组卡片）
│   ├── ApiManageScreen.kt            ← 云端 API 管理（多配置+添加弹窗）
│   ├── WallpaperScreen.kt            ← 自定义壁纸
│   ├── ModelManageScreen.kt          ← 模型管理（兼容性检测）
│   ├── PermissionScreen.kt           ← 权限管理（基础/ADB/ROOT）
│   ├── MigrateScreen.kt              ← 数据迁移（进度/日志/报告）
│   ├── LanTransferDialog.kt          ← 局域网传输弹窗
│   ├── DeviceCheckScreen.kt          ← 设备自检（首次启动）
│   ├── MemoryScreen.kt               ← 记忆页升级
│   ├── KnowledgeScreen.kt            ← 知识库页升级
│   └── ExtPrefs.kt                   ← 各页面设置项的统一"小本本"
│
└── app/src/main/java/com/memuo/app/
    └── MainActivity.kt               ← 大重写：主题/顶栏/抽屉/全部路由接线/Toast
```

**一句话总结**：网页里 1 个 4391 行的大文件，拆成了约 30 个各司其职的 Kotlin 文件。

## 3. 关键改动点（挑最重要的说）

### 3.1 配色怎么搬过来的？
网页在 CSS 顶部用 `:root { --brand: #4f46e5; ... }` 定义了整套配色。
迁移时把每个颜色变量翻译成 Kotlin 常量（`core/ui/theme/Color.kt`），
再汇总成 `MuyunTheme`。所有页面只要套上这个主题，颜色就自动统一了。
改品牌色也只需改一个文件 —— 这就是"设计令牌"的好处。

### 3.2 图标怎么保证一模一样？
项目只装了基础图标库（没有相机、麦克风等）。
解决：把网页 SVG 图标里的**路径数据原样复制**到 `AppIcons.kt`，
用 Compose 的 `PathParser` 重新画出来 —— 视觉 100% 一致，还不增加依赖体积。

### 3.3 顶栏（菜单+胶囊+按钮）放在哪？
网页的顶栏是每个页面自己画的；原生里顶栏只属于**主页**（常规/AI），
由 MainActivity 统一渲染；子页面改用 SubHeader。这样"常规|AI"胶囊、
"新建+回收站"按钮组、"新建会话+本地/云端"按钮组都能按路由自动切换。

### 3.4 数据从"模拟"变"真实"
- **备忘录/回收站**：原型用 localStorage，关页面就没；原生用 Room 数据库，重启 App 还在。
  回收站"30 天自动清理"的提示保留，物理清理逻辑挂在删除时间字段上。
- **设备自检**：原型写死"11GB / arm64"；原生用系统 API 读**真机**的内存/架构/存储。
- **云端 API**：切换"使用中"的 API 时，会同步写入引擎配置 —— 不是演示，是真的生效。

### 3.5 首次启动流程
原型默认进聊天页；原生增加**设备自检门**：第一次打开 App 先跑自检，
点"下一步"才进主页（用 DataStore 记录"已自检"，以后不再出现）。

## 4. 与原 HTML 的差异（都是有意为之）

| 差异点 | 网页原型 | 原生实现 | 为什么 |
|---|---|---|---|
| 壁纸"平铺" | CSS 无缝平铺 | 近似为等比缩放 | Compose 没有现成平铺背景，后续可自绘 |
| 毛玻璃效果 | 顶栏/输入栏磨砂 blur | 半透明纯色 | Android 无跨版本磨砂 API |
| 聊天发图片 | 图片压缩成文本存浏览器 | 存 Uri + 内存映射 | 大图不该进数据库；附件暂不跨重启（原型级） |
| 数据迁移/局域网 | 纯前端模拟 | 同样的状态机模拟 | 真实网络传输属于后续里程碑 |
| 语音输入 | 1.5 秒后填模拟文字 | 同款模拟 | 真实语音识别后续接入 |
| 记忆页 4 个 Tab | 任意分类 | "人际/项目"暂无数据（显示空态） | 数据库目前只有事实/偏好/待办三类 |
| 拍照 | 调相机 | 相机预览（低清） | 免额外配置文件，后续可升级 |

## 5. 需要注意的事项

1. **编码约定**：本项目要求源码逐行中文注释，新增文件均已遵守；提交前请跑
   `scripts/check-secrets.sh` 与 pre-push 钩子（项目安全基线）。
2. **模块依赖方向**：只允许 `feature → core`，新页面不能反向依赖 app 层；
   共享零件一律放 `core/ui`，别在单个 feature 里复制粘贴组件。
3. **图标新增**：如后续需要新图标，优先在 `AppIcons.kt` 用 SVG path 添加，
   避免引入 icons-extended 大依赖。
4. **顶栏规则**：新页面若自带 SubHeader，不要在 MainActivity 的 `showTopBar`
   白名单里登记；若想复用主页顶栏则需登记。
5. **编译方式**：本机网络无法下载 Gradle 发行版时，用
   `C:/Users/admin/.workbuddy/binaries/gradle/gradle-8.13/bin/gradle.bat` 直接构建
   （与 wrapper 版本一致）。
6. **总流程日志**：按项目约定，任何 UI 变更需同步记录到
   `D:\LQYMYH\ai备忘录\开发流程\总流程日志.md`。

## 6. 遗留待办（见 M-014 devlog 第六节）

- 真实局域网传输 / 数据迁移后端
- 语音识别真实接入
- 壁纸平铺真实实现
- 深色模式

## 7. 第二轮：旧版 UI 彻底清除（2026-08-28）

按「HTML 设计稿为唯一依据、旧 UI 零残留」要求，已删除以下旧版界面与入口：

| 已删除 | 功能去向 |
|---|---|
| 文件检索页（整个 feature:filesearch 模块） | HTML 无此页面；AI 对话里的「设备检索」工具不受影响 |
| 数据库配置页 | 数据库用默认目录，无需手动配置 |
| 旧云端 API 配置页 | 被新的「云端API管理」页完全替代 |
| 会话列表页 | 会话已内嵌侧边栏「今天」分组 |
| Markdown 渲染组件 | 新聊天气泡为纯文本（与 HTML 一致） |
| 侧边栏旧「对话引擎」单选区 + 导入模型 + 复制下载地址 | 引擎切换在聊天顶栏胶囊；模型导入在「设置→模型管理」 |

同时补齐了 HTML 有而首轮遗漏的交互：**会话胶囊左右滑动切换**（左滑→AI、右滑→常规）。

全工程 `grep` 验证：已删除类的代码引用为 0 残留；README/架构文档同步更新；构建与打包验证通过。
