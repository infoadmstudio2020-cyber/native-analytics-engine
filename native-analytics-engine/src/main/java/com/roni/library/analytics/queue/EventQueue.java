
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Persistent offline analytics event queue.
 *
 * Events are assigned monotonically increasing sequence numbers
 * to preserve FIFO ordering during retrieval.
 */
public class EventQueue {

    private static final String COLLECTION_NAME =
            "analytics_offline_events";

    private static final String FIELD_SEQUENCE = "sequence";

    private final File storageDirectory;

    private StorageCollection storageCollection;

    private final ConcurrentLinkedQueue<AnalyticsEvent>
            memoryFallbackQueue = new ConcurrentLinkedQueue<>();

    private final AtomicLong sequenceGenerator = new AtomicLong(0);

    private final Object diskLock = new Object();

    private volatile int diskPendingCount = 0;

    public EventQueue(@Nullable File storageDirectory) {
        this.storageDirectory = storageDirectory;
        initStorage();
    }

    private void initStorage() {
        if (storageDirectory == null) {
            return;
        }

        try {
            if (!storageDirectory.exists()
                    && !storageDirectory.mkdirs()
                    && !storageDirectory.exists()) {
                return;
            }

            StorageDatabase database =
                    new StorageDatabase(storageDirectory);

            storageCollection =
                    database.getCollection(COLLECTION_NAME);

            if (storageCollection != null) {
                synchronized (diskLock) {
                    List<Document> docs =
                            storageCollection.find(QueryFilter.all());

                    diskPendingCount = docs.size();

                    long maxSequence = -1L;

                    for (Document doc : docs) {
                        long sequence =
                                doc.getLong(FIELD_SEQUENCE, -1L);

                        if (sequence > maxSequence) {
                            maxSequence = sequence;
                        }
                    }

                    sequenceGenerator.set(maxSequence + 1L);
                }
            }

        } catch (Throwable ignored) {
            storageCollection = null;
            diskPendingCount = 0;
        }
    }

    /**
     * Enqueue an event.
     */
    public void enqueue(@NonNull AnalyticsEvent event) {
        if (event == null) {
            return;
        }

        long sequence = sequenceGenerator.getAndIncrement();

        if (storageCollection != null) {
            synchronized (diskLock) {
                try {
                    Map<String, Object> data = new HashMap<>();

                    data.put("name", event.getName());
                    data.put("timestamp", event.getTimestamp());
                    data.put(FIELD_SEQUENCE, sequence);

                    if (event.getParameters() != null) {
                        data.put(
                                "parameters",
                                new HashMap<>(event.getParameters())
                        );
                    }

                    Document doc = new Document(
                            UUID.randomUUID().toString(),
                            data
                    );

                    if (storageCollection.insert(doc)) {
                        storageCollection.checkpoint();
                        diskPendingCount++;
                        return;
                    }

                } catch (Throwable ignored) {
                    // Fall through to memory fallback.
                }
            }
        }

        memoryFallbackQueue.offer(event);
    }

    /**
     * Retrieve events without removing them.
     */
    @NonNull
    public List<AnalyticsEvent> peek(int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }

