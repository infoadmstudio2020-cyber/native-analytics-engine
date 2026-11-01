package com.roni.library.analytics.tracker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.roni.library.analytics.provider.IAnalyticsProvider;
import com.roni.library.contracts.analytics.PerformanceMetric;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * High-resolution performance tracker for NativeRuntime ecosystem.
 * Measures bridge execution latency, web view first paint, HTTP response times,
 * and custom operation traces.
 *
 * @author infoadmstudio2020-cyber
 */
public class PerformanceTracker {

    private final Map<String, Long> activeTraces = new ConcurrentHashMap<>();
    private final List<PerformanceMetric> recordedMetrics = new CopyOnWriteArrayList<>();
    private final IAnalyticsProvider provider;

    public PerformanceTracker() {
        this(null);
    }

    public PerformanceTracker(@Nullable IAnalyticsProvider provider) {
        this.provider = provider;
    }

    /**
     * Begin measuring a performance trace.
     *
     * @param traceName unique name of trace
     */
    public void startTrace(@NonNull String traceName) {
        if (traceName == null || traceName.trim().isEmpty()) return;
        activeTraces.put(traceName, System.currentTimeMillis());
    }

    /**
     * Stop measuring a trace and return elapsed milliseconds.
     * Automatically emits a {@link PerformanceMetric}.
     *
     * @param traceName name of active trace
     * @return duration in milliseconds, or -1 if trace not found
     */
    public long stopTrace(@NonNull String traceName) {
        if (traceName == null) return -1;
        Long startTime = activeTraces.remove(traceName);
        if (startTime == null) return -1;

        long duration = Math.max(0, System.currentTimeMillis() - startTime);
        PerformanceMetric metric = new PerformanceMetric(traceName + "_duration", duration, "ms");
        recordedMetrics.add(metric);

        if (provider != null) {
            provider.recordMetric(metric);
        }
        return duration;
    }

    /**
     * Record WebBridge RPC invocation latency.
     *
     * @param action RPC action name
     * @param durationMs duration in milliseconds
     */
    public void recordBridgeLatency(@NonNull String action, long durationMs) {
        String metricName = "bridge_latency_" + (action != null ? action : "unknown");
        PerformanceMetric metric = new PerformanceMetric(metricName, durationMs, "ms");
        recordedMetrics.add(metric);
        if (provider != null) {
            provider.recordMetric(metric);
        }
    }

    /**
     * Record web content first paint or first meaningful paint duration.
     *
     * @param durationMs duration in milliseconds
     */
    public void recordWebFirstPaint(long durationMs) {
        PerformanceMetric metric = new PerformanceMetric("web_first_paint", durationMs, "ms");
        recordedMetrics.add(metric);
        if (provider != null) {
            provider.recordMetric(metric);
        }
    }

    /**
     * Record HTTP network request response time and status code.
     *
     * @param endpoint URL endpoint or target host
     * @param statusCode HTTP status code
     * @param durationMs duration in milliseconds
     */
    public void recordHttpResponseTime(@NonNull String endpoint, int statusCode, long durationMs) {
        String sanitizedEndpoint = endpoint != null ? endpoint.replaceAll("[^a-zA-Z0-9_]", "_") : "endpoint";
        String metricName = "http_response_" + statusCode + "_" + sanitizedEndpoint;
        PerformanceMetric metric = new PerformanceMetric(metricName, durationMs, "ms");
        recordedMetrics.add(metric);
        if (provider != null) {
            provider.recordMetric(metric);
        }
    }

    /**
     * Check if a trace is currently active.
     *
     * @param traceName trace identifier
     * @return true if currently active
     */
    public boolean hasActiveTrace(@NonNull String traceName) {
        return traceName != null && activeTraces.containsKey(traceName);
    }

    /**
     * Get snapshot of active traces and start timestamps.
     *
     * @return map of active traces
     */
    @NonNull
    public Map<String, Long> getActiveTraces() {
        return new HashMap<>(activeTraces);
    }

    /**
     * Get all recorded performance metrics.
     *
     * @return list of metrics
     */
    @NonNull
    public List<PerformanceMetric> getRecordedMetrics() {
        return new ArrayList<>(recordedMetrics);
    }

    /**
     * Clear all active traces and recorded metrics.
     */
    public void clear() {
        activeTraces.clear();
        recordedMetrics.clear();
    }
}
