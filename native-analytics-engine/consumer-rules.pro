# Consumer ProGuard rules for NativeAnalyticsEngine
-keep public class com.roni.library.analytics.** { public *; }
-keep interface com.roni.library.analytics.** { *; }
-keepclassmembers class com.roni.library.analytics.** {
    public *;
    protected *;
}
-dontwarn com.roni.library.analytics.**
