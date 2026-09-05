package com.memuo.core.storage                            // 声明包名：core 层的存储抽象模块

import android.content.Context                            // 导入 Context：用于构建默认存储提供者
import dagger.Module                                      // 导入 Module：Hilt 的模块注解
import dagger.Provides                                    // 导入 Provides：Hilt 的提供方法注解
import dagger.hilt.android.qualifiers.ApplicationContext  // 导入 @ApplicationContext：明确告知 Hilt 此 Context 为应用级
import dagger.hilt.InstallIn                              // 导入 InstallIn：指定模块安装到哪个组件
import dagger.hilt.components.SingletonComponent          // 导入 SingletonComponent：应用级单例组件
import kotlinx.coroutines.runBlocking                      // 导入 runBlocking：同步读自定义路径偏好
import java.io.File                                       // 导入 File：自定义根目录
import javax.inject.Singleton                             // 导入 Singleton：单例作用域

/**
 * Hilt 装配模块：向依赖注入容器提供 StorageProvider 的实例。
 * 根据 AppPrefs.customStoragePath 选择实现：空 → DefaultStorageProvider；非空 → CustomStorageProvider（R5）。
 */
@Module                                                    // 声明这是一个 Hilt 模块
@InstallIn(SingletonComponent::class)                      // 安装到应用级单例组件（整个应用共享一个实例）
object StorageModule {                                     // 单例对象：提供存储相关依赖

    /** 提供 StorageProvider 实例：按自定义目录偏好选默认/自定义实现（单例）。 */
    @Provides                                              // 标记为"提供依赖"的方法
    @Singleton                                             // 声明单例作用域（全应用只创建一次）
    fun provideStorageProvider(                            // 提供方法
        @ApplicationContext context: Context,             // 应用级 Context
        appPrefs: AppPrefs,                               // 注入应用偏好（读自定义路径）
    ): StorageProvider {                                   // 返回存储提供者
        val customPath = runBlocking { appPrefs.customStoragePath() }  // 同步读自定义路径（首读快）
        return if (customPath.isNullOrBlank()) {           // 未自定义
            DefaultStorageProvider(context)                // 默认：应用私有目录
        } else {                                          // 已自定义
            CustomStorageProvider(File(customPath))        // 自定义：用户指定绝对路径
        }
    }
}
