package com.memuo.core.search.service                      // 声明包名：搜索模块的"检索服务"子包

/**
 * 文件查询 —— 只读已建好的索引（file_index FTS5），
 * 本身【不触发任何扫描/索引副作用】。扫描只发生在用户显式发起（见 FileIndexer）。
 */
data class FileQuery(                                      // 文件查询条件数据类
    val keyword: String,                                   // 搜索关键字（文件名/路径模糊匹配）
    val extension: String? = null,                         // 可选：按扩展名过滤（如 "pdf"）
    val dirFilter: String? = null,                         // 可选：限定搜索目录
    val limit: Int = 20,                                   // 返回结果上限（默认 20 条）
)

/** 命中结果；archivePath 非空表示文件位于压缩包内（R10 位置记录）。 */
data class FileHit(                                        // 文件命中结果数据类
    val path: String,                                      // 文件绝对路径（告知用户"文件在哪"）
    val name: String,                                      // 文件名
    val extension: String,                                 // 扩展名
    val sizeBytes: Long,                                   // 文件大小（字节）
    val modifiedAt: Long,                                  // 最后修改时间戳
    val archivePath: String? = null,                       // 若文件在压缩包内：压缩包路径（否则为 null）
)

/**
 * 检索服务：AI 工具（ToolCallingBus.search_file）与文件检索页共用。
 * 隐私红线：只返回元数据（路径/名称/大小/时间），绝不返回文件内容；
 * 云端问答默认只发送文件名与路径。
 */
interface SearchService {                                  // 检索服务接口：纯查询，无扫描副作用
    suspend fun search(query: FileQuery): List<FileHit>    // 挂起函数：按条件检索，返回命中列表
}
