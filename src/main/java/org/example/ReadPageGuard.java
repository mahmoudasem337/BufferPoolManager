package org.example;

public class ReadPageGuard implements AutoCloseable {
    private BufferPoolManager bufferPoolManager;
    private int pageId;

    public ReadPageGuard(BufferPoolManager bpm, int pageId) {
        this.bufferPoolManager = bpm;
        this.pageId = pageId;
    }

    public void drop() {
        if (bufferPoolManager != null) {
            bufferPoolManager.unpinPage(pageId);
            bufferPoolManager = null;  // Prevent double unpinning
        }
    }

    @Override
    public void close() {  // Required for try-with-resources
        drop();
    }
}

