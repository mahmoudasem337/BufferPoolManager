package org.example;

import java.util.concurrent.locks.ReentrantLock;

class FrameHeader {
    private final byte[] data;
    private int pinCount;
    private boolean isDirty;
    private final ReentrantLock lock;
    private int pageId;

    public FrameHeader(int frameId) {
        this.data = new byte[4096];
        this.pinCount = 0;
        this.isDirty = false;
        this.lock = new ReentrantLock();
        this.pageId = frameId; // Uninitialized
    }

    public byte[] getData() {
        return data;
    }

    public synchronized void pin() {
        pinCount++;
    }

    public synchronized void unpin() {
        if (pinCount > 0) {
            pinCount--;
        }
    }

    public synchronized int getPinCount() {
        return pinCount;
    }

    public synchronized boolean isDirty() {
        return isDirty;
    }

    public synchronized void markDirty(boolean b) {
        isDirty = true;
    }

    public synchronized void resetDirty() {
        isDirty = false;
    }

    public int getPageId() {
        return pageId;
    }

    public void setPageId(int pageId) {
        this.pageId = pageId;
    }

    public ReentrantLock getLock() {
        return lock;
    }

}
