// ============================================================
// settings.gradle.kts — Gradle 工程设置（模块注册 + 依赖仓库）
// 作用：告诉 Gradle 本工程包含哪些模块、从哪里下载依赖
// ============================================================

pluginManagement {                                    // 插件管理区：决定"构建插件"从哪里下载
    repositories {                                    // 插件仓库列表
        google {                                      // Google 仓库（Android 官方插件/AndroidX）
            content {                                 // 内容过滤器：只允许特定包名，加速解析
                includeGroupByRegex("com\\.android.*") // 允许 com.android.*（AGP 插件）
                includeGroupByRegex("com\\.google.*")  // 允许 com.google.*（Hilt 等）
                includeGroupByRegex("androidx.*")      // 允许 androidx.*（Jetpack 库）
            }
        }
        mavenCentral()                                // Maven 中央仓库（Kotlin/OkHttp 等）
        gradlePluginPortal()                          // Gradle 官方插件门户
    }
}

dependencyResolutionManagement {                      // 依赖解析管理区：决定"依赖库"从哪里下载
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS) // 禁止子模块自行声明仓库（统一管理）
    repositories {                                    // 依赖仓库列表
        google()                                      // Google 仓库
        mavenCentral()                                // Maven 中央仓库
        maven("https://jitpack.io")                   // JitPack 仓库（junrar / libsu 等 GitHub 项目）
    }
}

rootProject.name = "MuyunMiaoAI"                      // 工程根名称（与 GitHub 仓库名保持一致）

include(":app")                                       // 注册应用壳模块（可运行的 APK）

// :core —— 能力层（只依赖接口，被 feature 层复用）
include(":core:ui")                                   // 共享 UI：主题/通用组件/图标/Toast（HTML 原型迁移新增）
include(":core:storage")                              // 存储抽象与自定义目录（R4/R5）
include(":core:db")                                   // Room 数据库全库 + FTS5（M1 实现）
include(":core:ai:engine")                            // AI 引擎：本地 MNN / 云端 API 双实现
include(":core:ai:embed")                             // 文本嵌入（Embedding）双轨实现
include(":core:ai:tools")                             // AI 工具调用总线（search_file 等）
include(":core:ai:memory")                            // 会话自动记忆提炼（R6）
include(":core:ingest")                               // 文档/图片/压缩包解析入库（R8/R9）
include(":core:search")                               // 文件索引 + 搜索许可闸门（R11）
include(":core:models")                               // 模型管理：下载/评估/导入（R2/R3）
include(":core:device")                               // 设备检测：硬件信息 + 满足度判定（2026-08-28 新增）
include(":core:lan")                                  // 局域网传输：NSD 发现 + 断点续传（2026-08-28 新增）

// :feature —— 业务层（具体界面与用例）
include(":feature:notes")                             // 常规备忘录模块
include(":feature:chat")                              // AI 对话模块
include(":feature:knowledge")                         // 知识库管理模块
include(":feature:settings")                          // 设置模块（引擎/存储/隐私开关）
