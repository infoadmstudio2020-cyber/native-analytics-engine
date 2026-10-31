package com.roni.library.storage;

import android.content.Context;
import com.roni.library.storage.database.StorageDatabase;
import com.roni.library.storage.handler.StorageCommandHandler;
import com.roni.library.storage.preferences.EncryptedPreferences;
import com.roni.library.storage.service.NativeStorageService;

public final class StorageEngine {
    private StorageEngine() {}

    public static NativeStorageEngine getInstance() {
        return NativeStorageEngine.getInstance();
    }

    public static NativeStorageEngineResult<Boolean> init(Context context, NativeStorageEngineConfig config) {
        return NativeStorageEngine.getInstance().init(context, config);
    }

    public static boolean isInitialized() {
        return NativeStorageEngine.getInstance().isInitialized();
    }

    public static EncryptedPreferences getPreferences(String name) {
        return NativeStorageEngine.getInstance().getPreferences(name);
    }

    public static StorageDatabase getDatabase() {
        return NativeStorageEngine.getInstance().getDatabase();
    }

    public static EncryptedStore getEncryptedStore() {
        return NativeStorageEngine.getInstance().getEncryptedStore();
    }

    public static KeyValueStore getKeyValueStore() {
        return NativeStorageEngine.getInstance().getKeyValueStore();
    }

    public static LruDiskCache getDiskCache() {
        return NativeStorageEngine.getInstance().getDiskCache();
    }

    public static DatabaseHelper getDatabaseHelper() {
        return NativeStorageEngine.getInstance().getDatabaseHelper();
    }

    public static NativeStorageService getService() {
        return NativeStorageEngine.getInstance().getService();
    }

    public static StorageCommandHandler getCommandHandler() {
        return NativeStorageEngine.getInstance().getCommandHandler();
    }

    public static void release() {
        NativeStorageEngine.getInstance().release();
    }
}
