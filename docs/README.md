# 文档体系总纲（docs/README.md）

> 本文是 `docs/` 目录的**导航与规范**：说明文档分几类、放在哪、怎么写、什么时候必须更新。
> 目标：**新成员（人或 AI）只读 docs/ 即可无痛接手**（R12 强制）；任何文档缺失 = 对应功能不算完成。

---

## 1. 文档结构总览

```
docs/
├── README.md                  ← 本文：导航 + 写作规范（读这里开始）
├── 00-overview.md             ← 总纲：需求索引（R1-R12 状态）+ 路线图 + 文档索引
├── 01-architecture.md         ← 架构：分层 + 不变式 + 模块边界（改架构先改这里）
├── 02-technical-choices.md    ← 选型：技术选型、理由、否决记录（决策事实源）
├── 03-contracts.md            ← 契约：模块接口签名（单一事实源，改接口必同步）
├── 04-database.md             ← 数据：Room 表结构 + DataStore 键（改表必同步）
├── 05-model-hardware.md       ← 模型：支持模型清单 + 硬件评估（R2/R3）
├── adl/                       ← 架构决策记录 ADR-xxx（接口/架构变更先写这里）
├── devlog/                    ← 开发记录 M-xxx（每个功能当回合归档，R12 强制）
├── privacy-search-consent.md  ← 搜索隐私红线（用户强制，PR 必读）
├── security-*.md              ← 安全基线：审计/密钥/推送前自查
├── code-review-guide.md       ← 代码审查标准与流程（六维 + 三级 + 五阶段）
├── ui-migration-guide.md      ← UI 迁移说明（HTML 原型 → Compose，初学者版）
└── core-features-implementation-plan.md ← 核心功能实现方案（M-027 设计稿）
```

## 2. 四类文档（Divio 模型）与项目映射

| 类型 | 回答的问题 | 项目对应 | 写作要点 |
|---|---|---|---|
| **Tutorial 教程** | "第一次怎么上手" | devlog/M-000、ui-migration-guide | 分步骤、有预期输出、15 分钟内可完成 |
| **How-to 指南** | "怎么完成某任务" | security-pre-push、privacy-search-consent、code-review-guide | 面向任务、步骤可复制执行 |
| **Reference 参考** | "精确细节是什么" | 00（需求索引）、01（架构）、02（选型）、03（契约）、04（表）、05（模型） | 完整、精确、可核对，与代码一一对应 |
| **Explanation 解释** | "为什么这样设计" | ADR-xxx、core-features-implementation-plan | 讲决策背景与取舍，不重复 reference 内容 |

> 规则：**四类不混写**。一个文档只回答一类问题；参考文档里出现大段"为什么"应迁移到 ADR。

## 3. 写作规范（强制）

1. **第二人称、现在时、主动语态**：写「你导入模型后」，不写「模型被导入后」；
2. **代码示例必须可运行**：示例命令在本机真实执行过再发布；给出预期输出；
3. **不假设上下文**：每篇文档自洽或显式链接前置文档（如「先读 00-overview」）；
4. **明确失败情况**：写「如果你看到 `Error: X`，说明 Y，请做 Z」，不写「如有问题请反馈」；
5. **版本同步**：文档描述的是**当前代码状态**；代码变了文档必须当回合变（见 §5）；
6. **中文 + 专业术语并存**：首次出现的英文术语给出中文（如 `SearchConsentGate（许可闸门）`）；
7. **链接用相对路径**：仓库内互链一律相对路径（`../AGENTS.md`），保证在 GitHub 上可跳转；
8. **不写 TODO 占位**：未完成的内容要么标「⏳ 待办（登记 Issue）」要么不写，禁止留空章节。

## 4. 质量门（文档随代码一起合并）

| 门 | 检查项 | 违反后果 |
|---|---|---|
| G1 | 每个功能模块完成时，**当回合**归档 `docs/devlog/M-xxx` | PR 不合并（R12 硬性） |
| G2 | 接口/架构变更：**先写 ADR**（docs/adl/），再改代码，再同步 03-contracts | 按「接口先文档后代码」打回 |
| G3 | 数据表变更：同步 04-database；模型变更：同步 05 | 缺失即视为不完整 |
| G4 | 新需求/能力：更新 00-overview 需求状态行 | 需求索引失真 |
| G5 | README 5 秒测试：新用户 5 秒内看懂「是什么/为什么/怎么开始」 | 合并前由维护者检查 |

## 5. 维护流程（谁在什么时候更新什么）

| 事件 | 必须更新的文档 | 时限 |
|---|---|---|
| 新功能模块完成 | devlog/M-xxx（新建）+ 00-overview（状态行） | 当回合 |
| 接口签名变更 | adl/ADR-xxx（新建）+ 03-contracts（同步） | 改代码前写 ADR |
| 表结构变更 | 04-database（+ 数据库 version 升级说明） | 当回合 |
| 技术选型变更/否决 | 02-technical-choices（追加记录） | 决策当回合 |
| 架构/模块增减 | 01-architecture | 当回合 |
| 搜索类功能改动 | privacy-search-consent（若有影响）+ code-review-guide §2.2 | 当回合 |
| 任何文档引用失效 | 全文搜旧引用并修正（本仓库历史曾出现引用已删文件） | 发现即修 |

> **AI 协作者特别提醒**：更新文档时同步检查 `docs/00-overview.md` 的需求状态表与 `docs/devlog/` 的归档情况——这两处是本仓库历史上最容易滞后的位置（如 M-035 曾无 devlog、R1-R12 状态曾长期停在"规划"）。

## 6. 文档健康度检查清单（每阶段结束跑一遍）

- [ ] `docs/00-overview.md` 需求状态表与代码实际一致？
- [ ] `docs/03-contracts.md` 覆盖所有对外接口（含最新 EngineSettings/DeviceInfoProvider/LanProtocol 等）？
- [ ] `docs/04-database.md` 表清单 = `AppDatabase.kt` 实体清单？
- [ ] `docs/01-architecture.md` 模块图 = `settings.gradle.kts` 实际模块？
- [ ] 无文档引用已删除的文件（`grep -r "WallpaperPresets\|filesearch" docs/` 应为空）？
- [ ] 每个 devlog 有对应代码提交，每个功能有 devlog？

---

*本文件随文档体系演化持续更新；新增分册时先在 §1 结构图登记。*
