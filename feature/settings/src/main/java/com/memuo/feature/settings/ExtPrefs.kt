package com.memuo.feature.settings                         // 声明包名：设置业务模块

import android.content.Context                            // 导入 Context：应用上下文
import androidx.datastore.preferences.core.Preferences     // 导入 Preferences：键值容器
import androidx.datastore.preferences.core.booleanPreferencesKey  // 导入 booleanPreferencesKey：布尔键
import androidx.datastore.preferences.core.edit            // 导入 edit：写 DataStore
import androidx.datastore.preferences.core.longPreferencesKey  // 导入 longPreferencesKey：长整型键
import androidx.datastore.preferences.core.stringPreferencesKey  // 导入 stringPreferencesKey：字符串键
import androidx.datastore.preferences.preferencesDataStore  // 导入 preferencesDataStore：创建 DataStore
import dagger.hilt.android.qualifiers.ApplicationContext  // 导入 ApplicationContext：应用级上下文
import kotlinx.coroutines.flow.Flow                       // 导入 Flow：响应式数据流
import kotlinx.coroutines.flow.map                         // 导入 map：流变换
import javax.inject.Inject                                // 导入 Inject：构造函数注入
import javax.inject.Singleton                              // 导入 Singleton：单例作用域

/** 设置扩展偏好 DataStore 文件名。 */
private val Context.extPrefsDataStore by preferencesDataStore(name = "muyun_ext")  // DataStore 委托

/**
 * 扩展偏好（ExtPrefs）—— HTML 原型中放 localStorage 的各模块设置：
 * 权限模式、云端 API 列表（含当前使用）、模型列表、知识库文件夹与隐私开关、迁移日志。
 * 说明：原型迁移时 localStorage 的持久化对应为 DataStore（键值存储，线程安全）。
 */
@Singleton                                              // 单例
class ExtPrefs @Inject constructor(                     // 构造函数注入
    @ApplicationContext private val context: Context,   // 注入应用上下文
) {
    private object Keys {                                // 键集合
        val PERM_MODE = stringPreferencesKey("perm_mode")       // 权限模式（basic/adb/root）
        val API_LIST = stringPreferencesKey("api_list_json")    // API 列表 JSON
        val API_CURRENT = longPreferencesKey("api_current_id")  // 当前 API id
        val MODEL_LIST = stringPreferencesKey("model_list_json")  // 模型列表 JSON
        val KB_FOLDERS = stringPreferencesKey("kb_folders_json")  // 知识库文件夹 JSON
        val KB_PRIVACY = booleanPreferencesKey("kb_privacy")    // 隐私库开关
        val MIGRATE_LOGS = stringPreferencesKey("migrate_logs_json")  // 迁移日志 JSON
        val LAN_MODE = stringPreferencesKey("lan_receive_mode")  // 局域网接收方式
        val LAN_PATH = stringPreferencesKey("lan_save_path")     // 局域网保存路径
        val LOCAL_MODEL = stringPreferencesKey("local_model_id")  // 当前选中的本地模型 id（M-035）
    }

    // —— 权限模式 ——
    /** 权限模式流（basic/adb/root）。 */
    val permMode: Flow<String> = context.extPrefsDataStore.data.map { it[Keys.PERM_MODE] ?: "basic" }  // 读流

    /** 保存权限模式。 */
    suspend fun setPermMode(mode: String) {               // 写权限模式
        context.extPrefsDataStore.edit { it[Keys.PERM_MODE] = mode }  // 写入
    }

    // —— 云端 API 列表 ——
    /** API 列表 JSON 流（空串 = 无）。 */
    val apiListJson: Flow<String> = context.extPrefsDataStore.data.map { it[Keys.API_LIST] ?: "" }  // 读流

    /** 保存 API 列表 JSON。 */
    suspend fun setApiListJson(json: String) {            // 写 API 列表
        context.extPrefsDataStore.edit { it[Keys.API_LIST] = json }  // 写入
    }

    /** 当前使用 API id 流。 */
    val currentApiId: Flow<Long> = context.extPrefsDataStore.data.map { it[Keys.API_CURRENT] ?: -1L }  // 读流

    /** 保存当前 API id。 */
    suspend fun setCurrentApiId(id: Long) {               // 写当前 API
        context.extPrefsDataStore.edit { it[Keys.API_CURRENT] = id }  // 写入
    }

    // —— 模型列表 ——
    /** 模型列表 JSON 流。 */
    val modelListJson: Flow<String> = context.extPrefsDataStore.data.map { it[Keys.MODEL_LIST] ?: "" }  // 读流

    /** 保存模型列表 JSON。 */
    suspend fun setModelListJson(json: String) {          // 写模型列表
        context.extPrefsDataStore.edit { it[Keys.MODEL_LIST] = json }  // 写入
    }

    // —— 当前选中的本地模型（M-035）——
    /** 当前选中的本地模型 id 流（默认 "mnn-llm"）。 */
    val localModelId: Flow<String> = context.extPrefsDataStore.data.map { it[Keys.LOCAL_MODEL] ?: "mnn-llm" }  // 读流

    /** 保存当前选中的本地模型 id。 */
    suspend fun setLocalModelId(id: String) {             // 写选中模型
        context.extPrefsDataStore.edit { it[Keys.LOCAL_MODEL] = id }  // 写入
    }

    // —— 知识库文件夹与隐私开关 ——
    /** 知识库文件夹 JSON 流。 */
    val kbFoldersJson: Flow<String> = context.extPrefsDataStore.data.map { it[Keys.KB_FOLDERS] ?: "" }  // 读流

    /** 保存知识库文件夹 JSON。 */
    suspend fun setKbFoldersJson(json: String) {          // 写知识库文件夹
        context.extPrefsDataStore.edit { it[Keys.KB_FOLDERS] = json }  // 写入
    }

    /** 隐私库开关流（默认开，对应 HTML 初始 on）。 */
    val kbPrivacy: Flow<Boolean> = context.extPrefsDataStore.data.map { it[Keys.KB_PRIVACY] ?: true }  // 读流

    /** 保存隐私库开关。 */
    suspend fun setKbPrivacy(on: Boolean) {               // 写隐私库开关
        context.extPrefsDataStore.edit { it[Keys.KB_PRIVACY] = on }  // 写入
    }

    // —— 迁移日志 ——
    /** 迁移日志 JSON 流。 */
    val migrateLogsJson: Flow<String> = context.extPrefsDataStore.data.map { it[Keys.MIGRATE_LOGS] ?: "" }  // 读流

    /** 保存迁移日志 JSON。 */
    suspend fun setMigrateLogsJson(json: String) {        // 写迁移日志
        context.extPrefsDataStore.edit { it[Keys.MIGRATE_LOGS] = json }  // 写入
    }

    // —— 局域网传输设置 ——
    /** 接收方式流（manual/auto）。 */
    val lanReceiveMode: Flow<String> = context.extPrefsDataStore.data.map { it[Keys.LAN_MODE] ?: "manual" }  // 读流

    /** 保存接收方式。 */
    suspend fun setLanReceiveMode(mode: String) {         // 写接收方式
        context.extPrefsDataStore.edit { it[Keys.LAN_MODE] = mode }  // 写入
    }

    /** 保存路径流。 */
    val lanSavePath: Flow<String> = context.extPrefsDataStore.data.map { it[Keys.LAN_PATH] ?: "/storage/emulated/0/Download/沐云杪" }  // 读流

    /** 保存保存路径。 */
    suspend fun setLanSavePath(path: String) {            // 写保存路径
        context.extPrefsDataStore.edit { it[Keys.LAN_PATH] = path }  // 写入
    }
}
