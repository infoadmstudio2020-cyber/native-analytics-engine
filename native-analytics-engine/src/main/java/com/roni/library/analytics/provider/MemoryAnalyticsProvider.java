package com.roni.library.analytics.provider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.roni.library.contracts.analytics.AnalyticsEvent;
import com.roni.library.contracts.analytics.PerformanceMetric;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * In-memory analytics provider. Captures events and metrics in thread-safe memory buffers,
 * allowing instant query, validation, and debugging.
 *
 * @author infoadmstudio2020-cyber
 */
public class MemoryAnalyticsProvider implements IAnalyticsProvider {

    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final List<AnalyticsEvent> capturedEvents = new CopyOnWriteArrayList<>();
    private final List<PerformanceMetric> capturedMetrics = new CopyOnWriteArrayList<>();
    private final Map<String, String> userProperties = new ConcurrentHashMap<>();
    private volatile String userId;

    public MemoryAnalyticsProvider() {
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
        capturedEvents.add(event);
    }

    @Override
    public void recordMetric(@NonNull PerformanceMetric metric) {
        if (!isInitialized() || metric == null) return;
        capturedMetrics.add(metric);
    }

    @Override
    public void setUserId(@Nullable String userId) {
        this.userId = userId;
    }

    @Nullable
    public String getUserId() {
        return userId;
    }

    @Override
    public void setUserProperty(@NonNull String name, @Nullable String value) {
        if (name == null) return;
        if (value != null) {
            userProperties.put(name, value);
        } else {
            userProperties.remove(name);
        }
    }

    @NonNull
    public Map<String, String> getUserProperties() {
        return Collections.unmodifiableMap(userProperties);
    }

    @Override
    public void flush() {
        // In-memory provider holds items until clear() is invoked
    }

    @NonNull
    public List<AnalyticsEvent> getCapturedEvents() {
        return new ArrayList<>(capturedEvents);
    }

    @NonNull
    public List<PerformanceMetric> getCapturedMetrics() {
        return new ArrayList<>(capturedMetrics);
    }

    public int getEventCount() {
        return capturedEvents.size();
    }

    public int getMetricCount() {
        return capturedMetrics.size();
    }

    @Override
    public void clear() {
        capturedEvents.clear();
        capturedMetrics.clear();
        userProperties.clear();
        userId = null;
    }
}
