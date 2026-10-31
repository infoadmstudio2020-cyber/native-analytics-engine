package com.roni.library.analytics;

import com.roni.library.contracts.analytics.AnalyticsEvent;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Thread-safe bounded event queue for buffering analytics events.
 */
public class EventQueue {
    private final int capacity;
    private final LinkedList<AnalyticsEvent> queue;

    public EventQueue(int capacity) {
        this.capacity = capacity > 0 ? capacity : 1000;
        this.queue = new LinkedList<>();
    }

    public synchronized boolean enqueue(AnalyticsEvent event) {
        if (event == null) {
            return false;
        }
        if (queue.size() >= capacity) {
            queue.pollFirst(); // drop oldest to prevent unbounded memory growth
        }
        return queue.offerLast(event);
    }

    public synchronized List<AnalyticsEvent> dequeueBatch(int maxBatchSize) {
        List<AnalyticsEvent> batch = new ArrayList<>();
        int limit = Math.min(maxBatchSize, queue.size());
        for (int i = 0; i < limit; i++) {
            AnalyticsEvent event = queue.pollFirst();
            if (event != null) {
                batch.add(event);
            }
        }
        return batch;
    }

    public synchronized List<AnalyticsEvent> drain() {
        List<AnalyticsEvent> all = new ArrayList<>(queue);
        queue.clear();
        return all;
    }

    public synchronized int size() {
        return queue.size();
    }

    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }

    public synchronized void clear() {
        queue.clear();
    }
}
