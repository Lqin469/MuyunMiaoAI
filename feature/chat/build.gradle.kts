// ============================================================
// feature/chat/build.gradle.kts — 对话业务模块构建配置
// 作用：声明对话模块的构建参数与依赖；实现 AI 对话界面（流式渲染）
// ============================================================

plugins {                                              // 本模块启用的插件
    alias(libs.plugins.android.library)                // Android 库插件（产出 AAR）
    alias(libs.plugins.kotlin.android)                 // Kotlin 支持
    alias(libs.plugins.kotlin.compose)                 // Compose 编译器插件（Kotlin 2.0+ 必需）
    alias(libs.plugins.kapt)                           // kapt 注解处理器（Hilt 需要）
    alias(libs.plugins.hilt)                           // Hilt 依赖注入插件
}

android {                                              // Android 构建配置块
    namespace = "com.memuo.feature.chat"               // 包名空间
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
    implementation(project(":core:db"))                // 依赖数据库模块（会话/消息）
    implementation(project(":core:ai:engine"))         // 依赖引擎模块（ChatEngine/ChatEvent）
    implementation(project(":core:ai:memory"))         // 依赖记忆模块（每 N 轮提炼）

    implementation(platform(libs.compose.bom))         // Compose BOM 版本清单
    implementation(libs.compose.ui)                    // Compose UI 基础
    implementation(libs.compose.material3)             // Material3 组件
    implementation(libs.compose.material.icons.core)   // Material 基础图标（返回箭头）

    implementation(libs.hilt.android)                  // Hilt 运行时
    kapt(libs.hilt.compiler)                           // Hilt 注解处理器
    implementation(libs.hilt.navigation.compose)       // Hilt + Compose（hiltViewModel()）

    implementation(libs.lifecycle.viewmodel.compose)   // ViewModel 与 Compose 绑定
    implementation(libs.kotlinx.coroutines.android)    // 协程（状态流）
}
