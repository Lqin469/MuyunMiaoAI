package com.memuo.app                                    // 声明包名：com.memuo.app（应用壳模块的根包）

import android.app.Application                           // 导入 Application：Android 全局应用基类（整个 App 只创建一个实例）
import com.memuo.core.ingest.KnowledgeRepository          // 导入知识库仓库（R7 订阅的消费方）
import com.memuo.core.ingest.NoteBridge                   // 导入笔记事件总线
import dagger.hilt.android.HiltAndroidApp                // 导入 HiltAndroidApp 注解：Hilt 依赖注入的应用级入口
import kotlinx.coroutines.CoroutineScope                  // 导入 CoroutineScope：应用级协程作用域
import kotlinx.coroutines.Dispatchers                     // 导入 Dispatchers：协程调度器
import kotlinx.coroutines.SupervisorJob                   // 导入 SupervisorJob：子协程失败不影响其他
import javax.inject.Inject                                // 导入 Inject：字段注入注解

@HiltAndroidApp                                          // 注解：告诉 Hilt 这是依赖注入容器，启动时自动生成注入图
class MemoApp : Application() {                          // 自定义 Application 类：应用启动最先执行的地方

    @Inject lateinit var knowledgeRepository: KnowledgeRepository  // 注入知识库仓库（字段注入）
    @Inject lateinit var noteBridge: NoteBridge           // 注入笔记事件总线

    /** 应用级协程作用域（订阅 NoteBridge 用，跟随应用生命周期）。 */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)  // 后台作用域

    override fun onCreate() {                             // 应用创建回调
        super.onCreate()                                  // 调用父类初始化
        // 启动 R7 订阅：笔记增删改 → 自动同步进知识库（后台执行，不阻塞启动）
        knowledgeRepository.observeNoteBridge(appScope, noteBridge)  // 订阅笔记事件
    }
}
