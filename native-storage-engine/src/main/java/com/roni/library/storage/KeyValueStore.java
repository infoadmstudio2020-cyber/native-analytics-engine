package com.roni.library.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class KeyValueStore {
    private final String name;
    private final File file;
    private final ConcurrentHashMap<String, String> memory = new ConcurrentHashMap<>();

    public KeyValueStore(String name, File baseDir) {
        this.name = name;
        this.file = baseDir != null ? new File(baseDir, name + ".kv") : null;
        load();
    }

    public String getName() {
        return name;
    }

    public void putString(String key, String value) {
        if (key == null) return;
        if (value == null) {
            memory.remove(key);
        } else {
            memory.put(key, value);
        }
        persist();
    }

    public String getString(String key, String defaultValue) {
        if (key == null) return defaultValue;
        return memory.getOrDefault(key, defaultValue);
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

    public void putBoolean(String key, boolean value) {
        putString(key, String.valueOf(value));
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String s = getString(key, null);
        if (s == null) return defaultValue;
        return Boolean.parseBoolean(s);
    }

    public boolean contains(String key) {
        return key != null && memory.containsKey(key);
    }

    public boolean remove(String key) {
        if (key == null) return false;
        boolean removed = memory.remove(key) != null;
        if (removed) persist();
        return removed;
    }

    public void clear() {
        memory.clear();
        persist();
    }

    public Map<String, String> getAll() {
        return Collections.unmodifiableMap(new HashMap<>(memory));
    }

    private synchronized void persist() {
        if (file == null) return;
        try {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            Properties props = new Properties();
            props.putAll(memory);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                props.store(fos, "KeyValueStore");
            }
        } catch (Exception ignored) {}
    }

    private synchronized void load() {
        if (file == null || !file.exists()) return;
        try (FileInputStream fis = new FileInputStream(file)) {
            Properties props = new Properties();
            props.load(fis);
            for (String k : props.stringPropertyNames()) {
                memory.put(k, props.getProperty(k));
            }
        } catch (Exception ignored) {}
    }
}
