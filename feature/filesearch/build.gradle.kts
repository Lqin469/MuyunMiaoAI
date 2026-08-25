// ============================================================
// feature/filesearch/build.gradle.kts — 文件检索业务模块构建配置
// 作用：声明文件检索页的构建参数与依赖；UI 层实现"进度条 + 停止"（ADR-002）
// ============================================================

plugins {                                              // 本模块启用的插件
    alias(libs.plugins.android.library)                // Android 库插件（产出 AAR）
    alias(libs.plugins.kotlin.android)                 // Kotlin 支持
    alias(libs.plugins.kotlin.compose)                 // Compose 编译器插件（Kotlin 2.0+ 必需）
    alias(libs.plugins.kapt)                           // kapt 注解处理器（Hilt 需要）
    alias(libs.plugins.hilt)                           // Hilt 依赖注入插件
}

android {                                              // Android 构建配置块
    namespace = "com.memuo.feature.filesearch"         // 包名空间
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
    implementation(project(":core:search"))            // 依赖搜索能力模块（许可闸门/索引/提权/检索）
    implementation(project(":core:storage"))           // 依赖存储模块（应用私有目录范围）

    implementation(platform(libs.compose.bom))         // Compose BOM 版本清单
    implementation(libs.compose.ui)                    // Compose UI 基础
    implementation(libs.compose.material3)             // Material3 组件（进度条/按钮）

    implementation(libs.hilt.android)                  // Hilt 运行时（@HiltViewModel 需要）
    kapt(libs.hilt.compiler)                           // Hilt 注解处理器

    implementation(libs.lifecycle.viewmodel.compose)   // ViewModel 与 Compose 绑定
    implementation(libs.kotlinx.coroutines.android)    // 协程（状态流）
}
