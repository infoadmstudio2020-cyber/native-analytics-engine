package com.roni.library.analytics.tracker;

import com.roni.library.contracts.analytics.PerformanceMetric;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

/**
 * Tracks high-resolution performance metrics, trace durations, bridge latencies,
 * and HTTP response times.
 */
public class PerformanceTracker {
    public interface MetricListener {
        void onMetricRecorded(PerformanceMetric metric);
    }

    private final Map<String, Long> activeTraces = new ConcurrentHashMap<>();
    private final List<MetricListener> metricListeners = new CopyOnWriteArrayList<>();

    public void addMetricListener(MetricListener listener) {
        if (listener != null && !metricListeners.contains(listener)) {
            metricListeners.add(listener);
        }
    }

    public void removeMetricListener(MetricListener listener) {
        if (listener != null) {
            metricListeners.remove(listener);
        }
    }

    public void startTrace(String traceName) {
        if (traceName != null && !traceName.trim().isEmpty()) {
            activeTraces.put(traceName, System.currentTimeMillis());
        }
    }

    public long stopTrace(String traceName) {
        if (traceName == null) return -1;
        Long start = activeTraces.remove(traceName);
        if (start != null) {
            long durationMs = Math.max(0, System.currentTimeMillis() - start);
            notifyMetric(new PerformanceMetric("trace_" + traceName, durationMs, "ms"));
            return durationMs;
        }
        return -1;
    }

    public boolean hasActiveTrace(String traceName) {
        return traceName != null && activeTraces.containsKey(traceName);
    }

    public void recordBridgeLatency(String action, long durationMs) {
        String name = "bridge_latency_" + (action != null ? action : "unknown");
        notifyMetric(new PerformanceMetric(name, Math.max(0, durationMs), "ms"));
    }

    public void recordWebFirstPaint(long durationMs) {
        notifyMetric(new PerformanceMetric("web_first_paint", Math.max(0, durationMs), "ms"));
    }

    public void recordHttpResponseTime(String endpoint, int statusCode, long durationMs) {
        String name = "http_response_time_" + (endpoint != null ? endpoint.replaceAll("[^a-zA-Z0-9_]", "_") : "unknown");
        PerformanceMetric metric = new PerformanceMetric(name + "_" + statusCode, Math.max(0, durationMs), "ms");
        notifyMetric(metric);
    }

    private void notifyMetric(PerformanceMetric metric) {
        for (MetricListener listener : metricListeners) {
            try {
                listener.onMetricRecorded(metric);
            } catch (Throwable ignored) {
            }
        }
    }
}
