// ============================================================
// mnnllm_jni.cpp — MNN-LLM 本地对话引擎的 JNI 桥（M6）
// 作用：把 C++ 的 MNN::Transformer::Llm 封装成 JNI 接口，
//       供 Kotlin 的 MnnLlmNative / LocalChatEngine 调用。
// 产物：libmnnllm_jni.so（本地用 NDK clang++ 编译，提交到 jniLibs）。
// ============================================================

#include <jni.h>                                          // JNI 头文件（NDK 自带）
#include <android/log.h>                                  // Android 日志
#include <llm/llm.hpp>                                    // MNN LLM 核心 API（Llm 类，位于 include/llm/）
#include <streambuf>                                      // 自定义输出缓冲（流式回调）
#include <ostream>                                        // 输出流
#include <string>                                         // std::string
#include <exception>                                      // 异常捕获

using MNN::Transformer::Llm;                              // 简化命名空间

#define TAG "MnnLlmJni"                                   // 日志标签
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)   // 信息日志
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)  // 错误日志

/**
 * 流式回调缓冲（CallbackStreamBuf）—— 继承 std::streambuf，
 * 把 LLM 逐 token 写出的增量文本，通过 JNI 回调给 Java 的 onDelta(String)。
 * 原理：Llm::response 会把生成结果写进传入的 ostream；我们让这个 ostream
 * 指向本缓冲，每当有数据（overflow/sync）就截取并回调 Java。
 */
class CallbackStreamBuf : public std::streambuf {         // 自定义流缓冲
public:
    /** 构造：保存 JNI 环境与 Java 回调对象，初始化写入区。 */
    CallbackStreamBuf(JNIEnv* env, jobject callback)      // 构造函数
        : env_(env), callback_(callback) {                // 初始化成员
        setp(buf_, buf_ + sizeof(buf_) - 1);              // 设置写入区（留 1 字节哨兵）
    }

    /** 析构：把残留缓冲刷出。 */
    ~CallbackStreamBuf() override { flush(); }            // 析构刷出

protected:
    /** overflow：缓冲区写满或写入字符时触发，先接收字符再刷出。 */
    int_type overflow(int_type ch) override {             // 溢出处理
        if (ch != traits_type::eof()) {                   // 若非 EOF
            *pptr() = static_cast<char>(ch);              // 存入当前写指针
            pbump(1);                                     // 写指针前移
        }
        flush();                                          // 刷出到 Java
        return ch;                                        // 返回成功
    }

    /** sync：显式 flush 时触发，把缓冲内容刷出。 */
    int sync() override {                                 // 同步刷出
        flush();                                          // 刷出
        return 0;                                         // 成功
    }

private:
    /** 把当前缓冲内容回调给 Java 的 onDelta(String)。 */
    void flush() {                                        // 刷出方法
        std::ptrdiff_t n = pptr() - pbase();              // 缓冲内字节数
        if (n <= 0) return;                               // 空缓冲直接返回
        std::string text(pbase(), n);                     // 截取为字符串
        setp(buf_, buf_ + sizeof(buf_) - 1);              // 重置写入区

        jclass cls = env_->GetObjectClass(callback_);     // 取回调对象的类
        if (cls == nullptr) { env_->ExceptionClear(); return; }  // 取类失败则清理
        jmethodID mid = env_->GetMethodID(cls, "onDelta", "(Ljava/lang/String;)V");  // 取 onDelta 方法
        if (mid == nullptr) {                             // 方法不存在（异常）
            env_->ExceptionClear();                       // 清理异常
            env_->DeleteLocalRef(cls);                    // 释放局部引用
            return;                                       // 返回
        }
        jstring jtext = env_->NewStringUTF(text.c_str()); // 转 Java 字符串
        env_->CallVoidMethod(callback_, mid, jtext);      // 回调 Java onDelta
        if (env_->ExceptionCheck()) env_->ExceptionClear();  // 清理回调异常
        env_->DeleteLocalRef(jtext);                      // 释放字符串引用
        env_->DeleteLocalRef(cls);                        // 释放类引用
    }

