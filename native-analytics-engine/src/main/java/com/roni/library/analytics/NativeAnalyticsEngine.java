package com.roni.library.analytics;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.roni.library.analytics.handler.AnalyticsCommandHandler;
import com.roni.library.analytics.queue.EventQueue;
import com.roni.library.analytics.service.NativeAnalyticsService;
import com.roni.library.analytics.tracker.PerformanceTracker;
import com.roni.library.analytics.tracker.SessionTracker;
import com.roni.library.contracts.analytics.AnalyticsEvent;
import com.roni.library.contracts.analytics.PerformanceMetric;
import com.roni.library.contracts.common.RuntimeState;
import com.roni.library.service.NativeServiceSDK;
import com.roni.library.service.ServiceRegistry;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Primary entry point and facade for NativeAnalyticsEngine.
 * Implements thread-safe singleton state machine, lifecycle hooks,
 * event routing, and ecosystem service registration.
 *
 * @author infoadmstudio2020-cyber
 */
public class NativeAnalyticsEngine {

    private static volatile NativeAnalyticsEngine sInstance;
    private static final Object INSTANCE_LOCK = new Object();

    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private Context context;
    private NativeAnalyticsEngineConfig config;
    private NativeAnalyticsService analyticsService;
    private AnalyticsCommandHandler commandHandler;
    private RuntimeState runtimeState = RuntimeState.UNINITIALIZED;

    private NativeAnalyticsEngine() {
    }

    /**
     * Get the global singleton instance of NativeAnalyticsEngine.
     *
     * @return singleton instance
     */
    @NonNull
    public static NativeAnalyticsEngine getInstance() {
        if (sInstance == null) {
            synchronized (INSTANCE_LOCK) {
                if (sInstance == null) {
                    sInstance = new NativeAnalyticsEngine();
                }
            }
        }
        return sInstance;
    }

    /**
     * Initialize NativeAnalyticsEngine with context and custom configuration.
     *
     * @param context application context
     * @param config configuration settings (or null for default)
     * @return standardized initialization result
     */
    @NonNull
    public synchronized NativeAnalyticsEngineResult<Void> init(@Nullable Context context, @Nullable NativeAnalyticsEngineConfig config) {
        if (initialized.get()) {
            return NativeAnalyticsEngineResult.success(null);
        }

        try {
            this.context = context != null ? context.getApplicationContext() : null;
            this.config = config != null ? config : new NativeAnalyticsEngineConfig.Builder().build();

            this.analyticsService = new NativeAnalyticsService(this.context, this.config);
            this.analyticsService.initialize();
            this.analyticsService.start();

            this.commandHandler = new AnalyticsCommandHandler(analyticsService);

            // Register with ecosystem ServiceRegistry if available
            try {
                ServiceRegistry registry = ServiceRegistry.getInstance();
                if (registry != null) {
                    analyticsService.registerInRegistry(registry);
                }
            } catch (Throwable ignored) {
            }

            initialized.set(true);
            runtimeState = RuntimeState.READY;
            return NativeAnalyticsEngineResult.success(null);
        } catch (Throwable t) {
            runtimeState = RuntimeState.ERROR;
            return NativeAnalyticsEngineResult.error(500, "Initialization failed: " + t.getMessage());
        }
    }

    /**
     * Check if NativeAnalyticsEngine has been initialized.
     *
     * @return true if initialized
     */
    public boolean isInitialized() {
        return initialized.get();
    }

    /**
     * Get active configuration.
     *
     * @return config or null
     */
    @Nullable
    public NativeAnalyticsEngineConfig getConfig() {
        return config;
    }

    /**
     * Get current runtime state.
     *
     * @return runtime state enum
     */
    @NonNull
    public RuntimeState getRuntimeState() {
        return runtimeState;
    }

    /**
     * Track a named event with custom parameters.
     *
     * @param name event name
     * @param parameters key-value attributes
     */
    public void trackEvent(@NonNull String name, @Nullable Map<String, Object> parameters) {
        checkInitialized();
        analyticsService.trackEvent(name, parameters);
    }

    /**
     * Track a pre-constructed {@link AnalyticsEvent}.
     *
     * @param event analytics event
     */
    public void trackEvent(@NonNull AnalyticsEvent event) {
        checkInitialized();
        analyticsService.trackEvent(event);
    }

    /**
     * Record a high-resolution performance metric.
     *
     * @param metric metric object
     */
    public void recordMetric(@NonNull PerformanceMetric metric) {
        checkInitialized();
        analyticsService.recordMetric(metric);
    }

    /**
     * Record a metric by name, value, and unit.
     *
     * @param name metric name
     * @param value numerical measurement
     * @param unit measurement unit
     */
    public void recordMetric(@NonNull String name, double value, @NonNull String unit) {
        checkInitialized();
        analyticsService.recordMetric(name, value, unit);
    }

    /**
     * Flush in-memory queues and offline storage immediately.
     */
    public void flush() {
        if (initialized.get() && analyticsService != null) {
            analyticsService.flush();
        }
    }

    /**
     * Get the active {@link SessionTracker}.
     *
     * @return session tracker
     */
    @NonNull
    public SessionTracker getSessionTracker() {
        checkInitialized();
        return analyticsService.getSessionTracker();
    }

    /**
     * Get the active {@link PerformanceTracker}.
     *
     * @return performance tracker
     */
    @NonNull
    public PerformanceTracker getPerformanceTracker() {
        checkInitialized();
        return analyticsService.getPerformanceTracker();
    }

    /**
     * Get the underlying {@link NativeAnalyticsService}.
     *
     * @return analytics service
     */
    @NonNull
    public NativeAnalyticsService getService() {
        checkInitialized();
        return analyticsService;
    }

    /**
     * Get the RPC command handler.
     *
     * @return command handler
     */
    @NonNull
    public AnalyticsCommandHandler getCommandHandler() {
        checkInitialized();
        return commandHandler;
    }

    /**
     * Get the zero-loss offline {@link EventQueue}.
     *
     * @return event queue
     */
    @NonNull
    public EventQueue getEventQueue() {
        checkInitialized();
        return analyticsService.getEventQueue();
    }

    /**
     * Release all resources, timers, and stop services.
     * After release, a subsequent call to {@link #getInstance()} returns a clean instance.
     */
    public synchronized void release() {
        if (analyticsService != null) {
            try {
                analyticsService.stop();
                analyticsService.dispose();
            } catch (Throwable ignored) {
            }
            analyticsService = null;
        }
        commandHandler = null;
        config = null;
        context = null;
        initialized.set(false);
        runtimeState = RuntimeState.DISPOSED;

        synchronized (INSTANCE_LOCK) {
            sInstance = null;
        }
    }

    private void checkInitialized() {
        if (!initialized.get() || analyticsService == null) {
            throw new IllegalStateException("NativeAnalyticsEngine is not initialized. Call init() first.");
        }
    }
}
