# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Оптимизированные правила для ускорения R8

# Keep только критически важные Hilt компоненты
-keep class dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.InstallIn class *
-keep @dagger.hilt.android.AndroidEntryPoint class *

# Keep только пользовательские data классы и модели
-keep class ru.neverlands.abclient.data.model.** { *; }
-keep class ru.neverlands.abclient.data.api.** { *; }
-keep class ru.neverlands.abclient.data.dto.** { *; }

# Оптимизированные правила для network
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# Gson минимальные правила
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# WebView критически важные классы
-keep class android.webkit.JavascriptInterface
-keep class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ErrorProne (только предупреждения)
-dontwarn com.google.errorprone.annotations.**

# Google API Client (missing classes detected by R8)
-dontwarn com.google.api.client.http.GenericUrl
-dontwarn com.google.api.client.http.HttpHeaders
-dontwarn com.google.api.client.http.HttpRequest
-dontwarn com.google.api.client.http.HttpRequestFactory
-dontwarn com.google.api.client.http.HttpResponse
-dontwarn com.google.api.client.http.HttpTransport
-dontwarn com.google.api.client.http.javanet.NetHttpTransport$Builder
-dontwarn com.google.api.client.http.javanet.NetHttpTransport
-dontwarn org.joda.time.Instant

# Security crypto минимум
-keep class androidx.security.crypto.MasterKey { *; }
-keep class androidx.security.crypto.EncryptedFile { *; }
-keep class androidx.security.crypto.EncryptedSharedPreferences { *; }

# Compose оптимизация - позволяем R8 оптимизировать неиспользуемые компоненты
-keep class androidx.compose.runtime.Composer { *; }
-keep class androidx.compose.runtime.ComposerKt { *; }

# Kotlin metadata минимум
-keepattributes RuntimeVisibleAnnotations
-keepattributes SourceFile,LineNumberTable

# Standard Android rules
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}