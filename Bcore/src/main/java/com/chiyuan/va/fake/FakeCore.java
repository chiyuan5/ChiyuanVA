package com.chiyuan.va.fake;

import com.chiyuan.va.jnihook.ReflectCore;


public class FakeCore {
    public static void init() {
        ReflectCore.set(android.app.ActivityThread.class);
    }
}
