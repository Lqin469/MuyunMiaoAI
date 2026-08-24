package com.memuo.feature.settings                         // 声明包名：设置业务模块

import com.memuo.core.ai.engine.CloudConfigProvider       // 导入云端配置提供者接口
import com.memuo.core.ai.engine.EngineSettings             // 导入引擎设置接口
import com.memuo.core.search.consent.SearchSettings        // 导入搜索设置接口（core:search 契约）
import dagger.Binds                                        // 导入 Binds：Hilt 接口绑定注解
import dagger.Module                                      // 导入 Module：Hilt 模块注解
import dagger.hilt.InstallIn                              // 导入 InstallIn：指定安装组件
import dagger.hilt.components.SingletonComponent          // 导入 SingletonComponent：应用级单例组件
import javax.inject.Singleton                             // 导入 Singleton：单例作用域

/**
 * Hilt 装配模块：把 core 层的接口绑定到本模块实现（依赖倒置：core 不依赖 feature）。
 *  - SearchSettings → SettingsRepository
 *  - CloudConfigProvider → CloudConfigRepository
 */
@Module                                                    // 声明 Hilt 模块
@InstallIn(SingletonComponent::class)                      // 安装到应用级单例组件
abstract class SettingsModule {                            // 抽象模块类（用 @Binds 需要抽象）

    /** 绑定：SearchSettings 接口 → SettingsRepository 实现（单例）。 */
    @Binds                                                 // 接口绑定注解：告诉 Hilt 用哪个实现
    @Singleton                                             // 单例作用域
    abstract fun bindSearchSettings(                       // 抽象绑定方法（方法体由 Hilt 生成）
        repo: SettingsRepository,                          // 参数是具体实现
    ): SearchSettings                                      // 返回类型是接口

    /** 绑定：CloudConfigProvider 接口 → CloudConfigRepository 实现（单例）。 */
    @Binds                                                 // 接口绑定注解
    @Singleton                                             // 单例作用域
    abstract fun bindCloudConfigProvider(                  // 抽象绑定方法
        repo: CloudConfigRepository,                       // 参数是具体实现
    ): CloudConfigProvider                                 // 返回类型是接口

    /** 绑定：EngineSettings 接口 → SettingsRepository 实现（单例）。 */
    @Binds                                                 // 接口绑定注解
    @Singleton                                             // 单例作用域
    abstract fun bindEngineSettings(                       // 抽象绑定方法
        repo: SettingsRepository,                          // 参数是具体实现
    ): EngineSettings                                      // 返回类型是接口
}
