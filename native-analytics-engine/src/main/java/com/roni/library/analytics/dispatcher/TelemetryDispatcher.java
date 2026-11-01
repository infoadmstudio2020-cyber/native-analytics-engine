package com.roni.library.analytics.dispatcher;

import androidx.annotation.NonNull;

import com.roni.library.analytics.provider.IAnalyticsProvider;
import com.roni.library.contracts.analytics.AnalyticsEvent;
import com.roni.library.contracts.analytics.PerformanceMetric;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * High-throughput asynchronous dispatcher for telemetry events and performance metrics.
 * Offloads dispatching from the main UI thread to dedicated background workers.
 *
 * @author infoadmstudio2020-cyber
 */
public class TelemetryDispatcher {

    private final IAnalyticsProvider targetProvider;
    private final ExecutorService executor;

    public TelemetryDispatcher(@NonNull IAnalyticsProvider targetProvider) {
        this.targetProvider = targetProvider;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "NativeAnalytics-TelemetryDispatcher");
            t.setDaemon(true);
            return t;
        });
    }

    public void dispatchEvent(@NonNull AnalyticsEvent event) {
        if (event == null) return;
        executor.submit(() -> {
            try {
                targetProvider.trackEvent(event);
            } catch (Throwable ignored) {
            }
        });
    }

    public void dispatchMetric(@NonNull PerformanceMetric metric) {
        if (metric == null) return;
        executor.submit(() -> {
            try {
                targetProvider.recordMetric(metric);
            } catch (Throwable ignored) {
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
}
