package org.example;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

class DiskScheduler {
    private final BlockingQueue<DiskRequest> requestQueue = new LinkedBlockingQueue<>();
    private final DiskManager diskManager;
    private final Thread workerThread;
    private volatile boolean running = true;

    public DiskScheduler(DiskManager diskManager) {
        this.diskManager = diskManager;
        this.workerThread = new Thread(this::startWorkerThread);
        this.workerThread.start();
    }

    public void schedule(DiskRequest request) {
        requestQueue.offer(request);
    }

    private void startWorkerThread() {
        while (running) {
            try {
                DiskRequest request = requestQueue.take();
                processRequest(request);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void processRequest(DiskRequest request) {
        if (request.type == DiskRequest.Type.ReadOperation) {
            diskManager.readPage(request.pageId, request.data);
        } else {
            diskManager.writePage(request.pageId, request.data);
        }
        request.callback.complete(true);
    }

    public void shutdown() {
        running = false;
        workerThread.interrupt();
    }
}