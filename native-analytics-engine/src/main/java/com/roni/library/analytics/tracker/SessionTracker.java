package com.roni.library.analytics.tracker;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks user sessions, screen views, and foreground/background durations.
 */
public class SessionTracker {
    private String currentSessionId;
    private long sessionStartTimeMs;
    private long totalActiveDurationMs;
    private long lastForegroundTimeMs;
    private boolean sessionActive;
    private boolean foreground;
    private final Map<String, Long> screenStartTimes = new ConcurrentHashMap<>();

    public SessionTracker() {
        this.sessionActive = false;
        this.foreground = false;
        this.totalActiveDurationMs = 0;
    }

    public synchronized void startSession() {
        this.currentSessionId = UUID.randomUUID().toString();
        this.sessionStartTimeMs = System.currentTimeMillis();
        this.totalActiveDurationMs = 0;
        this.lastForegroundTimeMs = this.sessionStartTimeMs;
        this.sessionActive = true;
        this.foreground = true;
    }

    public synchronized void endSession() {
        if (sessionActive && foreground) {
            long now = System.currentTimeMillis();
            totalActiveDurationMs += Math.max(0, now - lastForegroundTimeMs);
        }
        this.sessionActive = false;
        this.foreground = false;
        this.screenStartTimes.clear();
    }

    public synchronized void onAppForeground() {
        if (!sessionActive) {
            startSession();
        } else {
            this.foreground = true;
            this.lastForegroundTimeMs = System.currentTimeMillis();
        }
    }

    public synchronized void onAppBackground() {
        if (sessionActive && foreground) {
            long now = System.currentTimeMillis();
            totalActiveDurationMs += Math.max(0, now - lastForegroundTimeMs);
            this.foreground = false;
        }
    }

    public void startScreenView(String screenName) {
        if (screenName != null && !screenName.trim().isEmpty()) {
            screenStartTimes.put(screenName, System.currentTimeMillis());
        }
    }

    public long endScreenView(String screenName) {
        if (screenName == null) return 0;
        Long start = screenStartTimes.remove(screenName);
        if (start != null) {
            return Math.max(0, System.currentTimeMillis() - start);
        }
        return 0;
    }

    public synchronized String getCurrentSessionId() {
        return currentSessionId;
    }

    public synchronized boolean isSessionActive() {
        return sessionActive;
    }

    public synchronized long getTotalActiveDurationMs() {
        if (sessionActive && foreground) {
            long elapsedSinceForeground = System.currentTimeMillis() - lastForegroundTimeMs;
            return totalActiveDurationMs + Math.max(0, elapsedSinceForeground);
        }
        return totalActiveDurationMs;
    }

    public synchronized long getSessionStartTimeMs() {
        return sessionStartTimeMs;
    }
}
