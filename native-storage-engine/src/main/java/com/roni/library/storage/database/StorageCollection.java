package com.roni.library.storage.database;

import com.roni.library.storage.StorageException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class StorageCollection {
    private final String name;
    private final File storageDir;
    private final ConcurrentHashMap<String, Document> documents = new ConcurrentHashMap<>();
    private final Set<String> indexedFields = Collections.synchronizedSet(new HashSet<>());

    public StorageCollection(String name, File storageDir) {
        this.name = name;
        this.storageDir = storageDir;
        loadFromDisk();
    }

    public String getName() {
        return name;
    }

    public void createIndex(String field) {
        if (field != null && !field.trim().isEmpty()) {
            indexedFields.add(field.trim());
        }
    }

    public boolean insert(Document document) {
        if (document == null || document.getId() == null) {
            throw new StorageException("INVALID_DOCUMENT", "Document or Document ID cannot be null");
        }
        documents.put(document.getId(), document);
        return true;
    }

    public Document findById(String id) {
        if (id == null) return null;
        return documents.get(id);
    }

    public List<Document> find(QueryFilter filter) {
        return find(filter, null, true, 0, 0);
    }

    public List<Document> find(QueryFilter filter, String sortByField, boolean ascending, int limit, int offset) {
        List<Document> matched = new ArrayList<>();
        for (Document doc : documents.values()) {
            if (filter == null || filter.test(doc)) {
                matched.add(doc);
            }
        }

        if (sortByField != null && !sortByField.isEmpty()) {
            matched.sort((d1, d2) -> {
                Object v1 = "_id".equals(sortByField) ? d1.getId() : d1.get(sortByField);
                Object v2 = "_id".equals(sortByField) ? d2.getId() : d2.get(sortByField);
                if (v1 == null && v2 == null) return 0;
                if (v1 == null) return ascending ? -1 : 1;
                if (v2 == null) return ascending ? 1 : -1;
                if (v1 instanceof Comparable && v2 instanceof Comparable) {
                    try {
                        @SuppressWarnings("unchecked")
                        int c = ((Comparable) v1).compareTo(v2);
                        return ascending ? c : -c;
                    } catch (Exception ignored) {}
                }
                return ascending ? v1.toString().compareTo(v2.toString()) : v2.toString().compareTo(v1.toString());
            });
        }

        int start = Math.max(0, offset);
        if (start >= matched.size()) {
            return Collections.emptyList();
        }
        int end = limit > 0 ? Math.min(matched.size(), start + limit) : matched.size();
        return new ArrayList<>(matched.subList(start, end));
    }

    public boolean update(String id, Map<String, Object> fieldsToUpdate) {
        if (id == null || fieldsToUpdate == null) return false;
        Document existing = documents.get(id);
        if (existing == null) return false;
        existing.putAll(fieldsToUpdate);
        return true;
    }

    public boolean delete(String id) {
        if (id == null) return false;
        return documents.remove(id) != null;
    }

    public int count(QueryFilter filter) {
        if (filter == null) return documents.size();
        int c = 0;
        for (Document doc : documents.values()) {
            if (filter.test(doc)) {
                c++;
            }
        }
        return c;
    }

    public synchronized void checkpoint() {
        if (storageDir == null) return;
        try {
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }
            File file = new File(storageDir, name + ".col");
            File tempFile = new File(storageDir, name + ".col.tmp");
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(tempFile))) {
                oos.writeObject(new ArrayList<>(documents.values()));
            }
            if (tempFile.exists()) {
                if (file.exists()) file.delete();
                tempFile.renameTo(file);
            }
        } catch (Exception e) {
            throw new StorageException("CHECKPOINT_FAILED", "Failed to checkpoint collection " + name, e);
        }
    }

    @SuppressWarnings("unchecked")
    private synchronized void loadFromDisk() {
        if (storageDir == null) return;
        File file = new File(storageDir, name + ".col");
        if (!file.exists()) return;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            List<Document> list = (List<Document>) ois.readObject();
            for (Document doc : list) {
                if (doc != null && doc.getId() != null) {
                    documents.put(doc.getId(), doc);
                }
            }
        } catch (Exception ignored) {}
    }

    public void clear() {
        documents.clear();
    }
}
