# NativeAnalyticsEngine

> **Architecture Layer:** Layer 2: Domain Engine Subsystems (`L2`)  
> **Package:** `com.roni.library.analytics`  
> **Target SDK:** API 21 - 34 • **Java Compatibility:** JDK 17  
> **Maven Coordinates:** `com.github.infoadmstudio2020-cyber:native-analytics-engine:1.0.6`

Event tracking, user session lifecycle metrics, high-resolution performance tracing, zero-loss offline SQLite/NoSQL event queue, and telemetry batching engine for the NativeRuntime ecosystem.

[![JitPack](https://jitpack.io/v/infoadmstudio2020-cyber/native-analytics-engine.svg)](https://jitpack.io/#infoadmstudio2020-cyber/native-analytics-engine)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg)](https://android-arsenal.com/api?level=21)
[![License](https://img.shields.io/badge/License-Apache-2.0-blue.svg)](LICENSE)

---

## 1. Architectural Role & Features

- **Layer Directionality:** Downward dependency only; inherits from `NativeRuntimeContracts` (Layer 0), registers with `NativeServiceSDK` (Layer 1), and persists offline telemetry using `native-storage-engine` (Layer 2).
- **Zero-Loss Offline Queue:** Enqueues telemetry to persistent storage collections via `EventQueue` and `DiskBufferAnalyticsProvider`, ensuring zero event loss during network interruptions or process terminations.
- **High-Resolution Performance Tracker:** Measures WebBridge RPC call latency, web content first-paint timings, HTTP network response times, and arbitrary custom operation traces.
- **Automated Session Tracking:** Tracks session identifiers, foreground/background durations, and active screen transitions.
- **Pluggable Composite Provider:** Multiplexes events and metrics to in-memory buffers, local disk queues, and external analytics endpoints simultaneously.
- **RPC Command Handler:** Implements `IApiHandler` under the `analytics` route prefix to service Layer 3 WebBridge and hybrid JavaScript clients seamlessly.

---

## 2. Installation & Dependency Declaration

### Step 1 — Add JitPack to your project repositories

In `settings.gradle` (or root `build.gradle`):

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

### Step 2 — Add Ecosystem Dependencies

In your module's `build.gradle`:

```groovy
dependencies {
    // 1. NativeAnalyticsEngine
    implementation 'com.github.infoadmstudio2020-cyber:native-analytics-engine:1.0.6'

    // 2. Transitive & Peer Foundation Dependencies
    api 'com.github.infoadmstudio2020-cyber:NativeRuntimeContracts:v1.0.2'
    implementation 'com.github.infoadmstudio2020-cyber:NativeServiceSDK:v1.0.1'
    implementation 'com.github.infoadmstudio2020-cyber:native-storage-engine:v1.0.0'
}
```

---

## 3. Quick Start & Initialization

Initialize the engine in your Application `onCreate()` or main entry point:

```java
import com.roni.library.analytics.NativeAnalyticsEngine;
import com.roni.library.analytics.NativeAnalyticsEngineConfig;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        NativeAnalyticsEngineConfig config = new NativeAnalyticsEngineConfig.Builder()
                .debugEnabled(BuildConfig.DEBUG)
                .tag("AppAnalytics")
                .batchSize(50)
                .flushIntervalMs(15000L)
                .offlinePersistenceEnabled(true)
                .build();

        NativeAnalyticsEngine.getInstance().init(this, config);
    }
}
```

---

## 4. Feature Usage Examples

### Tracking Custom Events

```java
import com.roni.library.analytics.AnalyticsEngine;
import java.util.HashMap;
import java.util.Map;

Map<String, Object> params = new HashMap<>();
params.put("item_id", "prod_9872");
params.put("category", "electronics");
params.put("price", 149.99);

AnalyticsEngine.trackEvent("purchase_completed", params);
```

### Measuring Performance Traces

```java
// Start a high-resolution trace
AnalyticsEngine.getPerformanceTracker().startTrace("database_indexing");

// Execute work
runDatabaseWork();

// Stop trace and automatically log PerformanceMetric
long durationMs = AnalyticsEngine.getPerformanceTracker().stopTrace("database_indexing");
```

### Recording Network & Bridge Latencies

```java
// WebBridge latency
AnalyticsEngine.getPerformanceTracker().recordBridgeLatency("invokePayment", 14);

// HTTP response time
AnalyticsEngine.getPerformanceTracker().recordHttpResponseTime("/v1/checkout", 200, 185);
```

### Session & Screen View Management

```java
// Track entering and leaving screens
AnalyticsEngine.getSessionTracker().startScreenView("CartActivity");

// On leaving screen
long timeSpentMs = AnalyticsEngine.getSessionTracker().endScreenView("CartActivity");
```

### Layer 3 RPC Command Invocation

```java
import com.roni.library.contracts.api.NativeApiRequest;
import com.roni.library.contracts.api.NativeApiResponse;

NativeApiRequest request = new NativeApiRequest.Builder()
        .action("trackEvent")
        .parameter("name", "web_cta_click")
        .build();

NativeApiResponse response = AnalyticsEngine.getCommandHandler().handleRequest(request);
```

---

## 5. Public API Summary

| Class / Component | Primary Responsibilities |
|---|---|
| `NativeAnalyticsEngine` | Singleton facade, lifecycle state machine (`init`, `release`, `flush`) |
| `AnalyticsEngine` | Static shortcut facade mirroring NativeAnalyticsEngine methods |
| `NativeAnalyticsEngineConfig` | Immutable builder configuration for batch size, flush interval, storage |
| `NativeAnalyticsEngineResult<T>` | Standardized result object with error codes and diagnostic messages |
| `CompositeAnalyticsProvider` | Multiplexes events and metrics to all registered providers |
| `MemoryAnalyticsProvider` | Fast in-memory cache of events and metrics for immediate retrieval |
| `EventQueue` | Zero-loss persistent offline event queue backed by `native-storage-engine` |
| `PerformanceTracker` | High-resolution trace timers and latency recorders |
| `SessionTracker` | Automatic session lifecycle, foreground/background durations, screen views |
| `NativeAnalyticsService` | `BaseService` subclass registered with `ServiceRegistry` |
| `AnalyticsCommandHandler` | RPC command router implementing `IApiHandler` for WebBridge integration |

---

## 6. Requirements

- Android API 21+ (Android 5.0 Lollipop or newer)
- Java 17+ (Compile compatibility)
- AGP 8.3+ / Gradle 8.7+

---

## 7. License

```
Copyright 2026 infoadmstudio2020-cyber

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```
