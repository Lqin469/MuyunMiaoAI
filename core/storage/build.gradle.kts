// ============================================================
// core/storage/build.gradle.kts — 存储抽象模块构建配置
// 作用：声明存储模块的构建参数与依赖；承载 StorageProvider 与目录迁移（R4/R5）
// ============================================================

plugins {                                              // 本模块启用的插件
    alias(libs.plugins.android.library)                // Android 库插件（产出 AAR）
    alias(libs.plugins.kotlin.android)                 // Kotlin 支持
    alias(libs.plugins.kapt)                           // kapt 注解处理器（Hilt 需要）
    alias(libs.plugins.hilt)                           // Hilt 依赖注入插件
}

android {                                              // Android 构建配置块
    namespace = "com.memuo.core.storage"               // 包名空间
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
    implementation(libs.hilt.android)                  // Hilt 运行时（@Module/@Provides 注解）
    kapt(libs.hilt.compiler)                           // Hilt 注解处理器（编译期生成注入代码）
    implementation(libs.datastore.preferences)         // DataStore 偏好存储（壁纸配置/首次启动标记）
}
