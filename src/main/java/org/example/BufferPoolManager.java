package org.example;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicInteger;

class BufferPoolManager {
    private final int poolSize;
    private final DiskScheduler diskScheduler;
    private final LRUKReplacer replacer;
    private final Map<Integer, FrameHeader> pageTable;
    private final List<FrameHeader> frames;
    private final ReentrantLock lock;
    private int nextPageId;

    public BufferPoolManager(int poolSize, DiskScheduler diskScheduler, LRUKReplacer replacer) {
        this.poolSize = poolSize;
        this.diskScheduler = diskScheduler;
        this.replacer = replacer;
        this.pageTable = new HashMap<>();
        this.frames = new ArrayList<>(Collections.nCopies(poolSize, null));
        this.lock = new ReentrantLock();
        this.nextPageId = 0;
    }

        public Integer newPage() {
        lock.lock();
        try {
            for (int i = 0; i < poolSize; i++) {
                if (!pageTable.containsKey(i)) {  // Unused frame found
                    return allocateNewPage(i);
                }
            }
                Optional<Integer> victimFrame = replacer.evict();
                if (victimFrame.isEmpty()) {
                    return null;  // Return null if no frame is available
                }
                int frameId = victimFrame.get();
                FrameHeader frame = new FrameHeader(frameId);

                // Assign a new page ID (you might need a counter to track page IDs)
                int pageId = generateNewPageId();

                // Associate the frame with the new page
                pageTable.put(pageId, frame);

                // Mark as pinned since it's being used
                frame.pin();
                replacer.setEvictable(frameId, false);
                replacer.recordAccess(frameId);
                return pageId;
            }
            finally{
                lock.unlock();
            }
    }
        // Allocates a new page in the given frame
        private Integer allocateNewPage ( int frameId){
            Integer newPageId = generateNewPageId();
            FrameHeader newFrame = new FrameHeader(frameId);
            pageTable.put(newPageId, newFrame);
            newFrame.pin();
            replacer.setEvictable(frameId, false);
            replacer.recordAccess(frameId);
            return newPageId;
        }

        // Utility method to generate new page IDs
        private int generateNewPageId () {
            return nextPageId++;
        }

        public boolean deletePage ( int pageId){
            lock.lock();
            try {
                FrameHeader frame = pageTable.get(pageId);
                if (frame == null || frame.getPinCount() > 0) {
                    return false;
                }
                replacer.remove(frame.getPageId());
                pageTable.remove(pageId);
                frames.set(frame.getPageId(), null);
                return true;
            } finally {
                lock.unlock();
            }
        }

        public boolean flushPage ( int pageId){
            lock.lock();
            try {
                return flushPageUnsafe(pageId);
            } finally {
                lock.unlock();
            }
        }

        private boolean flushPageUnsafe ( int pageId){
            FrameHeader frame = pageTable.get(pageId);
            if (frame == null || !frame.isDirty()) {
                return false;
            }

            DiskRequest request = new DiskRequest(DiskRequest.Type.WriteOperation, pageId, frame.getData());
            diskScheduler.schedule(request);
            request.callback.join();
            frame.markDirty(false);
            return true;
        }

        public void flushAllPages () {
            lock.lock();
            try {
                for (Integer pageId : pageTable.keySet()) {
                    flushPageUnsafe(pageId);
                }
            } finally {
                lock.unlock();
            }
        }

        public int getPinCount ( int pageId){
            lock.lock();
            try {
                FrameHeader frame = pageTable.get(pageId);
                return (frame != null) ? frame.getPinCount() : -1;
            } finally {
                lock.unlock();
            }
        }

        public Optional<ReadPageGuard> checkedReadPage ( int pageId){
            lock.lock();
            try {
                FrameHeader frame = pageTable.get(pageId);
                if (frame == null) {
                    return Optional.empty();
                }

                // Increment the pin count since a thread is accessing it
                frame.pin();
                replacer.setEvictable(frame.getPageId(), false);
                replacer.recordAccess(frame.getPageId());

                return Optional.of(new ReadPageGuard(this, frame.getPageId()));
            } finally {
                lock.unlock();
            }
        }

        public Optional<WritePageGuard> checkedWritePage ( int pageId){
            lock.lock();
            try {
                FrameHeader frame = pageTable.get(pageId);
                if (frame == null) {
                    return Optional.empty();
                }

                // Increment pin count & mark dirty
                frame.pin();
                frame.markDirty(true);
                replacer.setEvictable(frame.getPageId(), false);
                replacer.recordAccess(frame.getPageId());

                return Optional.of(new WritePageGuard(this, frame.getPageId()));
            } finally {
                lock.unlock();
            }
        }

        public void unpinPage ( int pageId){
            synchronized (this) {
                FrameHeader frame = pageTable.get(pageId);
                if (frame == null) {
                    System.out.println("Error: Attempted to unpin a non-existent page.");
                    return;
                }

                int currentPinCount = frame.getPinCount();
                if (currentPinCount > 0) {
                    frame.unpin();

                    // If pin count is zero, make the page evictable
                    if (frame.getPinCount() == 0) {
                        replacer.setEvictable(frame.getPageId(), true);
                    }
                } else {
                    System.out.println("Warning: Page " + pageId + " is already unpinned.");
                }
            }
        }

    }

