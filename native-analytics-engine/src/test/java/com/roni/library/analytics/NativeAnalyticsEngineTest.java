package com.roni.library.analytics;

import com.roni.library.analytics.handler.AnalyticsCommandHandler;
import com.roni.library.analytics.provider.CompositeAnalyticsProvider;
import com.roni.library.analytics.provider.DiskBufferAnalyticsProvider;
import com.roni.library.analytics.provider.MemoryAnalyticsProvider;
import com.roni.library.analytics.service.NativeAnalyticsService;
import com.roni.library.analytics.tracker.PerformanceTracker;
import com.roni.library.analytics.tracker.SessionTracker;
import com.roni.library.contracts.analytics.AnalyticsEvent;
import com.roni.library.contracts.analytics.PerformanceMetric;
import com.roni.library.contracts.api.IApiCallback;
import com.roni.library.contracts.api.NativeApiError;
import com.roni.library.contracts.api.NativeApiRequest;
import com.roni.library.contracts.api.NativeApiResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/**
 * Comprehensive JVM Unit tests for {@link NativeAnalyticsEngine} and its subsystem components.
 */
public class NativeAnalyticsEngineTest {

    @Before
    public void setUp() {
        NativeAnalyticsEngine.getInstance().release();
    }

    @After
    public void tearDown() {
        NativeAnalyticsEngine.getInstance().release();
    }

    @Test
    public void testConfigBuilderDefaults() {
        NativeAnalyticsEngineConfig config = NativeAnalyticsEngineConfig.builder().build();
        assertNotNull(config);
        assertFalse(config.isDebugEnabled());
        assertEquals("NativeAnalyticsEngine", config.getTag());
        assertEquals(30000, config.getTimeout());
        assertEquals(5000, config.getBatchIntervalMs());
        assertEquals(20, config.getBatchSizeThreshold());
        assertEquals(1000, config.getMaxQueueSize());
        assertTrue(config.isDiskBufferingEnabled());
    }

    @Test
    public void testConfigBuilderCustom() {
        NativeAnalyticsEngineConfig config = NativeAnalyticsEngineConfig.builder()
                .debugEnabled(true)
                .tag("CustomAnalytics")
                .timeout(15000)
                .batchIntervalMs(2000)
                .batchSizeThreshold(10)
                .maxQueueSize(500)
                .diskBufferingEnabled(false)
                .build();

        assertTrue(config.isDebugEnabled());
        assertEquals("CustomAnalytics", config.getTag());
        assertEquals(15000, config.getTimeout());
        assertEquals(2000, config.getBatchIntervalMs());
        assertEquals(10, config.getBatchSizeThreshold());
        assertEquals(500, config.getMaxQueueSize());
        assertFalse(config.isDiskBufferingEnabled());
    }

    @Test
    public void testResultModel() {
        NativeAnalyticsEngineResult<String> success = NativeAnalyticsEngineResult.success("analytics_ok");
        assertTrue(success.isSuccess());
        assertEquals("analytics_ok", success.getData());
        assertNull(success.getErrorMessage());
        assertEquals(0, success.getErrorCode());

        NativeAnalyticsEngineResult<Void> failure = NativeAnalyticsEngineResult.failure(404, "Not found");
        assertFalse(failure.isSuccess());
        assertNull(failure.getData());
        assertEquals("Not found", failure.getErrorMessage());
        assertEquals(404, failure.getErrorCode());
        assertTrue(failure.toString().contains("404"));
    }

    @Test
    public void testSingletonLifecycle() {
        NativeAnalyticsEngine engine = NativeAnalyticsEngine.getInstance();
        assertNotNull(engine);
        assertFalse(engine.isInitialized());

        NativeAnalyticsEngineConfig config = NativeAnalyticsEngineConfig.builder()
                .debugEnabled(true)
                .batchIntervalMs(1000)
                .build();

        NativeAnalyticsEngineResult<Boolean> res = engine.init(null, config);
        assertTrue(res.isSuccess());
        assertTrue(engine.isInitialized());
        assertNotNull(engine.getService());
        assertNotNull(engine.getCommandHandler());
        assertNotNull(engine.getSessionTracker());
        assertNotNull(engine.getPerformanceTracker());
        assertNotNull(engine.getEventQueue());
        assertNotNull(engine.getBatchUploader());
        assertNotNull(engine.getTelemetryDispatcher());

        // Re-init guard
        NativeAnalyticsEngineResult<Boolean> reInit = engine.init(null, config);
        assertTrue(reInit.isSuccess());

        // Release
        engine.release();
        assertFalse(engine.isInitialized());
        assertNull(engine.getService());
    }

