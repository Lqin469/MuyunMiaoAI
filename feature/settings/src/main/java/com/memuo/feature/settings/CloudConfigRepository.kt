package com.memuo.feature.settings                         // 声明包名：设置业务模块

import android.content.Context                            // 导入 Context：应用上下文
import android.content.SharedPreferences                  // 导入 SharedPreferences：键值存储接口
import androidx.datastore.core.DataStore                  // 导入 DataStore：偏好存储
import androidx.datastore.preferences.core.Preferences    // 导入 Preferences：键值对容器
import androidx.datastore.preferences.core.edit           // 导入 edit：写入偏好
import androidx.datastore.preferences.core.stringPreferencesKey  // 导入字符串型偏好键
import androidx.datastore.preferences.preferencesDataStore // 导入 preferencesDataStore：创建 DataStore 委托
import androidx.security.crypto.EncryptedSharedPreferences // 导入 EncryptedSharedPreferences：加密的偏好存储
import androidx.security.crypto.MasterKey                 // 导入 MasterKey：Android Keystore 主密钥
import com.memuo.core.ai.engine.CloudConfig                // 导入云端配置数据类
import com.memuo.core.ai.engine.CloudConfigProvider        // 导入云端配置提供者接口
import dagger.hilt.android.qualifiers.ApplicationContext  // 导入 ApplicationContext：应用级上下文
import kotlinx.coroutines.flow.first                       // 导入 first：取 Flow 第一个值
import javax.inject.Inject                                 // 导入 Inject：构造函数注入
import javax.inject.Singleton                              // 导入 Singleton：单例作用域

/** 云端设置的 DataStore 实例（顶层委托，name = "cloud_settings"，存非敏感项）。 */
private val Context.cloudSettingsDataStore by preferencesDataStore(name = "cloud_settings")  // 用委托创建 DataStore

/**
 * 云端配置仓库（CloudConfigRepository）—— 持久化用户自配的云端 API 配置（R1），
 * 并实现 core:ai:engine 的 CloudConfigProvider 接口（依赖倒置：core 不依赖 feature）。
 *
 * 安全分级存储：
 *  - baseUrl / model（非敏感）→ DataStore；
 *  - apiKey（敏感）→ EncryptedSharedPreferences（Android Keystore AES256_GCM 加密）。
 */
@Singleton                                               // 单例：全应用共享
class CloudConfigRepository @Inject constructor(         // 构造函数注入
    @ApplicationContext private val context: Context,    // 注入应用级上下文
) : CloudConfigProvider {                                // 实现云端配置提供者接口

    /** 加密偏好存储（apiKey 专用）：主密钥来自 Android Keystore，值用 AES256_GCM 加密。 */
    /** 加密偏好存储（apiKey 专用）：主密钥来自 Android Keystore，值用 AES256_GCM 加密。 */
    private val securePrefs: SharedPreferences by lazy { // 懒加载加密存储
        runCatching {                                    // 容错：Keystore 损坏/个别设备失败时不崩溃
            val masterKey = MasterKey.Builder(context)    // 构建主密钥（Keystore 生成，永不离开设备）
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)  // 用 AES256_GCM 方案
                .build()                                  // 构建
            EncryptedSharedPreferences.create(            // 创建加密偏好存储
                context,                                  // 上下文
                "secure_cloud_prefs",                     // 文件名
                masterKey,                                // 主密钥
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,   // 键名也加密
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM, // 值加密
            )
    /** 读取当前云端配置；未配置完整则返回 null（UI 据此提示"未配置"）。 */
    override suspend fun current(): CloudConfig? {       // 读取配置方法
        val prefs = context.cloudSettingsDataStore.data.first()  // 读 DataStore（非敏感项）
        val baseUrl = prefs[KEY_BASE_URL] ?: return null // 无地址则未配置
        val model = prefs[KEY_MODEL] ?: return null      // 无模型则未配置
        val apiKey = securePrefs.getString(KEY_API_KEY, null) ?: return null  // 从加密存储读密钥
        return CloudConfig(baseUrl = baseUrl, apiKey = apiKey, model = model)  // 组装配置
    }

    /** 保存云端配置（用户在设置页填写后调用）；apiKey 加密存储。 */
    suspend fun save(baseUrl: String, apiKey: String, model: String) {  // 保存配置方法
        context.cloudSettingsDataStore.edit { prefs ->   // 原子写入 DataStore（非敏感项）
            prefs[KEY_BASE_URL] = baseUrl.trimEnd('/')   // 保存地址（去掉末尾斜杠）
            prefs[KEY_MODEL] = model                     // 保存模型名
        }
        securePrefs.edit().putString(KEY_API_KEY, apiKey).apply()  // apiKey 写入加密存储（Keystore 加密）
    }

    companion object {                                    // 伴生对象：静态键
        private val KEY_BASE_URL = stringPreferencesKey("cloud_base_url")  // 地址键（DataStore）
        private const val KEY_API_KEY = "cloud_api_key"   // 密钥键（加密存储）
        private val KEY_MODEL = stringPreferencesKey("cloud_model")        // 模型键（DataStore）
    }
}
