package com.memuo.core.storage                          // 声明包名：存储模块

import android.content.Context                          // 导入 Context：应用上下文
import androidx.datastore.preferences.core.Preferences   // 导入 Preferences：DataStore 键值容器
import androidx.datastore.preferences.core.booleanPreferencesKey  // 导入 booleanPreferencesKey：布尔键构造
import androidx.datastore.preferences.core.edit          // 导入 edit：写 DataStore
import androidx.datastore.preferences.core.intPreferencesKey  // 导入 intPreferencesKey：整型键构造
import androidx.datastore.preferences.preferencesDataStore  // 导入 preferencesDataStore：创建 DataStore
import dagger.hilt.android.qualifiers.ApplicationContext // 导入 ApplicationContext：应用级上下文限定符
import kotlinx.coroutines.flow.Flow                      // 导入 Flow：响应式数据流
import kotlinx.coroutines.flow.map                        // 导入 map：流变换
import javax.inject.Inject                               // 导入 Inject：构造函数注入
import javax.inject.Singleton                             // 导入 Singleton：单例作用域

/** 笔记偏好 DataStore 文件名。 */
private val Context.notePrefsDataStore by preferencesDataStore(name = "note_prefs")  // DataStore 委托

/**
 * 笔记偏好（NotePrefs）—— 常规备忘录的可配置项（core:storage 提供，供 ingest 与 notes 模块共用）：
 * - 是否自动存入知识库（autoIngest，默认开，保持 R7 行为）
 * - 回收站内容保留天数（trashDays，默认 30 天）
 */
@Singleton                                              // 单例
class NotePrefs @Inject constructor(                    // 构造函数注入
    @ApplicationContext private val context: Context,   // 注入应用上下文
) {
    private object Keys {                                // 键集合
        val AUTO_INGEST = booleanPreferencesKey("note_auto_ingest")  // 自动入库开关
        val TRASH_DAYS = intPreferencesKey("note_trash_days")       // 回收站保留天数
    }

    /** 自动存入知识库开关流（默认 true）。 */
    val autoIngest: Flow<Boolean> =                      // 读取流
        context.notePrefsDataStore.data                   // DataStore 数据流
            .map { it[Keys.AUTO_INGEST] ?: true }         // 键不存在 → true（默认开）

    /** 保存自动入库开关。 */
    suspend fun setAutoIngest(on: Boolean) {             // 写开关
        context.notePrefsDataStore.edit { it[Keys.AUTO_INGEST] = on }  // 写入
    }

    /** 回收站保留天数流（默认 30 天）。 */
    val trashDays: Flow<Int> =                           // 读取流
        context.notePrefsDataStore.data                   // DataStore 数据流
            .map { it[Keys.TRASH_DAYS] ?: 30 }            // 键不存在 → 30 天

    /** 保存回收站保留天数。 */
    suspend fun setTrashDays(days: Int) {                // 写天数
        context.notePrefsDataStore.edit { it[Keys.TRASH_DAYS] = days.coerceIn(1, 365) }  // 钳制 1~365 天
    }
}
