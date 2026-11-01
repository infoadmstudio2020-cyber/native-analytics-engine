package com.roni.library.analytics.tracker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.roni.library.analytics.provider.IAnalyticsProvider;
import com.roni.library.contracts.analytics.AnalyticsEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Session tracker that automatically manages session lifecycle,
 * foreground/background duration tracking, and screen view navigation.
 *
 * @author infoadmstudio2020-cyber
 */
public class SessionTracker {

    private final IAnalyticsProvider provider;
    private String currentSessionId;
    private boolean sessionActive = false;
    private long sessionStartTime = 0;
    private long totalActiveDurationMs = 0;
    private long lastForegroundTimestamp = 0;
    private volatile String currentScreenName;

    private final Map<String, Long> activeScreenViews = new ConcurrentHashMap<>();

    public SessionTracker() {
        this(null);
    }

    public SessionTracker(@Nullable IAnalyticsProvider provider) {
        this.provider = provider;
        startSession();
    }

    /**
     * Explicitly start a new user session.
     */
    public synchronized void startSession() {
        if (sessionActive) {
            endSession();
        }

        currentSessionId = UUID.randomUUID().toString();
        sessionStartTime = System.currentTimeMillis();
        lastForegroundTimestamp = sessionStartTime;
        totalActiveDurationMs = 0;
        sessionActive = true;

        if (provider != null) {
            Map<String, Object> params = new HashMap<>();
            params.put("session_id", currentSessionId);
            params.put("start_time", sessionStartTime);
            provider.trackEvent(new AnalyticsEvent("session_start", params, sessionStartTime));
        }
    }

    /**
     * Explicitly end the current session.
     */
    public synchronized void endSession() {
        if (!sessionActive) return;

        long now = System.currentTimeMillis();
        if (lastForegroundTimestamp > 0) {
            totalActiveDurationMs += Math.max(0, now - lastForegroundTimestamp);
            lastForegroundTimestamp = 0;
        }

        long finalDuration = totalActiveDurationMs;
        String endedSessionId = currentSessionId;
        sessionActive = false;

        if (provider != null) {
            Map<String, Object> params = new HashMap<>();
            params.put("session_id", endedSessionId);
            params.put("duration_ms", finalDuration);
            params.put("end_time", now);
            provider.trackEvent(new AnalyticsEvent("session_end", params, now));
        }
    }

    /**
     * Called when the host application transitions to the foreground.
     */
    public synchronized void onAppForeground() {
        long now = System.currentTimeMillis();
        lastForegroundTimestamp = now;
        if (!sessionActive) {
            startSession();
        } else if (provider != null) {
            Map<String, Object> params = new HashMap<>();
            params.put("session_id", currentSessionId);
            provider.trackEvent(new AnalyticsEvent("app_foreground", params, now));
        }
    }

    /**
     * Called when the host application transitions to the background.
     */
    public synchronized void onAppBackground() {
        long now = System.currentTimeMillis();
        if (lastForegroundTimestamp > 0) {
            totalActiveDurationMs += Math.max(0, now - lastForegroundTimestamp);
            lastForegroundTimestamp = 0;
        }

        if (provider != null && sessionActive) {
            Map<String, Object> params = new HashMap<>();
            params.put("session_id", currentSessionId);
            params.put("active_duration_ms", totalActiveDurationMs);
            provider.trackEvent(new AnalyticsEvent("app_background", params, now));
        }
    }

    /**
     * Record entering a screen view.
     *
     * @param screenName name of the screen or route
     */
    public void startScreenView(@NonNull String screenName) {
        if (screenName == null) return;
        currentScreenName = screenName;
        long now = System.currentTimeMillis();
        activeScreenViews.put(screenName, now);

        if (provider != null) {
            Map<String, Object> params = new HashMap<>();
            params.put("screen_name", screenName);
            params.put("session_id", currentSessionId);
            provider.trackEvent(new AnalyticsEvent("screen_view", params, now));
        }
    }

    /**
     * Record leaving a screen view.
     *
     * @param screenName name of the screen
     * @return duration spent on screen in milliseconds
     */
    public long endScreenView(@NonNull String screenName) {
        if (screenName == null) return -1;
        Long startTime = activeScreenViews.remove(screenName);
        if (startTime == null) return -1;

        long duration = Math.max(0, System.currentTimeMillis() - startTime);
        if (screenName.equals(currentScreenName)) {
            currentScreenName = null;
        }

        if (provider != null) {
            Map<String, Object> params = new HashMap<>();
            params.put("screen_name", screenName);
            params.put("duration_ms", duration);
            params.put("session_id", currentSessionId);
            provider.trackEvent(new AnalyticsEvent("screen_leave", params, System.currentTimeMillis()));
        }
        return duration;
    }

    @Nullable
    public String getCurrentSessionId() {
        return currentSessionId;
    }

    public boolean isSessionActive() {
        return sessionActive;
    }

    public synchronized long getTotalActiveDurationMs() {
        long currentTotal = totalActiveDurationMs;
        if (sessionActive && lastForegroundTimestamp > 0) {
            currentTotal += Math.max(0, System.currentTimeMillis() - lastForegroundTimestamp);
        }
        return currentTotal;
    }

    public long getSessionStartTime() {
        return sessionStartTime;
    }

    @Nullable
    public String getCurrentScreenName() {
        return currentScreenName;
    }
}
