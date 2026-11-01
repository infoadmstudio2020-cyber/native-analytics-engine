# Changelog

All notable changes to **NativeAnalyticsEngine** are documented here.
Format based on [Keep a Changelog](https://keepachangelog.com).

## [1.0.6] — 2026-11-02

### Added
- **Core Engine**: Full production implementation of `NativeAnalyticsEngine` and `AnalyticsEngine` singleton facades.
- **Ecosystem Integration**: Direct integration with `NativeRuntimeContracts:v1.0.2`, `NativeServiceSDK:v1.0.1`, and `native-storage-engine:v1.0.6`.
- **Offline Persistence**: Implemented `EventQueue` and `DiskBufferAnalyticsProvider` powered by `StorageDatabase` and `StorageCollection` for zero-loss offline telemetry queueing.
- **Provider System**: Added `CompositeAnalyticsProvider` and `MemoryAnalyticsProvider` for multiplexed and in-memory event capture.
- **Performance Tracing**: Added `PerformanceTracker` with support for high-resolution custom traces, WebBridge RPC latency tracking, first-paint timings, and HTTP response duration metrics.
- **Session Lifecycle**: Implemented `SessionTracker` managing session UUIDs, active foreground/background durations, and screen transitions.
- **RPC Command Handler**: Created `AnalyticsCommandHandler` implementing `IApiHandler` under route prefix `analytics` for Layer 3 WebBridge RPC invocations.
- **Batching & Scheduling**: Added `BatchUploader` and `TelemetryDispatcher` for asynchronous background event streaming.
- **Automated Testing**: Created comprehensive JUnit 4 test suite with 10 test categories in `NativeAnalyticsEngineTest`.
- **Interactive Demo App**: Complete companion Android app with dashboard, feature explorer, diagnostic monitor, integration test center, and live console in `MainActivity`.
- **Packaging & ProGuard**: Added `consumer-rules.pro` keeping public APIs and `AndroidManifest.xml` for library module.
- **CI/CD Automation**: Updated GitHub Actions CI and Release workflows with `contents: write` permissions.
