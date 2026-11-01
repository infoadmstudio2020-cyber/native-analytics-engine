package com.roni.library.analytics.provider;

import com.roni.library.contracts.analytics.AnalyticsEvent;
import com.roni.library.contracts.analytics.PerformanceMetric;

/**
 * Core interface for analytics providers within NativeAnalyticsEngine.
 * Extends the ecosystem contract to include lifecycle and flushing capabilities.
 *
 * @author infoadmstudio2020-cyber
 */
public interface IAnalyticsProvider extends com.roni.library.contracts.analytics.IAnalyticsProvider {

    /**
     * Initialize this provider.
     */
    void initialize();

    /**
     * Check if this provider has been initialized.
     *
     * @return true if initialized
     */
    boolean isInitialized();

    /**
     * Flush any buffered events or metrics immediately.
     */
    void flush();

    /**
     * Clear all recorded events and metrics.
     */
    void clear();
}
