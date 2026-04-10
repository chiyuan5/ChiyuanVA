# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keep class com.chiyuan.va.** {*; }
-keep class com.chiyuan.va.jnihook.** {*; }
-keep class mirror.** {*; }
-keep class android.** {*; }
-keep class com.android.** {*; }

-keep class com.chiyuan.va.reflection.** {*; }
-keep @com.chiyuan.va.reflection.annotation.BClass class * {*;}
-keep @com.chiyuan.va.reflection.annotation.BClassName class * {*;}
-keep @com.chiyuan.va.reflection.annotation.BClassNameNotProcess class * {*;}
-keepclasseswithmembernames class * {
    @com.chiyuan.va.reflection.annotation.BField.* <methods>;
    @com.chiyuan.va.reflection.annotation.BFieldNotProcess.* <methods>;
    @com.chiyuan.va.reflection.annotation.BFieldSetNotProcess.* <methods>;
    @com.chiyuan.va.reflection.annotation.BFieldCheckNotProcess.* <methods>;
    @com.chiyuan.va.reflection.annotation.BMethod.* <methods>;
    @com.chiyuan.va.reflection.annotation.BStaticField.* <methods>;
    @com.chiyuan.va.reflection.annotation.BStaticMethod.* <methods>;
    @com.chiyuan.va.reflection.annotation.BMethodCheckNotProcess.* <methods>;
    @com.chiyuan.va.reflection.annotation.BConstructor.* <methods>;
    @com.chiyuan.va.reflection.annotation.BConstructorNotProcess.* <methods>;
}