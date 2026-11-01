package com.roni.library.analytics.dispatcher;

import androidx.annotation.NonNull;

import com.roni.library.analytics.queue.EventQueue;
import com.roni.library.contracts.analytics.AnalyticsEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Periodically drains buffered analytics events from {@link EventQueue}
 * and coordinates batch delivery to registered remote destinations or logging endpoints.
 *
 * @author infoadmstudio2020-cyber
 */
public class BatchUploader {

    public interface BatchUploadListener {
        void onBatchDispatched(@NonNull List<AnalyticsEvent> events);
        void onUploadFailed(@NonNull Throwable error);
    }

    private final EventQueue eventQueue;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final List<BatchUploadListener> listeners = new CopyOnWriteArrayList<>();

    private volatile int batchSize = 50;
    private volatile long flushIntervalMs = 15000;

    public BatchUploader(@NonNull EventQueue eventQueue) {
        this.eventQueue = eventQueue;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "NativeAnalytics-BatchUploader");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void addListener(@NonNull BatchUploadListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(@NonNull BatchUploadListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    /**
     * Start the periodic background uploader loop.
     */
    public synchronized void start() {
        if (isRunning.compareAndSet(false, true)) {
            scheduler.scheduleWithFixedDelay(this::flushSync, flushIntervalMs, flushIntervalMs, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Stop background uploader loop.
     */
    public synchronized void stop() {
        if (isRunning.compareAndSet(true, false)) {
            scheduler.shutdown();
        }
    }

    /**
     * Synchronously flush up to batchSize items from the queue.
     */
    public void flushSync() {
        try {
            if (eventQueue == null) return;
            List<AnalyticsEvent> batch = eventQueue.dequeue(batchSize);
            if (!batch.isEmpty()) {
                for (BatchUploadListener listener : listeners) {
                    try {
                        listener.onBatchDispatched(batch);
                    } catch (Throwable t) {
                        listener.onUploadFailed(t);
                    }
                }
            }
        } catch (Throwable t) {
            for (BatchUploadListener listener : listeners) {
                try {
                    listener.onUploadFailed(t);
                } catch (Throwable ignored) {
                }
            }
        }
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = Math.max(1, batchSize);
    }

    public long getFlushIntervalMs() {
        return flushIntervalMs;
    }

    public void setFlushIntervalMs(long flushIntervalMs) {
        this.flushIntervalMs = Math.max(1000, flushIntervalMs);
    }

    public boolean isRunning() {
        return isRunning.get();
    }
}
