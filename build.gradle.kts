// ============================================================
// build.gradle.kts（根）— 根工程构建脚本
// 作用：只声明各插件的版本（apply false），统一由子模块按需启用
// ============================================================

plugins {                                              // 根工程插件声明块
    alias(libs.plugins.android.application) apply false // Android 应用插件（声明版本，app 模块启用）
    alias(libs.plugins.android.library) apply false     // Android 库插件（core/feature 模块启用）
    alias(libs.plugins.kotlin.android) apply false      // Kotlin Android 插件
    alias(libs.plugins.kotlin.compose) apply false      // Kotlin Compose 编译器插件（配合 Kotlin 2.x）
    alias(libs.plugins.kapt) apply false                // kapt 注解处理器（Room/Hilt 生成代码用）
    alias(libs.plugins.hilt) apply false                // Hilt 依赖注入插件
}
