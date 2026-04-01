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

# ──────────────────────────────────────────────
# Retrofit 2
# ──────────────────────────────────────────────
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*

# ──────────────────────────────────────────────
# OkHttp 3
# ──────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ──────────────────────────────────────────────
# Gson
# ──────────────────────────────────────────────
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ──────────────────────────────────────────────
# DTO / Network response classes (Gson 직렬화 보호)
# ──────────────────────────────────────────────
-keep class com.example.howscat.dto.** { *; }
-keep class com.example.howscat.kakao.** { *; }

# ──────────────────────────────────────────────
# BuildConfig
# ──────────────────────────────────────────────
-keep class com.example.howscat.BuildConfig { *; }

# ──────────────────────────────────────────────
# Network / Auth (클래스명 문자열로 참조되므로 난독화 제외)
# ──────────────────────────────────────────────
-keep class com.example.howscat.network.** { *; }
-keep class com.example.howscat.LoginActivity { *; }
-keep class com.example.howscat.SignupActivity { *; }

# ──────────────────────────────────────────────
# SharedPreferences 키 보호 (리플렉션 없이 문자열 직접 참조)
# ──────────────────────────────────────────────
-keepattributes EnclosingMethod