package com.memuo.core.ui                                // 声明包名：共享 UI 模块

import android.content.Context                           // 导入 Context：应用上下文
import android.graphics.BitmapFactory                    // 导入 BitmapFactory：位图解码
import android.net.Uri                                   // 导入 Uri：内容标识
import androidx.compose.runtime.Composable               // 导入 Composable：可组合函数注解
import androidx.compose.runtime.LaunchedEffect            // 导入 LaunchedEffect：副作用
import androidx.compose.runtime.getValue                  // 导入 getValue：by 委托
import androidx.compose.runtime.mutableStateOf            // 导入 mutableStateOf：可变状态
import androidx.compose.runtime.remember                  // 导入 remember：记住状态
import androidx.compose.runtime.setValue                  // 导入 setValue：by 委托写
import androidx.compose.ui.graphics.ImageBitmap           // 导入 ImageBitmap：Compose 位图
import androidx.compose.ui.graphics.asImageBitmap         // 导入 asImageBitmap：转换
import androidx.compose.ui.platform.LocalContext          // 导入 LocalContext：组合上下文
import kotlinx.coroutines.Dispatchers                     // 导入 Dispatchers：调度器
import kotlinx.coroutines.withContext                     // 导入 withContext：切换线程

/**
 * 从 Uri 加载位图（rememberBitmap）—— 项目未引入图片加载库（如 Coil），
 * 这里用 ContentResolver + BitmapFactory 手动解码，模拟 HTML 原型的
 * 缩略图压缩逻辑（限制最大边长，避免大图内存溢出）。
 * 用于：聊天图片暂存预览、发送后的图片消息、自定义壁纸预览。
 */
@Composable                                              // 可组合函数
fun rememberBitmap(                                      // 加载位图
    uri: Uri?,                                           // 图片 Uri（null = 空）
    maxSize: Int = 1024,                                 // 最大边长（HTML 预览压缩为 320）
): ImageBitmap? {                                        // 返回位图（加载中为 null）
    val context = LocalContext.current                   // 取上下文
    var bitmap by remember(uri) { mutableStateOf<ImageBitmap?>(null) }  // 位图状态（uri 变化重置）

    LaunchedEffect(uri) {                                // uri 变化时重新加载
        if (uri == null) {                               // 空 uri
            bitmap = null                               // 清空
            return@LaunchedEffect                       // 直接返回
        }
        bitmap = withContext(Dispatchers.IO) {           // IO 线程解码（不阻塞 UI）
            runCatching {                               // 容错
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }  // 只读尺寸
                context.contentResolver.openInputStream(uri)?.use {                    // 读流
                    BitmapFactory.decodeStream(it, null, opts)                          // 解析尺寸
                }
                // 按最大边长计算采样率（2 的幂次，模拟 HTML 的等比压缩）
                var sample = 1                           // 初始采样率
                while (opts.outWidth / (sample * 2) >= maxSize || opts.outHeight / (sample * 2) >= maxSize) {  // 边长超过上限
                    sample *= 2                          // 采样率翻倍
                }
                val dec = BitmapFactory.Options().apply { inSampleSize = sample }  // 应用采样率
                context.contentResolver.openInputStream(uri)?.use {                 // 再读流
                    BitmapFactory.decodeStream(it, null, dec)?.asImageBitmap()      // 解码为 Compose 位图
                }
            }.getOrNull()                                // 失败返回 null（UI 显示错误占位）
        }
    }
    return bitmap                                        // 返回当前位图
}
