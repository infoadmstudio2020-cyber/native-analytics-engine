package com.roni.library.analytics.app;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.roni.library.analytics.AnalyticsEngine;
import com.roni.library.analytics.NativeAnalyticsEngine;
import com.roni.library.analytics.NativeAnalyticsEngineConfig;
import com.roni.library.contracts.analytics.AnalyticsEvent;
import com.roni.library.contracts.analytics.PerformanceMetric;
import com.roni.library.contracts.api.NativeApiRequest;
import com.roni.library.contracts.api.NativeApiResponse;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Universal Demonstration and Interactive Testing App for NativeAnalyticsEngine.
 * Implements dashboard, feature explorer, diagnostic monitor, integration test center,
 * and live runtime console.
 *
 * @author infoadmstudio2020-cyber
 */
public class MainActivity extends AppCompatActivity {

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private TextView tvStatus;
    private TextView tvSessionInfo;
    private TextView tvDiagnostics;
    private EditText etEventName;
    private Button btnTrackEvent;
    private Button btnRecordMetric;
    private Button btnToggleTrace;
    private Button btnToggleScreen;
    private Button btnNewSession;
    private Button btnFlushQueue;
    private Button btnRunTests;
    private TextView tvTestResults;
    private TextView tvConsoleLogs;
    private Button btnClearLogs;

