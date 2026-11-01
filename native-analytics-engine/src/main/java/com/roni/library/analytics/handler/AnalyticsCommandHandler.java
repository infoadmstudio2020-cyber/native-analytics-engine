
package com.roni.library.analytics.handler;

import androidx.annotation.NonNull;

import com.roni.library.analytics.service.NativeAnalyticsService;
import com.roni.library.contracts.api.IApiCallback;
import com.roni.library.contracts.api.IApiHandler;
import com.roni.library.contracts.api.NativeApiError;
import com.roni.library.contracts.api.NativeApiRequest;
import com.roni.library.contracts.api.NativeApiResponse;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * RPC Command Handler for NativeAnalyticsEngine.
 *
 * Implements IApiHandler to allow Layer 3 WebBridge
 * and external clients to invoke analytics capabilities
 * via structured API requests.
 *
 * @author infoadmstudio2020-cyber
 */
public class AnalyticsCommandHandler implements IApiHandler {

    public static final String ROUTE_PREFIX = "analytics";

    private final NativeAnalyticsService analyticsService;

    public AnalyticsCommandHandler(
            @NonNull NativeAnalyticsService analyticsService) {

        if (analyticsService == null) {
            throw new IllegalArgumentException(
                    "NativeAnalyticsService cannot be null"
            );
        }

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
    public NativeApiResponse handleRequest(
            @NonNull NativeApiRequest request) {

        if (request == null) {
            return error(
                    "unknown",
                    400,
                    "INVALID_REQUEST",
                    "Request cannot be null"
            );
        }

        String requestId = request.getRequestId();
        String action = request.getAction();

        if (action == null || action.trim().isEmpty()) {
            return error(
                    requestId,
                    400,
                    "MISSING_ACTION",
                    "Action must be specified"
            );
        }

        try {
            switch (action) {

                case "trackEvent": {

                    String eventName =
                            request.getStringParam("name", "");

                    if (eventName.trim().isEmpty()) {
                        return error(
                                requestId,
                                400,
                                "INVALID_PARAM",
                                "Parameter 'name' is required"
                        );
                    }

                    Object paramsObject =
                            request.getParameter("parameters");

                    Map<String, Object> params = null;

                    if (paramsObject instanceof Map) {
                        params = new HashMap<>();

                        Map<?, ?> source =
                                (Map<?, ?>) paramsObject;

                        for (Map.Entry<?, ?> entry :
                                source.entrySet()) {

                            if (entry.getKey() instanceof String) {
                                params.put(
                                        (String) entry.getKey(),
                                        entry.getValue()
                                );
                            }
                        }
                    }

                    analyticsService.trackEvent(
                            eventName.trim(),
                            params
                    );

                    Map<String, Object> data = new HashMap<>();
                    data.put("status", "queued");
                    data.put("event", eventName.trim());

                    return NativeApiResponse.success(
                            requestId,
                            data
                    );
                }

                case "recordMetric": {

                    String metricName =
                            request.getStringParam("name", "");

                    Object valObj =
                            request.getParameter("value");

                    String unit =
                            request.getStringParam("unit", "count");

                    if (metricName.trim().isEmpty()
                            || !(valObj instanceof Number)) {

                        return error(
                                requestId,
                                400,
                                "INVALID_PARAM",
                                "Valid 'name' and numeric 'value' are required"
                        );
                    }

                    double value =
                            ((Number) valObj).doubleValue();

                    if (Double.isNaN(value)
                            || Double.isInfinite(value)) {

                        return error(
                                requestId,
                                400,
                                "INVALID_PARAM",
                                "Metric value must be finite"
                        );
                    }

                    analyticsService.recordMetric(
                            metricName.trim(),
                            value,
                            unit
                    );

                    Map<String, Object> data = new HashMap<>();
                    data.put("status", "recorded");
                    data.put("metric", metricName.trim());

                    return NativeApiResponse.success(
                            requestId,
                            data
                    );
                }

                case "startSession": {

                    analyticsService.getSessionTracker()
                            .startSession();

                    Map<String, Object> data = new HashMap<>();

                    data.put(
                            "sessionId",
                            analyticsService.getSessionTracker()
                                    .getCurrentSessionId()
                    );

                    data.put(
                            "startTime",
                            analyticsService.getSessionTracker()
                                    .getSessionStartTime()
                    );

                    return NativeApiResponse.success(
                            requestId,
                            data
                    );
                }

                case "endSession": {

                    analyticsService.getSessionTracker()
                            .endSession();

                    Map<String, Object> data = new HashMap<>();

                    data.put("status", "ended");

                    data.put(
                            "activeDurationMs",
                            analyticsService.getSessionTracker()
                                    .getTotalActiveDurationMs()
                    );

                    return NativeApiResponse.success(
                            requestId,
                            data
                    );
                }

                case "startTrace": {

                    String traceName =
                            request.getStringParam("name", "");

                    if (traceName.trim().isEmpty()) {
                        return error(
                                requestId,
                                400,
                                "INVALID_PARAM",
                                "Parameter 'name' is required"
                        );
                    }

                    analyticsService.getPerformanceTracker()
                            .startTrace(traceName.trim());

                    Map<String, Object> data = new HashMap<>();
                    data.put("trace", traceName.trim());
                    data.put("status", "started");

                    return NativeApiResponse.success(
                            requestId,
                            data
                    );
                }

                case "stopTrace": {

                    String traceName =
                            request.getStringParam("name", "");

                    if (traceName.trim().isEmpty()) {
                        return error(
                                requestId,
                                400,
                                "INVALID_PARAM",
                                "Parameter 'name' is required"
                        );
                    }

                    long duration =
                            analyticsService.getPerformanceTracker()
                                    .stopTrace(traceName.trim());

                    Map<String, Object> data = new HashMap<>();
                    data.put("trace", traceName.trim());
                    data.put("durationMs", duration);

                    return NativeApiResponse.success(
                            requestId,
                            data
                    );
                }

                case "startScreenView": {

                    String screen =
                            request.getStringParam("screenName", "");

                    if (screen.trim().isEmpty()) {
                        return error(
                                requestId,
                                400,
                                "INVALID_PARAM",
                                "Parameter 'screenName' is required"
                        );
                    }

                    analyticsService.getSessionTracker()
                            .startScreenView(screen.trim());

                    Map<String, Object> data = new HashMap<>();
                    data.put("screenName", screen.trim());

                    return NativeApiResponse.success(
                            requestId,
                            data
                    );
                }

                case "endScreenView": {

                    String screen =
                            request.getStringParam("screenName", "");

                    if (screen.trim().isEmpty()) {
                        return error(
                                requestId,
                                400,
                                "INVALID_PARAM",
                                "Parameter 'screenName' is required"
                        );
                    }

                    long duration =
                            analyticsService.getSessionTracker()
                                    .endScreenView(screen.trim());

                    Map<String, Object> data = new HashMap<>();
                    data.put("screenName", screen.trim());
                    data.put("durationMs", duration);

                    return NativeApiResponse.success(
                            requestId,
                            data
                    );
                }

                case "getStats": {

                    Map<String, Object> stats = new HashMap<>();

                    stats.put(
                            "sessionId",
                            analyticsService.getSessionTracker()
                                    .getCurrentSessionId()
                    );

                    stats.put(
                            "sessionActive",
                            analyticsService.getSessionTracker()
                                    .isSessionActive()
                    );

                    stats.put(
                            "totalActiveDurationMs",
                            analyticsService.getSessionTracker()
                                    .getTotalActiveDurationMs()
                    );

                    stats.put(
                            "memoryEventCount",
                            analyticsService.getDefaultMemoryProvider()
                                    .getEventCount()
                    );

                    stats.put(
                            "memoryMetricCount",
                            analyticsService.getDefaultMemoryProvider()
                                    .getMetricCount()
                    );

                    stats.put(
                            "offlineQueueCount",
                            analyticsService.getEventQueue().count()
                    );

                    return NativeApiResponse.success(
                            requestId,
                            stats
                    );
                }

                case "flush": {

                    analyticsService.flush();

                    Map<String, Object> data = new HashMap<>();
                    data.put("status", "flushed");

                    return NativeApiResponse.success(
                            requestId,
                            data
                    );
                }

                case "clear": {

                    analyticsService.getDefaultMemoryProvider()
                            .clear();

                    analyticsService.getEventQueue().clear();

                    analyticsService.getPerformanceTracker()
                            .clear();

                    Map<String, Object> data = new HashMap<>();
                    data.put("status", "cleared");

                    return NativeApiResponse.success(
                            requestId,
                            data
                    );
                }

                default:

                    return error(
                            requestId,
                            404,
                            "UNKNOWN_ACTION",
                            "Action '" + action
                                    + "' is not supported"
                    );
            }

        } catch (Exception e) {

            String message = e.getMessage();

            return error(
                    requestId,
                    500,
                    "EXECUTION_ERROR",
                    message != null
                            ? message
                            : "Analytics command execution failed"
            );
        }
    }

    /**
     * Creates a standardized API error response.
     */
    @NonNull
    private NativeApiResponse error(
            @NonNull String requestId,
            int statusCode,
            @NonNull String errorCode,
            @NonNull String message) {

        return NativeApiResponse.error(
                requestId,
                statusCode,
                new NativeApiError(errorCode, message)
        );
    }

    /**
     * Async callback-based invocation handler.
     */
    public void handle(
            @NonNull NativeApiRequest request,
            @NonNull IApiCallback callback) {

        if (callback == null) {
            return;
        }

        NativeApiResponse response = handleRequest(request);

        if (response.isSuccess()) {
            callback.onResponse(response);
        } else {

            NativeApiError error = response.getError();

            if (error != null) {
                callback.onError(error);
            } else {
                callback.onError(
                        new NativeApiError(
                                "UNKNOWN_ERROR",
                                "Unknown execution failure"
                        )
                );
            }
        }
    }

    @NonNull
    public NativeAnalyticsService getAnalyticsService() {
        return analyticsService;
    }
}
