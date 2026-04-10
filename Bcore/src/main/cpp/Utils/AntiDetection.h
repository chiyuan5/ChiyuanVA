#ifndef CHIYUANVA_ANTIDETECTION_H
#define CHIYUANVA_ANTIDETECTION_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

void AntiDetection_enable(JNIEnv *env, const char* guestPkg, const char* hostPkg);

#ifdef __cplusplus
}
#endif

#endif
