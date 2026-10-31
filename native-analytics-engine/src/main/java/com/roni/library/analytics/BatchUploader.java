package com.roni.library.analytics;

import com.roni.library.contracts.analytics.AnalyticsEvent;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Periodically batches and uploads queued analytics events.
 */
public class BatchUploader {
    public interface UploadCallback {
        void onUpload(List<AnalyticsEvent> batch);
    }

    private final EventQueue eventQueue;
    private final int batchIntervalMs;
    private final int batchThreshold;
    private final UploadCallback callback;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledTask;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public BatchUploader(EventQueue eventQueue, int batchIntervalMs, int batchThreshold, UploadCallback callback) {
        this.eventQueue = eventQueue;
        this.batchIntervalMs = Math.max(500, batchIntervalMs);
        this.batchThreshold = Math.max(1, batchThreshold);
        this.callback = callback;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "NativeAnalytics-BatchUploader");
            t.setDaemon(true);
            return t;
        });
    }

    public synchronized void start() {
        if (running.compareAndSet(false, true)) {
            scheduledTask = scheduler.scheduleWithFixedDelay(
                    this::checkAndFlush,
                    batchIntervalMs,
                    batchIntervalMs,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    public void onEventAdded() {
        if (eventQueue.size() >= batchThreshold) {
            checkAndFlush();
        }
    }

    public synchronized void flush() {
        checkAndFlush();
    }

    private synchronized void checkAndFlush() {
        if (!eventQueue.isEmpty() && callback != null) {
            List<AnalyticsEvent> batch = eventQueue.dequeueBatch(batchThreshold);
            if (!batch.isEmpty()) {
                try {
                    callback.onUpload(batch);
                } catch (Throwable ignored) {
                }
            }
        }
    }

    public synchronized void stop() {
        if (running.compareAndSet(true, false)) {
            if (scheduledTask != null) {
                scheduledTask.cancel(false);
                scheduledTask = null;
            }
            flush();
            scheduler.shutdown();
        }
    }

    public boolean isRunning() {
        return running.get();
    }
}
