package com.memuo.core.models                            // 声明包名：模型管理模块

import android.content.Context                            // 导入 Context：SAF 目录读取
import android.net.Uri                                    // 导入 Uri：SAF 目录标识
import androidx.documentfile.provider.DocumentFile         // 导入 DocumentFile：SAF 目录遍历
import com.memuo.core.storage.StorageProvider             // 导入存储提供者（模型目标目录）
import kotlinx.coroutines.Dispatchers                      // 导入调度器（IO 线程复制大文件）
import kotlinx.coroutines.withContext                      // 导入 withContext：切换调度器
import java.io.File                                        // 导入 File：本地文件
import java.io.FileOutputStream                            // 导入 FileOutputStream：写文件
import javax.inject.Inject                                 // 导入 Inject：构造函数注入
import javax.inject.Singleton                              // 导入 Singleton：单例作用域

/**
 * 模型导入器（ModelImporter）—— 识别/检测/导入本地 MNN 模型（M6/M-011）。
 *
 * 能力：
 *  - [detectMnnModel]：检测目录是否为合法 MNN 模型（config.json + llm.mnn + llm.mnn.weight）；
 *  - [importMnnToAppDir]：从绝对路径目录复制到 modelsDir()/llm/；
 *  - [importFromUri]：从 SAF 目录（系统文件夹选择器）检测并复制到 modelsDir()/llm/。
 */
