package com.roni.library.analytics.provider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.roni.library.contracts.analytics.AnalyticsEvent;
import com.roni.library.contracts.analytics.PerformanceMetric;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Composite analytics provider. Aggregates multiple analytics providers and
 * broadcasts events, metrics, user identification, and flush commands to all of them.
 *
 * @author infoadmstudio2020-cyber
 */
public class CompositeAnalyticsProvider implements IAnalyticsProvider {

    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final List<IAnalyticsProvider> providers = new CopyOnWriteArrayList<>();

    public CompositeAnalyticsProvider() {
        initialize();
    }

    @Override
    public void initialize() {
        initialized.set(true);
        for (IAnalyticsProvider provider : providers) {
            if (provider != null && !provider.isInitialized()) {
                provider.initialize();
            }
        }
    }

    @Override
    public boolean isInitialized() {
        return initialized.get();
    }

    /**
     * Add an analytics provider to this composite.
     *
     * @param provider the provider to register
     */
    public void addProvider(@NonNull IAnalyticsProvider provider) {
        if (provider != null && !providers.contains(provider)) {
            providers.add(provider);
            if (isInitialized() && !provider.isInitialized()) {
                provider.initialize();
            }
        }
    }

    /**
     * Remove an analytics provider from this composite.
     *
     * @param provider the provider to unregister
     */
    public void removeProvider(@NonNull IAnalyticsProvider provider) {
        if (provider != null) {
            providers.remove(provider);
        }
    }

    /**
     * Get an unmodifiable copy of all registered providers.
     *
     * @return list of registered providers
     */
    @NonNull
    public List<IAnalyticsProvider> getProviders() {
        return Collections.unmodifiableList(new ArrayList<>(providers));
    }

    /**
     * Return count of registered providers.
     *
     * @return provider count
     */
    public int getProviderCount() {
        return providers.size();
    }

    @Override
    public void trackEvent(@NonNull AnalyticsEvent event) {
        if (event == null) return;
        for (IAnalyticsProvider provider : providers) {
            try {
                provider.trackEvent(event);
            } catch (Throwable ignored) {
                // Prevent single faulty provider from disrupting telemetry
            }
        }
    }

    @Override
    public void recordMetric(@NonNull PerformanceMetric metric) {
        if (metric == null) return;
        for (IAnalyticsProvider provider : providers) {
            try {
                provider.recordMetric(metric);
            } catch (Throwable ignored) {
                // Prevent single faulty provider from disrupting telemetry
            }
        }
    }

    @Override
    public void setUserId(@Nullable String userId) {
        for (IAnalyticsProvider provider : providers) {
            try {
                provider.setUserId(userId);
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public void setUserProperty(@NonNull String name, @Nullable String value) {
        if (name == null) return;
        for (IAnalyticsProvider provider : providers) {
            try {
                provider.setUserProperty(name, value);
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public void flush() {
        for (IAnalyticsProvider provider : providers) {
            try {
                provider.flush();
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public void clear() {
        for (IAnalyticsProvider provider : providers) {
            try {
                provider.clear();
            } catch (Throwable ignored) {
            }
        }
    }
}
