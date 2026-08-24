package com.memuo.core.models                            // 声明包名：模型管理模块

import dagger.Module                                      // 导入 Module：Hilt 模块
import dagger.Provides                                    // 导入 Provides：提供依赖
import dagger.hilt.InstallIn                              // 导入 InstallIn
import dagger.hilt.components.SingletonComponent          // 导入 SingletonComponent
import okhttp3.OkHttpClient                               // 导入 OkHttpClient
import java.util.concurrent.TimeUnit                       // 导入 TimeUnit
import javax.inject.Named                                 // 导入 Named：限定符
import javax.inject.Singleton                             // 导入 Singleton

/**
 * 模型模块装配（ModelModule）—— 提供模型下载专用的 OkHttpClient（@Named 区分云端引擎的实例）。
 */
@Module                                                  // Hilt 模块
@InstallIn(SingletonComponent::class)                    // 单例作用域
object ModelModule {                                     // 模型装配

    /** 提供模型下载专用 OkHttpClient（长超时 + 自动重试，适配 470M 大文件下载）。 */
    @Provides                                           // 提供依赖
    @Singleton                                          // 单例
    @Named("modelDownload")                              // 限定符（区分云端引擎的 OkHttp）
    fun provideDownloadOkHttp(): OkHttpClient =          // 提供方法
        OkHttpClient.Builder()                           // 构造
            .connectTimeout(30, TimeUnit.SECONDS)        // 连接超时 30s
            .readTimeout(300, TimeUnit.SECONDS)          // 读超时 5min（大文件）
            .writeTimeout(300, TimeUnit.SECONDS)         // 写超时 5min
            .retryOnConnectionFailure(true)              // 连接失败自动重试
            .build()                                     // 构建
}