        synchronized (diskLock) {
            return collectEvents(limit);
        }
    }

    /**
     * Retrieve and remove events in FIFO order.
     */
    @NonNull
    public List<AnalyticsEvent> dequeue(int maxBatchSize) {
        if (maxBatchSize <= 0) {
            return Collections.emptyList();
        }

        synchronized (diskLock) {
            List<AnalyticsEvent> result =
                    new ArrayList<>(maxBatchSize);

            // Memory fallback events are drained first.
            while (result.size() < maxBatchSize) {
                AnalyticsEvent event = memoryFallbackQueue.poll();

                if (event == null) {
                    break;
                }

                result.add(event);
            }

            if (result.size() >= maxBatchSize
                    || storageCollection == null) {
                return result;
            }

            try {
                List<Document> docs =
                        getOrderedDocuments();

                int needed = maxBatchSize - result.size();
                int count = Math.min(needed, docs.size());

                for (int i = 0; i < count; i++) {
                    Document doc = docs.get(i);

                    AnalyticsEvent event = toAnalyticsEvent(doc);

                    if (storageCollection.delete(doc.getId())) {
                        result.add(event);
                        diskPendingCount =
                                Math.max(0, diskPendingCount - 1);
                    }
                }

                if (count > 0) {
                    storageCollection.checkpoint();
                }

            } catch (Throwable ignored) {
                // Return successfully retrieved events.
            }

            return result;
        }
    }

    /**
     * Collect events in FIFO order without modifying storage.
     */
    @NonNull
    private List<AnalyticsEvent> collectEvents(int limit) {
        List<AnalyticsEvent> result = new ArrayList<>(limit);

        // Memory queue first.
        for (AnalyticsEvent event : memoryFallbackQueue) {
            if (result.size() >= limit) {
                return result;
            }

            result.add(event);
        }

        if (result.size() >= limit || storageCollection == null) {
            return result;
        }

        try {
            List<Document> docs = getOrderedDocuments();

            int needed = limit - result.size();
            int count = Math.min(needed, docs.size());

            for (int i = 0; i < count; i++) {
                result.add(toAnalyticsEvent(docs.get(i)));
            }

        } catch (Throwable ignored) {
            // Preserve already collected events.
        }

        return result;
    }

    /**
     * Sort disk documents using their sequence number.
     */

    @NonNull
    private List<Document> getOrderedDocuments() {
        List<Document> docs =
                storageCollection.find(QueryFilter.all());

        if (docs == null || docs.isEmpty()) {
            return new ArrayList<>();
        }

        List<Document> ordered = new ArrayList<>(docs);

        Collections.sort(
                ordered,
                new Comparator<Document>() {
                    @Override
                    public int compare(Document first, Document second) {
                        long firstSequence =
                                first.getLong(FIELD_SEQUENCE, Long.MAX_VALUE);

                        long secondSequence =
                                second.getLong(FIELD_SEQUENCE, Long.MAX_VALUE);

                        return Long.compare(firstSequence, secondSequence);
                    }
                }
        );

        return ordered;
    }


    @NonNull
    private AnalyticsEvent toAnalyticsEvent(@NonNull Document doc) {
        String name = doc.getString("name", "unknown_event");

        long timestamp = doc.getLong(
                "timestamp",
                System.currentTimeMillis()
        );

        Object rawParams = doc.get("parameters");

        Map<String, Object> params = null;

        if (rawParams instanceof Map) {
            params = new HashMap<>();

            Map<?, ?> source = (Map<?, ?>) rawParams;

            for (Map.Entry<?, ?> entry : source.entrySet()) {
                if (entry.getKey() instanceof String) {
                    params.put(
                            (String) entry.getKey(),
                            entry.getValue()
                    );
                }
            }
        }

        return new AnalyticsEvent(name, params, timestamp);
    }

    /**
     * Current pending event count.
     */
    public int count() {
        synchronized (diskLock) {
            return Math.max(
                    0,
                    diskPendingCount + memoryFallbackQueue.size()
            );
        }
    }

    /**
     * Remove all pending events.
     */
    public void clear() {
        synchronized (diskLock) {
            memoryFallbackQueue.clear();

            if (storageCollection != null) {
                try {
                    storageCollection.clear();
                    storageCollection.checkpoint();
                } catch (Throwable ignored) {
                }
            }

            diskPendingCount = 0;
        }
    }

    /**
     * Sync pending disk writes.
     */
    public void flush() {
        synchronized (diskLock) {
            if (storageCollection != null) {
                try {
                    storageCollection.checkpoint();
                } catch (Throwable ignored) {
                }
            }
        }
    }
}
