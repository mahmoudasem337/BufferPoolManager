package org.example;

import java.util.concurrent.CompletableFuture;

public class DiskRequest {
    enum Type { ReadOperation, WriteOperation }
    Type type;
    int pageId;
    byte[] data;
    CompletableFuture<Boolean> callback;

    public DiskRequest(Type type, int pageId, byte[] data) {
        this.type = type;
        this.pageId = pageId;
        this.data = data;
        this.callback = new CompletableFuture<>();
    }
}