package com.memuo.core.storage                          // 声明包名：存储模块

import android.content.Context                          // 导入 Context：应用上下文
import androidx.datastore.preferences.core.Preferences   // 导入 Preferences：DataStore 键值容器
import androidx.datastore.preferences.core.edit          // 导入 edit：写 DataStore
import androidx.datastore.preferences.core.stringPreferencesKey  // 导入 stringPreferencesKey：字符串键构造
import androidx.datastore.preferences.preferencesDataStore  // 导入 preferencesDataStore：创建 DataStore
import dagger.hilt.android.qualifiers.ApplicationContext // 导入 ApplicationContext：应用级上下文限定符
import kotlinx.coroutines.flow.Flow                      // 导入 Flow：响应式数据流
import kotlinx.coroutines.flow.map                        // 导入 map：流变换
import javax.inject.Inject                               // 导入 Inject：构造函数注入
import javax.inject.Singleton                             // 导入 Singleton：单例作用域

/** 壁纸 DataStore 文件名。 */
private val Context.wallpaperDataStore by preferencesDataStore(name = "wallpaper")  // DataStore 委托

/** 壁纸显示方式枚举（对应 HTML 壁纸页的 4 个分段：平铺/拉伸/居中/模糊）。 */
enum class WallpaperMode { TILE, STRETCH, CENTER, BLUR }  // 四种显示方式

/** 壁纸来源枚举：default=默认 / preset=预设渐变 / upload=上传图片。 */
enum class WallpaperSource { DEFAULT, PRESET, UPLOAD }    // 三种来源

/**
 * 壁纸配置（WallpaperConfig）—— 聊天页背景的数据模型（对应 HTML wallCfg）。
 */
data class WallpaperConfig(                              // 壁纸配置数据类
    val source: WallpaperSource = WallpaperSource.DEFAULT,  // 来源
    val presetId: String? = null,                        // 预设壁纸 ID（source=PRESET 时有效）
    val imageUri: String? = null,                        // 上传图片 Uri（source=UPLOAD 时有效）
    val mode: WallpaperMode = WallpaperMode.CENTER,      // 显示方式
)

/**
 * 壁纸偏好（WallpaperPrefs）—— 持久化聊天页壁纸配置（core:storage 提供，
 * 供 feature:chat 聊天页背景与 feature:settings 壁纸设置页共用）。
 * 说明：HTML 用 localStorage 存 wallCfg + dataURL 图片，原生改为
 * DataStore 存配置 + 图片 Uri 指向相册/SAF 位置（避免大图进内存）。
 */
@Singleton                                              // 单例
class WallpaperPrefs @Inject constructor(               // 构造函数注入
    @ApplicationContext private val context: Context,   // 注入应用上下文
) {
    private object Keys {                                // 键集合
        val SOURCE = stringPreferencesKey("wall_source") // 来源键
        val PRESET = stringPreferencesKey("wall_preset") // 预设 ID 键
        val URI = stringPreferencesKey("wall_uri")       // 图片 Uri 键
        val MODE = stringPreferencesKey("wall_mode")     // 显示方式键
    }

    /** 当前壁纸配置流（任何页面订阅即实时刷新）。 */
    val config: Flow<WallpaperConfig> = context.wallpaperDataStore.data.map { p ->  // 读取流
        WallpaperConfig(                                 // 组装配置
            source = runCatching { WallpaperSource.valueOf(p[Keys.SOURCE] ?: "") }.getOrDefault(WallpaperSource.DEFAULT),  // 来源（容错回默认）
            presetId = p[Keys.PRESET],                   // 预设 ID
            imageUri = p[Keys.URI],                      // 图片 Uri
            mode = runCatching { WallpaperMode.valueOf(p[Keys.MODE] ?: "") }.getOrDefault(WallpaperMode.CENTER),  // 方式（容错回居中）
        )
    }

    /** 保存壁纸配置（选中即生效，HTML 同款行为）。 */
    suspend fun save(config: WallpaperConfig) {          // 保存配置
        context.wallpaperDataStore.edit { p ->           // 写 DataStore
            p[Keys.SOURCE] = config.source.name          // 写来源
            p[Keys.PRESET] = config.presetId ?: ""       // 写预设 ID
            p[Keys.URI] = config.imageUri ?: ""          // 写图片 Uri
            p[Keys.MODE] = config.mode.name              // 写显示方式
        }
    }
}
