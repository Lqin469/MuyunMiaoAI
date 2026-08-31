// ============================================================
// core/db/build.gradle.kts — 数据库模块构建配置
// 作用：声明数据库模块的构建参数与依赖；承载 Room 全库（实体/DAO/AppDatabase）
// ============================================================

plugins {                                              // 本模块启用的插件
    alias(libs.plugins.android.library)                // Android 库插件（产出 AAR）
    alias(libs.plugins.kotlin.android)                 // Kotlin 支持
    alias(libs.plugins.kapt)                           // kapt 注解处理器（Room/Hilt 需要）
    alias(libs.plugins.hilt)                           // Hilt 依赖注入插件
}

android {                                              // Android 构建配置块
    namespace = "com.memuo.core.db"                    // 包名空间
    compileSdk = 36                                    // 编译 SDK 版本
    defaultConfig {                                    // 默认配置
        minSdk = 26                                    // 最低支持 Android 8.0
        // Room schema 导出目录：exportSchema=true 时生成版本化 schema JSON（供迁移校验与历史追溯）
        javaCompileOptions {                           // Java 编译选项
            annotationProcessorOptions {               // 注解处理器选项（kapt 读此配置）
                arguments += mapOf(                    // 追加参数（不覆盖已有）
                    "room.schemaLocation" to "$projectDir/schemas",  // schema JSON 输出到模块 schemas/ 目录
                )
            }
        }
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
    implementation(project(":core:storage"))           // 依赖存储模块（数据库路径走 StorageProvider）

    implementation(libs.room.runtime)                  // Room 运行时
    implementation(libs.room.ktx)                      // Room 协程扩展（Flow/suspend）
    kapt(libs.room.compiler)                           // Room 注解处理器（编译期生成实现代码）

    implementation(libs.hilt.android)                  // Hilt 运行时
    kapt(libs.hilt.compiler)                           // Hilt 注解处理器

    implementation(libs.kotlinx.coroutines.android)    // 协程（Flow 等）
}
