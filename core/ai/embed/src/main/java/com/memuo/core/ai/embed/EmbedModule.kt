package com.memuo.core.ai.embed                          // 声明包名：嵌入（Embedding）模块

import dagger.Binds                                        // 导入 Binds：Hilt 接口绑定注解
import dagger.Module                                      // 导入 Module：Hilt 模块注解
import dagger.hilt.InstallIn                              // 导入 InstallIn：指定安装组件
import dagger.hilt.components.SingletonComponent          // 导入 SingletonComponent：应用级单例组件
import javax.inject.Singleton                             // 导入 Singleton：单例作用域

/**
 * Hilt 装配模块：把 EmbeddingProvider 接口绑定到实现。
 * M6 用 bge（MNN 本地）；模型未就绪时 MnnEmbeddingProvider 内部降级为 SimpleHash。
 */
@Module                                                    // 声明 Hilt 模块
@InstallIn(SingletonComponent::class)                      // 安装到应用级单例组件
abstract class EmbedModule {                               // 抽象模块类（@Binds 需要抽象）

    /** 绑定：EmbeddingProvider 接口 → MnnEmbeddingProvider（bge，MNN 本地，单例）。 */
    @Binds                                                 // 接口绑定注解
    @Singleton                                             // 单例作用域
    abstract fun bindEmbeddingProvider(                    // 抽象绑定方法
        impl: MnnEmbeddingProvider,                        // 参数是 bge 实现
    ): EmbeddingProvider                                   // 返回类型是接口
}