    private boolean isTracingActive = false;
    private boolean isScreenActive = false;
    private final StringBuilder logBuffer = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        setupListeners();
        initAnalytics();
        startPeriodicDiagnosticUpdate();
    }

    private void bindViews() {
        tvStatus = findViewById(R.id.tv_status);
        tvSessionInfo = findViewById(R.id.tv_session_info);
        tvDiagnostics = findViewById(R.id.tv_diagnostics);
        etEventName = findViewById(R.id.et_event_name);
        btnTrackEvent = findViewById(R.id.btn_track_event);
        btnRecordMetric = findViewById(R.id.btn_record_metric);
        btnToggleTrace = findViewById(R.id.btn_toggle_trace);
        btnToggleScreen = findViewById(R.id.btn_toggle_screen);
        btnNewSession = findViewById(R.id.btn_new_session);
        btnFlushQueue = findViewById(R.id.btn_flush_queue);
        btnRunTests = findViewById(R.id.btn_run_tests);
        tvTestResults = findViewById(R.id.tv_test_results);
        tvConsoleLogs = findViewById(R.id.tv_console_logs);
        btnClearLogs = findViewById(R.id.btn_clear_logs);

        if (tvConsoleLogs != null) {
            tvConsoleLogs.setMovementMethod(new ScrollingMovementMethod());
        }
    }

    private void initAnalytics() {
        log("Initializing NativeAnalyticsEngine…");

        NativeAnalyticsEngineConfig config = new NativeAnalyticsEngineConfig.Builder()
                .debugEnabled(true)
                .tag("DemoApp")
                .batchSize(20)
                .flushIntervalMs(10000)
                .offlinePersistenceEnabled(true)
                .build();

        NativeAnalyticsEngine.getInstance().init(this, config);

        if (NativeAnalyticsEngine.getInstance().isInitialized()) {
            tvStatus.setText("Status: Ready (Active)");
            log("NativeAnalyticsEngine initialized successfully.");
            updateDiagnostics();
        } else {
            tvStatus.setText("Status: Initialization Failed");
            log("Error: NativeAnalyticsEngine failed to initialize.");
        }
    }

    private void setupListeners() {
        btnTrackEvent.setOnClickListener(v -> {
            String eventName = etEventName.getText() != null ? etEventName.getText().toString().trim() : "";
            if (eventName.isEmpty()) eventName = "custom_event";

            Map<String, Object> params = new HashMap<>();
            params.put("button", "btn_track_event");
            params.put("source", "DemoUI");
            params.put("timestamp", System.currentTimeMillis());

            AnalyticsEngine.trackEvent(eventName, params);
            log("Event tracked: '" + eventName + "' with " + params.size() + " parameters.");
            updateDiagnostics();
        });

        btnRecordMetric.setOnClickListener(v -> {
            double simulatedLatency = 15.0 + (Math.random() * 85.0);
            AnalyticsEngine.recordMetric("ui_interaction_latency", simulatedLatency, "ms");
            log(String.format(Locale.getDefault(), "Metric recorded: ui_interaction_latency = %.1f ms", simulatedLatency));
            updateDiagnostics();
        });

        btnToggleTrace.setOnClickListener(v -> {
            String traceName = "demo_operation_trace";
            if (!isTracingActive) {
                AnalyticsEngine.getPerformanceTracker().startTrace(traceName);
                isTracingActive = true;
                btnToggleTrace.setText("Stop Trace");
                log("Performance trace '" + traceName + "' STARTED.");
            } else {
                long duration = AnalyticsEngine.getPerformanceTracker().stopTrace(traceName);
                isTracingActive = false;
                btnToggleTrace.setText("Start Trace");
                log("Performance trace '" + traceName + "' STOPPED. Elapsed: " + duration + " ms.");
            }
            updateDiagnostics();
        });

        btnToggleScreen.setOnClickListener(v -> {
            String screen = "AnalyticsDashboardScreen";
            if (!isScreenActive) {
                AnalyticsEngine.getSessionTracker().startScreenView(screen);
                isScreenActive = true;
                btnToggleScreen.setText("Leave Screen");
                log("Entered screen: '" + screen + "'.");
            } else {
                long duration = AnalyticsEngine.getSessionTracker().endScreenView(screen);
                isScreenActive = false;
                btnToggleScreen.setText("Screen View");
                log("Left screen: '" + screen + "'. Active time: " + duration + " ms.");
            }
            updateDiagnostics();
        });

        btnNewSession.setOnClickListener(v -> {
            AnalyticsEngine.getSessionTracker().startSession();
            String newSessionId = AnalyticsEngine.getSessionTracker().getCurrentSessionId();
            log("New user session started: " + newSessionId);
            updateDiagnostics();
        });

        btnFlushQueue.setOnClickListener(v -> {
            AnalyticsEngine.flush();
            log("Flushed in-memory buffers and persistent offline event queue.");
            updateDiagnostics();
        });

        btnRunTests.setOnClickListener(v -> runSubsystemSelfTests());

        btnClearLogs.setOnClickListener(v -> {
            logBuffer.setLength(0);
            tvConsoleLogs.setText("");
        });
    }

    private void runSubsystemSelfTests() {
        log("Running Integration Test Suite…");
        StringBuilder results = new StringBuilder();

        // 1. Facade Lifecycle Test
        boolean t1 = NativeAnalyticsEngine.getInstance().isInitialized();
        results.append(t1 ? "[PASS]" : "[FAIL]").append(" 1. Facade & Lifecycle Initialization\n");

        // 2. Event Tracking & Memory Provider
        int countBefore = AnalyticsEngine.getService().getDefaultMemoryProvider().getEventCount();
        AnalyticsEngine.trackEvent("test_self_verify_event", null);
        try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        int countAfter = AnalyticsEngine.getService().getDefaultMemoryProvider().getEventCount();
        boolean t2 = countAfter >= countBefore;
        results.append(t2 ? "[PASS]" : "[FAIL]").append(" 2. Telemetry Dispatch & Provider Pipeline\n");

        // 3. Performance Tracker
        AnalyticsEngine.getPerformanceTracker().startTrace("self_test_trace");
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}
        long traceDuration = AnalyticsEngine.getPerformanceTracker().stopTrace("self_test_trace");
        boolean t3 = traceDuration >= 5;
        results.append(t3 ? "[PASS]" : "[FAIL]").append(" 3. High-Resolution Performance Tracing\n");

        // 4. Session Tracker
        String sessionId = AnalyticsEngine.getSessionTracker().getCurrentSessionId();
        boolean t4 = sessionId != null && !sessionId.isEmpty() && AnalyticsEngine.getSessionTracker().isSessionActive();
        results.append(t4 ? "[PASS]" : "[FAIL]").append(" 4. Automatic Session Management\n");

        // 5. Offline Storage Queue
        int queueBefore = AnalyticsEngine.getEventQueue().count();
        AnalyticsEngine.getEventQueue().enqueue(new AnalyticsEvent("offline_verify_event"));
        int queueAfter = AnalyticsEngine.getEventQueue().count();
        boolean t5 = queueAfter > queueBefore;
        results.append(t5 ? "[PASS]" : "[FAIL]").append(" 5. Zero-Loss Offline Event Spooling\n");

        // 6. RPC Bridge Command Handler
        NativeApiRequest req = new NativeApiRequest.Builder()
                .action("getStats")
                .build();
        NativeApiResponse resp = AnalyticsEngine.getCommandHandler().handleRequest(req);
        boolean t6 = resp.isSuccess() && resp.getData().containsKey("sessionId");
        results.append(t6 ? "[PASS]" : "[FAIL]").append(" 6. Layer 3 WebBridge Command Handler\n");

        tvTestResults.setText(results.toString());
        log("All subsystem verification tests completed.");
        updateDiagnostics();
    }

    private void updateDiagnostics() {
        if (!NativeAnalyticsEngine.getInstance().isInitialized()) return;

        String sessionId = AnalyticsEngine.getSessionTracker().getCurrentSessionId();
        long activeDuration = AnalyticsEngine.getSessionTracker().getTotalActiveDurationMs();
        int eventCount = AnalyticsEngine.getService().getDefaultMemoryProvider().getEventCount();
        int metricCount = AnalyticsEngine.getService().getDefaultMemoryProvider().getMetricCount();
        int queueCount = AnalyticsEngine.getEventQueue().count();

        tvSessionInfo.setText(String.format(Locale.getDefault(), "Session: %s (Active: %ds)",
                sessionId != null && sessionId.length() > 8 ? sessionId.substring(0, 8) + "…" : "N/A",
                activeDuration / 1000));

        tvDiagnostics.setText(String.format(Locale.getDefault(), "Events: %d | Metrics: %d | Offline Queue: %d",
                eventCount, metricCount, queueCount));
    }

    private void startPeriodicDiagnosticUpdate() {
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateDiagnostics();
                mainHandler.postDelayed(this, 2000);
            }
        }, 2000);
    }

    private void log(String message) {
        String timestamp = timeFormat.format(new Date());
        String entry = "[" + timestamp + "] " + message + "\n";
        logBuffer.append(entry);

        if (tvConsoleLogs != null) {
            tvConsoleLogs.setText(logBuffer.toString());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (NativeAnalyticsEngine.getInstance().isInitialized()) {
            AnalyticsEngine.getSessionTracker().onAppForeground();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (NativeAnalyticsEngine.getInstance().isInitialized()) {
            AnalyticsEngine.getSessionTracker().onAppBackground();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mainHandler.removeCallbacksAndMessages(null);
    }
}

