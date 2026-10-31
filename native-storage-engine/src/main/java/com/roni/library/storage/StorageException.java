package com.roni.library.storage;

public class StorageException extends RuntimeException {
    private final String errorCode;

    public StorageException(String message) {
        super(message);
        this.errorCode = "STORAGE_ERROR";
    }

    public StorageException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode != null ? errorCode : "STORAGE_ERROR";
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "STORAGE_ERROR";
    }

    public StorageException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode != null ? errorCode : "STORAGE_ERROR";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
