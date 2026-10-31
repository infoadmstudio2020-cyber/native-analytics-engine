package com.roni.library.analytics.provider;

import com.roni.library.contracts.analytics.AnalyticsEvent;
import com.roni.library.contracts.analytics.IAnalyticsProvider;
import com.roni.library.contracts.analytics.PerformanceMetric;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Composite analytics provider forwarding events and metrics to multiple sub-providers.
 */
public class CompositeAnalyticsProvider implements IAnalyticsProvider {
    private final List<IAnalyticsProvider> providers = new CopyOnWriteArrayList<>();

    public void addProvider(IAnalyticsProvider provider) {
        if (provider != null && !providers.contains(provider)) {
            providers.add(provider);
        }
    }

    public void removeProvider(IAnalyticsProvider provider) {
        if (provider != null) {
            providers.remove(provider);
        }
    }

    public List<IAnalyticsProvider> getProviders() {
        return Collections.unmodifiableList(new ArrayList<>(providers));
    }

    public int getProviderCount() {
        return providers.size();
    }

    public void initialize() {
        // Initialization hook
    }

    @Override
    public void trackEvent(AnalyticsEvent event) {
        if (event == null) return;
        for (IAnalyticsProvider provider : providers) {
            try {
                provider.trackEvent(event);
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public void recordMetric(PerformanceMetric metric) {
        if (metric == null) return;
        for (IAnalyticsProvider provider : providers) {
            try {
                provider.recordMetric(metric);
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public void setUserId(String userId) {
        for (IAnalyticsProvider provider : providers) {
            try {
                provider.setUserId(userId);
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public void setUserProperty(String key, String value) {
        for (IAnalyticsProvider provider : providers) {
            try {
                provider.setUserProperty(key, value);
            } catch (Throwable ignored) {
            }
        }
    }

    public void flush() {
        for (IAnalyticsProvider provider : providers) {
            if (provider instanceof MemoryAnalyticsProvider) {
                ((MemoryAnalyticsProvider) provider).flush();
            } else if (provider instanceof DiskBufferAnalyticsProvider) {
                ((DiskBufferAnalyticsProvider) provider).flush();
            }
        }
    }
}
