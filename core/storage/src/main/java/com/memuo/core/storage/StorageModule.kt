package com.memuo.core.storage                            // 声明包名：core 层的存储抽象模块

import android.content.Context                            // 导入 Context：用于构建默认存储提供者
import dagger.Module                                      // 导入 Module：Hilt 的模块注解
import dagger.Provides                                    // 导入 Provides：Hilt 的提供方法注解
import dagger.hilt.android.qualifiers.ApplicationContext  // 导入 @ApplicationContext：明确告知 Hilt 此 Context 为应用级
import dagger.hilt.InstallIn                              // 导入 InstallIn：指定模块安装到哪个组件
import dagger.hilt.components.SingletonComponent          // 导入 SingletonComponent：应用级单例组件
import javax.inject.Singleton                             // 导入 Singleton：单例作用域

/**
 * Hilt 装配模块：向依赖注入容器提供 StorageProvider 的实例。
 * 当前默认绑定 DefaultStorageProvider（应用私有目录）；后续在设置页支持自定义目录时切换实现。
 */
@Module                                                    // 声明这是一个 Hilt 模块
@InstallIn(SingletonComponent::class)                      // 安装到应用级单例组件（整个应用共享一个实例）
object StorageModule {                                     // 单例对象：提供存储相关依赖

    /** 提供 StorageProvider 实例（默认实现，单例）。 */
    @Provides                                              // 标记为"提供依赖"的方法
    @Singleton                                             // 声明单例作用域（全应用只创建一次）
    fun provideStorageProvider(@ApplicationContext context: Context): StorageProvider =  // @ApplicationContext 明确告知 Hilt 此 Context 为应用级
        DefaultStorageProvider(context)                    // 返回默认存储提供者（应用私有目录）
}
