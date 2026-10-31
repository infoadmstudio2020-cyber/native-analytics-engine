package com.roni.library.analytics;

import android.content.Context;

import com.roni.library.analytics.handler.AnalyticsCommandHandler;
import com.roni.library.analytics.service.NativeAnalyticsService;
import com.roni.library.analytics.tracker.PerformanceTracker;
import com.roni.library.analytics.tracker.SessionTracker;
import com.roni.library.contracts.analytics.AnalyticsEvent;
import com.roni.library.contracts.analytics.PerformanceMetric;

import java.util.Map;

/**
 * Public alias and facade entry point for AnalyticsEngine matching strict NativeRuntime specifications.
 */
public final class AnalyticsEngine {
    private AnalyticsEngine() {}

    public static NativeAnalyticsEngine getInstance() {
        return NativeAnalyticsEngine.getInstance();
    }

    public static NativeAnalyticsEngineResult<Boolean> init(Context context, NativeAnalyticsEngineConfig config) {
        return NativeAnalyticsEngine.getInstance().init(context, config);
    }

    public static boolean isInitialized() {
        return NativeAnalyticsEngine.getInstance().isInitialized();
    }

    public static NativeAnalyticsEngineConfig getConfig() {
        return NativeAnalyticsEngine.getInstance().getConfig();
    }

    public static NativeAnalyticsService getService() {
        return NativeAnalyticsEngine.getInstance().getService();
    }

    public static AnalyticsCommandHandler getCommandHandler() {
        return NativeAnalyticsEngine.getInstance().getCommandHandler();
    }

    public static SessionTracker getSessionTracker() {
        return NativeAnalyticsEngine.getInstance().getSessionTracker();
    }

    public static PerformanceTracker getPerformanceTracker() {
        return NativeAnalyticsEngine.getInstance().getPerformanceTracker();
    }

    public static void trackEvent(String name, Map<String, Object> params) {
        NativeAnalyticsEngine.getInstance().trackEvent(name, params);
    }

    public static void trackEvent(AnalyticsEvent event) {
        NativeAnalyticsEngine.getInstance().trackEvent(event);
    }

    public static void recordMetric(String name, double value, String unit) {
        NativeAnalyticsEngine.getInstance().recordMetric(name, value, unit);
    }

    public static void recordMetric(PerformanceMetric metric) {
        NativeAnalyticsEngine.getInstance().recordMetric(metric);
    }

    public static void flush() {
        NativeAnalyticsEngine.getInstance().flush();
    }

    public static void release() {
        NativeAnalyticsEngine.getInstance().release();
    }
}