    JNIEnv* env_;                                         // JNI 环境（调用线程）
    jobject callback_;                                    // Java 回调对象（onDelta）
    char buf_[1024];                                      // 内部缓冲区（1KB）
};

extern "C" {                                              // C 链接（JNI 要求）

/**
 * nativeInit —— 加载模型，返回 Llm 实例的原生指针（0 表示失败）。
 * @param modelDir 模型目录（含 config.json）
 */
JNIEXPORT jlong JNICALL                                   // JNI 导出
Java_com_memuo_core_ai_engine_MnnLlmNative_nativeInit(    // 方法全名（包.类.方法）
    JNIEnv* env, jobject, jstring modelDir) {             // 参数
    const char* dir = env->GetStringUTFChars(modelDir, nullptr);  // 取目录字符串
    std::string configPath = std::string(dir) + "/config.json";   // 拼 config.json 路径
    env->ReleaseStringUTFChars(modelDir, dir);            // 释放字符串

    Llm* llm = Llm::createLLM(configPath);                // 创建 Llm 实例
    if (llm == nullptr) {                                 // 创建失败
        LOGE("createLLM failed: %s", configPath.c_str()); // 记录错误
        return 0;                                         // 返回 0
    }
    if (!llm->load()) {                                   // 加载模型失败
        LOGE("llm load failed: %s", configPath.c_str());  // 记录错误
        Llm::destroy(llm);                                // 销毁实例
        return 0;                                         // 返回 0
    }
    LOGI("llm loaded: %s", configPath.c_str());           // 加载成功
    return reinterpret_cast<jlong>(llm);                  // 返回原生指针
}

/**
 * nativeResponse —— 生成回复（同步阻塞，Java 侧需放后台线程）。
 * 通过 CallbackStreamBuf 把增量文本流式回调给 callback.onDelta(String)。
 * @param ptr nativeInit 返回的指针
 * @param prompt 用户输入
 * @param callback 含 onDelta(String) 的回调对象
 */
JNIEXPORT jboolean JNICALL                                // JNI 导出
Java_com_memuo_core_ai_engine_MnnLlmNative_nativeResponse(  // 方法全名
    JNIEnv* env, jobject, jlong ptr, jstring prompt, jobject callback) {  // 参数
    Llm* llm = reinterpret_cast<Llm*>(ptr);               // 还原 Llm 指针
    if (llm == nullptr) return JNI_FALSE;                 // 空指针返回失败

    const char* p = env->GetStringUTFChars(prompt, nullptr);  // 取 prompt
    std::string promptStr(p);                             // 转 std::string
    env->ReleaseStringUTFChars(prompt, p);                // 释放

    try {                                                 // 捕获 C++ 异常
        CallbackStreamBuf buf(env, callback);             // 构造回调缓冲
        std::ostream os(&buf);                            // 指向缓冲的输出流
        llm->response(promptStr, &os);                    // 生成（内部走 chat 模板）
    } catch (const std::exception& e) {                   // 生成异常
        LOGE("response exception: %s", e.what());         // 记录异常
        return JNI_FALSE;                                 // 返回失败
    }
    return JNI_TRUE;                                      // 成功
}

/**
 * nativeRelease —— 释放 Llm 实例。
 * @param ptr nativeInit 返回的指针
 */
JNIEXPORT void JNICALL                                    // JNI 导出
Java_com_memuo_core_ai_engine_MnnLlmNative_nativeRelease(  // 方法全名
    JNIEnv*, jobject, jlong ptr) {                        // 参数
    Llm* llm = reinterpret_cast<Llm*>(ptr);               // 还原指针
    if (llm != nullptr) Llm::destroy(llm);                // 非空则销毁
}

}                                                         // extern "C" 结束
