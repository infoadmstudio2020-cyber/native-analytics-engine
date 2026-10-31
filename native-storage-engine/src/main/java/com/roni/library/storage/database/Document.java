package com.roni.library.storage.database;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Document implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final long createdAt;
    private long updatedAt;
    private final Map<String, Object> data;

    public Document() {
        this(UUID.randomUUID().toString());
    }

    public Document(String id) {
        this(id, new HashMap<>());
    }

    public Document(String id, Map<String, Object> initialData) {
        this.id = (id != null && !id.trim().isEmpty()) ? id : UUID.randomUUID().toString();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
        this.data = new HashMap<>();
        if (initialData != null) {
            this.data.putAll(initialData);
        }
    }

    public String getId() {
        return id;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Object get(String key) {
        return data.get(key);
    }

    public String getString(String key, String defaultValue) {
        Object val = data.get(key);
        return val != null ? String.valueOf(val) : defaultValue;
    }

    public int getInt(String key, int defaultValue) {
        Object val = data.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        } else if (val instanceof String) {
            try {
                return Integer.parseInt((String) val);
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    public long getLong(String key, long defaultValue) {
        Object val = data.get(key);
        if (val instanceof Number) {
            return ((Number) val).longValue();
        } else if (val instanceof String) {
            try {
                return Long.parseLong((String) val);
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    public double getDouble(String key, double defaultValue) {
        Object val = data.get(key);
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        } else if (val instanceof String) {
            try {
                return Double.parseDouble((String) val);
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        Object val = data.get(key);
        if (val instanceof Boolean) {
            return (Boolean) val;
        } else if (val instanceof String) {
            return Boolean.parseBoolean((String) val);
        }
        return defaultValue;
    }

    public Document put(String key, Object value) {
        data.put(key, value);
        this.updatedAt = System.currentTimeMillis();
        return this;
    }

    public Document putAll(Map<String, Object> fields) {
        if (fields != null) {
            data.putAll(fields);
            this.updatedAt = System.currentTimeMillis();
        }
        return this;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> copy = new HashMap<>(data);
        copy.put("_id", id);
        copy.put("_createdAt", createdAt);
        copy.put("_updatedAt", updatedAt);
        return Collections.unmodifiableMap(copy);
    }

    public Map<String, Object> getData() {
        return Collections.unmodifiableMap(data);
    }

    @Override
    public String toString() {
        return "Document{id='" + id + "', createdAt=" + createdAt + ", updatedAt=" + updatedAt + ", data=" + data + "}";
    }
}
