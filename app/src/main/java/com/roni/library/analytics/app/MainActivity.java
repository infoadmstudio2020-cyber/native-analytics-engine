package com.roni.library.analytics.app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.roni.library.analytics.NativeAnalyticsEngine;
import com.roni.library.analytics.NativeAnalyticsEngineConfig;
import com.roni.library.contracts.analytics.AnalyticsEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Main Activity demonstrating NativeAnalyticsEngine usage.
 */
public class MainActivity extends AppCompatActivity {
    private TextView tvStatus;
    private TextView tvLog;
    private StringBuilder logBuffer = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tv_status);
        tvLog = findViewById(R.id.tv_log);

        Button btnInit = findViewById(R.id.btn_init);
        Button btnTrackEvent = findViewById(R.id.btn_track_event);
        Button btnRecordMetric = findViewById(R.id.btn_record_metric);
        Button btnTrace = findViewById(R.id.btn_trace);
        Button btnSession = findViewById(R.id.btn_session);
        Button btnFlush = findViewById(R.id.btn_flush);

        btnInit.setOnClickListener(v -> initEngine());
        btnTrackEvent.setOnClickListener(v -> trackSampleEvent());
        btnRecordMetric.setOnClickListener(v -> recordSampleMetric());
        btnTrace.setOnClickListener(v -> runTraceTest());
        btnSession.setOnClickListener(v -> toggleSession());
        btnFlush.setOnClickListener(v -> flushTelemetry());

        initEngine();
    }

    private void log(String msg) {
        logBuffer.append("[").append(System.currentTimeMillis() % 100000).append("] ").append(msg).append("\n");
        if (tvLog != null) {
            tvLog.setText(logBuffer.toString());
        }
    }

    private void initEngine() {
        NativeAnalyticsEngineConfig config = NativeAnalyticsEngineConfig.builder()
                .debugEnabled(true)
                .tag("DemoAnalytics")
                .batchIntervalMs(3000)
                .batchSizeThreshold(5)
                .build();

        NativeAnalyticsEngine.getInstance().init(this, config);
        boolean initialized = NativeAnalyticsEngine.getInstance().isInitialized();
        if (tvStatus != null) {
            tvStatus.setText("Status: " + (initialized ? "INITIALIZED (ACTIVE)" : "INIT FAILED"));
        }
        log("NativeAnalyticsEngine initialized: " + initialized);
    }

    private void trackSampleEvent() {
        Map<String, Object> params = new HashMap<>();
        params.put("screen", "MainActivity");
        params.put("action", "button_click");
        params.put("timestamp", System.currentTimeMillis());

        NativeAnalyticsEngine.getInstance().trackEvent("ui_interaction", params);
        log("Event tracked: 'ui_interaction'");
    }

    private void recordSampleMetric() {
        double latency = Math.round((Math.random() * 50 + 10) * 100.0) / 100.0;
        NativeAnalyticsEngine.getInstance().recordMetric("bridge_latency", latency, "ms");
        log("Metric recorded: bridge_latency = " + latency + " ms");
    }

    private void runTraceTest() {
        String traceName = "compute_hash";
        NativeAnalyticsEngine.getInstance().getPerformanceTracker().startTrace(traceName);
        log("Trace started: " + traceName);

        getWindow().getDecorView().postDelayed(() -> {
            long duration = NativeAnalyticsEngine.getInstance().getPerformanceTracker().stopTrace(traceName);
            log("Trace stopped: " + traceName + ", duration: " + duration + " ms");
        }, 150);
    }

    private void toggleSession() {
        boolean active = NativeAnalyticsEngine.getInstance().getSessionTracker().isSessionActive();
        if (active) {
            long duration = NativeAnalyticsEngine.getInstance().getSessionTracker().getTotalActiveDurationMs();
            NativeAnalyticsEngine.getInstance().getSessionTracker().endSession();
            log("Session ended. Active duration: " + duration + " ms");
        } else {
            NativeAnalyticsEngine.getInstance().getSessionTracker().startSession();
            log("Session started: " + NativeAnalyticsEngine.getInstance().getSessionTracker().getCurrentSessionId());
        }
    }

    private void flushTelemetry() {
        NativeAnalyticsEngine.getInstance().flush();
        int eventCount = NativeAnalyticsEngine.getInstance().getCapturedEventCount();
        log("Flushed telemetry. Memory provider total events: " + eventCount);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NativeAnalyticsEngine.getInstance().release();
    }
}
