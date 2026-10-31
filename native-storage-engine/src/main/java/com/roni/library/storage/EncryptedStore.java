package com.roni.library.storage;

import com.roni.library.storage.preferences.EncryptedPreferences;
import java.util.Map;

public class EncryptedStore {
    private final EncryptedPreferences preferences;

    public EncryptedStore(EncryptedPreferences preferences) {
        this.preferences = preferences;
    }

    public void put(String key, String value) {
        preferences.putString(key, value);
    }

    public String get(String key, String defaultValue) {
        return preferences.getString(key, defaultValue);
    }

    public boolean contains(String key) {
        return preferences.contains(key);
    }

    public boolean remove(String key) {
        return preferences.remove(key);
    }

    public void clear() {
        preferences.clear();
    }

    public Map<String, Object> getAll() {
        return preferences.getAll();
    }
}
