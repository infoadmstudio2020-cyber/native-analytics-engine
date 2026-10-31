package com.roni.library.analytics.handler;

import com.roni.library.analytics.service.NativeAnalyticsService;
import com.roni.library.contracts.analytics.AnalyticsEvent;
import com.roni.library.contracts.analytics.PerformanceMetric;
import com.roni.library.contracts.api.IApiCallback;
import com.roni.library.contracts.api.IApiHandler;
import com.roni.library.contracts.api.NativeApiError;
import com.roni.library.contracts.api.NativeApiRequest;
import com.roni.library.contracts.api.NativeApiResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Command handler processing analytics API requests via the standard IApiHandler contract.
 */
public class AnalyticsCommandHandler implements IApiHandler {
    private final NativeAnalyticsService service;

    public AnalyticsCommandHandler(NativeAnalyticsService service) {
        this.service = service;
    }

    @Override
    public String getPath() {
        return "analytics";
    }

    @Override
    public String getAction() {
        return "*";
    }

    public String getRoutePrefix() {
        return "analytics";
    }

    public NativeAnalyticsService getAnalyticsService() {
        return service;
    }

    @Override
    public NativeApiResponse handleRequest(NativeApiRequest request) {
        if (request == null) {
            return NativeApiResponse.error("unknown", 400, new NativeApiError("NULL_REQUEST", "Request cannot be null"));
        }

        String reqId = request.getRequestId() != null ? request.getRequestId() : "unknown";
        String action = request.getAction();

        try {
            if ("trackEvent".equalsIgnoreCase(action)) {
                String eventName = request.getStringParam("name", "");
                Map<String, Object> params = request.getParameters();
                service.trackEvent(eventName, params);

                Map<String, Object> data = new HashMap<>();
                data.put("status", "tracked");
                data.put("eventName", eventName);
                return NativeApiResponse.success(reqId, data);
            } else if ("recordMetric".equalsIgnoreCase(action)) {
                String metricName = request.getStringParam("name", "");
                Object valObj = request.getParameter("value");
                double value = 0.0;
                if (valObj instanceof Number) {
                    value = ((Number) valObj).doubleValue();
                } else if (valObj instanceof String) {
                    try {
                        value = Double.parseDouble((String) valObj);
                    } catch (NumberFormatException ignored) {}
                }
                String unit = request.getStringParam("unit", "ms");
                service.recordMetric(metricName, value, unit);

                Map<String, Object> data = new HashMap<>();
                data.put("status", "recorded");
                data.put("metricName", metricName);
                data.put("value", value);
                return NativeApiResponse.success(reqId, data);
            } else if ("startSession".equalsIgnoreCase(action)) {
                service.getSessionTracker().startSession();
                Map<String, Object> data = new HashMap<>();
                data.put("sessionId", service.getSessionTracker().getCurrentSessionId());
                data.put("active", service.getSessionTracker().isSessionActive());
                return NativeApiResponse.success(reqId, data);
            } else if ("endSession".equalsIgnoreCase(action)) {
                long duration = service.getSessionTracker().getTotalActiveDurationMs();
                service.getSessionTracker().endSession();
                Map<String, Object> data = new HashMap<>();
                data.put("totalDurationMs", duration);
                data.put("active", false);
                return NativeApiResponse.success(reqId, data);
            } else if ("startTrace".equalsIgnoreCase(action)) {
                String traceName = request.getStringParam("traceName", "");
                service.getPerformanceTracker().startTrace(traceName);
                Map<String, Object> data = new HashMap<>();
                data.put("traceName", traceName);
                data.put("status", "started");
                return NativeApiResponse.success(reqId, data);
            } else if ("stopTrace".equalsIgnoreCase(action)) {
                String traceName = request.getStringParam("traceName", "");
                long duration = service.getPerformanceTracker().stopTrace(traceName);
                Map<String, Object> data = new HashMap<>();
                data.put("traceName", traceName);
                data.put("durationMs", duration);
                return NativeApiResponse.success(reqId, data);
            } else if ("flush".equalsIgnoreCase(action)) {
                service.flush();
                Map<String, Object> data = new HashMap<>();
                data.put("status", "flushed");
                return NativeApiResponse.success(reqId, data);
            } else if ("getCapturedEvents".equalsIgnoreCase(action)) {
                List<AnalyticsEvent> events = service.getDefaultMemoryProvider().getCapturedEvents();
                Map<String, Object> data = new HashMap<>();
                data.put("count", events.size());
                return NativeApiResponse.success(reqId, data);
            } else {
                return NativeApiResponse.error(reqId, 404, new NativeApiError("UNKNOWN_ACTION", "Action not supported: " + action));
            }
        } catch (Exception e) {
            return NativeApiResponse.error(reqId, 500, new NativeApiError("INTERNAL_ERROR", e.getMessage()));
        }
    }

    public void handle(NativeApiRequest request, IApiCallback callback) {
        if (callback == null) return;
        try {
            NativeApiResponse response = handleRequest(request);
            if (response.isSuccess()) {
                callback.onResponse(response);
            } else {
                callback.onError(response.getError());
            }
        } catch (Throwable t) {
            callback.onError(new NativeApiError("HANDLE_ERROR", t.getMessage()));
        }
    }
}
