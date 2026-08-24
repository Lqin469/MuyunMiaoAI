package com.memuo.core.models                            // 声明包名：模型管理模块

import com.memuo.core.storage.StorageProvider             // 导入存储提供者（模型目标目录）
import kotlinx.coroutines.Dispatchers                      // 导入调度器（IO 线程复制大文件）
import kotlinx.coroutines.withContext                      // 导入 withContext：切换调度器
import java.io.File                                        // 导入 File：本地文件
import javax.inject.Inject                                 // 导入 Inject：构造函数注入
import javax.inject.Singleton                              // 导入 Singleton：单例作用域

/**
 * 模型导入器（ModelImporter）—— 识别用户本地模型格式，并复制到 app 私有模型目录（M6）。
 *
 * 两条路径：
 *  - [importFromPath]：识别格式返回 ModelItem（元信息，不复制）；
 *  - [importMnnDir]：把 MNN 模型目录真正复制到 modelsDir()/llm/，供 LocalChatEngine 加载（R3）。
 */
@Singleton                                               // 单例
class ModelImporter @Inject constructor(                 // 构造函数注入
    private val storage: StorageProvider,                // 注入存储提供者（决定目标目录）
) {

    /** 导入一个本地文件/目录，返回 ModelItem；不支持则返回 null。 */
    fun importFromPath(path: String): ModelItem? {        // 导入方法（识别格式）
        val file = File(path)                             // 构造文件
        if (!file.exists()) return null                   // 不存在返回 null
        return when (detectFormat(file)) {                // 按格式分发
            Format.MNN_DIR -> importMnnDir(file)          // MNN 目录
            Format.GGUF    -> importGguf(file)            // GGUF 文件
            Format.UNKNOWN -> null                         // 未知
        }
    }

    /**
     * 把 MNN 模型目录复制到 app 私有目录 modelsDir()/llm/（供 LocalChatEngine 加载）。
     * @param sourceDir 源模型目录（含 config.json + llm.mnn + llm.mnn.weight + tokenizer.txt）
     * @return 复制成功返回 true；源目录不含 config.json 则返回 false
     */
    suspend fun importMnnToAppDir(sourceDir: File): Boolean = withContext(Dispatchers.IO) {  // IO 线程复制大文件
        val config = File(sourceDir, "config.json")      // 校验：必须有 config.json
        if (!config.exists()) return@withContext false    // 非法 MNN 目录
        val target = File(storage.modelsDir(), "llm")     // 目标目录 modelsDir/llm/
        if (target.exists()) target.deleteRecursively()   // 清掉旧模型（幂等导入）
        target.mkdirs()                                   // 建目标目录
        sourceDir.copyRecursively(target, overwrite = true)  // 复制（返回 null 表示失败）
        val ok = File(target, "config.json").exists()     // 校验复制结果
        ok                                                // 返回是否成功
    }

    /** 本地模型是否已就绪（modelsDir()/llm/config.json 存在）。 */
    fun hasLocalModel(): Boolean =                         // 模型就绪检测
        File(storage.modelsDir(), "llm/config.json").exists()  // config.json 存在即就绪

    /** 探测文件/目录格式。 */
    private fun detectFormat(file: File): Format {       // 格式探测
        if (file.isDirectory && File(file, "config.json").exists()) return Format.MNN_DIR  // 目录 + config.json → MNN
        if (file.isFile && file.name.endsWith(".gguf", ignoreCase = true)) return Format.GGUF  // .gguf 文件 → GGUF
        return Format.UNKNOWN                              // 其他
    }

    /** 识别 MNN 目录（构造项返回，不复制）。 */
    private fun importMnnDir(dir: File): ModelItem {      // MNN 目录识别
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

    /** 识别 GGUF 文件（构造项返回；实际需 llama.cpp 运行时，后续）。 */
    private fun importGguf(file: File): ModelItem {       // GGUF 识别
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
