package com.roni.library.storage;

import java.io.File;

public class NativeStorageEngineConfig {
    private final boolean debugEnabled;
    private final String tag;
    private final int timeout;
    private final File storageDirectory;
    private final String encryptionPassword;

    private NativeStorageEngineConfig(Builder builder) {
        this.debugEnabled = builder.debugEnabled;
        this.tag = builder.tag != null ? builder.tag : "NativeStorageEngine";
        this.timeout = builder.timeout > 0 ? builder.timeout : 30000;
        this.storageDirectory = builder.storageDirectory;
        this.encryptionPassword = builder.encryptionPassword != null ? builder.encryptionPassword : "NativeStorageEngineKey2026!";
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public String getTag() {
        return tag;
    }

    public int getTimeout() {
        return timeout;
    }

    public File getStorageDirectory() {
        return storageDirectory;
    }

    public String getEncryptionPassword() {
        return encryptionPassword;
    }

    public static class Builder {
        private boolean debugEnabled = false;
        private String tag = "NativeStorageEngine";
        private int timeout = 30000;
        private File storageDirectory = null;
        private String encryptionPassword = "NativeStorageEngineKey2026!";

        public Builder debugEnabled(boolean debugEnabled) {
            this.debugEnabled = debugEnabled;
            return this;
        }

        public Builder tag(String tag) {
            this.tag = tag;
            return this;
        }

        public Builder timeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder storageDirectory(File storageDirectory) {
            this.storageDirectory = storageDirectory;
            return this;
        }

        public Builder encryptionPassword(String encryptionPassword) {
            this.encryptionPassword = encryptionPassword;
            return this;
        }

        public NativeStorageEngineConfig build() {
            return new NativeStorageEngineConfig(this);
        }
    }
}
