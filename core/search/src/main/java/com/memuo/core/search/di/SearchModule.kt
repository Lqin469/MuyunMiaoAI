package com.memuo.core.search.di                          // 声明包名：搜索模块的"依赖注入"子包

import com.memuo.core.search.consent.SearchConsentGate    // 导入许可闸门
import com.memuo.core.search.consent.SearchSettings        // 导入搜索设置接口
import com.memuo.core.search.index.FileIndexer             // 导入索引器接口
import com.memuo.core.search.index.FileIndexerImpl         // 导入索引器实现
import com.memuo.core.search.service.SearchService         // 导入检索服务接口
import com.memuo.core.search.service.SearchServiceImpl     // 导入检索服务实现
import dagger.Binds                                        // 导入 Binds：接口绑定
import dagger.Module                                       // 导入 Module：Hilt 模块
import dagger.Provides                                     // 导入 Provides：提供实例
import dagger.hilt.InstallIn                               // 导入 InstallIn
import dagger.hilt.components.SingletonComponent           // 导入单例组件
import javax.inject.Singleton                               // 导入 Singleton

/**
 * 搜索模块装配（SearchModule）—— 把 M7 的实现类绑定到接口（Hilt）。
 * FileIndexer → FileIndexerImpl；SearchService → SearchServiceImpl；
 * SearchConsentGate 由 SearchSettings（feature:settings 实现）构造。
 */
@Module                                                   // Hilt 模块
@InstallIn(SingletonComponent::class)                     // 单例作用域
abstract class SearchModule {                             // 抽象模块（@Binds 需要抽象类）

    /** 绑定：FileIndexer 接口 → FileIndexerImpl 实现（单例）。 */
    @Binds                                                // 接口绑定
    @Singleton                                            // 单例
    abstract fun bindFileIndexer(                         // 抽象绑定方法
        impl: FileIndexerImpl,                            // 实现类
    ): FileIndexer                                        // 接口类型

    /** 绑定：SearchService 接口 → SearchServiceImpl 实现（单例）。 */
    @Binds                                                // 接口绑定
    @Singleton                                            // 单例
    abstract fun bindSearchService(                       // 抽象绑定方法
        impl: SearchServiceImpl,                          // 实现类
    ): SearchService                                      // 接口类型

    companion object {                                    // 伴生对象：@Provides 静态方法区

        /** 提供 SearchConsentGate（依赖 SearchSettings，由 feature:settings 提供实现）。 */
        @Provides                                        // 提供实例
        @Singleton                                       // 单例
        fun provideConsentGate(settings: SearchSettings): SearchConsentGate =  // 提供方法
            SearchConsentGate(settings)                  // 构造许可闸门
    }
}
