// ============================================================
// core/search/build.gradle.kts — 搜索能力模块构建配置
// 作用：声明搜索模块的构建参数与依赖；本模块承载"搜索许可闸门 +
//       进度上报契约 + 提权管理（Shizuku/libsu）+ 文件索引实现"（M7）
// ============================================================

plugins {                                              // 本模块启用的插件
    alias(libs.plugins.android.library)                // Android 库插件（产出 AAR，不可独立运行）
    alias(libs.plugins.kotlin.android)                 // Kotlin 支持
    alias(libs.plugins.kapt)                           // kapt 注解处理器（Hilt 需要）
    alias(libs.plugins.hilt)                           // Hilt 依赖注入插件
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
    implementation(project(":core:db"))                // 依赖数据库模块（FileLocationDao 文件索引表）
    implementation(project(":core:storage"))           // 依赖存储模块（应用私有目录范围）

    implementation(libs.shizuku.api)                   // Shizuku API（提权检测/授权请求）
    implementation(libs.shizuku.provider)              // ShizukuProvider（运行时授权 ContentProvider）
    implementation(libs.libsu.core)                    // libsu（root shell 执行，L2 能力）
    implementation(libs.androidx.documentfile)         // DocumentFile（SAF 授权树遍历）

    implementation(libs.hilt.android)                  // Hilt 运行时
    kapt(libs.hilt.compiler)                           // Hilt 注解处理器
    implementation(libs.kotlinx.coroutines.android)    // 协程（StateFlow 进度流等）
}
