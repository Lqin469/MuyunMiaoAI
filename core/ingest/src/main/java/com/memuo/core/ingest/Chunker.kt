package com.memuo.core.ingest                            // 声明包名：内容入库模块

/**
 * 文本分块器（Chunker）—— 把长文档切成适合检索的小块（中文优化）。
 * 策略：先按中文句读切句，再贪心合并到 maxLen，超长句硬切并保留 overlap（上下文重叠）。
 */
object Chunker {                                          // 单例对象：分块逻辑

    /**
     * 分块主方法。
     * @param text 原始文本
     * @param maxLen 单块最大长度（字符）
     * @param overlap 相邻块之间的重叠长度（保留上下文）
     * @return 分块列表（过滤掉过短碎片）
     */
    fun split(text: String, maxLen: Int = 400, overlap: Int = 80): List<String> {  // 分块方法
        // 按中文句读 + 换行切分（保留分隔符）
        val sentences = text.split(Regex("(?<=[。！？；!?;\n])"))  // 正向预查，切分后保留标点

        val chunks = mutableListOf<String>()              // 结果列表
        val cur = StringBuilder()                         // 当前累积块

        for (s in sentences) {                            // 遍历每个句子
            if (cur.length + s.length > maxLen && cur.isNotEmpty()) {  // 当前块已满且非空
                chunks += cur.toString().trim()           // 提交当前块
                val tail = cur.takeLast(overlap)          // 取尾部 overlap 字符作重叠
                cur.clear().append(tail)                  // 新块以重叠开头（保留上下文）
            }
            cur.append(s)                                 // 追加当前句
        }
        if (cur.isNotBlank()) chunks += cur.toString().trim()  // 提交最后一块

        return chunks.map { it.trim() }.filter { it.length >= 8 }  // 过滤过短碎片（噪音）
    }
}