@Singleton                                               // 单例
class ModelImporter @Inject constructor(                 // 构造函数注入
    private val storage: StorageProvider,                // 注入存储提供者（决定目标目录）
) {

    /** 检测目录是否为合法 MNN 模型目录（含 config.json + llm.mnn + llm.mnn.weight）。 */
    fun detectMnnModel(dir: File): Boolean =             // 模型检测（单目录）
        File(dir, "config.json").exists() &&             // config.json（createLLM 入口）
            File(dir, "llm.mnn").exists() &&             // 模型结构
            File(dir, "llm.mnn.weight").exists()         // 模型权重

    /** 校验模型完整性：三项齐全 + 权重文件大小合理（>100MB，防复制不完整）。 */
    private fun verifyModel(dir: File): Boolean {        // 完整性校验
        if (!detectMnnModel(dir)) return false           // 三项齐全
        return File(dir, "llm.mnn.weight").length() > 100L * 1024 * 1024  // 权重 >100MB（Qwen 0.8B 约 449M）
    }

    /** 递归查找含 config.json + llm.mnn + llm.mnn.weight 的目录（最多 5 层，兼容 HF 缓存嵌套）。 */
    private fun findModelDir(dir: File, depth: Int = 0): File? {  // 递归查找模型目录
        if (depth > 5) return null                        // 防过深
        if (detectMnnModel(dir)) return dir               // 当前目录即模型目录
        dir.listFiles()?.forEach { child ->               // 遍历子项
            if (child.isDirectory) {                      // 子目录
                findModelDir(child, depth + 1)?.let { return it }  // 递归
            }
        }
        return null                                       // 未找到
    }

    /** 本地模型是否已就绪（modelsDir()/llm/config.json 存在）。 */
    fun hasLocalModel(): Boolean =                        // 模型就绪检测
        File(storage.modelsDir(), "llm/config.json").exists()  // config.json 存在即就绪

    /**
     * 从绝对路径目录导入 MNN 模型：递归定位模型目录，再复制到 modelsDir()/llm/。
     * @return 检测失败返回 false；复制成功返回 true
     */
    suspend fun importMnnToAppDir(sourceDir: File): Boolean = withContext(Dispatchers.IO) {  // IO 线程复制
        val modelDir = findModelDir(sourceDir) ?: return@withContext false  // 递归定位（含外层目录）
        val target = File(storage.modelsDir(), "llm")     // 目标目录
        if (target.exists()) target.deleteRecursively()   // 清掉旧模型（幂等）
        target.mkdirs()                                   // 建目录
        modelDir.copyRecursively(target, overwrite = true)  // 复制
        verifyModel(target)                            // 校验结果（三项齐全）
    }

    /**
     * 从 SAF 目录 Uri 导入模型（系统文件夹选择器选中后调用）：
     * 递归定位模型目录（兼容"选外层目录 / HF 缓存嵌套"），再复制到 modelsDir()/llm/。
     * @return 检测失败或复制失败返回 false
     */
    suspend fun importFromUri(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {  // IO 线程
        val root = DocumentFile.fromTreeUri(context, uri) ?: return@withContext false  // 取目录根
        val modelDir = findModelDirInTree(root) ?: return@withContext false  // 递归定位模型目录
        val target = File(storage.modelsDir(), "llm")     // 目标目录
        if (target.exists()) target.deleteRecursively()   // 清旧
        target.mkdirs()                                   // 建目录
        val ok = copyDocumentTree(context, modelDir, target)  // 递归复制模型目录
        ok && verifyModel(target)                      // 校验（三项齐全）
    }

    /** 判断 DocumentFile 是否为合法模型目录（含 config.json + llm.mnn + llm.mnn.weight）。 */
    private fun isModelDir(doc: DocumentFile): Boolean {  // SAF 模型目录判断
        val names = doc.listFiles().mapNotNull { it.name }.toSet()  // 收集文件名
        return "config.json" in names && "llm.mnn" in names && "llm.mnn.weight" in names  // 三项齐全
    }

    /** 递归查找 SAF 目录树中的模型目录（最多 5 层，兼容 HF 缓存 `snapshots/_no_sha_` 嵌套）。 */
    private fun findModelDirInTree(doc: DocumentFile, depth: Int = 0): DocumentFile? {  // 递归查找
        if (depth > 5) return null                        // 防过深
        if (isModelDir(doc)) return doc                   // 当前即模型目录
        doc.listFiles().forEach { child ->                // 遍历子项
            if (child.isDirectory) {                      // 子目录
                findModelDirInTree(child, depth + 1)?.let { return it }  // 递归
            }
        }
        return null                                       // 未找到
    }

    /** 递归复制 SAF 目录树到本地目录（DocumentFile → File）。 */
    private fun copyDocumentTree(context: Context, doc: DocumentFile, target: File): Boolean {  // 递归复制
        return when {                                     // 按类型分发
            doc.isDirectory -> {                          // 目录
                target.mkdirs()                           // 建子目录
                doc.listFiles().all { child ->            // 遍历子项（全部成功才算成功）
                    copyDocumentTree(context, child, File(target, child.name ?: "unnamed"))  // 递归
                }
            }
            doc.isFile -> {                               // 文件
                context.contentResolver.openInputStream(doc.uri)?.use { input ->  // 打开输入流
                    FileOutputStream(target).use { output -> input.copyTo(output) }  // 写本地文件
                } != null                                 // 复制成功返回 true
            }
            else -> false                                 // 其他（不可访问）
        }
    }

    /** 识别一个本地文件/目录格式，返回 ModelItem；不支持则返回 null。 */
    fun importFromPath(path: String): ModelItem? {        // 格式识别（元信息）
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
        if (file.isFile && file.name.endsWith(".gguf", ignoreCase = true)) return Format.GGUF  // .gguf → GGUF
        return Format.UNKNOWN                              // 其他
    }

    /** 识别 MNN 目录（构造项返回，不复制）。 */
    private fun importMnnDir(dir: File): ModelItem {      // MNN 目录识别
        return ModelItem(                                 // 构造项
            id = "local-${dir.name}-${dir.absolutePath.hashCode()}",  // 本地 ID
            name = dir.name,                              // 显示名
            kind = ModelKind.LLM,                         // 默认假设 LLM
            quant = "MNN",                                // 标记
            source = ModelSource.LOCAL_IMPORT,            // 来源
            downloadUrl = null,                            // 本地无下载
            sha256 = null,                                // 后续计算
            sizeBytes = dir.walkTopDown().filter { it.isFile }.sumOf { it.length() },  // 总大小
            minRamMb = 4096,                              // 默认要求
            minStorageMb = 0,                             // 已在本地
            cpuNote = "ARMv8",                            // 假设
            gpuNote = "未知",                              // 未探测
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
            sha256 = null,                                // 后续计算
            sizeBytes = file.length(),                    // 文件大小
            minRamMb = 4096,                              // 默认要求
            minStorageMb = 0,                             // 本地无
            cpuNote = "ARMv8",                            // 假设
            gpuNote = "未知",                              // 未探测
        )
    }

    /** 文件格式枚举。 */
    private enum class Format { MNN_DIR, GGUF, UNKNOWN }
}
