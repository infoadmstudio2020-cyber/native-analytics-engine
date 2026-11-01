package com.roni.library.analytics.queue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.roni.library.contracts.analytics.AnalyticsEvent;
import com.roni.library.storage.database.Document;
import com.roni.library.storage.database.QueryFilter;
import com.roni.library.storage.database.StorageCollection;
import com.roni.library.storage.database.StorageDatabase;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * High-performance, zero-loss offline event queue powered by native-storage-engine.
 * Backs telemetry events to persistent disk collections to ensure zero event loss
 * during network disruptions, process terminations, or offline scenarios.
 *
 * @author infoadmstudio2020-cyber
 */
public class EventQueue {

    private static final String COLLECTION_NAME = "analytics_offline_events";
    private final File storageDirectory;
    private StorageCollection storageCollection;
    private final ConcurrentLinkedQueue<AnalyticsEvent> memoryFallbackQueue = new ConcurrentLinkedQueue<>();
    private final AtomicInteger pendingCount = new AtomicInteger(0);
    private final Object diskLock = new Object();

    public EventQueue(@Nullable File storageDirectory) {
        this.storageDirectory = storageDirectory;
        initStorage();
    }

    private void initStorage() {
        if (storageDirectory != null) {
            try {
                if (!storageDirectory.exists()) {
                    storageDirectory.mkdirs();
                }
                StorageDatabase database = new StorageDatabase(storageDirectory);
                storageCollection = database.getCollection(COLLECTION_NAME);
                if (storageCollection != null) {
                    storageCollection.loadFromDisk();
                    int diskCount = storageCollection.count(QueryFilter.all());
                    pendingCount.set(diskCount);
                }
            } catch (Throwable t) {
                // Graceful fallback to memory queue if file system has access constraints
                storageCollection = null;
            }
        }
    }

    /**
     * Enqueue an analytics event to offline storage.
     *
     * @param event the event to persist
     */
    public void enqueue(@NonNull AnalyticsEvent event) {
        if (event == null) return;

        boolean savedToDisk = false;
        if (storageCollection != null) {
            synchronized (diskLock) {
                try {
                    String eventId = UUID.randomUUID().toString();
                    Map<String, Object> data = new HashMap<>();
                    data.put("name", event.getName());
                    data.put("timestamp", event.getTimestamp());
                    if (event.getParameters() != null) {
                        data.put("parameters", new HashMap<>(event.getParameters()));
                    }
                    Document doc = new Document(eventId, data);
                    if (storageCollection.insert(doc)) {
                        savedToDisk = true;
                        storageCollection.checkpoint();
                        pendingCount.incrementAndGet();
                    }
                } catch (Throwable ignored) {
                }
            }
        }

        if (!savedToDisk) {
            memoryFallbackQueue.offer(event);
            pendingCount.incrementAndGet();
        }
    }

    /**
     * Dequeue up to maxBatchSize events for transmission.
     *
     * @param maxBatchSize maximum events to retrieve
     * @return batch of events
     */
    @NonNull
    public List<AnalyticsEvent> dequeue(int maxBatchSize) {
        if (maxBatchSize <= 0) return Collections.emptyList();
        List<AnalyticsEvent> batch = new ArrayList<>();

        // 1. Drain memory fallback queue first
        while (batch.size() < maxBatchSize && !memoryFallbackQueue.isEmpty()) {
            AnalyticsEvent event = memoryFallbackQueue.poll();
            if (event != null) {
                batch.add(event);
                pendingCount.decrementAndGet();
            }
        }

        // 2. Drain persistent storage collection if more needed
        if (batch.size() < maxBatchSize && storageCollection != null) {
            synchronized (diskLock) {
                try {
                    List<Document> docs = storageCollection.find(QueryFilter.all());
                    int needed = maxBatchSize - batch.size();
                    int count = Math.min(needed, docs.size());
                    for (int i = 0; i < count; i++) {
                        Document doc = docs.get(i);
                        String name = doc.getString("name", "unknown_event");
                        long timestamp = doc.getLong("timestamp", System.currentTimeMillis());
                        @SuppressWarnings("unchecked")
                        Map<String, Object> params = (Map<String, Object>) doc.get("parameters");
                        AnalyticsEvent event = new AnalyticsEvent(name, params, timestamp);
                        batch.add(event);
                        storageCollection.delete(doc.getId());
                        pendingCount.decrementAndGet();
                    }
                    if (count > 0) {
                        storageCollection.checkpoint();
                    }
                } catch (Throwable ignored) {
                }
            }
        }

        return batch;
    }

    /**
     * Peek at available events without removing them.
     *
     * @param limit maximum count
     * @return list of preview events
     */
    @NonNull
    public List<AnalyticsEvent> peek(int limit) {
        List<AnalyticsEvent> result = new ArrayList<>();
        for (AnalyticsEvent ev : memoryFallbackQueue) {
            if (result.size() >= limit) return result;
            result.add(ev);
        }

        if (result.size() < limit && storageCollection != null) {
            synchronized (diskLock) {
                try {
                    List<Document> docs = storageCollection.find(QueryFilter.all());
                    int needed = limit - result.size();
                    for (int i = 0; i < Math.min(needed, docs.size()); i++) {
                        Document doc = docs.get(i);
                        String name = doc.getString("name", "unknown_event");
                        long timestamp = doc.getLong("timestamp", System.currentTimeMillis());
                        @SuppressWarnings("unchecked")
                        Map<String, Object> params = (Map<String, Object>) doc.get("parameters");
                        result.add(new AnalyticsEvent(name, params, timestamp));
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        return result;
    }

    /**
     * Get count of pending events in queue.
     *
     * @return pending events count
     */
    public int count() {
        int cnt = pendingCount.get();
        return Math.max(0, cnt);
    }

    /**
     * Clear all pending events both in memory and on disk.
     */
    public void clear() {
        memoryFallbackQueue.clear();
        if (storageCollection != null) {
            synchronized (diskLock) {
                try {
                    storageCollection.clear();
                    storageCollection.checkpoint();
                } catch (Throwable ignored) {
                }
            }
        }
        pendingCount.set(0);
    }

    /**
     * Ensure all pending disk writes are synced.
     */
    public void flush() {
        if (storageCollection != null) {
            synchronized (diskLock) {
                try {
                    storageCollection.checkpoint();
                } catch (Throwable ignored) {
                }
            }
        }
    }
}
