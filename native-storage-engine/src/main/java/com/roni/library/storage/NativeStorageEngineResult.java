package com.roni.library.storage;

import java.io.Serializable;

public class NativeStorageEngineResult<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private final boolean success;
    private final T data;
    private final String errorMessage;
    private final int errorCode;

    public NativeStorageEngineResult(boolean success, T data, String errorMessage, int errorCode) {
        this.success = success;
        this.data = data;
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
    }

    public static <T> NativeStorageEngineResult<T> success(T data) {
        return new NativeStorageEngineResult<>(true, data, null, 0);
    }

    public static <T> NativeStorageEngineResult<T> error(int errorCode, String errorMessage) {
        return new NativeStorageEngineResult<>(false, null, errorMessage, errorCode);
    }

    public static <T> NativeStorageEngineResult<T> error(String errorMessage) {
        return new NativeStorageEngineResult<>(false, null, errorMessage, -1);
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
        return "NativeStorageEngineResult{success=" + success + ", data=" + data +
                ", errorCode=" + errorCode + ", errorMessage='" + errorMessage + "'}";
    }
}
