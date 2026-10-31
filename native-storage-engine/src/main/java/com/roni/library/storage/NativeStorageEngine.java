package com.roni.library.storage;

import android.content.Context;
import com.roni.library.contracts.common.RuntimeState;
import com.roni.library.service.ServiceRegistry;
import com.roni.library.storage.database.StorageDatabase;
import com.roni.library.storage.handler.StorageCommandHandler;
import com.roni.library.storage.preferences.EncryptedPreferences;
import com.roni.library.storage.service.NativeStorageService;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

public class NativeStorageEngine {
    private static volatile NativeStorageEngine sInstance;

    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private NativeStorageEngineConfig config;
    private Context context;
    private NativeStorageService storageService;
    private StorageCommandHandler commandHandler;
    private EncryptedStore encryptedStore;
    private KeyValueStore keyValueStore;
    private LruDiskCache diskCache;
    private DatabaseHelper databaseHelper;
    private RuntimeState runtimeState = RuntimeState.UNINITIALIZED;

    private NativeStorageEngine() {}

    public static NativeStorageEngine getInstance() {
        if (sInstance == null) {
            synchronized (NativeStorageEngine.class) {
                if (sInstance == null) {
                    sInstance = new NativeStorageEngine();
                }
            }
        }
        return sInstance;
    }

    public synchronized NativeStorageEngineResult<Boolean> init(Context context, NativeStorageEngineConfig config) {
        if (initialized.get()) {
            return NativeStorageEngineResult.success(true);
        }

        this.context = context != null ? context.getApplicationContext() : null;
        this.config = config != null ? config : new NativeStorageEngineConfig.Builder().build();

        File rootDir = this.config.getStorageDirectory();
        if (rootDir == null) {
            if (this.context != null && this.context.getFilesDir() != null) {
                rootDir = new File(this.context.getFilesDir(), "native_storage");
            } else {
                rootDir = new File(System.getProperty("java.io.tmpdir", "/tmp"), "native_storage");
            }
        }
        if (!rootDir.exists()) {
            rootDir.mkdirs();
        }

        this.storageService = new NativeStorageService(rootDir, this.config.getEncryptionPassword());
        this.storageService.onInitialize();
        this.storageService.onStart();

        this.commandHandler = new StorageCommandHandler(this.storageService);
        this.encryptedStore = new EncryptedStore(this.storageService.getPreferences("secure_store"));
        this.keyValueStore = new KeyValueStore("kv_store", new File(rootDir, "kv"));
        this.diskCache = new LruDiskCache(new File(rootDir, "cache"), 20 * 1024 * 1024);
        this.databaseHelper = new DatabaseHelper(new File(rootDir, "databases"));

        try {
            ServiceRegistry.getInstance().registerService(NativeStorageService.class, this.storageService);
        } catch (Throwable ignored) {}

        this.initialized.set(true);
        this.runtimeState = RuntimeState.READY;

        return NativeStorageEngineResult.success(true);
    }

    public boolean isInitialized() {
        return initialized.get();
    }

    public RuntimeState getRuntimeState() {
        return runtimeState;
    }

    public NativeStorageEngineConfig getConfig() {
        return config;
    }

    public NativeStorageService getService() {
        return storageService;
    }

    public StorageCommandHandler getCommandHandler() {
        return commandHandler;
    }

    public EncryptedPreferences getPreferences(String name) {
        checkInitialized();
        return storageService.getPreferences(name);
    }

    public StorageDatabase getDatabase() {
        checkInitialized();
        return storageService.getDatabase();
    }

    public EncryptedStore getEncryptedStore() {
        checkInitialized();
        return encryptedStore;
    }

    public KeyValueStore getKeyValueStore() {
        checkInitialized();
        return keyValueStore;
    }

    public LruDiskCache getDiskCache() {
        checkInitialized();
        return diskCache;
    }

    public DatabaseHelper getDatabaseHelper() {
        checkInitialized();
        return databaseHelper;
    }

    public synchronized void release() {
        if (storageService != null) {
            storageService.onStop();
            storageService.onDispose();
        }
        if (databaseHelper != null) {
            databaseHelper.checkpoint();
        }
        initialized.set(false);
        runtimeState = RuntimeState.TERMINATED;
        sInstance = null;
    }

    private void checkInitialized() {
        if (!initialized.get()) {
            init(null, null);
        }
    }
}
