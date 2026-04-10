



#ifndef CHIYUANVA_RUNTIMEHOOK_H
#define CHIYUANVA_RUNTIMEHOOK_H


#include "BaseHook.h"
#include <jni.h>

class RuntimeHook : public BaseHook {
public:
    static void init(JNIEnv *env);
};


#endif 