    @Test
    public void testFacadeAnalyticsEngine() {
        NativeAnalyticsEngineConfig config = NativeAnalyticsEngineConfig.builder().build();
        NativeAnalyticsEngineResult<Boolean> res = AnalyticsEngine.init(null, config);
        assertTrue(res.isSuccess());
        assertTrue(AnalyticsEngine.isInitialized());
        assertNotNull(AnalyticsEngine.getInstance());
        assertNotNull(AnalyticsEngine.getConfig());
        assertNotNull(AnalyticsEngine.getService());
        assertNotNull(AnalyticsEngine.getCommandHandler());
        assertNotNull(AnalyticsEngine.getSessionTracker());
        assertNotNull(AnalyticsEngine.getPerformanceTracker());

        AnalyticsEngine.trackEvent("user_login", null);
        AnalyticsEngine.recordMetric("fps", 60.0, "fps");
        AnalyticsEngine.flush();

        AnalyticsEngine.release();
        assertFalse(AnalyticsEngine.isInitialized());
    }

    @Test
    public void testEventQueueOperations() {
        EventQueue queue = new EventQueue(5);
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());

        for (int i = 0; i < 5; i++) {
            assertTrue(queue.enqueue(new AnalyticsEvent("event_" + i)));
        }
        assertEquals(5, queue.size());

        // Overflow drop oldest
        queue.enqueue(new AnalyticsEvent("event_overflow"));
        assertEquals(5, queue.size());

        List<AnalyticsEvent> batch = queue.dequeueBatch(2);
        assertEquals(2, batch.size());
        assertEquals(3, queue.size());

