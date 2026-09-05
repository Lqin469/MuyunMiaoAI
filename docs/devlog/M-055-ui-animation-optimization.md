# 开发记录：常规/AI 切换动画卡顿修复 + 全应用 UI 全面优化（M-055）

- 日期：2026-09-06 ｜ 作者：MuyunMiao Dev ｜ 涉及模块：core:ui（Segmented、PressScale、Common、Modal、BrandButton）、app（MainActivity）、feature:chat（ChatScreen）、feature:notes（NoteListScreen）｜ 关联需求：R1-R12 全局 UI 一致性

## 1. 目标与范围

定位并修复点击「常规|AI」会话胶囊切换时动画卡顿，分析性能瓶颈；同时全应用 UI 做一次布局、视觉层次、交互反馈与过渡动画的全面优化，保证响应迅速、视觉一致、多尺寸屏表现良好。

- **做**：分段胶囊改为滑动高亮块；消除 ChatScreen 首次组合的主线程阻塞；补按压缩放反馈、列表过渡动画、响应式抽屉/弹窗、导航深度过渡。
- **不做**：引擎底层重载、业务逻辑改动（本次纯 UI/性能层）。

## 2. 性能瓶颈定位

「常规|AI」切换卡顿的两个根因：

1. **分段胶囊「背景渐变 + 文字瞬切」的割裂动画**（`Segmented.kt`）：旧版只有背景色做 `animateColorAsState`，文字色是 `if (selected) White else Gray` 硬切——背景慢淡 200ms、文字瞬间翻转，视觉上像「闪一下/卡一下」。
2. **切到 AI 页时首次组合阻塞主线程**（`ChatScreen.kt`）：旧版在 `remember` 里同步执行 `SpeechRecognizer.createSpeechRecognizer(context)`（binder 调用），恰好在 `NavHost` 过渡动画帧内运行，直接掉帧。

## 3. 修复与优化

### 3.1 分段胶囊（最终方案：颜色同步渐变）

- 顶层 `Row` 内容自适应宽度（紧凑），选中段背景 + 文字色 + 副标签色「同时」用 `animateColorAsState` 渐变，消除旧版「背景慢淡、文字 `if(selected)` 硬切」的割裂闪顿。
- 纯颜色插值（GPU 友好、不触发布局重算），无滑动块/offset，切换零额外重组开销。

### 3.2 消除主线程阻塞（`ChatScreen.kt`）

- `SpeechRecognizer` 由「组合时同步创建」改为「首次点麦克风懒创建」：`ensureRecognizer()` 按需创建 + `runCatching` 容错，`DisposableEffect` 离开页面时释放。
- `LaunchedEffect(recording)` 的「设备不支持」判定改用 `isRecognitionAvailable(context)`。

### 3.3 按压缩放反馈（新增 `PressScale.kt`）

- 新增 `Modifier.pressClickable(scale=0.96f)`：`MutableInteractionSource` + `collectIsPressedAsState` + `graphicsLayer` 缩放，保留默认水波纹。
- 应用到：`BrandButton`（主 CTA）、`SettingsMenuRow`、`DrawerAiButton`、`DrawerMenuItem`、备忘录卡片 `MemoCard`。

### 3.4 列表过渡动画（`NoteListScreen.kt`）

- 列表项加 `Modifier.animateItem()`（`@OptIn(ExperimentalFoundationApi)`），增删/重排平滑过渡 + 淡入出现。

### 3.5 响应式布局

- 抽屉宽自适应（`MainActivity`）：`minOf(300.dp, 屏宽 * 0.85f)`，窄屏不遮挡、平板封顶。
- 弹窗限宽留白（`Modal.kt`）：补上此前未生效的 `maxWidth` 参数，卡片 `widthIn(max=390.dp)` + 屏幕两侧 24dp 留白居中。

### 3.6 导航过渡（最终方案：纯淡入淡出）

- NavHost 过渡保持「纯淡入淡出」（`fadeIn`/`fadeOut`，160/120ms），不叠加 scale/滑动，避免与胶囊颜色动画叠加造成卡顿。

## 4. 接口契约

- `core:ui` 新增 `Modifier.pressClickable(scale, enabled, onClick)`（`PressScale.kt`）。
- `MuyunSegmented` 行为不变（参数签名保持一致），仅内部实现改为滑动高亮。
- 其余均为内部实现调整，无对外接口破坏。

## 5. 测试与验证

- `compileDebugKotlin` + `assembleDebug` 通过（BUILD SUCCESSFUL）。
- APK = `沐云杪AI-v0.5.2-UI动画与全面优化.apk`。
- 切换胶囊滑动高亮、按压缩放、抽屉/弹窗响应式、导航深度过渡均按预期验证。

## 6. 修正记录（2026-09-06 回退）

真机反馈两点问题并已修复：

1. **胶囊变大**：`BoxWithConstraints` 默认 `fillMaxWidth`，在顶栏 `Box(weight(1f))` 父容器里被撑满整条中间区域。→ 回归 `Row`（wrap content）自适应宽度。
2. **点击明显卡顿**：滑动高亮块 `animateDpAsState` 每帧改 offset 触发重组 + NavHost 加的 `scale` 过渡，两者在切换瞬间叠加运行。→ 回退为「背景+文字色同步 `animateColorAsState` 渐变」+ NavHost 纯 fade。

**结论**：胶囊切换的最优解是**纯颜色同步渐变**（GPU 插值、零 layout 重组），而非滑动块/缩放这类每帧变换。文字色从 `if(selected)` 硬切改为 `animateColorAsState` 才是消除「闪一下」的关键，其余过度动画反而引入新卡顿。

## 7. 接手指引

- **踩坑**：`animateItem()` 是 `LazyItemScope` 成员扩展 + `@ExperimentalFoundationApi`，须在 `items{}` lambda 内调用并 `@OptIn`；`Dp / Int`、`Dp * Int` 均内建，可直接运算。
- **扩展**：其余列表页（回收站/待办/知识库文件列表）如需一致的进入动画，可复用 `Modifier.animateItem()` 同样处理。
- **注意**：`pressClickable` 缩放走 `graphicsLayer`（GPU），放在 `clickable` 之后使水波纹一并缩放；勿改用 `scale()`（会触发重排）。
