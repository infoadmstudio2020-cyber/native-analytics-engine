# NativeAnalyticsEngine API Reference

## 1. Primary Entry Points

### `NativeAnalyticsEngine`
Singleton facade for the entire analytics subsystem.

#### Methods
- `static NativeAnalyticsEngine getInstance()`: Returns the global singleton instance.
- `NativeAnalyticsEngineResult<Void> init(Context context, NativeAnalyticsEngineConfig config)`: Initializes engine, sets up providers, offline queue, and registers service.
- `boolean isInitialized()`: Returns `true` if initialized.
- `NativeAnalyticsEngineConfig getConfig()`: Returns active configuration.
- `RuntimeState getRuntimeState()`: Returns lifecycle state (`UNINITIALIZED`, `READY`, `ERROR`, `DISPOSED`).
- `void trackEvent(String name, Map<String, Object> parameters)`: Tracks an event with key-value parameters.
- `void trackEvent(AnalyticsEvent event)`: Tracks an `AnalyticsEvent` contract DTO.
- `void recordMetric(PerformanceMetric metric)`: Records a performance metric.
- `void recordMetric(String name, double value, String unit)`: Records a metric by parameters.
- `void flush()`: Immediately flushes in-memory buffers and persistent disk queue.
- `SessionTracker getSessionTracker()`: Returns active session tracker.
- `PerformanceTracker getPerformanceTracker()`: Returns active performance tracker.
- `NativeAnalyticsService getService()`: Returns underlying Layer 1 `BaseService`.
- `AnalyticsCommandHandler getCommandHandler()`: Returns RPC command handler.
- `EventQueue getEventQueue()`: Returns offline persistent event queue.
- `void release()`: Releases all resources and resets the singleton instance.

### `AnalyticsEngine`
Static convenience facade mirroring all `NativeAnalyticsEngine` public methods for cleaner caller syntax.

---

## 2. Configuration & Results

### `NativeAnalyticsEngineConfig.Builder`

| Method | Default | Description |
|--------|---------|-------------|
| `debugEnabled(boolean)` | `false` | Enables diagnostic logging |
| `tag(String)` | `"NativeAnalyticsEngine"` | Logcat tag |
| `timeout(int)` | `30000` | Network/operation timeout in ms |
| `batchSize(int)` | `50` | Maximum events flushed per batch |
| `flushIntervalMs(long)` | `15000` | Periodic flush schedule interval in ms |
| `offlinePersistenceEnabled(boolean)` | `true` | Enables zero-loss disk queue |
| `storageDirectory(File)` | `filesDir/native_analytics` | Custom directory for offline storage |
| `build()` | — | Creates immutable config object |

### `NativeAnalyticsEngineResult<T>`

| Method | Description |
|--------|-------------|
| `isSuccess()` | True if operation completed successfully |
| `getData()` | Result payload or null |
| `getErrorCode()` | Numeric error code (0 on success) |
| `getErrorMessage()` | Diagnostic message or null |

---

## 3. Trackers & Queue

### `SessionTracker`
- `void startSession()`: Explicitly starts a new session with unique UUID.
- `void endSession()`: Ends the active session and logs duration.
- `void onAppForeground()`: Marks app active and updates duration timers.
- `void onAppBackground()`: Accumulates active session duration.
- `void startScreenView(String screenName)`: Records screen transition start.
- `long endScreenView(String screenName)`: Records screen leave and returns elapsed ms.
- `String getCurrentSessionId()`: Returns active UUID or null.
- `boolean isSessionActive()`: Returns session status.
- `long getTotalActiveDurationMs()`: Returns total accumulated active foreground ms.

### `PerformanceTracker`
- `void startTrace(String traceName)`: Starts measuring execution duration.
- `long stopTrace(String traceName)`: Stops measurement, records metric, returns duration ms.
- `void recordBridgeLatency(String action, long durationMs)`: Records RPC bridge execution time.
- `void recordWebFirstPaint(long durationMs)`: Records web view first paint timing.
- `void recordHttpResponseTime(String endpoint, int statusCode, long durationMs)`: Records network response timing.
- `boolean hasActiveTrace(String traceName)`: Checks if trace is ongoing.

### `EventQueue`
- `void enqueue(AnalyticsEvent event)`: Persists event to offline collection.
- `List<AnalyticsEvent> dequeue(int maxBatchSize)`: Drains up to batch size events from disk/memory.
- `List<AnalyticsEvent> peek(int limit)`: Previews events without removing.
- `int count()`: Returns total pending event count.
- `void flush()`: Checkpoints disk collection.
- `void clear()`: Purges all pending events.

---

## 4. RPC Command Routing (`AnalyticsCommandHandler`)

Implements `com.roni.library.contracts.api.IApiHandler`.
Route Prefix: `analytics`

Supported Actions:
- `trackEvent`: `{ name: String, parameters: Map }`
- `recordMetric`: `{ name: String, value: Double, unit: String }`
- `startSession`: `{}` -> `{ sessionId, startTime }`
- `endSession`: `{}` -> `{ status: "ended", activeDurationMs }`
- `startTrace`: `{ name: String }` -> `{ trace, status: "started" }`
- `stopTrace`: `{ name: String }` -> `{ trace, durationMs }`
- `startScreenView`: `{ screenName: String }`
- `endScreenView`: `{ screenName: String }` -> `{ screenName, durationMs }`
- `getStats`: `{}` -> `{ sessionId, sessionActive, totalActiveDurationMs, memoryEventCount, memoryMetricCount, offlineQueueCount }`
- `flush`: `{}` -> `{ status: "flushed" }`
- `clear`: `{}` -> `{ status: "cleared" }`

