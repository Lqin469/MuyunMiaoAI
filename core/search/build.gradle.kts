// ============================================================
// core/search/build.gradle.kts — 搜索能力模块构建配置
// 作用：声明搜索模块的构建参数与依赖；本模块承载"搜索许可闸门 +
//       进度上报契约"（用户强制约束，详见 docs/privacy-search-consent.md）
// ============================================================

plugins {                                              // 本模块启用的插件
    alias(libs.plugins.android.library)                // Android 库插件（产出 AAR，不可独立运行）
    alias(libs.plugins.kotlin.android)                 // Kotlin 支持
}

android {                                              // Android 构建配置块
    namespace = "com.memuo.core.search"                // 包名空间（本模块的包根）
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
    implementation(libs.kotlinx.coroutines.android)    // 协程（StateFlow 进度流等）
}
