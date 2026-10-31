package com.roni.library.analytics.provider;

import com.roni.library.contracts.analytics.AnalyticsEvent;
import com.roni.library.contracts.analytics.IAnalyticsProvider;
import com.roni.library.contracts.analytics.PerformanceMetric;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * In-memory analytics provider collecting events and metrics for immediate retrieval or debugging.
 */
public class MemoryAnalyticsProvider implements IAnalyticsProvider {
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final List<AnalyticsEvent> capturedEvents = new CopyOnWriteArrayList<>();
    private final List<PerformanceMetric> capturedMetrics = new CopyOnWriteArrayList<>();
    private final Map<String, String> userProperties = new ConcurrentHashMap<>();
    private String userId;

    public void initialize() {
        initialized.set(true);
    }

    public boolean isInitialized() {
        return initialized.get();
    }

    @Override
    public void trackEvent(AnalyticsEvent event) {
        if (event != null) {
            capturedEvents.add(event);
        }
    }

    @Override
    public void recordMetric(PerformanceMetric metric) {
        if (metric != null) {
            capturedMetrics.add(metric);
        }
    }

    @Override
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public void setUserProperty(String key, String value) {
        if (key != null) {
            if (value != null) {
                userProperties.put(key, value);
            } else {
                userProperties.remove(key);
            }
        }
    }

    public String getUserId() {
        return userId;
    }

    public Map<String, String> getUserProperties() {
        return Collections.unmodifiableMap(userProperties);
    }

    public List<AnalyticsEvent> getCapturedEvents() {
        return Collections.unmodifiableList(new ArrayList<>(capturedEvents));
    }

    public List<PerformanceMetric> getCapturedMetrics() {
        return Collections.unmodifiableList(new ArrayList<>(capturedMetrics));
    }

    public void flush() {
        // In-memory flush
    }

    public void clear() {
        capturedEvents.clear();
        capturedMetrics.clear();
        userProperties.clear();
        userId = null;
    }
}
