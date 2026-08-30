package com.memuo.core.models                            // 声明包名：模型管理模块

import android.content.Context                            // 导入 Context：SAF 目录读取
import android.net.Uri                                    // 导入 Uri：SAF 目录标识
import androidx.documentfile.provider.DocumentFile         // 导入 DocumentFile：SAF 目录遍历
import com.memuo.core.storage.StorageProvider             // 导入存储提供者（模型目标目录）
import kotlinx.coroutines.Dispatchers                      // 导入调度器（IO 线程复制大文件）
import kotlinx.coroutines.withContext                      // 导入 withContext：切换调度器
import org.json.JSONObject                                 // 导入 JSONObject：解析 config.json 的 is_visual
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

    /** MNN 模型基础必需文件（纯文本 LLM 与多模态 VLM 都必需）。 */
    private val BASE_FILES = listOf(                      // 基础文件清单
        "config.json", "llm.mnn", "llm.mnn.weight", "tokenizer.txt",  // 文本 LLM 核心
    )

    /** 视觉模型附加文件（仅 config.json 中 is_visual:true 时必需）。 */
    private val VISUAL_FILES = listOf("visual.mnn", "visual.mnn.weight")  // 多模态视觉模型附加文件

    /** 读取 config.json 判断是否为多模态视觉模型。 */
    private fun isVisualModel(dir: File): Boolean =       // 是否视觉模型
        runCatching {
            JSONObject(File(dir, "config.json").readText()).optBoolean("is_visual", false)  // 解析 is_visual 字段
        }.getOrDefault(false)                             // 读取失败按纯文本处理

    /** 当前模型目录应具备的必需文件清单（纯文本/多模态动态判定）。 */
    private fun requiredFiles(dir: File): List<String> =  // 必需文件清单
        BASE_FILES + if (isVisualModel(dir)) VISUAL_FILES else emptyList()  // 多模态才要求视觉文件

    fun detectMnnModel(dir: File): Boolean =             // 模型检测（单目录）
        requiredFiles(dir).all { File(dir, it).exists() }  // 必需文件齐全

    /** 返回目录中缺失的必需文件（诊断用，空列表 = 齐全）。 */
    fun missingFiles(dir: File): List<String> =          // 缺失文件诊断
        requiredFiles(dir).filterNot { File(dir, it).exists() }  // 返回缺失项

    /** 校验模型完整性：必需文件齐全 + 关键文件大小合理（M-027 增强，防复制中断产生不完整文件）。 */
    private fun verifyModel(dir: File): Boolean {        // 完整性校验
        if (!detectMnnModel(dir)) return false           // 必需文件齐全
        val visual = isVisualModel(dir)                  // 是否多模态
        // 关键文件最小合理大小（Qwen3.5-0.8B 实测：weight 449MB / visual.weight 63MB / llm.mnn 2MB / visual.mnn 251KB）
        return File(dir, "llm.mnn.weight").length() > 100L * 1024 * 1024 &&      // 文本权重 >100MB
            File(dir, "llm.mnn").length() > 100L * 1024 &&                       // 模型结构 >100KB
            (!visual || (File(dir, "visual.mnn.weight").length() > 30L * 1024 * 1024 &&  // 视觉权重 >30MB（仅多模态））
                         File(dir, "visual.mnn").length() > 100L * 1024))                     // 视觉结构 >100KB（仅多模态）
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

    /** 判断 DocumentFile 是否为合法模型目录（必需文件齐全：config.json + llm.mnn + llm.mnn.weight + tokenizer.txt）。 */
    /** 判断 DocumentFile 是否为合法模型目录（基础文件齐全；视觉文件在复制后由 verifyModel 校验）。 */
    private fun isModelDir(doc: DocumentFile): Boolean {  // SAF 模型目录判断
        val names = doc.listFiles().mapNotNull { it.name }.toSet()  // 收集文件名
        return BASE_FILES.all { it in names }            // 基础必需文件齐全（视觉文件复制后校验）
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

    /**
     * 导入 GGUF 模型文件到模型目录（modelsDir()/gguf/，M-027 落地）。
     * GGUF 运行需要 llama.cpp 运行时（后续里程碑），本次完成「导入 + 管理」。
     * @return 导入成功返回 true（复制完成 + 大小校验）
     */
    suspend fun importGgufToAppDir(sourceFile: File): Boolean = withContext(Dispatchers.IO) {  // IO 线程
        runCatching {                                    // 容错
            if (!sourceFile.isFile) return@withContext false  // 非文件
            val targetDir = File(storage.modelsDir(), "gguf")  // GGUF 模型目录
            targetDir.mkdirs()                            // 建目录
            val target = File(targetDir, sourceFile.name) // 目标文件
            sourceFile.copyTo(target, overwrite = true)   // 复制（同名覆盖）
            target.length() == sourceFile.length()        // 大小校验（防复制不完整）
        }.getOrDefault(false)                             // 失败返回 false
    }

    /** 从 SAF 文件 Uri 导入 GGUF（系统文件选择器选中单个 .gguf 后调用）。 */
    suspend fun importGgufFromUri(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {  // IO 线程
        runCatching {                                    // 容错
            val targetDir = File(storage.modelsDir(), "gguf")  // GGUF 模型目录
            targetDir.mkdirs()                            // 建目录
            val name = context.contentResolver.query(uri, null, null, null, null)?.use { c ->  // 查询文件名
                val i = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)  // 名称列
                if (c.moveToFirst() && i >= 0) c.getString(i) else null  // 读名称
            } ?: uri.lastPathSegment ?: "model.gguf"      // 兜底名
            val target = File(targetDir, name)            // 目标文件
            context.contentResolver.openInputStream(uri)?.use { input ->  // 打开输入流
                target.outputStream().use { input.copyTo(it) }  // 复制
            }
            target.length() > 0                           // 非空即成功
        }.getOrDefault(false)                             // 失败返回 false
    }

    /** GGUF 模型是否已导入（modelsDir()/gguf/ 下存在 .gguf 文件）。 */
    fun hasGgufModel(): Boolean =                         // GGUF 就绪检测
        File(storage.modelsDir(), "gguf").listFiles()?.any { it.name.endsWith(".gguf", ignoreCase = true) } == true  // 存在 gguf

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

    /**
     * 枚举已安装的本地模型（M-035 新增）：供「本地模型选择页」列出可选模型。
     *  - MNN：modelsDir()/llm/ 下的主模型（含 config.json 即视为已安装）；
     *  - GGUF：modelsDir()/gguf/ 下的 .gguf 文件（逐个列出）。
     */
    fun listLocalModels(): List<LocalModelInfo> {        // 枚举本地模型
        val result = mutableListOf<LocalModelInfo>()     // 结果集
        // ① MNN 主模型
        val mnnDir = File(storage.modelsDir(), "llm")    // MNN 目录
        if (File(mnnDir, "config.json").exists()) {      // 已安装 MNN 模型
            val totalSize = mnnDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }  // 总大小
            result += LocalModelInfo(                    // 组装
                id = "mnn-llm",                          // 固定 ID（单 MNN 模型）
                name = readMnnModelName(mnnDir) ?: "本地 MNN 模型",  // 读 config 里的模型名
                format = "MNN",                          // 格式
                sizeBytes = totalSize,                   // 总大小
                runnable = true,                         // MNN 可运行（LocalChatEngine 直接加载）
            )
        }
        // ② GGUF 模型
        val ggufDir = File(storage.modelsDir(), "gguf")  // GGUF 目录
        ggufDir.listFiles()?.filter { it.isFile && it.name.endsWith(".gguf", ignoreCase = true) }?.forEach { f ->  // 遍历 gguf
            result += LocalModelInfo(                    // 组装
                id = "gguf-${f.nameWithoutExtension}",   // ID
                name = f.nameWithoutExtension,           // 显示名
                format = "GGUF",                         // 格式
                sizeBytes = f.length(),                  // 大小
                runnable = false,                        // GGUF 需 llama.cpp 运行时（后续里程碑）
            )
        }
        return result.sortedBy { it.format }             // MNN 在前
    }

    /** 读 MNN 模型名（config.json 的 llm_model 或目录名，尽力而为）。 */
    private fun readMnnModelName(dir: File): String? {   // 读模型名
        return runCatching {                             // 容错
            val cfg = File(dir, "config.json").readText()  // 读 config
            // 简单提取 "llm_model": "xxx.mnn" 里的名字，或忽略返回 null
            null                                        // 暂不解析，用目录名
        }.getOrNull()
    }

    /**
     * 删除本地模型（M-027 新增）：按格式删除磁盘文件。
     *  - MNN：删除整个 modelsDir()/llm/ 目录（当前只支持单个 MNN 模型）；
     *  - GGUF：删除 modelsDir()/gguf/ 下匹配 [modelName] 的文件。
     * @return 删除成功返回 true
     */
    fun deleteLocalModel(format: String, modelName: String? = null): Boolean {  // 删除模型
        return try {                                     // 容错
            when (format.uppercase()) {                  // 按格式分发
                "MNN" -> File(storage.modelsDir(), "llm").deleteRecursively()  // 删 MNN 目录
                "GGUF" -> {                              // 删 GGUF 文件
                    val ggufDir = File(storage.modelsDir(), "gguf")  // gguf 目录
                    if (modelName != null) {             // 指定名称
                        listOf(modelName, "$modelName.gguf").any { n -> File(ggufDir, n).delete() }  // 按名删除
                    } else {                             // 未指定
                        ggufDir.deleteRecursively()      // 删整个目录
                    }
                }
                else -> false                            // 未知格式
            }
        } catch (e: Exception) {                         // 异常
            false                                        // 失败
        }
    }
}
