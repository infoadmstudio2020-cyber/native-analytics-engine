package com.roni.library.storage;

import com.roni.library.storage.database.Document;
import com.roni.library.storage.database.QueryFilter;
import com.roni.library.storage.database.StorageCollection;
import com.roni.library.storage.database.StorageDatabase;
import java.io.File;
import java.util.List;
import java.util.Map;

public class DatabaseHelper {
    private final StorageDatabase database;

    public DatabaseHelper(File dbDir) {
        this.database = new StorageDatabase(dbDir);
    }

    public StorageDatabase getDatabase() {
        return database;
    }

    public StorageCollection table(String tableName) {
        return database.getCollection(tableName);
    }

    public boolean insert(String tableName, Document document) {
        return table(tableName).insert(document);
    }

    public Document findById(String tableName, String id) {
        return table(tableName).findById(id);
    }

    public List<Document> query(String tableName, QueryFilter filter) {
        return table(tableName).find(filter);
    }

    public boolean update(String tableName, String id, Map<String, Object> fields) {
        return table(tableName).update(id, fields);
    }

    public boolean delete(String tableName, String id) {
        return table(tableName).delete(id);
    }

    public void checkpoint() {
        database.checkpointAll();
    }
}
