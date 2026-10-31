# Changelog

All notable changes to NativeStorageEngine are documented here.
Format based on [Keep a Changelog](https://keepachangelog.com).

## [1.0.1] — 2026-10-31

### Added
- Initial production-ready release of NativeStorageEngine (Layer 2 Domain Engine).
- AES-GCM Encrypted Preferences Store (`EncryptedStore` / `EncryptedPreferences`).
- NoSQL Document Store with Query Filters and batch transactional commits (`StorageDatabase`, `StorageCollection`, `Document`, `QueryFilter`).
- Size-bounded LRU Disk Cache with automatic least-recently-used eviction (`LruDiskCache`).
- Persistent concurrent Key-Value Store (`KeyValueStore`).
- Database helper facade (`DatabaseHelper`).
- Standardized RPC command handler (`StorageCommandHandler`).
- Universal interactive companion demonstration application with Dashboard, Feature Explorer, Test Center, and Live Console.
- Comprehensive JVM unit test suite (`StorageEngineTest`).
- Strict ProGuard / R8 consumer preservation rules in `consumer-rules.pro`.
- Automated GitHub Actions CI and Release pipelines.
- JitPack cloud build and Maven publishing configurations.
