package com.memuo.core.ingest                            // 声明包名：内容入库模块

import java.io.File                                       // 导入 File：图片文件

/**
 * OCR 引擎接口（OcrEngine）—— 图片文字识别（架构不变式：可换 MNN-PaddleOCR / Google ML Kit 等）。
 */
interface OcrEngine {                                    // OCR 引擎接口
    /** 识别一张图片中的文字，返回纯文本。 */
    suspend fun recognize(file: File): String           // 挂起函数：识别
}
