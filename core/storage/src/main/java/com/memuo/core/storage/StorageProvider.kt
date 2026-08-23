package com.memuo.core.storage                            // 声明包名：core 层的存储抽象模块

import java.io.File                                       // 导入 File：表示本地文件系统路径

/**
 * 存储提供者（StorageProvider）—— 架构不变式：所有"写文件 / 建目录"必须经本接口，禁止散落硬编码路径。
 *
 * 作用：把数据库、模型、知识库、索引等所有持久化位置统一收口到一处，
 * 从而支持 R4（单一数据库目录）与 R5（用户自定义存储目录）的切换。
 */
interface StorageProvider {                               // 存储提供者接口：定义各类目录的统一获取方式
    /** 存储根目录：所有子目录都挂在它下面。 */
    val root: File                                        // 根目录（默认=应用私有目录；自定义=用户指定目录）

    /** 数据库目录：Room 的 .db 文件放在这里。 */
    fun dbDir(): File                                     // 数据库目录

    /** 模型目录：下载/导入的 AI 模型（MNN/GGUF）放在这里。 */
    fun modelsDir(): File                                 // 模型目录

    /** 知识库目录：用户投喂的原始素材（文档/图片/压缩包）放在这里。 */
    fun knowledgeDir(): File                              // 知识库素材目录

    /** 索引目录：向量 / 文件索引 / 记忆等派生数据放在这里。 */
    fun indexDir(): File                                  // 索引与派生数据目录

    /** 确保所有目录存在（应用启动或切换存储位置时调用）。 */
    fun ensureDirs()                                      // 创建缺失的目录
}
