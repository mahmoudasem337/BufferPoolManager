package org.example;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DiskManager {
    private final Map<Integer, byte[]> storage = new ConcurrentHashMap<>();

    public void readPage(int pageId, byte[] buffer) {
        byte[] data = storage.getOrDefault(pageId, new byte[buffer.length]);
        System.arraycopy(data, 0, buffer, 0, buffer.length);
    }

    public void writePage(int pageId, byte[] data) {
        storage.put(pageId, data.clone());
    }
}
