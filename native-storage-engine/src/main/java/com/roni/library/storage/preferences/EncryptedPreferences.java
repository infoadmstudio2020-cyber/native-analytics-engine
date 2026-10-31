package com.roni.library.storage.preferences;

import com.roni.library.storage.StorageException;
import com.roni.library.storage.crypto.CryptoUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class EncryptedPreferences {
    private final String name;
    private final File storageFile;
    private final String secretKey;
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    public EncryptedPreferences(String name, File storageDir, String secretKey) {
        this.name = name;
        this.storageFile = storageDir != null ? new File(storageDir, name + ".encpref") : null;
        this.secretKey = (secretKey != null && !secretKey.isEmpty()) ? secretKey : "NativeStorageDefaultPassphrase2026!";
        load();
    }

    public String getName() {
        return name;
    }

    public synchronized void putString(String key, String value) {
        if (key == null) return;
        if (value == null) {
            remove(key);
            return;
        }
        String encrypted = CryptoUtils.encrypt(value, secretKey);
        cache.put(key, encrypted);
        persist();
    }

    public String getString(String key, String defaultValue) {
        if (key == null) return defaultValue;
        String encrypted = cache.get(key);
        if (encrypted == null) return defaultValue;
        try {
            return CryptoUtils.decrypt(encrypted, secretKey);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public void putInt(String key, int value) {
        putString(key, String.valueOf(value));
    }

    public int getInt(String key, int defaultValue) {
        String s = getString(key, null);
        if (s == null) return defaultValue;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public void putLong(String key, long value) {
        putString(key, String.valueOf(value));
    }

    public long getLong(String key, long defaultValue) {
        String s = getString(key, null);
        if (s == null) return defaultValue;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public void putBoolean(String key, boolean value) {
        putString(key, String.valueOf(value));
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String s = getString(key, null);
        if (s == null) return defaultValue;
        return Boolean.parseBoolean(s);
    }

    public boolean contains(String key) {
        return key != null && cache.containsKey(key);
    }

    public synchronized boolean remove(String key) {
        if (key == null) return false;
        boolean removed = cache.remove(key) != null;
        if (removed) persist();
        return removed;
    }

    public synchronized void clear() {
        cache.clear();
        persist();
    }

    public Map<String, Object> getAll() {
        Map<String, Object> res = new HashMap<>();
        for (Map.Entry<String, String> entry : cache.entrySet()) {
            try {
                res.put(entry.getKey(), CryptoUtils.decrypt(entry.getValue(), secretKey));
            } catch (Exception e) {
                res.put(entry.getKey(), "[DECRYPTION_ERROR]");
            }
        }
        return Collections.unmodifiableMap(res);
    }

    private synchronized void persist() {
        if (storageFile == null) return;
        try {
            if (!storageFile.getParentFile().exists()) {
                storageFile.getParentFile().mkdirs();
            }
            Properties props = new Properties();
            props.putAll(cache);
            try (FileOutputStream fos = new FileOutputStream(storageFile)) {
                props.store(fos, "EncryptedPreferences");
            }
        } catch (Exception e) {
            throw new StorageException("PERSISTENCE_FAILED", "Failed to save preferences", e);
        }
    }

    private synchronized void load() {
        if (storageFile == null || !storageFile.exists()) return;
        try (FileInputStream fis = new FileInputStream(storageFile)) {
            Properties props = new Properties();
            props.load(fis);
            for (String key : props.stringPropertyNames()) {
                cache.put(key, props.getProperty(key));
            }
        } catch (Exception ignored) {}
    }
}
