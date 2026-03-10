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


# ------------------------------------------------------------------
# Release 版：移除所有 android.util.Log 呼叫，減少無效日誌與記憶體分配
# 這段只在 minifyEnabled=true 的 buildType（通常是 release）生效。
# ------------------------------------------------------------------
-assumenosideeffects class android.util.Log {
    *;
}
