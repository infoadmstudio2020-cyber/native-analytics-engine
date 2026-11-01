package com.roni.library.analytics.service;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.roni.library.analytics.NativeAnalyticsEngineConfig;
import com.roni.library.analytics.dispatcher.BatchUploader;
import com.roni.library.analytics.dispatcher.TelemetryDispatcher;
import com.roni.library.analytics.provider.CompositeAnalyticsProvider;
import com.roni.library.analytics.provider.MemoryAnalyticsProvider;
import com.roni.library.analytics.queue.DiskBufferAnalyticsProvider;
import com.roni.library.analytics.queue.EventQueue;
import com.roni.library.analytics.tracker.PerformanceTracker;
import com.roni.library.analytics.tracker.SessionTracker;
import com.roni.library.contracts.analytics.AnalyticsEvent;
import com.roni.library.contracts.analytics.PerformanceMetric;
import com.roni.library.service.BaseService;
import com.roni.library.service.ServiceRegistry;

import java.io.File;
import java.util.Map;

/**
 * Layer 1/2 microkernel service implementation for NativeAnalyticsEngine.
 * Integrates into {@link ServiceRegistry} and coordinates composite providers,
 * offline persistent disk queues, session management, and performance tracking.
 *
 * @author infoadmstudio2020-cyber
 */
public class NativeAnalyticsService extends BaseService {

    public static final String SERVICE_ID = "NativeAnalyticsService";

    private final CompositeAnalyticsProvider compositeProvider;
    private final MemoryAnalyticsProvider defaultMemoryProvider;
    private final DiskBufferAnalyticsProvider diskBufferProvider;
    private final EventQueue eventQueue;
    private final PerformanceTracker performanceTracker;
    private final SessionTracker sessionTracker;
    private final BatchUploader batchUploader;
    private final TelemetryDispatcher telemetryDispatcher;

    public NativeAnalyticsService() {
        this(null, null);
    }

    public NativeAnalyticsService(@Nullable Context context, @Nullable NativeAnalyticsEngineConfig config) {
        super(SERVICE_ID, 50);

        File storageDir = null;
        if (config != null && config.getStorageDirectory() != null) {
            storageDir = config.getStorageDirectory();
        } else if (context != null) {
            storageDir = new File(context.getFilesDir(), "native_analytics");
        }

        this.eventQueue = new EventQueue(storageDir);
        this.diskBufferProvider = new DiskBufferAnalyticsProvider(eventQueue);
        this.defaultMemoryProvider = new MemoryAnalyticsProvider();

        this.compositeProvider = new CompositeAnalyticsProvider();
        this.compositeProvider.addProvider(defaultMemoryProvider);

        if (config == null || config.isOfflinePersistenceEnabled()) {
            this.compositeProvider.addProvider(diskBufferProvider);
        }

        this.performanceTracker = new PerformanceTracker(compositeProvider);
        this.sessionTracker = new SessionTracker(compositeProvider);
        this.batchUploader = new BatchUploader(eventQueue);
        if (config != null) {
            this.batchUploader.setBatchSize(config.getBatchSize());
            this.batchUploader.setFlushIntervalMs(config.getFlushIntervalMs());
        }
        this.telemetryDispatcher = new TelemetryDispatcher(compositeProvider);
    }

    @Override
    protected void onInitialize() {
        compositeProvider.initialize();
    }

    @Override
    protected void onServiceStart() {
        batchUploader.start();
        sessionTracker.onAppForeground();
    }

    @Override
    protected void onServiceStop() {
        sessionTracker.onAppBackground();
        batchUploader.stop();
        flush();
    }

    @Override
    protected void onDispose() {
        telemetryDispatcher.shutdown();
        batchUploader.stop();
        compositeProvider.clear();
        performanceTracker.clear();
    }

    /**
     * Record a named analytics event with custom parameters.
     *
     * @param name event name
     * @param parameters key-value parameters
     */
    public void trackEvent(@NonNull String name, @Nullable Map<String, Object> parameters) {
        AnalyticsEvent event = new AnalyticsEvent(name, parameters, System.currentTimeMillis());
        trackEvent(event);
    }

    /**
     * Record an analytics event.
     *
     * @param event the event to record
     */
    public void trackEvent(@NonNull AnalyticsEvent event) {
        if (event == null) return;
        telemetryDispatcher.dispatchEvent(event);
    }

    /**
     * Record a performance metric.
     *
     * @param metric the metric to record
     */
    public void recordMetric(@NonNull PerformanceMetric metric) {
        if (metric == null) return;
        telemetryDispatcher.dispatchMetric(metric);
    }

    /**
     * Record a metric by name, value, and unit.
     *
     * @param name metric name
     * @param value numerical value
     * @param unit unit of measurement (e.g. "ms", "count")
     */
    public void recordMetric(@NonNull String name, double value, @NonNull String unit) {
        PerformanceMetric metric = new PerformanceMetric(name, value, unit, System.currentTimeMillis());
        recordMetric(metric);
    }

    /**
     * Force immediate flush of all providers and disk buffers.
     */
    public void flush() {
        compositeProvider.flush();
        batchUploader.flushSync();
    }

    @NonNull
    public CompositeAnalyticsProvider getCompositeProvider() {
        return compositeProvider;
    }

    @NonNull
    public MemoryAnalyticsProvider getDefaultMemoryProvider() {
        return defaultMemoryProvider;
    }

    @NonNull
    public DiskBufferAnalyticsProvider getDiskBufferProvider() {
        return diskBufferProvider;
    }

    @NonNull
    public EventQueue getEventQueue() {
        return eventQueue;
    }

    @NonNull
    public PerformanceTracker getPerformanceTracker() {
        return performanceTracker;
    }

    @NonNull
    public SessionTracker getSessionTracker() {
        return sessionTracker;
    }

    @NonNull
    public BatchUploader getBatchUploader() {
        return batchUploader;
    }

    /**
     * Register this service in the ecosystem {@link ServiceRegistry}.
     *
     * @param registry service registry
     */
    public void registerInRegistry(@NonNull ServiceRegistry registry) {
        if (registry != null) {
            registry.registerService(com.roni.library.contracts.service.IService.class, this);
        }
    }
}
