// ============================================================
// app/build.gradle.kts — 应用壳模块构建配置
// 作用：声明应用自身的构建参数（包名/SDK 版本/依赖），产出可安装的 APK
// ============================================================

plugins {                                              // 本模块启用的插件
    alias(libs.plugins.android.application)            // Android 应用插件（生成 APK）
    alias(libs.plugins.kotlin.android)                 // Kotlin 支持
    alias(libs.plugins.kotlin.compose)                 // Compose 编译器插件
    alias(libs.plugins.kapt)                           // kapt 注解处理器（Hilt 需要）
    alias(libs.plugins.hilt)                           // Hilt 依赖注入插件
}

android {                                              // Android 构建配置块
    namespace = "com.memuo.app"                        // 包名空间（决定 R 类与 BuildConfig 包名）
    compileSdk = 36                                    // 编译用的 SDK 版本（Android 16）
    defaultConfig {                                    // 默认构建配置
        applicationId = "com.memuo.app"                // 应用唯一 ID（发布后不可改）
        minSdk = 26                                    // 最低支持 Android 8.0（Operit 同款门槛）
        targetSdk = 36                                 // 目标 SDK（适配最新系统行为）
        versionCode = 1                                // 版本号（整数，用于升级判断）
        versionName = "0.1.0"                          // 版本名（展示给用户）
    }
    buildFeatures {                                    // 构建特性开关
        compose = true                                 // 启用 Jetpack Compose UI
    }
    compileOptions {                                   // Java 字节码版本配置
        sourceCompatibility = JavaVersion.VERSION_17   // 源码兼容 Java 17
        targetCompatibility = JavaVersion.VERSION_17   // 目标字节码 Java 17
    }
    kotlinOptions {                                    // Kotlin 编译选项
        jvmTarget = "17"                               // Kotlin 也编译为 Java 17 字节码
    }
}

dependencies {                                         // 本模块依赖列表
    // ---- Compose / UI（界面相关）----
    implementation(platform(libs.compose.bom))         // 引入 Compose BOM：统一 Compose 库版本
    implementation(libs.compose.ui)                    // Compose UI 基础
    implementation(libs.compose.material3)             // Material3 组件（按钮/进度条等）
    implementation(libs.compose.ui.tooling.preview)    // 布局预览（AS 设计视图）
    implementation(libs.androidx.activity.compose)     // Activity 的 Compose 集成（setContent）
    implementation(libs.lifecycle.runtime.ktx)         // Lifecycle 运行时
    implementation(libs.lifecycle.viewmodel.compose)   // ViewModel 与 Compose 绑定

    // ---- DI（依赖注入）----
    implementation(libs.hilt.android)                  // Hilt 运行时
    kapt(libs.hilt.compiler)                           // Hilt 注解处理器（编译期生成注入代码）

    // ---- 模块装配（依赖方向：feature → core → 基础设施）----
    implementation(project(":feature:filesearch"))     // 文件检索业务模块（含进度条 UI）
    implementation(project(":feature:settings"))       // 设置业务模块（提供 SearchSettings 实现）
    implementation(project(":core:search"))            // 搜索能力模块（许可闸门/进度契约）
    implementation(project(":core:db"))                // 数据库模块（Room 全库）
    implementation(project(":core:storage"))           // 存储抽象模块（StorageProvider）
}
