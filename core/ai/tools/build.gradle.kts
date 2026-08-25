// ============================================================
// core/ai/tools/build.gradle.kts — AI 工具调用总线模块构建配置
// 作用：ToolCallingBus（工具注册/分发）+ search_file/tell_location 工具（M7）
// ============================================================

plugins {                                              // 本模块启用的插件
    alias(libs.plugins.android.library)                // Android 库插件
    alias(libs.plugins.kotlin.android)                 // Kotlin 支持
    alias(libs.plugins.kapt)                           // kapt 注解处理器（Hilt 需要）
    alias(libs.plugins.hilt)                           // Hilt 依赖注入插件
}

android {                                              // Android 构建配置块
    namespace = "com.memuo.core.ai.tools"              // 包名空间
    compileSdk = 36                                    // 编译 SDK 版本
    defaultConfig {                                    // 默认配置
        minSdk = 26                                    // 最低支持 Android 8.0
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
    implementation(project(":core:search"))            // 依赖搜索模块（SearchService 检索能力）

    implementation(libs.hilt.android)                  // Hilt 运行时
    kapt(libs.hilt.compiler)                           // Hilt 注解处理器
    implementation(libs.kotlinx.coroutines.android)    // 协程
}
