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

# Preserve llamacpp-kotlin JNI bindings and native callbacks
-keep class org.nehuatl.llamacpp.** { *; }
-keepclassmembers class org.nehuatl.llamacpp.** { *; }

# Preserve Vosk and JNA native bindings for offline speech-to-text
-keep class com.sun.jna.** { *; }
-keepclassmembers class * extends com.sun.jna.** { public *; }
-dontwarn com.sun.jna.**
-keep class org.vosk.** { *; }
-keepclassmembers class org.vosk.** { *; }

# Preserve Room entities, DAOs, Database, and model classes
-keep class com.example.data.local.** { *; }
-keep class com.example.data.model.** { *; }
-keepattributes Signature,InnerClasses,EnclosingMethod

# Strip verbose/debug/info logs from release builds
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# OkHttp / Okio optional platform annotations and optional TLS providers
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**


