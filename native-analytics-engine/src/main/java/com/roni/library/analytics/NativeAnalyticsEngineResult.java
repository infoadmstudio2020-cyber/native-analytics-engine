package com.roni.library.analytics;

/**
 * Standard operation result model for NativeAnalyticsEngine.
 *
 * @param <T> data type
 */
public class NativeAnalyticsEngineResult<T> {
    private final boolean success;
    private final T data;
    private final String errorMessage;
    private final int errorCode;

    public NativeAnalyticsEngineResult(boolean success, T data, String errorMessage, int errorCode) {
        this.success = success;
        this.data = data;
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
    }

    public static <T> NativeAnalyticsEngineResult<T> success(T data) {
        return new NativeAnalyticsEngineResult<>(true, data, null, 0);
    }

    public static <T> NativeAnalyticsEngineResult<T> failure(int errorCode, String errorMessage) {
        return new NativeAnalyticsEngineResult<>(false, null, errorMessage, errorCode);
    }

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        return "NativeAnalyticsEngineResult{" +
                "success=" + success +
                ", data=" + data +
                ", errorMessage='" + errorMessage + '\'' +
                ", errorCode=" + errorCode +
                '}';
    }
}
