
#ifndef CHIYUANVA_DEXFILEHOOK_H
#define CHIYUANVA_DEXFILEHOOK_H

#include "BaseHook.h"

class DexFileHook : public BaseHook{
public:
    static void init(JNIEnv *env);
    static void setFileReadonly(const char* filePath);
};


#endif 
