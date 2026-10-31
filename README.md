# NativeStorageEngine (Layer 2 Domain Engine)

[![Release](https://img.shields.io/badge/Release-v1.0.1-blue.svg)](https://github.com/com.roni.library.logger/native-storage-engine/releases)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg)](https://android-arsenal.com/api?level=21)
[![License](https://img.shields.io/badge/License-Apache-2.0-blue.svg)](LICENSE)
[![Architecture](https://img.shields.io/badge/Architecture-NativeRuntime%20L2-orange.svg)](#architecture)

High-performance, decoupled, zero-loss persistent storage, AES-GCM encrypted preferences, document NoSQL database, and LRU disk cache engine for the **NativeRuntime Android SDK Ecosystem**.

---

## Architecture Overview

`NativeStorageEngine` is classified under **Layer 2: Domain Engine Subsystems** within the 6-layer NativeRuntime architecture.

```
+-----------------------------------------------------------------------------------+
| Layer 4 / 5: Application / Master SDK (NativeRuntimeSDK)                          |
+-----------------------------------------------------------------------------------+
                                        | Dispatches RPC
+-----------------------------------------------------------------------------------+
| Layer 3: Transport Bridge (NativeWebBridgeSDK)                                    |
+-----------------------------------------------------------------------------------+
                                        | Routes Requests
+-----------------------------------------------------------------------------------+
| Layer 2: Domain Engines                                                           |
|  -> NativeStorageEngine (EncryptedStore • NoSQL DB • LRU Cache • KeyValueStore)   |
+-----------------------------------------------------------------------------------+
                                        | Managed Service & Diagnostics
+-----------------------------------------------------------------------------------+
| Layer 1: Core Infrastructure (NativeServiceSDK, NativeLoggerSDK)                  |
+-----------------------------------------------------------------------------------+
                                        | Inherits
+-----------------------------------------------------------------------------------+
| Layer 0: Universal Foundation Contracts (NativeRuntimeContracts)                  |
+-----------------------------------------------------------------------------------+
```

---

## Key Subsystems & Features

1. **Encrypted Preferences (`EncryptedStore` / `EncryptedPreferences`)**:
   - Industry-standard AES-GCM authenticated encryption.
   - Transparent encryption and decryption of strings, integers, booleans, and floats.
2. **Document NoSQL Database (`StorageDatabase` & `StorageCollection`)**:
   - Schema-less document storage supporting nested keys, indexing, and transactional batch commits.
   - Flexible query filtering (`QueryFilter.eq`, `QueryFilter.gt`, `QueryFilter.all`).
3. **LRU Disk Cache (`LruDiskCache`)**:
   - Size-bounded disk cache with automated eviction of least recently used byte arrays.
   - Configurable maximum byte size threshold.
4. **Fast Key-Value Store (`KeyValueStore`)**:
   - In-memory concurrent map with synchronous background disk flush.
5. **Database Helper (`DatabaseHelper`)**:
   - Unified helper facade for structured tabular and collection records.
6. **Command Handler (`StorageCommandHandler`)**:
   - Standardized `NativeApiRequest` / `NativeApiResponse` RPC dispatcher for web bridge integration.

---

## Consumer Integration Guide

### 1. Add Repository in `settings.gradle`
```groovy
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

### 2. Declare Dependency in `app/build.gradle`
```groovy
dependencies {
    implementation 'com.github.infoadmstudio2020-cyber:NativeRuntimeContracts:v1.0.1'
    implementation 'com.github.infoadmstudio2020-cyber:NativeServiceSDK:1.0.1'
    implementation 'com.github.com.roni.library.logger:native-storage-engine:1.0.1'
}
```

### 3. ProGuard / R8 Rules (`consumer-rules.pro`)
Consumer rules are automatically included in the AAR:
```proguard
-keep public class com.roni.library.storage.** { public *; }
-keep interface com.roni.library.storage.** { *; }
-keep class com.roni.library.storage.database.** { *; }
-keep class com.roni.library.storage.preferences.** { *; }
-keep class com.roni.library.storage.crypto.** { *; }
-keep class com.roni.library.storage.handler.** { *; }
-keep class com.roni.library.storage.service.** { *; }
-keep class com.roni.library.storage.NativeStorageEngineResult { *; }
-keep class com.roni.library.storage.NativeStorageEngineConfig { *; }
```

---

## Code Examples

### Initialization
```java
NativeStorageEngineConfig config = new NativeStorageEngineConfig.Builder()
        .debugEnabled(BuildConfig.DEBUG)
        .tag("StorageEngine")
        .encryptionPassword("SecureAppPassword2026!")
        .build();

NativeStorageEngineResult<Boolean> result = NativeStorageEngine.getInstance().init(context, config);
```

### Encrypted Preferences
```java
EncryptedPreferences prefs = NativeStorageEngine.getInstance().getPreferences("secure_user_prefs");
prefs.putString("auth_token", "jwt_token_header_payload_signature");
prefs.putInt("user_id", 10042);

String token = prefs.getString("auth_token", null);
```

### Document Store (NoSQL)
```java
StorageCollection users = NativeStorageEngine.getInstance().getDatabase().getCollection("users");

// Insert
Document doc = new Document("user_42")
        .put("username", "alex_hunter")
        .put("role", "admin")
        .put("score", 95.8);
users.insert(doc);

// Query
Document retrieved = users.findById("user_42");
List<Document> admins = users.find(QueryFilter.eq("role", "admin"));
```

### LRU Disk Cache
```java
LruDiskCache cache = NativeStorageEngine.getInstance().getDiskCache();
cache.put("avatar_42", imageByteArray);

byte[] cachedImage = cache.get("avatar_42");
```

---

## Interactive Companion Demo App

The companion demonstration app in `:app` includes:
- **Engine Dashboard:** Live status, active storage directory, cache size metrics.
- **Feature Explorer:** Live forms to test Encrypted Preferences, Document Database, LRU Cache, and KeyValueStore.
- **Integration Test Center:** One-click execution of 6 automated integration tests with instant PASS/FAIL reporting.
- **Runtime Console:** Real-time scrolling telemetry log with clear and copy capabilities.

---

## Testing & Verification

Run JVM Unit Tests:
```bash
./gradlew :native-storage-engine:test
```

Build Release Artifacts:
```bash
./gradlew clean :native-storage-engine:assembleRelease :app:assembleRelease
```

---

## License

```
Copyright 2026 infoadmstudio2020-cyber

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```
