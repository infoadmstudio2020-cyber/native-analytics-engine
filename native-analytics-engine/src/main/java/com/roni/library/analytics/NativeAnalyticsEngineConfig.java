package com.roni.library.analytics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;

/**
 * Configuration for {@link NativeAnalyticsEngine}.
 * Uses builder pattern to construct immutable settings for offline buffering,
 * batching intervals, and debug logging.
 *
 * @author infoadmstudio2020-cyber
 */
public final class NativeAnalyticsEngineConfig {

    private final boolean debugEnabled;
    private final String tag;
    private final int timeout;
    private final int batchSize;
    private final long flushIntervalMs;
    private final boolean offlinePersistenceEnabled;
    private final File storageDirectory;

    private NativeAnalyticsEngineConfig(Builder builder) {
        this.debugEnabled = builder.debugEnabled;
        this.tag = builder.tag != null ? builder.tag : "NativeAnalyticsEngine";
        this.timeout = builder.timeout > 0 ? builder.timeout : 30000;
        this.batchSize = builder.batchSize > 0 ? builder.batchSize : 50;
        this.flushIntervalMs = builder.flushIntervalMs > 0 ? builder.flushIntervalMs : 15000L;
        this.offlinePersistenceEnabled = builder.offlinePersistenceEnabled;
        this.storageDirectory = builder.storageDirectory;
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    @NonNull
    public String getTag() {
        return tag;
    }

    public int getTimeout() {
        return timeout;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public long getFlushIntervalMs() {
        return flushIntervalMs;
    }

    public boolean isOfflinePersistenceEnabled() {
        return offlinePersistenceEnabled;
    }

    @Nullable
    public File getStorageDirectory() {
        return storageDirectory;
    }

    public static final class Builder {
        private boolean debugEnabled = false;
        private String tag = "NativeAnalyticsEngine";
        private int timeout = 30000;
        private int batchSize = 50;
        private long flushIntervalMs = 15000L;
        private boolean offlinePersistenceEnabled = true;
        private File storageDirectory = null;

        public Builder() {
        }

        public Builder debugEnabled(boolean debugEnabled) {
            this.debugEnabled = debugEnabled;
            return this;
        }

        public Builder tag(@NonNull String tag) {
            this.tag = tag;
            return this;
        }

        public Builder timeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder batchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public Builder flushIntervalMs(long flushIntervalMs) {
            this.flushIntervalMs = flushIntervalMs;
            return this;
        }

        public Builder offlinePersistenceEnabled(boolean enabled) {
            this.offlinePersistenceEnabled = enabled;
            return this;
        }

        public Builder storageDirectory(@Nullable File storageDirectory) {
            this.storageDirectory = storageDirectory;
            return this;
        }

        @NonNull
        public NativeAnalyticsEngineConfig build() {
            return new NativeAnalyticsEngineConfig(this);
        }
    }
}
