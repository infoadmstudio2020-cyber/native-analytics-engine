# NativeAnalyticsEngine API Reference

## NativeAnalyticsEngine

Main entry point for the analytics library.

### Methods

#### `static getInstance()`
Returns the singleton instance.

#### `init(Context context, NativeAnalyticsEngineConfig config)`
Initializes the engine with configuration. Must be called before tracking events or recording metrics.

**Parameters:**
- `context` — application context (nullable for pure JVM usage)
- `config` — configuration object

#### `isInitialized()`
Returns `true` if the engine has been initialized.

#### `getConfig()`
Returns the active `NativeAnalyticsEngineConfig` or null.

#### `trackEvent(String name, Map<String, Object> params)`
Enqueues and dispatches an analytics event with parameters.

#### `trackEvent(AnalyticsEvent event)`
Directly dispatches a strongly typed `AnalyticsEvent`.

#### `recordMetric(String name, double value, String unit)`
Records a performance metric.

#### `recordMetric(PerformanceMetric metric)`
Records a strongly typed `PerformanceMetric`.

#### `flush()`
Flushes pending queued events through batch uploader and telemetry providers.

#### `release()`
Releases all workers, services, and dispatchers. Resetting singleton state.

---

## NativeAnalyticsEngineConfig.Builder

| Method | Default | Description |
|--------|---------|-------------|
| `debugEnabled(boolean)` | `false` | Enable verbose diagnostic logging |
| `tag(String)` | `"NativeAnalyticsEngine"` | Logger tag |
| `timeout(int)` | `30000` | Timeout in milliseconds |
| `batchIntervalMs(long)` | `5000` | Interval between automated batch uploads |
| `batchSizeThreshold(int)` | `20` | Event count threshold triggering immediate upload |
| `maxQueueSize(int)` | `1000` | Maximum memory queue buffer capacity |
| `diskBufferingEnabled(boolean)` | `true` | Enable zero-loss offline disk buffer using NativeStorageEngine |
| `build()` | — | Create configuration |

---

## NativeAnalyticsEngineResult<T>

| Method | Description |
|--------|-------------|
| `isSuccess()` | True if operation succeeded |
| `getData()` | Result payload or null |
| `getErrorMessage()` | Error description or null |
| `getErrorCode()` | Status error code (0 for success) |
