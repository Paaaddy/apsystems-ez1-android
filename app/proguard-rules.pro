# Retrofit + Gson
-keepattributes Signature
-keepattributes EnclosingMethod
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-keep class com.google.gson.** { *; }
-keep class com.apsystems.ez1monitor.data.api.models.** { *; }
-keep class com.apsystems.ez1monitor.data.repository.EZ1Result { *; }
-keep class com.apsystems.ez1monitor.data.repository.EZ1Result$* { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.jetbrains.annotations.**
