package com.roni.library.analytics.service;

import android.content.Context;

import com.roni.library.analytics.provider.CompositeAnalyticsProvider;
import com.roni.library.analytics.provider.DiskBufferAnalyticsProvider;
import com.roni.library.analytics.provider.MemoryAnalyticsProvider;
import com.roni.library.analytics.tracker.PerformanceTracker;
import com.roni.library.analytics.tracker.SessionTracker;
import com.roni.library.contracts.analytics.AnalyticsEvent;
import com.roni.library.contracts.analytics.PerformanceMetric;
import com.roni.library.service.BaseService;
import com.roni.library.service.ServiceRegistry;

import java.util.Map;

/**
 * Core Layer 2 NativeAnalyticsService providing session metrics, performance traces,
 * and composite provider dispatching.
 */
public class NativeAnalyticsService extends BaseService implements com.roni.library.contracts.service.IService {
    private final CompositeAnalyticsProvider compositeProvider;
    private final MemoryAnalyticsProvider defaultMemoryProvider;
    private final DiskBufferAnalyticsProvider diskBufferProvider;
    private final PerformanceTracker performanceTracker;
    private final SessionTracker sessionTracker;

    public NativeAnalyticsService() {
        this(null);
    }

    public NativeAnalyticsService(Context context) {
        super("NativeAnalyticsService", 200);
        this.compositeProvider = new CompositeAnalyticsProvider();
        this.defaultMemoryProvider = new MemoryAnalyticsProvider();
        this.defaultMemoryProvider.initialize();
        this.compositeProvider.addProvider(defaultMemoryProvider);

        this.diskBufferProvider = new DiskBufferAnalyticsProvider(context);
        this.compositeProvider.addProvider(diskBufferProvider);

        this.performanceTracker = new PerformanceTracker();
        this.sessionTracker = new SessionTracker();

        // Forward performance tracker metrics to providers
        this.performanceTracker.addMetricListener(this::recordMetric);
    }

    @Override
    protected void onInitialize() throws Exception {
        compositeProvider.initialize();
    }

    @Override
    protected void onServiceStart() throws Exception {
        sessionTracker.startSession();
    }

    @Override
    protected void onServiceStop() throws Exception {
        sessionTracker.endSession();
        flush();
    }

    @Override
    protected void onDispose() throws Exception {
        flush();
    }

    public void trackEvent(String name, Map<String, Object> parameters) {
        if (name == null || name.trim().isEmpty()) return;
        AnalyticsEvent event = new AnalyticsEvent(name, parameters, System.currentTimeMillis());
        trackEvent(event);
    }

    public void trackEvent(AnalyticsEvent event) {
        if (event == null) return;
        compositeProvider.trackEvent(event);
    }

    public void recordMetric(PerformanceMetric metric) {
        if (metric == null) return;
        compositeProvider.recordMetric(metric);
    }

    public void recordMetric(String name, double value, String unit) {
        if (name == null || name.trim().isEmpty()) return;
        PerformanceMetric metric = new PerformanceMetric(name, value, unit != null ? unit : "");
        recordMetric(metric);
    }

    public void flush() {
        compositeProvider.flush();
    }

    public CompositeAnalyticsProvider getCompositeProvider() {
        return compositeProvider;
    }

    public MemoryAnalyticsProvider getDefaultMemoryProvider() {
        return defaultMemoryProvider;
    }

    public DiskBufferAnalyticsProvider getDiskBufferProvider() {
        return diskBufferProvider;
    }

    public PerformanceTracker getPerformanceTracker() {
        return performanceTracker;
    }

    public SessionTracker getSessionTracker() {
        return sessionTracker;
    }

    public void registerInRegistry(ServiceRegistry registry) {
        if (registry != null) {
            registry.registerService(com.roni.library.contracts.service.IService.class, this);
        }
    }
}
