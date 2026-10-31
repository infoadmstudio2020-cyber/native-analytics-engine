package com.roni.library.analytics;

import android.content.Context;

import com.roni.library.analytics.handler.AnalyticsCommandHandler;
import com.roni.library.analytics.service.NativeAnalyticsService;
import com.roni.library.analytics.tracker.PerformanceTracker;
import com.roni.library.analytics.tracker.SessionTracker;
import com.roni.library.contracts.analytics.AnalyticsEvent;
import com.roni.library.contracts.analytics.PerformanceMetric;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main singleton entry point for NativeAnalyticsEngine.
 */
public class NativeAnalyticsEngine {
    private static volatile NativeAnalyticsEngine instance;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private NativeAnalyticsEngineConfig config;
    private NativeAnalyticsService analyticsService;
    private AnalyticsCommandHandler commandHandler;
    private EventQueue eventQueue;
    private BatchUploader batchUploader;
    private TelemetryDispatcher telemetryDispatcher;
    private Context context;

    private NativeAnalyticsEngine() {}

    public static NativeAnalyticsEngine getInstance() {
        if (instance == null) {
            synchronized (NativeAnalyticsEngine.class) {
                if (instance == null) {
                    instance = new NativeAnalyticsEngine();
                }
            }
        }
        return instance;
    }

    public synchronized NativeAnalyticsEngineResult<Boolean> init(Context context, NativeAnalyticsEngineConfig config) {
        if (initialized.get()) {
            return NativeAnalyticsEngineResult.success(true);
        }
        try {
            this.context = context != null ? context.getApplicationContext() : null;
            this.config = config != null ? config : NativeAnalyticsEngineConfig.builder().build();

            this.eventQueue = new EventQueue(this.config.getMaxQueueSize());
            this.telemetryDispatcher = new TelemetryDispatcher();
            this.analyticsService = new NativeAnalyticsService(this.context);
            this.analyticsService.initialize();
            this.analyticsService.start();

            this.commandHandler = new AnalyticsCommandHandler(this.analyticsService);

            this.batchUploader = new BatchUploader(
                    this.eventQueue,
                    this.config.getBatchIntervalMs(),
                    this.config.getBatchSizeThreshold(),
                    batch -> {
                        for (AnalyticsEvent event : batch) {
                            this.analyticsService.trackEvent(event);
                            this.telemetryDispatcher.dispatchEvent(event);
                        }
                    }
            );
            this.batchUploader.start();

            this.initialized.set(true);
            return NativeAnalyticsEngineResult.success(true);
        } catch (Throwable t) {
            return NativeAnalyticsEngineResult.failure(500, "Initialization failed: " + t.getMessage());
        }
    }

    public boolean isInitialized() {
        return initialized.get();
    }

    public NativeAnalyticsEngineConfig getConfig() {
        return config;
    }

    public NativeAnalyticsService getService() {
        return analyticsService;
    }

    public AnalyticsCommandHandler getCommandHandler() {
        return commandHandler;
    }

    public SessionTracker getSessionTracker() {
        return analyticsService != null ? analyticsService.getSessionTracker() : null;
    }

    public PerformanceTracker getPerformanceTracker() {
        return analyticsService != null ? analyticsService.getPerformanceTracker() : null;
    }

    public EventQueue getEventQueue() {
        return eventQueue;
    }

    public BatchUploader getBatchUploader() {
        return batchUploader;
    }

    public TelemetryDispatcher getTelemetryDispatcher() {
        return telemetryDispatcher;
    }

    public void trackEvent(String name, Map<String, Object> params) {
        if (!initialized.get() || analyticsService == null) return;
        AnalyticsEvent event = new AnalyticsEvent(name, params, System.currentTimeMillis());
        trackEvent(event);
    }

    public void trackEvent(AnalyticsEvent event) {
        if (!initialized.get() || event == null) return;
        if (eventQueue != null) {
            eventQueue.enqueue(event);
        }
        if (batchUploader != null) {
            batchUploader.onEventAdded();
        }
        if (analyticsService != null) {
            analyticsService.trackEvent(event);
        }
        if (telemetryDispatcher != null) {
            telemetryDispatcher.dispatchEvent(event);
        }
    }

    public void recordMetric(String name, double value, String unit) {
        if (!initialized.get() || analyticsService == null) return;
        PerformanceMetric metric = new PerformanceMetric(name, value, unit != null ? unit : "");
        recordMetric(metric);
    }

    public void recordMetric(PerformanceMetric metric) {
        if (!initialized.get() || metric == null) return;
        if (analyticsService != null) {
            analyticsService.recordMetric(metric);
        }
        if (telemetryDispatcher != null) {
            telemetryDispatcher.dispatchMetric(metric);
        }
    }

    public void flush() {
        if (batchUploader != null) {
            batchUploader.flush();
        }
        if (analyticsService != null) {
            analyticsService.flush();
        }
    }

    public int getCapturedEventCount() {
        return analyticsService != null ? analyticsService.getDefaultMemoryProvider().getCapturedEvents().size() : 0;
    }

    public synchronized void release() {
        if (batchUploader != null) {
            batchUploader.stop();
            batchUploader = null;
        }
        if (analyticsService != null) {
            analyticsService.stop();
            analyticsService.dispose();
            analyticsService = null;
        }
        if (eventQueue != null) {
            eventQueue.clear();
            eventQueue = null;
        }
        if (telemetryDispatcher != null) {
            telemetryDispatcher.clearListeners();
            telemetryDispatcher = null;
        }
        commandHandler = null;
        config = null;
        context = null;
        initialized.set(false);
        instance = null;
    }
}
