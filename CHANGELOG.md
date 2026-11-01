# Changelog

All notable changes to NativeAnalyticsEngine are documented here.
Format based on [Keep a Changelog](https://keepachangelog.com).

## [1.0.3] — 2026-11-01

### Changed
- Decoupled internal storage implementation: completely removed duplicate `native-storage-engine` code and packages from the Analytics project.
- Integrated independent `NativeStorageEngine` via official Gradle dependency (`com.github.infoadmstudio2020-cyber:native-storage-engine:1.0.0`).
- Updated companion demo application in `:app` to demonstrate `NativeAnalyticsEngine` live features.
- Configured clean multi-module build targets: `:native-analytics-engine` (library) and `:app` (demo application).

### Added
- Complete event tracking engine with bounded thread-safe `EventQueue` and periodic `BatchUploader`.
- User session lifecycle tracking (`SessionTracker`).
- High-resolution performance metric tracing (`PerformanceTracker`).
- Zero-loss offline persistence buffering (`DiskBufferAnalyticsProvider`) using external `NativeStorageEngine`.
- Pluggable telemetry dispatching architecture (`CompositeAnalyticsProvider`, `TelemetryDispatcher`).
- Standardized RPC command handler for Layer 3 transport bridge (`AnalyticsCommandHandler`).
- Comprehensive JVM unit test suite (`NativeAnalyticsEngineTest`).
