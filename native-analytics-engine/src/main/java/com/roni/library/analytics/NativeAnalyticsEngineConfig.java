package com.roni.library.analytics;

/**
 * Configuration options for NativeAnalyticsEngine.
 */
public class NativeAnalyticsEngineConfig {
    private final boolean debugEnabled;
    private final String tag;
    private final int timeout;
    private final int batchIntervalMs;
    private final int batchSizeThreshold;
    private final int maxQueueSize;
    private final boolean diskBufferingEnabled;

    private NativeAnalyticsEngineConfig(Builder builder) {
        this.debugEnabled = builder.debugEnabled;
        this.tag = builder.tag;
        this.timeout = builder.timeout;
        this.batchIntervalMs = builder.batchIntervalMs;
        this.batchSizeThreshold = builder.batchSizeThreshold;
        this.maxQueueSize = builder.maxQueueSize;
        this.diskBufferingEnabled = builder.diskBufferingEnabled;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public String getTag() {
        return tag;
    }

    public int getTimeout() {
        return timeout;
    }

    public int getBatchIntervalMs() {
        return batchIntervalMs;
    }

    public int getBatchSizeThreshold() {
        return batchSizeThreshold;
    }

    public int getMaxQueueSize() {
        return maxQueueSize;
    }

    public boolean isDiskBufferingEnabled() {
        return diskBufferingEnabled;
    }

    public static class Builder {
        private boolean debugEnabled = false;
        private String tag = "NativeAnalyticsEngine";
        private int timeout = 30000;
        private int batchIntervalMs = 5000;
        private int batchSizeThreshold = 20;
        private int maxQueueSize = 1000;
        private boolean diskBufferingEnabled = true;

        public Builder debugEnabled(boolean debugEnabled) {
            this.debugEnabled = debugEnabled;
            return this;
        }

        public Builder tag(String tag) {
            if (tag != null && !tag.trim().isEmpty()) {
                this.tag = tag;
            }
            return this;
        }

        public Builder timeout(int timeout) {
            this.timeout = Math.max(1000, timeout);
            return this;
        }

        public Builder batchIntervalMs(int batchIntervalMs) {
            this.batchIntervalMs = Math.max(500, batchIntervalMs);
            return this;
        }

        public Builder batchSizeThreshold(int batchSizeThreshold) {
            this.batchSizeThreshold = Math.max(1, batchSizeThreshold);
            return this;
        }

        public Builder maxQueueSize(int maxQueueSize) {
            this.maxQueueSize = Math.max(10, maxQueueSize);
            return this;
        }

        public Builder diskBufferingEnabled(boolean diskBufferingEnabled) {
            this.diskBufferingEnabled = diskBufferingEnabled;
            return this;
        }

        public NativeAnalyticsEngineConfig build() {
            return new NativeAnalyticsEngineConfig(this);
        }
    }
}
