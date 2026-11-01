package com.roni.library.analytics;

import com.roni.library.analytics.dispatcher.BatchUploader;
import com.roni.library.analytics.handler.AnalyticsCommandHandler;
import com.roni.library.analytics.provider.CompositeAnalyticsProvider;
import com.roni.library.analytics.provider.MemoryAnalyticsProvider;
import com.roni.library.analytics.queue.DiskBufferAnalyticsProvider;
import com.roni.library.analytics.queue.EventQueue;
import com.roni.library.analytics.service.NativeAnalyticsService;
import com.roni.library.analytics.tracker.PerformanceTracker;
import com.roni.library.analytics.tracker.SessionTracker;
import com.roni.library.contracts.analytics.AnalyticsEvent;
import com.roni.library.contracts.analytics.PerformanceMetric;
import com.roni.library.contracts.api.IApiCallback;
import com.roni.library.contracts.api.NativeApiError;
import com.roni.library.contracts.api.NativeApiRequest;
import com.roni.library.contracts.api.NativeApiResponse;
import com.roni.library.contracts.common.RuntimeState;
import com.roni.library.service.ServiceRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/**
 * Comprehensive automated JUnit test suite for {@link NativeAnalyticsEngine}.
 * Verifies lifecycle, state machine, providers, offline event queue,
 * session tracking, performance tracing, RPC routing, and thread safety.
 *
 * @author infoadmstudio2020-cyber
 */
public class NativeAnalyticsEngineTest {

