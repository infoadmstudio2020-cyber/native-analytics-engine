package com.roni.library.storage;

import com.roni.library.contracts.api.NativeApiRequest;
import com.roni.library.contracts.api.NativeApiResponse;
import com.roni.library.contracts.common.RuntimeState;
import com.roni.library.storage.crypto.CryptoUtils;
import com.roni.library.storage.database.Document;
import com.roni.library.storage.database.QueryFilter;
import com.roni.library.storage.database.StorageCollection;
import com.roni.library.storage.database.StorageDatabase;
import com.roni.library.storage.handler.StorageCommandHandler;
import com.roni.library.storage.preferences.EncryptedPreferences;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;

public class StorageEngineTest {
    private File tempDir;

    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("storage_engine_test").toFile();
        NativeStorageEngine.getInstance().release();
    }

    @After
    public void tearDown() {
        NativeStorageEngine.getInstance().release();
        if (tempDir != null && tempDir.exists()) {
            deleteRecursive(tempDir);
        }
    }

    private void deleteRecursive(File f) {
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            if (files != null) {
                for (File child : files) deleteRecursive(child);
            }
        }
        f.delete();
    }

    @Test
    public void testEngineInitializationLifecycle() {
        NativeStorageEngine engine = NativeStorageEngine.getInstance();
        assertFalse(engine.isInitialized());

        NativeStorageEngineConfig config = new NativeStorageEngineConfig.Builder()
                .debugEnabled(true)
                .tag("TestStorage")
                .storageDirectory(tempDir)
                .encryptionPassword("TestSecretKey123!")
                .build();

        NativeStorageEngineResult<Boolean> initResult = engine.init(null, config);
        assertTrue(initResult.isSuccess());
        assertTrue(engine.isInitialized());
        assertEquals(RuntimeState.READY, engine.getRuntimeState());
        assertEquals("TestStorage", engine.getConfig().getTag());

        // Re-init guard test
        NativeStorageEngineResult<Boolean> reInit = engine.init(null, config);
        assertTrue(reInit.isSuccess());

        // Release test
        engine.release();
        assertFalse(engine.isInitialized());
    }

    @Test
    public void testCryptoUtilsEncryptionDecryption() {
        String secret = "VeryStrongPassword123!";
        String plain = "Hello, NativeRuntime Secure World!";

        String encrypted = CryptoUtils.encrypt(plain, secret);
        assertNotNull(encrypted);
        assertNotEquals(plain, encrypted);

        String decrypted = CryptoUtils.decrypt(encrypted, secret);
        assertEquals(plain, decrypted);
    }

    @Test
    public void testEncryptedPreferences() {
        NativeStorageEngine engine = NativeStorageEngine.getInstance();
        engine.init(null, new NativeStorageEngineConfig.Builder().storageDirectory(tempDir).build());

        EncryptedPreferences prefs = engine.getPreferences("user_session");
        prefs.putString("auth_token", "jwt_token_abc_123");
        prefs.putInt("user_id", 42);
        prefs.putBoolean("is_premium", true);

        assertEquals("jwt_token_abc_123", prefs.getString("auth_token", null));
        assertEquals(42, prefs.getInt("user_id", 0));
        assertTrue(prefs.getBoolean("is_premium", false));

        assertTrue(prefs.contains("auth_token"));
        prefs.remove("auth_token");
        assertFalse(prefs.contains("auth_token"));
    }

    @Test
    public void testStorageDatabaseAndCollectionCRUD() {
        NativeStorageEngine engine = NativeStorageEngine.getInstance();
        engine.init(null, new NativeStorageEngineConfig.Builder().storageDirectory(tempDir).build());

        StorageDatabase db = engine.getDatabase();
        StorageCollection collection = db.getCollection("customers");

        Document doc1 = new Document("cust_1");
        doc1.put("name", "Alice");
        doc1.put("age", 30);
        doc1.put("balance", 1250.50);
        collection.insert(doc1);

        Document doc2 = new Document("cust_2");
        doc2.put("name", "Bob");
        doc2.put("age", 25);
        doc2.put("balance", 500.0);
        collection.insert(doc2);

        Document found = collection.findById("cust_1");
        assertNotNull(found);
        assertEquals("Alice", found.getString("name", ""));
        assertEquals(30, found.getInt("age", 0));

        // Query filter test
        List<Document> filtered = collection.find(QueryFilter.eq("name", "Bob"));
        assertEquals(1, filtered.size());
        assertEquals("cust_2", filtered.get(0).getId());

        // Range query test
        List<Document> older = collection.find(new QueryFilter().gt("age", 26));
        assertEquals(1, older.size());
        assertEquals("Alice", older.get(0).getString("name", ""));

        // Count test
        assertEquals(2, collection.count(QueryFilter.all()));

        // Delete test
        assertTrue(collection.delete("cust_2"));
        assertEquals(1, collection.count(QueryFilter.all()));
    }

    @Test
    public void testDatabaseTransactions() {
        NativeStorageEngine engine = NativeStorageEngine.getInstance();
        engine.init(null, new NativeStorageEngineConfig.Builder().storageDirectory(tempDir).build());

        StorageDatabase db = engine.getDatabase();
        StorageDatabase.Transaction tx = db.beginTransaction();

        tx.insert("orders", new Document("ord_1").put("total", 99.9));
        tx.insert("orders", new Document("ord_2").put("total", 149.9));
        tx.commit();

        assertEquals(2, db.getCollection("orders").count(QueryFilter.all()));
    }

    @Test
    public void testLruDiskCache() {
        File cacheDir = new File(tempDir, "lru");
        LruDiskCache cache = new LruDiskCache(cacheDir, 1024); // 1KB limit

        byte[] payload1 = new byte[400];
        payload1[0] = 1;
        cache.put("key1", payload1);

        byte[] payload2 = new byte[400];
        payload2[0] = 2;
        cache.put("key2", payload2);

        assertTrue(cache.contains("key1"));
        assertTrue(cache.contains("key2"));

        // Inserting payload3 (400 bytes) exceeds 1024 bytes limit -> key1 evicted
        byte[] payload3 = new byte[400];
        payload3[0] = 3;
        cache.put("key3", payload3);

        assertFalse(cache.contains("key1"));
        assertTrue(cache.contains("key2"));
        assertTrue(cache.contains("key3"));
    }

    @Test
    public void testStorageCommandHandlerApi() {
        NativeStorageEngine engine = NativeStorageEngine.getInstance();
        engine.init(null, new NativeStorageEngineConfig.Builder().storageDirectory(tempDir).build());

        StorageCommandHandler handler = engine.getCommandHandler();
        assertEquals("storage", handler.getPath());

        Map<String, Object> params = new HashMap<>();
        params.put("prefName", "api_prefs");
        params.put("key", "test_key");
        params.put("value", "sample_val");

        NativeApiRequest request = new NativeApiRequest("req_1", "storage", "storage.set", params, System.currentTimeMillis());
        NativeApiResponse response = handler.handleRequest(request);
        assertTrue(response.isSuccess());

        NativeApiRequest getReq = new NativeApiRequest("req_2", "storage", "storage.get", params, System.currentTimeMillis());
        NativeApiResponse getResp = handler.handleRequest(getReq);
        assertTrue(getResp.isSuccess());
        assertEquals("sample_val", getResp.getData().get("value"));
    }
}
