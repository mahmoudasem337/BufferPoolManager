package org.example;

public class WritePageGuard implements AutoCloseable {
    private BufferPoolManager bufferPoolManager;
    private int pageId;

    public WritePageGuard(BufferPoolManager bpm, int pageId) {
        this.bufferPoolManager = bpm;
        this.pageId = pageId;
    }

    public void flush() {
        if (bufferPoolManager != null) {
            bufferPoolManager.flushPage(pageId);
        }
    }

    public void drop() {
        if (bufferPoolManager != null) {
            bufferPoolManager.unpinPage(pageId);
            bufferPoolManager = null;
        }
    }

    @Override
    public void close() {  // Required for try-with-resources
        flush();  // Ensure data is written before closing
        drop();
    }
}