    private File tempDir;

    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("analytics_test_").toFile();
        NativeAnalyticsEngine.getInstance().release();
    }

    @After
    public void tearDown() {
        NativeAnalyticsEngine.getInstance().release();
        if (tempDir != null && tempDir.exists()) {
            deleteRecursively(tempDir);
        }
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }

    // =========================================================================
    // 1. Singleton Lifecycle & State Machine
    // =========================================================================

    @Test
    public void testSingletonInstanceIdentity() {
        NativeAnalyticsEngine instance1 = NativeAnalyticsEngine.getInstance();
        NativeAnalyticsEngine instance2 = NativeAnalyticsEngine.getInstance();
        assertNotNull(instance1);
        assertSame("Singleton must return same instance", instance1, instance2);
    }

    @Test
    public void testInitializationLifecycle() {
        NativeAnalyticsEngine engine = NativeAnalyticsEngine.getInstance();
        assertFalse(engine.isInitialized());

        NativeAnalyticsEngineConfig config = new NativeAnalyticsEngineConfig.Builder()
                .debugEnabled(true)
                .tag("TestTag")
                .timeout(5000)
                .batchSize(20)
                .flushIntervalMs(5000)
                .offlinePersistenceEnabled(true)
                .storageDirectory(tempDir)
                .build();

        NativeAnalyticsEngineResult<Void> initResult = engine.init(null, config);
        assertTrue(initResult.isSuccess());
        assertTrue(engine.isInitialized());
        assertEquals(RuntimeState.READY, engine.getRuntimeState());
        assertNotNull(engine.getConfig());
        assertEquals("TestTag", engine.getConfig().getTag());
        assertEquals(20, engine.getConfig().getBatchSize());

        // Idempotent re-initialization
        NativeAnalyticsEngineResult<Void> secondInit = engine.init(null, config);
        assertTrue(secondInit.isSuccess());
    }

    @Test
    public void testReleaseResetsState() {
        NativeAnalyticsEngine engine = NativeAnalyticsEngine.getInstance();
        NativeAnalyticsEngineConfig config = new NativeAnalyticsEngineConfig.Builder()
                .storageDirectory(tempDir)
                .build();
        engine.init(null, config);
        assertTrue(engine.isInitialized());

        engine.release();
        assertFalse(engine.isInitialized());
        assertEquals(RuntimeState.DISPOSED, engine.getRuntimeState());

        // Subsequent getInstance returns clean instance
        NativeAnalyticsEngine newEngine = NativeAnalyticsEngine.getInstance();
        assertFalse(newEngine.isInitialized());
    }

    @Test(expected = IllegalStateException.class)
    public void testUninitializedAccessThrows() {
        NativeAnalyticsEngine engine = NativeAnalyticsEngine.getInstance();
        engine.trackEvent("test_event", null);
    }

    // =========================================================================
    // 2. Configuration & Result DTOs
    // =========================================================================

    @Test
    public void testConfigDefaults() {
        NativeAnalyticsEngineConfig config = new NativeAnalyticsEngineConfig.Builder().build();
        assertFalse(config.isDebugEnabled());
        assertEquals("NativeAnalyticsEngine", config.getTag());
        assertEquals(30000, config.getTimeout());
        assertEquals(50, config.getBatchSize());
        assertEquals(15000L, config.getFlushIntervalMs());
        assertTrue(config.isOfflinePersistenceEnabled());
        assertNull(config.getStorageDirectory());
    }

    @Test
    public void testResultFactoryMethods() {
        NativeAnalyticsEngineResult<String> successResult = NativeAnalyticsEngineResult.success("HelloData");
        assertTrue(successResult.isSuccess());
        assertEquals("HelloData", successResult.getData());
        assertEquals(0, successResult.getErrorCode());
        assertNull(successResult.getErrorMessage());

        NativeAnalyticsEngineResult<String> errorResult = NativeAnalyticsEngineResult.error(404, "Not Found");
        assertFalse(errorResult.isSuccess());
        assertEquals(404, errorResult.getErrorCode());
        assertEquals("Not Found", errorResult.getErrorMessage());
    }

    // =========================================================================
    // 3. Memory & Composite Providers
    // =========================================================================

    @Test
    public void testMemoryAnalyticsProvider() {
        MemoryAnalyticsProvider provider = new MemoryAnalyticsProvider();
        assertTrue(provider.isInitialized());

        Map<String, Object> params = new HashMap<>();
        params.put("tier", "gold");
        params.put("amount", 99.5);
        AnalyticsEvent event = new AnalyticsEvent("purchase", params);

        provider.trackEvent(event);
        assertEquals(1, provider.getEventCount());
        assertEquals("purchase", provider.getCapturedEvents().get(0).getName());

        PerformanceMetric metric = new PerformanceMetric("render_time", 12.4, "ms");
        provider.recordMetric(metric);
        assertEquals(1, provider.getMetricCount());
        assertEquals(12.4, provider.getCapturedMetrics().get(0).getValue(), 0.001);

        provider.setUserId("user_123");
        assertEquals("user_123", provider.getUserId());

        provider.setUserProperty("env", "staging");
        assertEquals("staging", provider.getUserProperties().get("env"));

        provider.clear();
        assertEquals(0, provider.getEventCount());
        assertEquals(0, provider.getMetricCount());
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

        AnalyticsEvent event = new AnalyticsEvent("login_click");
        composite.trackEvent(event);

        assertEquals(1, p1.getEventCount());
        assertEquals(1, p2.getEventCount());

        composite.removeProvider(p1);
        assertEquals(1, composite.getProviderCount());

        AnalyticsEvent event2 = new AnalyticsEvent("logout_click");
        composite.trackEvent(event2);

        assertEquals(1, p1.getEventCount());
        assertEquals(2, p2.getEventCount());
    }

    // =========================================================================
    // 4. Offline EventQueue & Disk Persistence
    // =========================================================================

    @Test
    public void testEventQueuePersistenceAndDequeue() {
        EventQueue queue = new EventQueue(tempDir);
        assertEquals(0, queue.count());

        for (int i = 0; i < 25; i++) {
            Map<String, Object> p = new HashMap<>();
            p.put("index", i);
            queue.enqueue(new AnalyticsEvent("batch_item_" + i, p));
        }

        assertTrue("Queue count should reflect enqueued items", queue.count() >= 25);

        List<AnalyticsEvent> peeked = queue.peek(5);
        assertEquals(5, peeked.size());

        List<AnalyticsEvent> batch1 = queue.dequeue(10);
        assertEquals(10, batch1.size());
        assertEquals("batch_item_0", batch1.get(0).getName());

        List<AnalyticsEvent> batch2 = queue.dequeue(20);
        assertEquals(15, batch2.size());
        assertEquals(0, queue.count());
    }

    @Test
    public void testDiskBufferAnalyticsProvider() {
        DiskBufferAnalyticsProvider diskProvider = new DiskBufferAnalyticsProvider(tempDir);
        assertTrue(diskProvider.isInitialized());

        diskProvider.trackEvent(new AnalyticsEvent("offline_click"));
        diskProvider.recordMetric(new PerformanceMetric("cpu_load", 45.0, "%"));

        assertTrue(diskProvider.getEventQueue().count() >= 2);
        diskProvider.clear();
        assertEquals(0, diskProvider.getEventQueue().count());
    }

    // =========================================================================
    // 5. PerformanceTracker
    // =========================================================================

    @Test
    public void testPerformanceTrackerTraces() throws InterruptedException {
        MemoryAnalyticsProvider mem = new MemoryAnalyticsProvider();
        PerformanceTracker tracker = new PerformanceTracker(mem);

        tracker.startTrace("db_query");
        assertTrue(tracker.hasActiveTrace("db_query"));

        Thread.sleep(15);
        long duration = tracker.stopTrace("db_query");
        assertTrue("Duration should be >= 10ms", duration >= 10);
        assertFalse(tracker.hasActiveTrace("db_query"));

        assertEquals(1, mem.getMetricCount());
        PerformanceMetric metric = mem.getCapturedMetrics().get(0);
        assertEquals("db_query_duration", metric.getMetricName());
        assertEquals("ms", metric.getUnit());

        tracker.recordBridgeLatency("invokeNative", 8);
        tracker.recordWebFirstPaint(250);
        tracker.recordHttpResponseTime("/api/v1/auth", 200, 120);

        assertEquals(4, mem.getMetricCount());
    }

    // =========================================================================
    // 6. SessionTracker
    // =========================================================================

    @Test
    public void testSessionTrackerLifecycle() throws InterruptedException {
        MemoryAnalyticsProvider mem = new MemoryAnalyticsProvider();
        SessionTracker sessionTracker = new SessionTracker(mem);

        assertTrue(sessionTracker.isSessionActive());
        assertNotNull(sessionTracker.getCurrentSessionId());
        String initialSessionId = sessionTracker.getCurrentSessionId();

        sessionTracker.startScreenView("HomeScreen");
        assertEquals("HomeScreen", sessionTracker.getCurrentScreenName());

        Thread.sleep(10);
        long viewDuration = sessionTracker.endScreenView("HomeScreen");
        assertTrue("Screen view duration should be >= 5ms", viewDuration >= 5);
        assertNull(sessionTracker.getCurrentScreenName());

        sessionTracker.onAppBackground();
        sessionTracker.onAppForeground();
        assertTrue(sessionTracker.getTotalActiveDurationMs() >= 0);

        sessionTracker.endSession();
        assertFalse(sessionTracker.isSessionActive());

        // Verify session events captured in provider
        boolean hasStart = false;
        boolean hasEnd = false;
        boolean hasScreenView = false;
        for (AnalyticsEvent ev : mem.getCapturedEvents()) {
            if ("session_start".equals(ev.getName())) hasStart = true;
            if ("session_end".equals(ev.getName())) hasEnd = true;
            if ("screen_view".equals(ev.getName())) hasScreenView = true;
        }
        assertTrue("Should contain session_start", hasStart);
        assertTrue("Should contain session_end", hasEnd);
        assertTrue("Should contain screen_view", hasScreenView);
    }

    // =========================================================================
    // 7. NativeAnalyticsService & Ecosystem Registration
    // =========================================================================

    @Test
    public void testNativeAnalyticsService() {
        NativeAnalyticsEngineConfig config = new NativeAnalyticsEngineConfig.Builder()
                .storageDirectory(tempDir)
                .build();
        NativeAnalyticsService service = new NativeAnalyticsService(null, config);

        assertEquals("NativeAnalyticsService", service.getServiceId());
        service.initialize();
        service.start();
        assertTrue(service.isRunning());

        service.trackEvent("test_event_service", Collections.singletonMap("k", "v"));
        service.recordMetric("test_metric_service", 42.0, "units");
        service.flush();

        // Verify in default memory provider
        MemoryAnalyticsProvider mem = service.getDefaultMemoryProvider();
        // Wait briefly for telemetry dispatcher background thread
        try { Thread.sleep(50); } catch (InterruptedException ignored) {}

        assertTrue("Event should be dispatched to memory provider", mem.getEventCount() > 0);

        // Test registering in ServiceRegistry
        ServiceRegistry registry = ServiceRegistry.getInstance();
        service.registerInRegistry(registry);
        assertTrue(registry.hasService(com.roni.library.contracts.service.IService.class));

        service.stop();
        service.dispose();
        assertTrue(service.isDisposed());
    }

    // =========================================================================
    // 8. AnalyticsCommandHandler (RPC Integration)
    // =========================================================================

    @Test
    public void testCommandHandlerActions() {
        NativeAnalyticsEngineConfig config = new NativeAnalyticsEngineConfig.Builder()
                .storageDirectory(tempDir)
                .build();
        NativeAnalyticsService service = new NativeAnalyticsService(null, config);
        service.initialize();
        service.start();

        AnalyticsCommandHandler handler = new AnalyticsCommandHandler(service);
        assertEquals("analytics", handler.getRoutePrefix());
        assertEquals("analytics", handler.getPath());
        assertEquals("*", handler.getAction());

        // 1. trackEvent RPC
        NativeApiRequest trackReq = new NativeApiRequest.Builder()
                .action("trackEvent")
                .parameter("name", "rpc_event")
                .parameter("parameters", Collections.singletonMap("source", "webview"))
                .build();
        NativeApiResponse trackResp = handler.handleRequest(trackReq);
        assertTrue(trackResp.isSuccess());

        // 2. recordMetric RPC
        NativeApiRequest metricReq = new NativeApiRequest.Builder()
                .action("recordMetric")
                .parameter("name", "rpc_latency")
                .parameter("value", 34.5)
                .parameter("unit", "ms")
                .build();
        NativeApiResponse metricResp = handler.handleRequest(metricReq);
        assertTrue(metricResp.isSuccess());

        // 3. startTrace & stopTrace RPC
        NativeApiRequest startTraceReq = new NativeApiRequest.Builder()
                .action("startTrace")
                .parameter("name", "rpc_trace")
                .build();
        assertTrue(handler.handleRequest(startTraceReq).isSuccess());

        NativeApiRequest stopTraceReq = new NativeApiRequest.Builder()
                .action("stopTrace")
                .parameter("name", "rpc_trace")
                .build();
        NativeApiResponse stopTraceResp = handler.handleRequest(stopTraceReq);
        assertTrue(stopTraceResp.isSuccess());
        assertNotNull(stopTraceResp.getData().get("durationMs"));

        // 4. getStats RPC
        NativeApiRequest statsReq = new NativeApiRequest.Builder()
                .action("getStats")
                .build();
        NativeApiResponse statsResp = handler.handleRequest(statsReq);
        assertTrue(statsResp.isSuccess());
        assertNotNull(statsResp.getData().get("sessionId"));

        // 5. Invalid action RPC
        NativeApiRequest invalidReq = new NativeApiRequest.Builder()
                .action("non_existent_action")
                .build();
        NativeApiResponse invalidResp = handler.handleRequest(invalidReq);
        assertFalse(invalidResp.isSuccess());
        assertEquals(404, invalidResp.getStatusCode());

        // 6. Async callback handler test
        AtomicBoolean callbackCalled = new AtomicBoolean(false);
        handler.handle(trackReq, new IApiCallback() {
            @Override
            public void onResponse(NativeApiResponse response) {
                callbackCalled.set(true);
            }

            @Override
            public void onError(NativeApiError error) {
                fail("Should not trigger error callback");
            }
        });
        assertTrue(callbackCalled.get());

        service.stop();
        service.dispose();
    }

    // =========================================================================
    // 9. AnalyticsEngine Static Facade Interoperability
    // =========================================================================

    @Test
    public void testAnalyticsEngineFacade() {
        NativeAnalyticsEngineConfig config = new NativeAnalyticsEngineConfig.Builder()
                .storageDirectory(tempDir)
                .build();

        NativeAnalyticsEngineResult<Void> res = AnalyticsEngine.init(null, config);
        assertTrue(res.isSuccess());
        assertTrue(AnalyticsEngine.isInitialized());

        AnalyticsEngine.trackEvent("facade_event", Collections.singletonMap("key", "val"));
        AnalyticsEngine.recordMetric("facade_metric", 100.0, "points");
        AnalyticsEngine.flush();

        assertNotNull(AnalyticsEngine.getSessionTracker());
        assertNotNull(AnalyticsEngine.getPerformanceTracker());
        assertNotNull(AnalyticsEngine.getService());
        assertNotNull(AnalyticsEngine.getCommandHandler());
        assertNotNull(AnalyticsEngine.getEventQueue());

        AnalyticsEngine.release();
        assertFalse(AnalyticsEngine.isInitialized());
    }

    // =========================================================================
    // 10. Concurrency & Multithreading Stress Test
    // =========================================================================

    @Test
    public void testConcurrentEventTracking() throws InterruptedException {
        NativeAnalyticsEngine engine = NativeAnalyticsEngine.getInstance();
        NativeAnalyticsEngineConfig config = new NativeAnalyticsEngineConfig.Builder()
                .storageDirectory(tempDir)
                .build();
        engine.init(null, config);

        int threadCount = 10;
        int eventsPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < eventsPerThread; i++) {
                        Map<String, Object> params = new HashMap<>();
                        params.put("thread", threadId);
                        params.put("seq", i);
                        engine.trackEvent("concurrent_event", params);
                        engine.recordMetric("concurrent_metric_" + threadId, i * 1.5, "cnt");
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue("All threads should complete within 10 seconds", latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        engine.flush();
        // Give background dispatcher time to complete
        Thread.sleep(100);

        MemoryAnalyticsProvider mem = engine.getService().getDefaultMemoryProvider();
        assertTrue("Memory provider should capture events under concurrent load", mem.getEventCount() > 0);
    }
}

