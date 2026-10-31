package com.roni.library.storage.service;

import com.roni.library.service.BaseService;
import com.roni.library.storage.database.StorageDatabase;
import com.roni.library.storage.preferences.EncryptedPreferences;
import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

public class NativeStorageService extends BaseService {
    private final File storageRootDirectory;
    private final String secretKey;
    private final StorageDatabase database;
    private final ConcurrentHashMap<String, EncryptedPreferences> preferencesMap = new ConcurrentHashMap<>();

    public NativeStorageService() {
        this(new File(System.getProperty("java.io.tmpdir", "/tmp"), "NativeStorageEngine"), "NativeStorageDefaultPassphrase2026!");
    }

    public NativeStorageService(File storageRootDirectory, String secretKey) {
        super("NativeStorageService", 10);
        this.storageRootDirectory = storageRootDirectory;
        this.secretKey = secretKey;
        if (this.storageRootDirectory != null && !this.storageRootDirectory.exists()) {
            this.storageRootDirectory.mkdirs();
        }
        File dbDir = new File(this.storageRootDirectory, "databases");
        this.database = new StorageDatabase(dbDir);
    }

    @Override
    public void onInitialize() {
        try {
            super.onInitialize();
        } catch (Exception ignored) {}
    }

    @Override
    public void onDispose() {
        try {
            super.onDispose();
        } catch (Exception ignored) {}
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        if (database != null) {
            database.checkpointAll();
        }
        super.onStop();
    }

    public EncryptedPreferences getPreferences(String name) {
        String safeName = (name != null && !name.trim().isEmpty()) ? name.trim() : "default";
        return preferencesMap.computeIfAbsent(safeName, n -> {
            File prefDir = new File(storageRootDirectory, "preferences");
            return new EncryptedPreferences(n, prefDir, secretKey);
        });
    }

    public StorageDatabase getDatabase() {
        return database;
    }

    public File getStorageRootDirectory() {
        return storageRootDirectory;
    }
}
