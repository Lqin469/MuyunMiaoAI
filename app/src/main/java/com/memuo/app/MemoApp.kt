package com.memuo.app                                    // 声明包名：com.memuo.app（应用壳模块的根包）

import android.app.Application                           // 导入 Application：Android 全局应用基类（整个 App 只创建一个实例）
import dagger.hilt.android.HiltAndroidApp                // 导入 HiltAndroidApp 注解：Hilt 依赖注入的应用级入口

@HiltAndroidApp                                          // 注解：告诉 Hilt 这是依赖注入容器，启动时自动生成注入图
class MemoApp : Application()                            // 自定义 Application 类：应用启动最先执行的地方（可放全局初始化）
