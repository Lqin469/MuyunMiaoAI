package com.memuo.core.storage                            // 声明包名：core 层的存储抽象模块

import java.io.File                                       // 导入 File：本地文件路径

/**
 * 存储迁移器（StorageMigrator）—— 把数据从一个目录安全迁移到另一个目录（R5 自定义目录切换）。
 *
 * 三步走（保证失败可回滚、不丢数据）：
 *  1) 复制（保留目录结构）；
 *  2) 校验（抽查文件大小一致）；
 *  3) 切换（由调用方把 StorageProvider 指向新目录，旧目录标记待清理，而非立即删除）。
 */
object StorageMigrator {                                  // 单例对象：迁移逻辑（无状态，用 object）

    /**
     * 把 from 目录的全部内容复制到 to 目录。
     * @return 复制成功的文件数
     */
    fun migrate(from: File, to: File): Int {              // 迁移方法：复制 + 校验
        require(from.exists()) { "源目录不存在：$from" }    // 前置校验：源目录必须存在
        to.mkdirs()                                       // 确保目标目录存在

        var copied = 0                                    // 计数器：成功复制的文件数
        from.walkTopDown()                                // 自顶向下遍历源目录的所有文件
            .filter { it.isFile }                         // 只处理文件（目录稍后按需创建）
            .forEach { src ->                             // 遍历每个源文件
                val rel = src.relativeTo(from)            // 计算相对路径（保持目录结构）
                val dst = File(to, rel.path)              // 目标文件 = 目标根 + 相对路径
                dst.parentFile?.mkdirs()                  // 确保目标文件的父目录存在
                src.copyTo(dst, overwrite = true)         // 复制文件（覆盖已存在的）
                copied++                                  // 计数 +1
            }

        verify(from, to)                                  // 复制完成后做一致性校验
        return copied                                     // 返回复制文件数
    }

    /** 一致性校验：抽查每个文件的大小是否与源一致（防止复制中途损坏）。 */
    private fun verify(from: File, to: File) {            // 校验方法：逐文件比对大小
        from.walkTopDown()                                // 遍历源目录
            .filter { it.isFile }                         // 只校验文件
            .forEach { src ->                             // 遍历每个源文件
                val dst = File(to, src.relativeTo(from).path) // 对应的目标文件
                check(dst.exists()) { "校验失败：目标缺失 ${dst.name}" }        // 目标必须存在
                check(dst.length() == src.length()) { "校验失败：大小不一致 ${src.name}" } // 大小必须一致
            }
    }
}
