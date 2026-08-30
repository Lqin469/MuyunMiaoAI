package com.memuo.core.storage                          // 声明包名：存储模块

import android.content.Context                          // 导入 Context：应用上下文
import androidx.datastore.preferences.core.Preferences   // 导入 Preferences：DataStore 键值容器
import androidx.datastore.preferences.core.booleanPreferencesKey  // 导入 booleanPreferencesKey：布尔键构造
import androidx.datastore.preferences.core.edit          // 导入 edit：写 DataStore
import androidx.datastore.preferences.preferencesDataStore  // 导入 preferencesDataStore：创建 DataStore
import dagger.hilt.android.qualifiers.ApplicationContext // 导入 ApplicationContext：应用级上下文限定符
import kotlinx.coroutines.flow.Flow                      // 导入 Flow：响应式数据流
import kotlinx.coroutines.flow.catch                     // 导入 catch：读取异常兜底
import kotlinx.coroutines.flow.first                     // 导入 first：取一次值
import kotlinx.coroutines.flow.map                        // 导入 map：流变换
import javax.inject.Inject                               // 导入 Inject：构造函数注入
import javax.inject.Singleton                             // 导入 Singleton：单例作用域

/** 应用级偏好 DataStore 文件名。 */
private val Context.appPrefsDataStore by preferencesDataStore(name = "app_prefs")  // DataStore 委托

/**
 * 应用偏好（AppPrefs）—— 全局开关类配置（core:storage 提供，app 层消费）。
 * 当前承载：首次启动标记（firstRunDone），控制「设备自检页 → 主页」的启动流程。
 */
@Singleton                                              // 单例
class AppPrefs @Inject constructor(                     // 构造函数注入
    @ApplicationContext private val context: Context,   // 注入应用上下文
) {
    private object Keys {                                // 键集合
        val FIRST_RUN_DONE = booleanPreferencesKey("first_run_done")  // 首次启动完成标记
        val DARK_MODE = booleanPreferencesKey("dark_mode")  // 暗色主题标记
    }

    /**
     * 首次启动标记流（true = 已完成自检；false = 未完成）。
     * 【重要修复】此前返回 Flow&lt;Boolean?&gt;：键不存在时发出 null，与界面层的
     * 「加载中（initial = null）」状态混淆 → 全新安装用户永远停在空白占位页。
     * 现改为 Flow&lt;Boolean&gt;：DataStore 读完必发确定值（键不存在 = false = 未自检），
     * 界面层可用 initial=null 的三态区分「加载中 / 未自检 / 已自检」；
     * 另加 catch 兜底：读取异常时按「未自检」处理，避免启动崩溃或挂死。
     */
    val firstRunDone: Flow<Boolean> =                    // 读取流（必发确定值）
        context.appPrefsDataStore.data                   // DataStore 数据流
            .map { it[Keys.FIRST_RUN_DONE] == true }     // 键不存在 → false（未自检）
            .catch { emit(false) }                       // 读取异常 → false 兜底（不崩溃不挂死）

    /** 同步读取一次（导航启动前判定用）。 */
    suspend fun isFirstRunDone(): Boolean =              // 一次性读取
        context.appPrefsDataStore.data.first()[Keys.FIRST_RUN_DONE] == true  // 取当前值

    /** 标记自检完成（设备自检页「下一步」调用）。 */
    suspend fun setFirstRunDone() {                      // 写入标记
        context.appPrefsDataStore.edit { it[Keys.FIRST_RUN_DONE] = true }  // 置 true
    }

    /** 暗色主题流（true = 暗色；默认 false = 亮色）。 */
    val darkMode: Flow<Boolean> =                        // 读取流（必发确定值）
        context.appPrefsDataStore.data                   // DataStore 数据流
            .map { it[Keys.DARK_MODE] == true }          // 键不存在 → false（亮色）
            .catch { emit(false) }                       // 读取异常 → false 兜底

    /** 同步读取一次（启动初始化主题用）。 */
    suspend fun isDarkMode(): Boolean =                  // 一次性读取
        context.appPrefsDataStore.data.first()[Keys.DARK_MODE] == true  // 取当前值

    /** 保存暗色主题开关。 */
    suspend fun setDarkMode(dark: Boolean) {             // 写入标记
        context.appPrefsDataStore.edit { it[Keys.DARK_MODE] = dark }  // 置值
    }
}
