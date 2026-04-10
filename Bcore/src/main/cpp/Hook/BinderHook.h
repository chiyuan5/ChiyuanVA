



#ifndef CHIYUANVA_BINDERHOOK_H
#define CHIYUANVA_BINDERHOOK_H


#include "BaseHook.h"

class BinderHook : public BaseHook{
public:
    static void init(JNIEnv *env);
};

#endif 
