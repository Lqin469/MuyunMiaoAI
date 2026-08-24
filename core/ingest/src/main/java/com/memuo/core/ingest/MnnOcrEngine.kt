package com.memuo.core.ingest                            // 声明包名：内容入库模块

import java.io.File                                       // 导入 File：图片文件
import javax.inject.Inject                                 // 导入 Inject：构造函数注入
import javax.inject.Singleton                              // 导入 Singleton：单例作用域

/**
 * MNN-PaddleOCR 引擎（MnnOcrEngine）—— 本地图片 OCR 实现（M6）。
 *
 * 当前状态：**桩**（stub）。真实集成需要 MNN AAR 里的 PaddleOCR 模型加载和推理
 *（参考 MNN 官方 project/android/demo/OCR 示例）。集成步骤：
 *  1) 在本地用 AS 拉 MNN 源码，构建带 OCR 能力的 AAR；
 *  2) 把 paddleocr-vl-mobile.mnn 放到 StorageProvider.modelsDir() 下的 ocr/ 子目录；
 *  3) 在本类的 recognize 里加载 AAR、读位图、调用 JNI 提取文字、返回。
 *
 * 桩行为：调用即返回"未集成"的占位文本，避免静默失败。
 */
@Singleton                                               // 单例
class MnnOcrEngine @Inject constructor() : OcrEngine {  // 构造函数注入
    override suspend fun recognize(file: File): String {  // 桩识别
        // 桩实现：返回占位提示（避免图片 OCR 静默失败）
        return "[MNN-PaddleOCR 尚未集成（M6 桩）] 图片：${file.name}\n" +  // 占位文本
               "请在本地用 AS 拉 MNN 源码构建 AAR（含 OCR），并把模型放到 modelsDir/ocr/，\n" +  // 续
               "然后在 MnnOcrEngine.recognize 里加载 AAR 读位图并调 JNI。\n"  // 续
    }
}
