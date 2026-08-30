// ============================================================
// core/lan/build.gradle.kts — 局域网传输能力模块构建配置
// 作用：声明局域网传输模块的构建参数与依赖；本模块承载
//       「NSD 设备发现 + TCP 文件传输 + 断点续传」（2026-08-28 新增）
// ============================================================

plugins {                                              // 本模块启用的插件
    alias(libs.plugins.android.library)                // Android 库插件（产出 AAR）
    alias(libs.plugins.kotlin.android)                 // Kotlin 支持
    alias(libs.plugins.kapt)                           // kapt 注解处理器（Hilt 需要）
    alias(libs.plugins.hilt)                           // Hilt 依赖注入插件
}

android {                                              // Android 构建配置块
    namespace = "com.memuo.core.lan"                   // 包名空间（本模块的包根）
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
    implementation(project(":core:storage"))           // 依赖存储模块（传输接收目录）

    implementation(libs.hilt.android)                  // Hilt 运行时
    kapt(libs.hilt.compiler)                           // Hilt 注解处理器
    implementation(libs.kotlinx.coroutines.android)    // 协程（传输会话/状态流）
    // NSD 设备发现 / Socket 传输均使用 Android Framework 原生 API，零第三方网络库
}
