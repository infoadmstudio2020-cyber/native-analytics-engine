package com.roni.library.analytics.queue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.roni.library.analytics.provider.IAnalyticsProvider;
import com.roni.library.contracts.analytics.AnalyticsEvent;
import com.roni.library.contracts.analytics.PerformanceMetric;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Disk buffer analytics provider that wraps an {@link EventQueue}.
 * Implements {@link IAnalyticsProvider} to integrate transparently into
 * composite analytics dispatch pipelines.
 *
 * @author infoadmstudio2020-cyber
 */
public class DiskBufferAnalyticsProvider implements IAnalyticsProvider {

    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final EventQueue eventQueue;

    public DiskBufferAnalyticsProvider(@Nullable File storageDirectory) {
        this.eventQueue = new EventQueue(storageDirectory);
        initialize();
    }

    public DiskBufferAnalyticsProvider(@NonNull EventQueue eventQueue) {
        this.eventQueue = eventQueue != null ? eventQueue : new EventQueue(null);
        initialize();
    }

    @Override
    public void initialize() {
        initialized.set(true);
    }

    @Override
    public boolean isInitialized() {
        return initialized.get();
    }

    @Override
    public void trackEvent(@NonNull AnalyticsEvent event) {
        if (!isInitialized() || event == null) return;
        eventQueue.enqueue(event);
    }

    @Override
    public void recordMetric(@NonNull PerformanceMetric metric) {
        // Performance metrics can also be spooled or preserved as events
        if (!isInitialized() || metric == null) return;
        AnalyticsEvent metricEvent = new AnalyticsEvent("perf_metric_" + metric.getMetricName(), null, metric.getTimestamp());
        eventQueue.enqueue(metricEvent);
    }

    @Override
    public void setUserId(@Nullable String userId) {
        // No-op for raw event disk queue
    }

    @Override
    public void setUserProperty(@NonNull String name, @Nullable String value) {
        // No-op for raw event disk queue
    }

    @Override
    public void flush() {
        eventQueue.flush();
    }

    @Override
    public void clear() {
        eventQueue.clear();
    }

    @NonNull
    public EventQueue getEventQueue() {
        return eventQueue;
    }
}
