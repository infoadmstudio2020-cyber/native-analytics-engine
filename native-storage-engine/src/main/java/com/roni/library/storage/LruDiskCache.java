package com.roni.library.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

public class LruDiskCache {
    private final File cacheDir;
    private final long maxSizeBytes;
    private final LinkedHashMap<String, Long> lruMap;
    private long currentSizeBytes = 0;

    public LruDiskCache(File cacheDir, long maxSizeBytes) {
        this.cacheDir = cacheDir;
        this.maxSizeBytes = maxSizeBytes > 0 ? maxSizeBytes : 10 * 1024 * 1024; // 10 MB default
        this.lruMap = new LinkedHashMap<>(16, 0.75f, true);
        if (this.cacheDir != null && !this.cacheDir.exists()) {
            this.cacheDir.mkdirs();
        }
        scanExistingFiles();
    }

    public synchronized void put(String key, byte[] data) {
        if (key == null || data == null) return;
        String fileName = hashKey(key);
        File file = new File(cacheDir, fileName);

        // Check if eviction is necessary
        while (currentSizeBytes + data.length > maxSizeBytes && !lruMap.isEmpty()) {
            String oldestKey = lruMap.keySet().iterator().next();
            remove(oldestKey);
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
            Long oldSize = lruMap.put(key, (long) data.length);
            currentSizeBytes += data.length;
            if (oldSize != null) {
                currentSizeBytes -= oldSize;
            }
        } catch (Exception ignored) {}
    }

    public synchronized byte[] get(String key) {
        if (key == null) return null;
        if (!lruMap.containsKey(key)) return null;

        String fileName = hashKey(key);
        File file = new File(cacheDir, fileName);
        if (!file.exists()) {
            lruMap.remove(key);
            return null;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buf = new byte[(int) file.length()];
            int read = fis.read(buf);
            if (read != -1) {
                lruMap.get(key); // update access order
                return buf;
            }
        } catch (Exception ignored) {}
        return null;
    }

    public synchronized boolean contains(String key) {
        return key != null && lruMap.containsKey(key);
    }

    public synchronized boolean remove(String key) {
        if (key == null) return false;
        Long size = lruMap.remove(key);
        if (size != null) {
            currentSizeBytes = Math.max(0, currentSizeBytes - size);
        }
        File file = new File(cacheDir, hashKey(key));
        return file.exists() && file.delete();
    }

    public synchronized void clear() {
        lruMap.clear();
        currentSizeBytes = 0;
        if (cacheDir != null && cacheDir.exists()) {
            File[] files = cacheDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
        }
    }

    public synchronized long getCurrentSizeBytes() {
        return currentSizeBytes;
    }

    public long getMaxSizeBytes() {
        return maxSizeBytes;
    }

    private String hashKey(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(key.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(key.hashCode());
        }
    }

    private synchronized void scanExistingFiles() {
        if (cacheDir == null || !cacheDir.exists()) return;
        File[] files = cacheDir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile()) {
                    currentSizeBytes += f.length();
                }
            }
        }
    }
}
