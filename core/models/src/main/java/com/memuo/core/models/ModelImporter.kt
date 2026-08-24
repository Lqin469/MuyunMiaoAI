package com.memuo.core.models                            // 声明包名：模型管理模块

import java.io.File                                       // 导入 File：本地文件
import javax.inject.Inject                                 // 导入 Inject：构造函数注入
import javax.inject.Singleton                              // 导入 Singleton：单例作用域

/**
 * 模型导入器（ModelImporter）—— 识别用户从本地导入的模型格式并注册（M6）。
 * v1：识别 MNN 目录（含 config.json）+ GGUF 单文件（占位）；M6+ 完善校验/解压。
 */
@Singleton                                               // 单例
class ModelImporter @Inject constructor() {             // 构造函数注入

    /** 导入一个本地文件/目录，返回 ModelItem；不支持则返回 null。 */
    fun importFromPath(path: String): ModelItem? {        // 导入方法
        val file = File(path)                             // 构造文件
        if (!file.exists()) return null                   // 不存在返回 null
        return when (detectFormat(file)) {                // 按格式分发
            Format.MNN_DIR -> importMnnDir(file)          // MNN 目录
            Format.GGUF    -> importGguf(file)            // GGUF 文件
            Format.UNKNOWN -> null                         // 未知
        }
    }

    /** 探测文件/目录格式。 */
    private fun detectFormat(file: File): Format {       // 格式探测
        if (file.isDirectory && File(file, "config.json").exists()) return Format.MNN_DIR  // 目录 + config.json → MNN
        if (file.isFile && file.name.endsWith(".gguf", ignoreCase = true)) return Format.GGUF  // .gguf 文件 → GGUF
        return Format.UNKNOWN                              // 其他
    }

    /** 导入 MNN 目录（v1：构造项返回；实际校验/注册后续补）。 */
    private fun importMnnDir(dir: File): ModelItem {      // MNN 目录导入
        return ModelItem(                                 // 构造项
            id = "local-${dir.name}-${dir.absolutePath.hashCode()}",  // 本地 ID
            name = dir.name,                              // 显示名
            kind = ModelKind.LLM,                         // M6 默认假设 LLM
            quant = "MNN",                                // 标记
            source = ModelSource.LOCAL_IMPORT,            // 来源
            downloadUrl = null,                            // 本地导入无下载
            sha256 = null,                                // M6 完善时计算
            sizeBytes = dir.walkTopDown().filter { it.isFile }.sumOf { it.length() },  // 目录总大小
            minRamMb = 4096,                              // 默认要求
            minStorageMb = 0,                             // 已在本地
            cpuNote = "ARMv8",                            // 假设
            gpuNote = "未知",                              // 导入时未探测
        )
    }

    /** 导入 GGUF 文件（v1：返回项；实际需走 llama.cpp 运行时，M6 后续）。 */
    private fun importGguf(file: File): ModelItem {       // GGUF 导入
        return ModelItem(                                 // 构造项
            id = "local-gguf-${file.nameWithoutExtension}",  // ID
            name = file.nameWithoutExtension,              // 显示名
            kind = ModelKind.LLM,                         // GGUF 通常 LLM
            quant = "GGUF",                               // 标记
            source = ModelSource.LOCAL_IMPORT,            // 来源
            downloadUrl = null,                            // 本地无下载
            sha256 = null,                                // M6 完善
            sizeBytes = file.length(),                    // 文件大小
            minRamMb = 4096,                              // 默认要求
            minStorageMb = 0,                             // 本地无
            cpuNote = "ARMv8",                            // 假设
            gpuNote = "未知",                              // 导入时未探测
        )
    }

    /** 文件格式枚举。 */
    private enum class Format { MNN_DIR, GGUF, UNKNOWN }
}
