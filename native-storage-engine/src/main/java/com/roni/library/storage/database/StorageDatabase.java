package com.roni.library.storage.database;

import com.roni.library.storage.StorageException;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StorageDatabase {
    private final File databaseDir;
    private final ConcurrentHashMap<String, StorageCollection> collections = new ConcurrentHashMap<>();

    public StorageDatabase(File databaseDir) {
        this.databaseDir = databaseDir;
    }

    public StorageCollection getCollection(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new StorageException("INVALID_NAME", "Collection name cannot be empty");
        }
        return collections.computeIfAbsent(name.trim(), n -> new StorageCollection(n, databaseDir));
    }

    public List<String> listCollections() {
        return Collections.unmodifiableList(new ArrayList<>(collections.keySet()));
    }

    public synchronized void checkpointAll() {
        for (StorageCollection col : collections.values()) {
            col.checkpoint();
        }
    }

    public Transaction beginTransaction() {
        return new Transaction(this);
    }

    public static class Transaction {
        private final StorageDatabase db;
        private final List<Runnable> operations = new ArrayList<>();
        private boolean committed = false;

        public Transaction(StorageDatabase db) {
            this.db = db;
        }

        public Transaction insert(String collectionName, Document document) {
            operations.add(() -> db.getCollection(collectionName).insert(document));
            return this;
        }

        public Transaction update(String collectionName, String documentId, Map<String, Object> fields) {
            operations.add(() -> db.getCollection(collectionName).update(documentId, fields));
            return this;
        }

        public Transaction delete(String collectionName, String documentId) {
            operations.add(() -> db.getCollection(collectionName).delete(documentId));
            return this;
        }

        public synchronized void commit() {
            if (committed) return;
            for (Runnable op : operations) {
                op.run();
            }
            db.checkpointAll();
            committed = true;
        }

        public synchronized void rollback() {
            operations.clear();
            committed = true;
        }
    }
}
