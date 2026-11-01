# NativeAnalyticsEngine (Layer 2 Domain Engine)

[![Release](https://img.shields.io/badge/Release-v1.0.2-blue.svg)](https://github.com/com.roni.library.logger/native-analytics-engine/releases)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg)](https://android-arsenal.com/api?level=21)
[![License](https://img.shields.io/badge/License-Apache-2.0-blue.svg)](LICENSE)
[![Architecture](https://img.shields.io/badge/Architecture-NativeRuntime%20L2-orange.svg)](#architecture)

High-performance, decoupled, event tracking, user session metrics, and telemetry batching engine for the **NativeRuntime Android SDK Ecosystem**.

---

## Architecture Overview

`NativeAnalyticsEngine` is classified under **Layer 2: Domain Engine Subsystems** within the 6-layer NativeRuntime architecture. It uses the decoupled, independent `NativeStorageEngine` (`com.github.infoadmstudio2020-cyber:native-storage-engine`) for zero-loss offline persistent disk buffering without duplicate internal storage implementations.

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
|  -> NativeAnalyticsEngine (SessionTracker • PerformanceTracker • BatchUploader)   |
|     |                                                                             |
|     +---> uses NativeStorageEngine (Layer 2 Decoupled Dependency)                 |
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

1. **Event Queue & Batching (`EventQueue`, `BatchUploader`)**:
   - Thread-safe bounded in-memory event queue with overflow protection.
   - Configurable batch intervals, size thresholds, and automatic periodic flush.
2. **Session Lifecycle Tracking (`SessionTracker`)**:
   - Foreground and background lifecycle duration tracking.
   - Per-screen view timing and unique session generation.
3. **Performance Metrics & Traces (`PerformanceTracker`)**:
   - High-resolution code execution tracing (`startTrace` / `stopTrace`).
   - Web bridge latency, HTTP response time, and first-paint metrics reporting.
4. **Offline Disk Buffer (`DiskBufferAnalyticsProvider`)**:
   - Zero-loss offline buffering powered by independent `NativeStorageEngine` (`KeyValueStore`).
5. **Multi-Destination Telemetry (`CompositeAnalyticsProvider`, `TelemetryDispatcher`)**:
   - Pluggable memory, disk buffer, and remote analytics dispatchers.
6. **Command Handler (`AnalyticsCommandHandler`)**:
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
    implementation 'com.github.infoadmstudio2020-cyber:native-storage-engine:1.0.0'
    implementation 'com.github.com.roni.library.logger:native-analytics-engine:1.0.2'
}
```

### 3. ProGuard / R8 Rules (`consumer-rules.pro`)
Consumer rules are automatically included in the AAR:
```proguard
-keep public class com.roni.library.analytics.** { public *; }
```

---

## Code Examples

### Initialization
```java
NativeAnalyticsEngineConfig config = NativeAnalyticsEngineConfig.builder()
        .debugEnabled(BuildConfig.DEBUG)
        .tag("AnalyticsEngine")
        .batchIntervalMs(5000)
        .batchSizeThreshold(20)
        .maxQueueSize(1000)
        .diskBufferingEnabled(true)
        .build();

NativeAnalyticsEngineResult<Boolean> result = NativeAnalyticsEngine.getInstance().init(context, config);
```

### Track Events
```java
Map<String, Object> params = new HashMap<>();
params.put("item_id", "prod_99");
params.put("category", "shoes");

NativeAnalyticsEngine.getInstance().trackEvent("view_item", params);
```

### Performance Metrics & Tracing
```java
// Performance Traces
NativeAnalyticsEngine.getInstance().getPerformanceTracker().startTrace("load_feed");
// ... load feed operation ...
long durationMs = NativeAnalyticsEngine.getInstance().getPerformanceTracker().stopTrace("load_feed");

// Record Custom Metric
NativeAnalyticsEngine.getInstance().recordMetric("render_fps", 59.8, "fps");
```

### Session Lifecycle
```java
SessionTracker tracker = NativeAnalyticsEngine.getInstance().getSessionTracker();
tracker.startSession();
tracker.startScreenView("HomeActivity");
// ... user navigates ...
tracker.endScreenView("HomeActivity");
```

---

## Interactive Companion Demo App

The companion demonstration app in `:app` includes:
- **Engine Dashboard:** Live status, active session ID, batch queue stats.
- **Analytics Controls:** Interactive buttons to trigger events, metrics, performance traces, and flush telemetry.
- **Runtime Telemetry Log:** Real-time console log showing telemetry dispatches.

---

## Testing & Verification

Run JVM Unit Tests:
```bash
./gradlew :native-analytics-engine:test
```

Build Release Artifacts:
```bash
./gradlew clean :native-analytics-engine:assembleRelease :app:assembleRelease
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
