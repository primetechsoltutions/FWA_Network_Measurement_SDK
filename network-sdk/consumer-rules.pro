# --------- General SDK Protection ---------
-keep class com.ptsl.fwa_network_sdk.** { *; }
-dontwarn com.ptsl.fwa_network_sdk.**

# --------- Retrofit & OkHttp ---------
-keepattributes Signature
-keepattributes *Annotation*
-keepclasseswithmembers class * {
    @retrofit2.* <methods>;
}
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# --------- Gson ---------
-keep class com.google.gson.** { *; }
-keep class * {
  @com.google.gson.annotations.SerializedName <fields>;
}
-dontwarn com.google.gson.**

# --------- Room ---------
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# --------- WorkManager ---------
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# --------- Optional TLS Providers ---------
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# --------- Android Reflection Safety ---------
-keepclassmembers class * {
    public <init>(...);
}

# --------- Gson TypeToken ---------
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

-dontwarn java.lang.invoke.StringConcatFactory
-keep class java.lang.invoke.StringConcatFactory { *; }
