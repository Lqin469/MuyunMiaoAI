package com.memuo.feature.settings                         // 声明包名：设置业务模块

import android.content.Context                            // 导入 Context：应用上下文
import android.content.SharedPreferences                  // 导入 SharedPreferences：键值存储接口
import androidx.datastore.preferences.core.Preferences     // 导入 Preferences：键值容器
import androidx.datastore.preferences.core.booleanPreferencesKey  // 导入 booleanPreferencesKey：布尔键
import androidx.datastore.preferences.core.edit            // 导入 edit：写 DataStore
import androidx.datastore.preferences.core.longPreferencesKey  // 导入 longPreferencesKey：长整型键
import androidx.datastore.preferences.core.stringPreferencesKey  // 导入 stringPreferencesKey：字符串键
import androidx.datastore.preferences.preferencesDataStore  // 导入 preferencesDataStore：创建 DataStore
import androidx.security.crypto.EncryptedSharedPreferences // 导入 EncryptedSharedPreferences：加密偏好存储
import androidx.security.crypto.MasterKey                 // 导入 MasterKey：Keystore 主密钥
import dagger.hilt.android.qualifiers.ApplicationContext  // 导入 ApplicationContext：应用级上下文
import kotlinx.coroutines.flow.Flow                       // 导入 Flow：响应式数据流
import kotlinx.coroutines.flow.MutableStateFlow            // 导入 MutableStateFlow：可变状态流
import kotlinx.coroutines.flow.asStateFlow                 // 导入 asStateFlow：转只读状态流
import kotlinx.coroutines.flow.first                       // 导入 first：取流首值（旧数据迁移）
import kotlinx.coroutines.flow.map                         // 导入 map：流变换
import kotlinx.coroutines.runBlocking                      // 导入 runBlocking：同步读（旧数据迁移）
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

    // —— 云端 API 列表（密钥加密存储）——
    /** 加密偏好存储（API 列表专用）：Keystore AES256_GCM 加密，与 CloudConfigRepository 同模式。 */
    private val securePrefs: SharedPreferences by lazy { // 懒加载加密存储
        runCatching {                                    // 容错：Keystore 损坏时不崩溃
            val masterKey = MasterKey.Builder(context)    // 构建主密钥（Keystore 生成，永不离开设备）
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)  // AES256_GCM 方案
                .build()                                  // 构建
            EncryptedSharedPreferences.create(            // 创建加密偏好存储
                context,                                  // 上下文
                "secure_api_list",                        // 文件名
                masterKey,                                // 主密钥
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,   // 键名加密
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM, // 值加密
            )
        }.getOrElse {                                    // 回退：加密存储初始化失败时用普通偏好（仅兜底）
            context.getSharedPreferences("secure_api_list", Context.MODE_PRIVATE)
        }
    }

    /** API 列表 JSON 流（空串 = 无）；密钥加密存储（响应式用 StateFlow 包装）。 */
    private val _apiListJson = MutableStateFlow(loadApiList())  // 初始加载（含旧数据迁移）
    val apiListJson: Flow<String> = _apiListJson.asStateFlow()  // 只读暴露

    /** 初始加载 API 列表：优先加密存储，空则迁移旧 DataStore 明文数据（一次性）。 */
    private fun loadApiList(): String {                  // 加载 + 迁移
        val secure = securePrefs.getString(API_LIST_KEY, "") ?: ""  // 读加密存储
        if (secure.isNotBlank()) return secure            // 已有加密数据，直接返回
        val legacy = runBlocking { context.extPrefsDataStore.data.first()[Keys.API_LIST] }  // 读旧 DataStore 明文
        return if (legacy.isNullOrBlank()) ""             // 无旧数据，返回空
        else {                                           // 有旧数据，迁移
            securePrefs.edit().putString(API_LIST_KEY, legacy).apply()  // 迁到加密存储
            runBlocking { context.extPrefsDataStore.edit { it.remove(Keys.API_LIST) } }  // 清除旧明文
            legacy                                       // 返回旧值
        }
    }

    /** 保存 API 列表 JSON（写入加密存储 + 更新状态流）。 */
    suspend fun setApiListJson(json: String) {            // 写 API 列表
        securePrefs.edit().putString(API_LIST_KEY, json).apply()  // 写入加密存储
        _apiListJson.value = json                         // 更新状态流
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

    companion object {                                    // 伴生对象：静态常量
        /** 加密存储里的 API 列表键。 */
        private const val API_LIST_KEY = "api_list_json"  // 键名（与旧 DataStore 键同名，便于理解）
    }
}