        List<AnalyticsEvent> remaining = queue.drain();
        assertEquals(3, remaining.size());
        assertTrue(queue.isEmpty());
    }

    @Test
    public void testTelemetryDispatcher() {
        TelemetryDispatcher dispatcher = new TelemetryDispatcher();
        AtomicBoolean eventReceived = new AtomicBoolean(false);
        AtomicBoolean metricReceived = new AtomicBoolean(false);

        TelemetryDispatcher.TelemetryListener listener = new TelemetryDispatcher.TelemetryListener() {
            @Override
            public void onEventDispatched(AnalyticsEvent event) {
                eventReceived.set(true);
            }

            @Override
            public void onMetricDispatched(PerformanceMetric metric) {
                metricReceived.set(true);
            }
        };

        dispatcher.addListener(listener);
        assertEquals(1, dispatcher.getListenerCount());

        dispatcher.dispatchEvent(new AnalyticsEvent("test_dispatch"));
        assertTrue(eventReceived.get());

        dispatcher.dispatchMetric(new PerformanceMetric("latency", 42.0, "ms"));
        assertTrue(metricReceived.get());

        dispatcher.removeListener(listener);
        assertEquals(0, dispatcher.getListenerCount());
    }

    @Test
    public void testSessionTrackerLifecycle() {
        SessionTracker tracker = new SessionTracker();
        assertFalse(tracker.isSessionActive());
        assertEquals(0, tracker.getTotalActiveDurationMs());

        tracker.startSession();
        assertTrue(tracker.isSessionActive());
        assertNotNull(tracker.getCurrentSessionId());

        tracker.startScreenView("HomeScreen");
        try {
            Thread.sleep(10);
        } catch (InterruptedException ignored) {}
        long screenDuration = tracker.endScreenView("HomeScreen");
        assertTrue(screenDuration >= 5);

        tracker.onAppBackground();
        tracker.onAppForeground();
        assertTrue(tracker.getTotalActiveDurationMs() >= 5);

        tracker.endSession();
        assertFalse(tracker.isSessionActive());
    }

    @Test
    public void testPerformanceTrackerMetrics() {
        PerformanceTracker tracker = new PerformanceTracker();
        AtomicBoolean metricNotified = new AtomicBoolean(false);
        tracker.addMetricListener(metric -> metricNotified.set(true));

        tracker.startTrace("db_query");
        assertTrue(tracker.hasActiveTrace("db_query"));

        try {
            Thread.sleep(10);
        } catch (InterruptedException ignored) {}

        long duration = tracker.stopTrace("db_query");
        assertTrue(duration >= 5);
        assertFalse(tracker.hasActiveTrace("db_query"));
        assertTrue(metricNotified.get());

        // Non-existent trace
        assertEquals(-1, tracker.stopTrace("non_existent"));

        // Helper records
        tracker.recordBridgeLatency("send_payload", 15);
        tracker.recordWebFirstPaint(120);
        tracker.recordHttpResponseTime("api/v1/data", 200, 45);
    }

    @Test
    public void testMemoryAnalyticsProvider() {
        MemoryAnalyticsProvider provider = new MemoryAnalyticsProvider();
        provider.initialize();
        assertTrue(provider.isInitialized());

        AnalyticsEvent ev = new AnalyticsEvent("button_click");
        provider.trackEvent(ev);
        assertEquals(1, provider.getCapturedEvents().size());
        assertEquals("button_click", provider.getCapturedEvents().get(0).getName());

        PerformanceMetric metric = new PerformanceMetric("cpu_load", 35.5, "%");
        provider.recordMetric(metric);
        assertEquals(1, provider.getCapturedMetrics().size());
        assertEquals(35.5, provider.getCapturedMetrics().get(0).getValue(), 0.001);

        provider.setUserId("user_123");
        assertEquals("user_123", provider.getUserId());

        provider.setUserProperty("plan", "pro");
        assertEquals("pro", provider.getUserProperties().get("plan"));

        provider.clear();
        assertEquals(0, provider.getCapturedEvents().size());
        assertEquals(0, provider.getCapturedMetrics().size());
        assertNull(provider.getUserId());
    }

    @Test
    public void testCompositeAnalyticsProvider() {
        CompositeAnalyticsProvider composite = new CompositeAnalyticsProvider();
        MemoryAnalyticsProvider p1 = new MemoryAnalyticsProvider();
        MemoryAnalyticsProvider p2 = new MemoryAnalyticsProvider();

        composite.addProvider(p1);
        composite.addProvider(p2);
        assertEquals(2, composite.getProviderCount());

        composite.trackEvent(new AnalyticsEvent("multi_track"));
        assertEquals(1, p1.getCapturedEvents().size());
        assertEquals(1, p2.getCapturedEvents().size());

        composite.recordMetric(new PerformanceMetric("ram", 1024, "MB"));
        assertEquals(1, p1.getCapturedMetrics().size());
        assertEquals(1, p2.getCapturedMetrics().size());

        composite.removeProvider(p2);
        assertEquals(1, composite.getProviderCount());
    }

    @Test
    public void testNativeAnalyticsService() throws Exception {
        NativeAnalyticsService service = new NativeAnalyticsService();
        service.initialize();
        service.start();

        assertNotNull(service.getCompositeProvider());
        assertNotNull(service.getDefaultMemoryProvider());
        assertNotNull(service.getSessionTracker());
        assertNotNull(service.getPerformanceTracker());

        Map<String, Object> params = new HashMap<>();
        params.put("category", "checkout");
        service.trackEvent("purchase", params);

        service.recordMetric("checkout_latency", 250, "ms");

        assertEquals(1, service.getDefaultMemoryProvider().getCapturedEvents().size());
        assertTrue(service.getDefaultMemoryProvider().getCapturedMetrics().size() >= 1);

        service.stop();
        service.dispose();
    }

    @Test
    public void testAnalyticsCommandHandler() {
        NativeAnalyticsService service = new NativeAnalyticsService();
        AnalyticsCommandHandler handler = new AnalyticsCommandHandler(service);

        assertEquals("analytics", handler.getPath());
        assertEquals("*", handler.getAction());
        assertEquals("analytics", handler.getRoutePrefix());
        assertEquals(service, handler.getAnalyticsService());

        // TrackEvent action
        Map<String, Object> params = new HashMap<>();
        params.put("name", "app_start");
        NativeApiRequest reqTrack = new NativeApiRequest("req_1", "analytics", "trackEvent", params, System.currentTimeMillis());
        NativeApiResponse respTrack = handler.handleRequest(reqTrack);
        assertTrue(respTrack.isSuccess());
        assertEquals("tracked", respTrack.getData().get("status"));

        // RecordMetric action
        Map<String, Object> metricParams = new HashMap<>();
        metricParams.put("name", "memory_usage");
        metricParams.put("value", 128.5);
        metricParams.put("unit", "MB");
        NativeApiRequest reqMetric = new NativeApiRequest("req_2", "analytics", "recordMetric", metricParams, System.currentTimeMillis());
        NativeApiResponse respMetric = handler.handleRequest(reqMetric);
        assertTrue(respMetric.isSuccess());
        assertEquals("recorded", respMetric.getData().get("status"));

        // Session actions
        NativeApiRequest reqStartSession = new NativeApiRequest("req_3", "analytics", "startSession", null, System.currentTimeMillis());
        NativeApiResponse respStartSession = handler.handleRequest(reqStartSession);
        assertTrue(respStartSession.isSuccess());

        NativeApiRequest reqEndSession = new NativeApiRequest("req_4", "analytics", "endSession", null, System.currentTimeMillis());
        NativeApiResponse respEndSession = handler.handleRequest(reqEndSession);
        assertTrue(respEndSession.isSuccess());

        // Trace actions
        Map<String, Object> traceParams = new HashMap<>();
        traceParams.put("traceName", "render_screen");
        NativeApiRequest reqStartTrace = new NativeApiRequest("req_5", "analytics", "startTrace", traceParams, System.currentTimeMillis());
        assertTrue(handler.handleRequest(reqStartTrace).isSuccess());

        NativeApiRequest reqStopTrace = new NativeApiRequest("req_6", "analytics", "stopTrace", traceParams, System.currentTimeMillis());
        assertTrue(handler.handleRequest(reqStopTrace).isSuccess());

        // Async callback test
        AtomicBoolean callbackSuccess = new AtomicBoolean(false);
        handler.handle(reqTrack, new IApiCallback() {
            @Override
            public void onResponse(NativeApiResponse response) {
                callbackSuccess.set(response.isSuccess());
            }

            @Override
            public void onError(NativeApiError error) {}
        });
        assertTrue(callbackSuccess.get());

        // Unknown action error
        NativeApiRequest reqUnknown = new NativeApiRequest("req_err", "analytics", "unknownAction", null, System.currentTimeMillis());
        NativeApiResponse respUnknown = handler.handleRequest(reqUnknown);
        assertFalse(respUnknown.isSuccess());
    }

    @Test
    public void testDiskBufferAnalyticsProvider() {
        DiskBufferAnalyticsProvider provider = new DiskBufferAnalyticsProvider(null);
        provider.initialize();

        AnalyticsEvent event = new AnalyticsEvent("disk_buffered_event");
        provider.trackEvent(event);
        assertFalse(provider.getBufferedEvents().isEmpty());

        PerformanceMetric metric = new PerformanceMetric("disk_write_latency", 12.4, "ms");
        provider.recordMetric(metric);
        assertFalse(provider.getBufferedMetrics().isEmpty());

        provider.setUserId("user_disk_99");
        provider.setUserProperty("tier", "enterprise");

        provider.flush();
        provider.clear();
        assertTrue(provider.getBufferedEvents().isEmpty());
        assertTrue(provider.getBufferedMetrics().isEmpty());
    }

    @Test
    public void testBatchUploader() throws Exception {
        EventQueue queue = new EventQueue(100);
        AtomicBoolean uploadCalled = new AtomicBoolean(false);

        BatchUploader uploader = new BatchUploader(queue, 500, 2, batch -> uploadCalled.set(true));
        uploader.start();
        assertTrue(uploader.isRunning());

        queue.enqueue(new AnalyticsEvent("ev_1"));
        queue.enqueue(new AnalyticsEvent("ev_2"));
        uploader.onEventAdded();

        uploader.flush();
        assertTrue(uploadCalled.get());

        uploader.stop();
        assertFalse(uploader.isRunning());
    }
}
