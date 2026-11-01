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

import java.util.Map;

/**
 * Convenient static entry point for NativeAnalyticsEngine.
 * Mirrors {@link NativeAnalyticsEngine} to support both facade invocation styles.
 *
 * @author infoadmstudio2020-cyber
 */
public final class AnalyticsEngine {

    private AnalyticsEngine() {
    }

    @NonNull
    public static NativeAnalyticsEngine getInstance() {
        return NativeAnalyticsEngine.getInstance();
    }

    @NonNull
    public static NativeAnalyticsEngineResult<Void> init(@Nullable Context context, @Nullable NativeAnalyticsEngineConfig config) {
        return NativeAnalyticsEngine.getInstance().init(context, config);
    }

    public static boolean isInitialized() {
        return NativeAnalyticsEngine.getInstance().isInitialized();
    }

    public static void trackEvent(@NonNull String name, @Nullable Map<String, Object> parameters) {
        NativeAnalyticsEngine.getInstance().trackEvent(name, parameters);
    }

    public static void trackEvent(@NonNull AnalyticsEvent event) {
        NativeAnalyticsEngine.getInstance().trackEvent(event);
    }

    public static void recordMetric(@NonNull PerformanceMetric metric) {
        NativeAnalyticsEngine.getInstance().recordMetric(metric);
    }

    public static void recordMetric(@NonNull String name, double value, @NonNull String unit) {
        NativeAnalyticsEngine.getInstance().recordMetric(name, value, unit);
    }

    public static void flush() {
        NativeAnalyticsEngine.getInstance().flush();
    }

    @NonNull
    public static SessionTracker getSessionTracker() {
        return NativeAnalyticsEngine.getInstance().getSessionTracker();
    }

    @NonNull
    public static PerformanceTracker getPerformanceTracker() {
        return NativeAnalyticsEngine.getInstance().getPerformanceTracker();
    }

    @NonNull
    public static NativeAnalyticsService getService() {
        return NativeAnalyticsEngine.getInstance().getService();
    }

    @NonNull
    public static AnalyticsCommandHandler getCommandHandler() {
        return NativeAnalyticsEngine.getInstance().getCommandHandler();
    }

    @NonNull
    public static EventQueue getEventQueue() {
        return NativeAnalyticsEngine.getInstance().getEventQueue();
    }

    public static void release() {
        NativeAnalyticsEngine.getInstance().release();
    }
}
