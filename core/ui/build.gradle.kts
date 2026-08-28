// ============================================================
// core/ui/build.gradle.kts — 共享 UI 模块构建配置
// 作用：承载品牌主题（沐云杪 v19 设计令牌）、通用组件、手绘图标、Toast、位图加载
// 说明：界面原型 HTML 迁移为 Compose 原生实现时新增，供 feature 层复用
// ============================================================

plugins {                                              // 本模块启用的插件
    alias(libs.plugins.android.library)                // Android 库插件（产出 AAR）
    alias(libs.plugins.kotlin.android)                 // Kotlin 支持
    alias(libs.plugins.kotlin.compose)                 // Compose 编译器插件（Kotlin 2.0+ 必需）
}

android {                                              // Android 构建配置块
    namespace = "com.memuo.core.ui"                    // 包名空间
    compileSdk = 36                                    // 编译 SDK 版本
    defaultConfig {                                    // 默认配置
        minSdk = 26                                    // 最低支持 Android 8.0
    }
    buildFeatures {                                    // 构建特性开关
        compose = true                                 // 本模块使用 Compose 写 UI
    }
    compileOptions {                                   // Java 字节码版本
        sourceCompatibility = JavaVersion.VERSION_17   // 源码兼容 Java 17
        targetCompatibility = JavaVersion.VERSION_17   // 目标字节码 Java 17
    }
    kotlinOptions {                                    // Kotlin 编译选项
        jvmTarget = "17"                               // 编译为 Java 17 字节码
    }
}

dependencies {                                         // 本模块依赖列表
    implementation(platform(libs.compose.bom))         // Compose BOM 版本清单
    implementation(libs.compose.ui)                    // Compose UI 基础（组件/图形/手势）
    implementation(libs.compose.material3)             // Material3 组件（主题基座）
    implementation(libs.androidx.core.ktx)             // AndroidX 核心（Context 扩展）
}
