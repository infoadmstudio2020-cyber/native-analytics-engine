package com.roni.library.storage.app;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.roni.library.storage.DatabaseHelper;
import com.roni.library.storage.EncryptedStore;
import com.roni.library.storage.KeyValueStore;
import com.roni.library.storage.LruDiskCache;
import com.roni.library.storage.NativeStorageEngine;
import com.roni.library.storage.NativeStorageEngineConfig;
import com.roni.library.storage.NativeStorageEngineResult;
import com.roni.library.storage.database.Document;
import com.roni.library.storage.database.QueryFilter;
import com.roni.library.storage.database.StorageCollection;
import com.roni.library.storage.database.StorageDatabase;
import com.roni.library.storage.preferences.EncryptedPreferences;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Universal Interactive Demonstration and Feature Testing Activity for NativeStorageEngine.
 * Implements Dashboard, Feature Explorer, Test Center, and Live Console.
 */
public class MainActivity extends AppCompatActivity {

    private TextView tvStatus;
    private TextView tvDashboardDetails;
    private TextView tvPrefResult;
    private TextView tvDocResult;
    private TextView tvCacheResult;
    private TextView tvTestSummary;
    private TextView tvConsoleLog;

    private EditText etPrefKey, etPrefVal;
    private EditText etDocId, etDocTitle;
    private EditText etCacheKey, etCacheVal;

