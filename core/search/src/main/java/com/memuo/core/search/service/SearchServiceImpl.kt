package com.memuo.core.search.service                      // 声明包名：搜索模块的"检索服务"子包

import com.memuo.core.db.dao.FileLocationDao               // 导入文件位置 DAO
import kotlinx.coroutines.Dispatchers                      // 导入 Dispatchers：IO 线程
import kotlinx.coroutines.withContext                      // 导入 withContext：切换线程
import javax.inject.Inject                                 // 导入 Inject
import javax.inject.Singleton                              // 导入 Singleton

/**
 * 检索服务实现（SearchServiceImpl）—— 只读索引表，纯查询、无扫描副作用（M7）。
 * 隐私红线：只返回元数据（路径/名称/大小/时间），绝不返回文件内容。
 */
@Singleton                                               // 单例
class SearchServiceImpl @Inject constructor(            // 构造函数注入
    private val dao: FileLocationDao,                   // 文件位置 DAO
) : SearchService {                                      // 实现 SearchService 接口

    override suspend fun search(query: FileQuery): List<FileHit> = withContext(Dispatchers.IO) {  // IO 线程查询
        // 放大取回量：先按扩展名/目录在内存中过滤，再截断到 limit，避免过滤后结果不足
        val rows = dao.search(query.keyword, query.limit * 3 + 20)  // DAO 模糊匹配（名称/路径 LIKE）
        rows.asSequence()                                 // 转为惰性序列（避免中间列表）
            .filter { query.extension == null || it.ext == query.extension }  // 扩展名过滤
            .filter { query.dirFilter == null || it.path.startsWith(query.dirFilter) }  // 目录过滤
            .map {                                        // 实体 → 结果
                FileHit(
                    path = it.path,                       // 绝对路径
                    name = it.name,                       // 文件名
                    extension = it.ext,                   // 扩展名
                    sizeBytes = it.sizeBytes,             // 大小
                    modifiedAt = it.mtime,                // 修改时间
                    archivePath = it.archivePath,         // 压缩包内路径（可能 null）
                )
            }
            .take(query.limit)                            // 截断到请求上限
            .toList()                                     // 收集为列表
    }
}