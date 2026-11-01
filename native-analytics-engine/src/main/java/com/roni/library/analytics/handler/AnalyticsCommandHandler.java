package com.roni.library.analytics.handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.roni.library.analytics.service.NativeAnalyticsService;
import com.roni.library.contracts.analytics.AnalyticsEvent;
import com.roni.library.contracts.api.IApiCallback;
import com.roni.library.contracts.api.IApiHandler;
import com.roni.library.contracts.api.NativeApiError;
import com.roni.library.contracts.api.NativeApiRequest;
import com.roni.library.contracts.api.NativeApiResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * RPC Command Handler for NativeAnalyticsEngine.
 * Implements {@link IApiHandler} to allow Layer 3 WebBridge and external clients
 * to invoke analytics capabilities via structured API requests.
 *
 * @author infoadmstudio2020-cyber
 */
public class AnalyticsCommandHandler implements IApiHandler {

    public static final String ROUTE_PREFIX = "analytics";
    private final NativeAnalyticsService analyticsService;

    public AnalyticsCommandHandler(@NonNull NativeAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @NonNull
    public String getRoutePrefix() {
        return ROUTE_PREFIX;
    }

    @NonNull
    @Override
    public String getPath() {
        return ROUTE_PREFIX;
    }

    @NonNull
    @Override
    public String getAction() {
        return "*";
    }

    @NonNull
    @Override
    public NativeApiResponse handleRequest(@NonNull NativeApiRequest request) {
        if (request == null) {
            NativeApiError err = new NativeApiError("INVALID_REQUEST", "Request cannot be null");
            return NativeApiResponse.error("unknown", 400, err);
        }

        String action = request.getAction();
        String requestId = request.getRequestId() != null ? request.getRequestId() : "req_" + System.currentTimeMillis();

        if (action == null || action.trim().isEmpty()) {
            NativeApiError err = new NativeApiError("MISSING_ACTION", "Action must be specified");
            return NativeApiResponse.error(requestId, 400, err);
        }

        try {
            switch (action) {
                case "trackEvent": {
                    String eventName = request.getStringParam("name");
                    if (eventName == null || eventName.trim().isEmpty()) {
                        NativeApiError err = new NativeApiError("INVALID_PARAM", "Parameter 'name' is required");
                        return NativeApiResponse.error(requestId, 400, err);
                    }
                    @SuppressWarnings("unchecked")
                    Map<String, Object> params = (Map<String, Object>) request.getParameter("parameters");
                    analyticsService.trackEvent(eventName, params);

                    Map<String, Object> data = new HashMap<>();
                    data.put("status", "queued");
                    data.put("event", eventName);
                    return NativeApiResponse.success(requestId, data);
                }

                case "recordMetric": {
                    String metricName = request.getStringParam("name");
                    Object valObj = request.getParameter("value");
                    String unit = request.getStringParam("unit");
                    if (metricName == null || valObj == null) {
                        NativeApiError err = new NativeApiError("INVALID_PARAM", "Parameters 'name' and 'value' are required");
                        return NativeApiResponse.error(requestId, 400, err);
                    }
                    double value = valObj instanceof Number ? ((Number) valObj).doubleValue() : Double.parseDouble(valObj.toString());
                    analyticsService.recordMetric(metricName, value, unit != null ? unit : "count");

                    Map<String, Object> data = new HashMap<>();
                    data.put("status", "recorded");
                    data.put("metric", metricName);
                    return NativeApiResponse.success(requestId, data);
                }

                case "startSession": {
                    analyticsService.getSessionTracker().startSession();
                    Map<String, Object> data = new HashMap<>();
                    data.put("sessionId", analyticsService.getSessionTracker().getCurrentSessionId());
                    data.put("startTime", analyticsService.getSessionTracker().getSessionStartTime());
                    return NativeApiResponse.success(requestId, data);
                }

                case "endSession": {
                    analyticsService.getSessionTracker().endSession();
                    Map<String, Object> data = new HashMap<>();
                    data.put("status", "ended");
                    data.put("activeDurationMs", analyticsService.getSessionTracker().getTotalActiveDurationMs());
                    return NativeApiResponse.success(requestId, data);
                }

                case "startTrace": {
                    String traceName = request.getStringParam("name");
                    if (traceName == null) {
                        NativeApiError err = new NativeApiError("INVALID_PARAM", "Parameter 'name' is required");
                        return NativeApiResponse.error(requestId, 400, err);
                    }
                    analyticsService.getPerformanceTracker().startTrace(traceName);
                    Map<String, Object> data = new HashMap<>();
                    data.put("trace", traceName);
                    data.put("status", "started");
                    return NativeApiResponse.success(requestId, data);
                }

                case "stopTrace": {
                    String traceName = request.getStringParam("name");
                    if (traceName == null) {
                        NativeApiError err = new NativeApiError("INVALID_PARAM", "Parameter 'name' is required");
                        return NativeApiResponse.error(requestId, 400, err);
                    }
                    long duration = analyticsService.getPerformanceTracker().stopTrace(traceName);
                    Map<String, Object> data = new HashMap<>();
                    data.put("trace", traceName);
                    data.put("durationMs", duration);
                    return NativeApiResponse.success(requestId, data);
                }

                case "startScreenView": {
                    String screen = request.getStringParam("screenName");
                    if (screen == null) {
                        NativeApiError err = new NativeApiError("INVALID_PARAM", "Parameter 'screenName' is required");
                        return NativeApiResponse.error(requestId, 400, err);
                    }
                    analyticsService.getSessionTracker().startScreenView(screen);
                    Map<String, Object> data = new HashMap<>();
                    data.put("screenName", screen);
                    return NativeApiResponse.success(requestId, data);
                }

                case "endScreenView": {
                    String screen = request.getStringParam("screenName");
                    if (screen == null) {
                        NativeApiError err = new NativeApiError("INVALID_PARAM", "Parameter 'screenName' is required");
                        return NativeApiResponse.error(requestId, 400, err);
                    }
                    long duration = analyticsService.getSessionTracker().endScreenView(screen);
                    Map<String, Object> data = new HashMap<>();
                    data.put("screenName", screen);
                    data.put("durationMs", duration);
                    return NativeApiResponse.success(requestId, data);
                }

                case "getStats": {
                    Map<String, Object> stats = new HashMap<>();
                    stats.put("sessionId", analyticsService.getSessionTracker().getCurrentSessionId());
                    stats.put("sessionActive", analyticsService.getSessionTracker().isSessionActive());
                    stats.put("totalActiveDurationMs", analyticsService.getSessionTracker().getTotalActiveDurationMs());
                    stats.put("memoryEventCount", analyticsService.getDefaultMemoryProvider().getEventCount());
                    stats.put("memoryMetricCount", analyticsService.getDefaultMemoryProvider().getMetricCount());
                    stats.put("offlineQueueCount", analyticsService.getEventQueue().count());
                    return NativeApiResponse.success(requestId, stats);
                }

                case "flush": {
                    analyticsService.flush();
                    Map<String, Object> data = new HashMap<>();
                    data.put("status", "flushed");
                    return NativeApiResponse.success(requestId, data);
                }

                case "clear": {
                    analyticsService.getDefaultMemoryProvider().clear();
                    analyticsService.getEventQueue().clear();
                    analyticsService.getPerformanceTracker().clear();
                    Map<String, Object> data = new HashMap<>();
                    data.put("status", "cleared");
                    return NativeApiResponse.success(requestId, data);
                }

                default: {
                    NativeApiError err = new NativeApiError("UNKNOWN_ACTION", "Action '" + action + "' is not supported");
                    return NativeApiResponse.error(requestId, 404, err);
                }
            }
        } catch (Throwable t) {
            NativeApiError err = new NativeApiError("EXECUTION_ERROR", t.getMessage());
            return NativeApiResponse.error(requestId, 500, err);
        }
    }

    /**
     * Async callback-based invocation handler.
     *
     * @param request the incoming request
     * @param callback callback to notify upon completion
     */
    public void handle(@NonNull NativeApiRequest request, @NonNull IApiCallback callback) {
        if (callback == null) return;
        NativeApiResponse response = handleRequest(request);
        if (response.isSuccess()) {
            callback.onResponse(response);
        } else {
            NativeApiError error = response.getError() != null
                    ? response.getError()
                    : new NativeApiError("UNKNOWN_ERROR", "Unknown execution failure");
            callback.onError(error);
        }
    }

    @NonNull
    public NativeAnalyticsService getAnalyticsService() {
        return analyticsService;
    }
}