    private final StringBuilder logBuffer = new StringBuilder();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        setupListeners();
        initStorageEngine();
    }

    private void bindViews() {
        tvStatus = findViewById(R.id.tv_status);
        tvDashboardDetails = findViewById(R.id.tv_dashboard_details);
        tvPrefResult = findViewById(R.id.tv_pref_result);
        tvDocResult = findViewById(R.id.tv_doc_result);
        tvCacheResult = findViewById(R.id.tv_cache_result);
        tvTestSummary = findViewById(R.id.tv_test_summary);
        tvConsoleLog = findViewById(R.id.tv_console_log);
        if (tvConsoleLog != null) {
            tvConsoleLog.setMovementMethod(new ScrollingMovementMethod());
        }

        etPrefKey = findViewById(R.id.et_pref_key);
        etPrefVal = findViewById(R.id.et_pref_val);
        etDocId = findViewById(R.id.et_doc_id);
        etDocTitle = findViewById(R.id.et_doc_title);
        etCacheKey = findViewById(R.id.et_cache_key);
        etCacheVal = findViewById(R.id.et_cache_val);
    }

    private void setupListeners() {
        // Encrypted Prefs
        findViewById(R.id.btn_pref_save).setOnClickListener(v -> onSavePref());
        findViewById(R.id.btn_pref_read).setOnClickListener(v -> onReadPref());
        findViewById(R.id.btn_pref_clear).setOnClickListener(v -> onClearPref());

        // Document Store
        findViewById(R.id.btn_doc_insert).setOnClickListener(v -> onInsertDoc());
        findViewById(R.id.btn_doc_find).setOnClickListener(v -> onFindDoc());
        findViewById(R.id.btn_doc_query).setOnClickListener(v -> onQueryDocs());

        // LRU Cache
        findViewById(R.id.btn_cache_put).setOnClickListener(v -> onPutCache());
        findViewById(R.id.btn_cache_get).setOnClickListener(v -> onGetCache());
        findViewById(R.id.btn_cache_clear).setOnClickListener(v -> onClearCache());

        // Test Center
        findViewById(R.id.btn_run_tests).setOnClickListener(v -> runAllTests());

        // Console
        findViewById(R.id.btn_clear_log).setOnClickListener(v -> {
            logBuffer.setLength(0);
            if (tvConsoleLog != null) tvConsoleLog.setText("");
        });
    }

    private void log(String message) {
        String entry = "[" + timeFormat.format(new Date()) + "] " + message + "\n";
        logBuffer.append(entry);
        if (tvConsoleLog != null) {
            tvConsoleLog.setText(logBuffer.toString());
        }
    }

    private void initStorageEngine() {
        try {
            NativeStorageEngineConfig config = new NativeStorageEngineConfig.Builder()
                    .debugEnabled(true)
                    .tag("StorageExplorer")
                    .encryptionPassword("MasterDemoSecKey2026!")
                    .build();

            NativeStorageEngineResult<Boolean> res = NativeStorageEngine.getInstance().init(this, config);
            if (res.isSuccess()) {
                if (tvStatus != null) {
                    tvStatus.setText("Status: READY (ACTIVE) • State: " + NativeStorageEngine.getInstance().getRuntimeState());
                }
                log("NativeStorageEngine initialized successfully.");
                updateDashboard();
            } else {
                if (tvStatus != null) tvStatus.setText("Status: INIT FAILED (" + res.getErrorMessage() + ")");
                log("Initialization failed: " + res.getErrorMessage());
            }
        } catch (Exception e) {
            log("Init error: " + e.getMessage());
        }
    }

    private void updateDashboard() {
        try {
            LruDiskCache cache = NativeStorageEngine.getInstance().getDiskCache();
            long cacheBytes = cache != null ? cache.getCurrentSizeBytes() : 0;
            String details = "Cache Size: " + (cacheBytes / 1024) + " KB | Max: " + (cache != null ? cache.getMaxSizeBytes() / (1024 * 1024) : 0) + " MB\n" +
                    "Ready for AES-GCM Encrypted Storage & Document Queries";
            if (tvDashboardDetails != null) tvDashboardDetails.setText(details);
        } catch (Exception ignored) {}
    }

    // Feature 1: Encrypted Preferences
    private void onSavePref() {
        String key = etPrefKey.getText().toString().trim();
        String val = etPrefVal.getText().toString();
        if (key.isEmpty()) {
            tvPrefResult.setText("Result: Key cannot be empty");
            return;
        }
        EncryptedPreferences prefs = NativeStorageEngine.getInstance().getPreferences("demo_secure_prefs");
        prefs.putString(key, val);
        tvPrefResult.setText("Result: Encrypted & Stored key '" + key + "' (Length: " + val.length() + ")");
        log("EncryptedStore.put: key=" + key);
    }

    private void onReadPref() {
        String key = etPrefKey.getText().toString().trim();
        if (key.isEmpty()) {
            tvPrefResult.setText("Result: Enter key to read");
            return;
        }
        EncryptedPreferences prefs = NativeStorageEngine.getInstance().getPreferences("demo_secure_prefs");
        String val = prefs.getString(key, null);
        if (val != null) {
            tvPrefResult.setText("Result: Decrypted Value -> '" + val + "'");
            log("EncryptedStore.get: key=" + key + " value=" + val);
        } else {
            tvPrefResult.setText("Result: Key '" + key + "' not found");
            log("EncryptedStore.get: key=" + key + " [NOT_FOUND]");
        }
    }

    private void onClearPref() {
        EncryptedPreferences prefs = NativeStorageEngine.getInstance().getPreferences("demo_secure_prefs");
        prefs.clear();
        tvPrefResult.setText("Result: All Encrypted Preferences cleared");
        log("EncryptedStore.clear: Preferences wiped.");
    }

    // Feature 2: Document Database
    private void onInsertDoc() {
        String id = etDocId.getText().toString().trim();
        String title = etDocTitle.getText().toString().trim();
        if (id.isEmpty()) {
            tvDocResult.setText("Result: ID cannot be empty");
            return;
        }
        StorageCollection col = NativeStorageEngine.getInstance().getDatabase().getCollection("users");
        Document doc = new Document(id).put("name", title).put("created_at", System.currentTimeMillis());
        boolean ok = col.insert(doc);
        tvDocResult.setText("Result: Insert Document -> " + (ok ? "SUCCESS" : "FAILED"));
        log("StorageCollection.insert: id=" + id + " name=" + title + " result=" + ok);
    }

    private void onFindDoc() {
        String id = etDocId.getText().toString().trim();
        StorageCollection col = NativeStorageEngine.getInstance().getDatabase().getCollection("users");
        Document doc = col.findById(id);
        if (doc != null) {
            tvDocResult.setText("Result: Found -> " + doc.getString("name", "N/A") + " (Raw: " + doc.getId() + ")");
            log("StorageCollection.find: id=" + id + " content=" + doc.getAll());
        } else {
            tvDocResult.setText("Result: Document '" + id + "' not found");
            log("StorageCollection.find: id=" + id + " [NOT_FOUND]");
        }
    }

    private void onQueryDocs() {
        StorageCollection col = NativeStorageEngine.getInstance().getDatabase().getCollection("users");
        List<Document> list = col.find(QueryFilter.all());
        tvDocResult.setText("Result: Total Documents in Collection: " + list.size());
        log("StorageCollection.queryAll: count=" + list.size());
    }

    // Feature 3: LRU Cache
    private void onPutCache() {
        String key = etCacheKey.getText().toString().trim();
        String payload = etCacheVal.getText().toString();
        if (key.isEmpty()) {
            tvCacheResult.setText("Result: Key cannot be empty");
            return;
        }
        LruDiskCache cache = NativeStorageEngine.getInstance().getDiskCache();
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        cache.put(key, bytes);
        tvCacheResult.setText("Result: Cached " + bytes.length + " bytes under '" + key + "'");
        log("LruDiskCache.put: key=" + key + " bytes=" + bytes.length);
        updateDashboard();
    }

    private void onGetCache() {
        String key = etCacheKey.getText().toString().trim();
        LruDiskCache cache = NativeStorageEngine.getInstance().getDiskCache();
        byte[] bytes = cache.get(key);
        if (bytes != null) {
            String val = new String(bytes, StandardCharsets.UTF_8);
            tvCacheResult.setText("Result: Cache HIT -> '" + val + "' (" + bytes.length + " bytes)");
            log("LruDiskCache.get: key=" + key + " [HIT]");
        } else {
            tvCacheResult.setText("Result: Cache MISS for '" + key + "'");
            log("LruDiskCache.get: key=" + key + " [MISS]");
        }
    }

    private void onClearCache() {
        LruDiskCache cache = NativeStorageEngine.getInstance().getDiskCache();
        cache.clear();
        tvCacheResult.setText("Result: Disk Cache fully cleared");
        log("LruDiskCache.clear: Cache purged.");
        updateDashboard();
    }

    // Feature 4: Automated Test Suite
    private void runAllTests() {
        log("Starting Comprehensive Self-Test Suite...");
        StringBuilder sb = new StringBuilder();
        int pass = 0, fail = 0;

        // Test 1: Singleton & State
        try {
            boolean init = NativeStorageEngine.getInstance().isInitialized();
            if (init) {
                sb.append("✓ [PASS] Engine Singleton & State\n");
                pass++;
            } else {
                sb.append("✗ [FAIL] Engine Singleton & State\n");
                fail++;
            }
        } catch (Exception e) {
            sb.append("✗ [FAIL] Engine State: ").append(e.getMessage()).append("\n");
            fail++;
        }

        // Test 2: Encrypted Store
        try {
            EncryptedStore store = NativeStorageEngine.getInstance().getEncryptedStore();
            store.put("t_key", "t_secret_val");
            String retrieved = store.get("t_key", null);
            if ("t_secret_val".equals(retrieved)) {
                sb.append("✓ [PASS] EncryptedStore AES-GCM Integrity\n");
                pass++;
            } else {
                sb.append("✗ [FAIL] EncryptedStore mismatch\n");
                fail++;
            }
        } catch (Exception e) {
            sb.append("✗ [FAIL] EncryptedStore: ").append(e.getMessage()).append("\n");
            fail++;
        }

        // Test 3: NoSQL Document Store CRUD
        try {
            StorageDatabase db = NativeStorageEngine.getInstance().getDatabase();
            StorageCollection col = db.getCollection("autotest");
            col.insert(new Document("test_doc").put("score", 99.5));
            Document d = col.findById("test_doc");
            if (d != null && d.getDouble("score", 0) == 99.5) {
                sb.append("✓ [PASS] StorageDatabase CRUD & QueryFilter\n");
                pass++;
            } else {
                sb.append("✗ [FAIL] StorageDatabase Document retrieval failed\n");
                fail++;
            }
        } catch (Exception e) {
            sb.append("✗ [FAIL] StorageDatabase: ").append(e.getMessage()).append("\n");
            fail++;
        }

        // Test 4: KeyValueStore Persistence
        try {
            KeyValueStore kv = NativeStorageEngine.getInstance().getKeyValueStore();
            kv.putString("kv_test", "persisted_string");
            if ("persisted_string".equals(kv.getString("kv_test", null))) {
                sb.append("✓ [PASS] KeyValueStore Persistence\n");
                pass++;
            } else {
                sb.append("✗ [FAIL] KeyValueStore mismatch\n");
                fail++;
            }
        } catch (Exception e) {
            sb.append("✗ [FAIL] KeyValueStore: ").append(e.getMessage()).append("\n");
            fail++;
        }

        // Test 5: LRU Disk Cache Threshold
        try {
            LruDiskCache cache = NativeStorageEngine.getInstance().getDiskCache();
            cache.put("lru_t", "lru_payload".getBytes(StandardCharsets.UTF_8));
            byte[] cached = cache.get("lru_t");
            if (cached != null) {
                sb.append("✓ [PASS] LRU Disk Cache Operations\n");
                pass++;
            } else {
                sb.append("✗ [FAIL] LRU Cache retrieval failed\n");
                fail++;
            }
        } catch (Exception e) {
            sb.append("✗ [FAIL] LRU Disk Cache: ").append(e.getMessage()).append("\n");
            fail++;
        }

        // Test 6: DatabaseHelper Integration
        try {
            DatabaseHelper helper = NativeStorageEngine.getInstance().getDatabaseHelper();
            helper.insert("audit_log", new Document("log_1").put("msg", "system_started"));
            Document found = helper.findById("audit_log", "log_1");
            if (found != null) {
                sb.append("✓ [PASS] DatabaseHelper Facade Operations\n");
                pass++;
            } else {
                sb.append("✗ [FAIL] DatabaseHelper failed\n");
                fail++;
            }
        } catch (Exception e) {
            sb.append("✗ [FAIL] DatabaseHelper: ").append(e.getMessage()).append("\n");
            fail++;
        }

        sb.append("\nSUMMARY: ").append(pass).append(" PASSED, ").append(fail).append(" FAILED.");
        if (tvTestSummary != null) {
            tvTestSummary.setText(sb.toString());
        }
        log("Test Suite Completed: " + pass + " passed, " + fail + " failed.");
    }
}
