#include <jni.h>
#include <signal.h>
#include <android/log.h>

// "GoLog" will automatically get picked up by JetBird's logging
#define JETBIRD_LOG_TAG "GoLog"
#define JETBIRD_LOG(...) __android_log_print(ANDROID_LOG_INFO, JETBIRD_LOG_TAG, __VA_ARGS__)
#define JETBIRD_LOG_ERROR(...) __android_log_print(ANDROID_LOG_ERROR, JETBIRD_LOG_TAG, __VA_ARGS__)

void ack(int sig, siginfo_t *info, void *ctx) {
    JETBIRD_LOG("JetBird caught fatal SIGSYS - continuing...");
}

JNIEXPORT void JNICALL
Java_dev_bg_jetbird_JetBirdKt_catchFatalSignal(JNIEnv *env, jobject _this) {
    struct sigaction action = {
        .sa_sigaction = ack,
        .sa_flags = SA_SIGINFO
    };
    sigemptyset(&action.sa_mask);
    if (sigaction(SIGSYS, &action, NULL) == -1) {
        JETBIRD_LOG_ERROR("Failed to install SIGSYS handler");
    }
}
