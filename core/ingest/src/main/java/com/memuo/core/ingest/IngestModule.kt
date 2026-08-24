package com.memuo.core.ingest                            // 声明包名：内容入库模块

import dagger.Binds                                        // 导入 Binds：Hilt 接口绑定注解
import dagger.Module                                      // 导入 Module：Hilt 模块注解
import dagger.hilt.InstallIn                              // 导入 InstallIn：指定安装组件
import dagger.hilt.components.SingletonComponent          // 导入 SingletonComponent：应用级单例组件
import javax.inject.Singleton                             // 导入 Singleton：单例作用域

/**
 * Hilt 装配模块：把 OcrEngine 接口绑定到实现。
 * M6 桩阶段绑定 MnnOcrEngine（占位返回提示文本）；MNN AAR 落地后换真正实现只需改这里。
 */
@Module                                                    // 声明 Hilt 模块
@InstallIn(SingletonComponent::class)                      // 安装到应用级单例组件
abstract class IngestModule {                              // 抽象模块类（@Binds 需要抽象）

    /** 绑定：OcrEngine 接口 → MnnOcrEngine 实现（单例）。 */
    @Binds                                                 // 接口绑定注解
    @Singleton                                             // 单例作用域
    abstract fun bindOcrEngine(                            // 抽象绑定方法
        impl: MnnOcrEngine,                                // 参数是具体实现
    ): OcrEngine                                           // 返回类型是接口
}
