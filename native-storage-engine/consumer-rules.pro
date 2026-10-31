# Consumer ProGuard rules for NativeStorageEngine
-keep public class com.roni.library.storage.** { public *; }
-keep interface com.roni.library.storage.** { *; }
-keep class com.roni.library.storage.database.** { *; }
-keep class com.roni.library.storage.preferences.** { *; }
-keep class com.roni.library.storage.crypto.** { *; }
-keep class com.roni.library.storage.handler.** { *; }
-keep class com.roni.library.storage.service.** { *; }
-keep class com.roni.library.storage.NativeStorageEngineResult { *; }
-keep class com.roni.library.storage.NativeStorageEngineConfig { *; }
