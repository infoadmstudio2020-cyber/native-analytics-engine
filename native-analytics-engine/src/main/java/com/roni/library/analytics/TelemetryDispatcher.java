package com.roni.library.analytics;

import com.roni.library.contracts.analytics.AnalyticsEvent;
import com.roni.library.contracts.analytics.PerformanceMetric;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dispatcher to route telemetry events and performance metrics to destinations.
 */
public class TelemetryDispatcher {
    public interface TelemetryListener {
        void onEventDispatched(AnalyticsEvent event);
        void onMetricDispatched(PerformanceMetric metric);
    }

    private final List<TelemetryListener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(TelemetryListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(TelemetryListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public void dispatchEvent(AnalyticsEvent event) {
        if (event == null) return;
        for (TelemetryListener listener : listeners) {
            try {
                listener.onEventDispatched(event);
            } catch (Throwable ignored) {
            }
        }
    }

    public void dispatchMetric(PerformanceMetric metric) {
        if (metric == null) return;
        for (TelemetryListener listener : listeners) {
            try {
                listener.onMetricDispatched(metric);
            } catch (Throwable ignored) {
            }
        }
    }

    public int getListenerCount() {
        return listeners.size();
    }

    public void clearListeners() {
        listeners.clear();
    }
}
