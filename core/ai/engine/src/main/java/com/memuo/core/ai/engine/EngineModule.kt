package com.memuo.core.ai.engine                          // 声明包名：AI 引擎模块

import dagger.Module                                      // 导入 Module：Hilt 模块注解
import dagger.Provides                                    // 导入 Provides：Hilt 提供方法注解
import dagger.hilt.InstallIn                              // 导入 InstallIn：指定安装组件
import dagger.hilt.components.SingletonComponent          // 导入 SingletonComponent：应用级单例组件
import okhttp3.OkHttpClient                               // 导入 OkHttpClient：HTTP 客户端
import javax.inject.Singleton                             // 导入 Singleton：单例作用域

/**
 * Hilt 装配模块：提供云端引擎所需的依赖（OkHttpClient + ChatEngine）。
 * M3 默认把 ChatEngine 绑定为云端实现；M6 加入本地 MNN 后改为按用户设置动态切换。
 */
@Module                                                    // 声明 Hilt 模块
@InstallIn(SingletonComponent::class)                      // 安装到应用级单例组件
object EngineModule {                                      // 单例对象：提供引擎依赖

    /** 提供 OkHttpClient（单例，云端 SSE 用；含超时配置，M-027 增强）。 */
    @Provides                                              // 标记为提供依赖
    @Singleton                                             // 单例（复用连接池）
    fun provideOkHttpClient(): OkHttpClient =              // 提供方法
        OkHttpClient.Builder()                             // 构造 OkHttp
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)   // 连接超时 10s（快速失败供重试）
            .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)     // 读超时 120s（SSE 长连接）
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)     // 写超时 30s
            .retryOnConnectionFailure(false)               // 关闭底层自动重试（重试由 CloudApiClient 显式控制，防重复）
            .build()                                       // 构建

    /** 提供 ChatEngine（M6 起经 EngineRouter 按用户设置动态切换本地/云端）。 */
    @Provides                                              // 标记为提供依赖
    @Singleton                                             // 单例
    fun provideChatEngine(router: EngineRouter): ChatEngine = router  // 路由器实现（内部按设置路由）
}
