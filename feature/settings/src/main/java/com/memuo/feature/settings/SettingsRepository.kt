package com.memuo.feature.settings                         // 声明包名：设置业务模块

import android.content.Context                            // 导入 Context：应用上下文
import androidx.datastore.core.DataStore                  // 导入 DataStore：偏好存储核心
import androidx.datastore.preferences.core.Preferences    // 导入 Preferences：键值对容器
import androidx.datastore.preferences.core.booleanPreferencesKey  // 导入布尔型偏好键
import androidx.datastore.preferences.core.edit           // 导入 edit：写入偏好
import androidx.datastore.preferences.core.stringPreferencesKey  // 导入字符串型偏好键
import androidx.datastore.preferences.preferencesDataStore // 导入 preferencesDataStore：创建 DataStore 的委托
import com.memuo.core.ai.engine.EngineSettings             // 导入引擎设置接口（本类实现它）
import com.memuo.core.db.entity.EngineType                 // 导入引擎类型枚举
import com.memuo.core.search.consent.SearchSettings        // 导入 SearchSettings 接口（本类实现它）
import dagger.hilt.android.qualifiers.ApplicationContext  // 导入 ApplicationContext：Hilt 提供应用级上下文
import kotlinx.coroutines.CoroutineScope                   // 导入 CoroutineScope：协程作用域
import kotlinx.coroutines.Dispatchers                      // 导入 Dispatchers：协程调度器（IO/Default）
import kotlinx.coroutines.SupervisorJob                    // 导入 SupervisorJob：子协程失败不影响其他
import kotlinx.coroutines.flow.SharingStarted              // 导入 SharingStarted：状态流启动策略
import kotlinx.coroutines.flow.StateFlow                   // 导入 StateFlow：只读状态流
import kotlinx.coroutines.flow.map                          // 导入 map：把 Flow 数据流转换
import kotlinx.coroutines.flow.stateIn                      // 导入 stateIn：把冷流转为热状态流
import javax.inject.Inject                                 // 导入 Inject：Hilt 构造函数注入
import javax.inject.Singleton                              // 导入 Singleton：单例作用域

/** 应用设置的 DataStore 实例（顶层委托，全应用唯一，name = "settings"）。 */
private val Context.settingsDataStore by preferencesDataStore(name = "settings")  // 用委托创建 DataStore

/**
 * 设置仓库（SettingsRepository）—— 持久化用户设置，并实现 core:search 的 SearchSettings 接口。
 *
 * 关键约束（用户强制）：[backgroundIndexingEnabled]（后台自动索引）**默认必须为 false**，
 * 只有用户在设置页显式打开才允许后台/计划搜索。
 */
@Singleton                                               // 单例：全应用共享一个设置仓库
class SettingsRepository @Inject constructor(            // 构造函数注入（Hilt 自动装配）
    @ApplicationContext private val context: Context,    // 注入应用级上下文（DataStore 用）
) : SearchSettings, EngineSettings {                     // 实现搜索设置 + 引擎设置两个接口

    /** 本类专用的协程作用域（用于把 DataStore 的冷流转为状态流）。 */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)  // 后台作用域，子协程独立失败

    /**
     * 后台自动索引开关状态流。
     * 从 DataStore 读取，默认 false（用户未开启时绝不允许后台搜索）。
     */
    override val backgroundIndexingEnabled: StateFlow<Boolean> =  // 重写接口属性：只读状态流
        context.settingsDataStore.data                    // 读取 DataStore 的偏好流（冷流）
            .map { prefs -> prefs[KEY_BACKGROUND_INDEXING] ?: false }  // 取键值，缺失时默认 false（关键默认值）
            .stateIn(scope, SharingStarted.Eagerly, false)  // 转为热状态流，立即开始收集，初始值 false

    /** 设置后台自动索引开关（用户在设置页操作时调用）。 */
    suspend fun setBackgroundIndexingEnabled(enabled: Boolean) {  // 挂起函数：写入设置
        context.settingsDataStore.edit { prefs ->         // 编辑 DataStore（原子写入）
            prefs[KEY_BACKGROUND_INDEXING] = enabled      // 把开关值写入偏好
        }
    }

    /** 当前对话引擎类型状态流（默认 CLOUD）。 */
    override val engineType: StateFlow<EngineType> =      // 实现引擎设置接口：只读引擎类型
        context.settingsDataStore.data                    // 读 DataStore 偏好流
            .map { prefs ->                              // 转换
                runCatching { EngineType.valueOf(prefs[KEY_ENGINE_TYPE] ?: EngineType.CLOUD.name) }  // 字符串转枚举（容错）
                    .getOrDefault(EngineType.CLOUD)        // 非法值回退云端
            }                                            // map 转换结束
            .stateIn(scope, SharingStarted.Eagerly, EngineType.CLOUD)  // 转热状态流，初始 CLOUD

    /** 设置对话引擎类型（用户在设置页切换引擎时调用）。 */
    override suspend fun setEngineType(type: EngineType) {  // 实现引擎设置接口：写入
        context.settingsDataStore.edit { prefs ->         // 编辑 DataStore
            prefs[KEY_ENGINE_TYPE] = type.name            // 存枚举名字符串
        }
    }

    companion object {                                    // 伴生对象：静态常量区
        /** DataStore 的键：后台自动索引开关。 */
        private val KEY_BACKGROUND_INDEXING = booleanPreferencesKey("background_indexing_enabled")  // 布尔型偏好键

        /** DataStore 的键：对话引擎类型（"CLOUD" / "LOCAL"）。 */
        private val KEY_ENGINE_TYPE = stringPreferencesKey("engine_type")  // 字符串型偏好键
    }
}
