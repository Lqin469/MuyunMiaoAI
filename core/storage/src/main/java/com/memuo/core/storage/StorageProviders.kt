package com.memuo.core.storage                            // 声明包名：core 层的存储抽象模块

import android.content.Context                            // 导入 Context：Android 应用上下文（获取私有目录）
import java.io.File                                       // 导入 File：本地文件路径

/**
 * 默认存储提供者：使用应用私有目录（无需任何存储权限，最安全）。
 * 路径示例：/data/data/com.memuo.app/files/muyunmiao/{db,models,knowledge,index}
 */
class DefaultStorageProvider(                             // 默认实现：数据全放应用私有目录
    private val context: Context,                         // 注入应用上下文
) : StorageProvider {                                     // 实现存储提供者接口

    /** 根目录：应用私有 files 目录下的 muyunmiao 子目录。 */
    override val root: File                               // 重写根目录属性
        get() = context.filesDir.resolve("muyunmiao")     // filesDir = /data/data/<包名>/files；resolve 拼接子路径

    /** 数据库目录 = root/db。 */
    override fun dbDir(): File = root.resolve("db")       // 返回 root 下的 db 子目录

    /** 模型目录 = root/models。 */
    override fun modelsDir(): File = root.resolve("models") // 返回 root 下的 models 子目录

    /** 知识库素材目录 = root/knowledge。 */
    override fun knowledgeDir(): File = root.resolve("knowledge") // 返回 root 下的 knowledge 子目录

    /** 索引目录 = root/index。 */
    override fun indexDir(): File = root.resolve("index") // 返回 root 下的 index 子目录

    /** 确保所有目录存在。 */
    override fun ensureDirs() {                           // 重写：创建目录
        listOf(root, dbDir(), modelsDir(), knowledgeDir(), indexDir()) // 组装所有需要存在的目录
            .forEach { it.mkdirs() }                      // 逐个 mkdirs（不存在则创建，含父目录）
    }
}

/**
 * 自定义存储提供者：使用用户指定的绝对路径（R5）。
 * 注意：使用外部绝对路径需要相应权限（MANAGE_EXTERNAL_STORAGE 或 root/Shizuku，M7 提权后开放），
 * 未授权时不应使用本实现。
 */
class CustomStorageProvider(                              // 自定义实现：数据放到用户指定目录
    private val customRoot: File,                         // 用户指定的根目录（绝对路径）
) : StorageProvider {                                     // 实现存储提供者接口

    /** 根目录：用户指定的绝对路径。 */
    override val root: File get() = customRoot            // 返回用户指定根目录

    /** 数据库目录 = root/db。 */
    override fun dbDir(): File = root.resolve("db")       // db 子目录

    /** 模型目录 = root/models。 */
    override fun modelsDir(): File = root.resolve("models") // models 子目录

    /** 知识库素材目录 = root/knowledge。 */
    override fun knowledgeDir(): File = root.resolve("knowledge") // knowledge 子目录

    /** 索引目录 = root/index。 */
    override fun indexDir(): File = root.resolve("index") // index 子目录

    /** 确保所有目录存在。 */
    override fun ensureDirs() {                           // 重写：创建目录
        listOf(root, dbDir(), modelsDir(), knowledgeDir(), indexDir()) // 组装目录列表
            .forEach { it.mkdirs() }                      // 逐个创建
    }
}
