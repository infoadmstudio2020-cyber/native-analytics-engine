package com.roni.library.analytics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;

/**
 * Standardized result wrapper for {@link NativeAnalyticsEngine} operations.
 *
 * @param <T> data payload type
 * @author infoadmstudio2020-cyber
 */
public class NativeAnalyticsEngineResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final boolean success;
    private final T data;
    private final int errorCode;
    private final String errorMessage;

    public NativeAnalyticsEngineResult(boolean success, @Nullable T data, int errorCode, @Nullable String errorMessage) {
        this.success = success;
        this.data = data;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    @NonNull
    public static <T> NativeAnalyticsEngineResult<T> success(@Nullable T data) {
        return new NativeAnalyticsEngineResult<>(true, data, 0, null);
    }

    @NonNull
    public static <T> NativeAnalyticsEngineResult<T> error(int errorCode, @NonNull String errorMessage) {
        return new NativeAnalyticsEngineResult<>(false, null, errorCode, errorMessage);
    }

    @NonNull
    public static <T> NativeAnalyticsEngineResult<T> error(@NonNull String errorMessage) {
        return new NativeAnalyticsEngineResult<>(false, null, -1, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    @Nullable
    public T getData() {
        return data;
    }

    public int getErrorCode() {
        return errorCode;
    }

    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String toString() {
        return "NativeAnalyticsEngineResult{" +
                "success=" + success +
                ", data=" + data +
                ", errorCode=" + errorCode +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
