# Retrofit + Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-keep class com.google.gson.** { *; }
-keep class com.apsystems.ez1monitor.data.api.models.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
