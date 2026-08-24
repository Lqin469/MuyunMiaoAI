// ============================================================
// core/ingest/build.gradle.kts — 内容入库模块构建配置
// 作用：声明入库模块的构建参数与依赖；承载解析/分块/入库/检索增强问答
// ============================================================

plugins {                                              // 本模块启用的插件
    alias(libs.plugins.android.library)                // Android 库插件（产出 AAR）
    alias(libs.plugins.kotlin.android)                 // Kotlin 支持
    alias(libs.plugins.kapt)                           // kapt 注解处理器（Hilt 需要）
    alias(libs.plugins.hilt)                           // Hilt 依赖注入插件
}

android {                                              // Android 构建配置块
    namespace = "com.memuo.core.ingest"                // 包名空间
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
    implementation(project(":core:db"))                // 依赖数据库模块（KbDao/NoteDao）
    implementation(project(":core:storage"))           // 依赖存储模块（解压临时目录 StorageProvider）
    implementation(project(":core:ai:embed"))          // 依赖嵌入模块（EmbeddingProvider/HybridRetriever）
    implementation(project(":core:ai:engine"))         // 依赖引擎模块（RagService 用 ChatEngine）
    implementation(project(":core:ai:memory"))         // 依赖记忆模块（RagService 并入长期记忆）

    implementation(libs.hilt.android)                  // Hilt 运行时
    kapt(libs.hilt.compiler)                           // Hilt 注解处理器

    implementation(libs.commons.compress)              // 压缩包解析（TAR/GZ/BZ2/XZ）
    implementation(libs.pdfbox.android)                // PDF 解析

    implementation(libs.kotlinx.coroutines.android)    // 协程
}
