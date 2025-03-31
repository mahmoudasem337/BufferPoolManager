package org.example;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class LRUKReplacer {
    private final int k; //accesses for tracking
    private final int capacity; // size
    private final Map<Integer, Deque<Long>> accessHistory;
    private final Set<Integer> evictableFrames;
    private final ReentrantLock lock;
    private long currentTimestamp;

    public LRUKReplacer(int capacity, int k) {
        this.capacity = capacity;
        this.k = k;
        this.accessHistory = new HashMap<>();
        this.evictableFrames = new HashSet<>();
        this.lock = new ReentrantLock();
        this.currentTimestamp = 0;
    }

    public Optional<Integer> evict() {
        lock.lock();
        try {
            if (evictableFrames.isEmpty())
                return Optional.empty();

            Integer victim = null;
            long maxDistance = Long.MIN_VALUE;// largest backward k-distance
            long earliestTimestamp = Long.MAX_VALUE;//Helps in breaking ties when multiple frames have +∞ distance.

            for (int frame : evictableFrames) {
                Deque<Long> history = accessHistory.get(frame);
                long distance = (history.size() >= k) ? currentTimestamp - history.peekLast() : Long.MAX_VALUE;
                long oldestAccess = history.peekFirst();

                if (distance > maxDistance || (distance == maxDistance && oldestAccess < earliestTimestamp)) {
                    maxDistance = distance;
                    earliestTimestamp = oldestAccess;
                    victim = frame;
                }
            }

            if (victim != null) {
                accessHistory.remove(victim);
                evictableFrames.remove(victim);
            }
            return Optional.ofNullable(victim);
        } finally {
            lock.unlock();
        }
    }

    public void recordAccess(int frameId) {
        lock.lock();
        try {
            currentTimestamp++;
            accessHistory.putIfAbsent(frameId, new ArrayDeque<>());
            Deque<Long> history = accessHistory.get(frameId);
            history.addLast(currentTimestamp);
            if (history.size() > k) history.pollFirst();
        } finally {
            lock.unlock();
        }
    }

    public void remove(int frameId) {
        lock.lock();
        try {
            accessHistory.remove(frameId);
            evictableFrames.remove(frameId);
        } finally {
            lock.unlock();
        }
    }

    public void setEvictable(int frameId, boolean setEvictable) {
        lock.lock();
        try {
            if (setEvictable) {
                evictableFrames.add(frameId);
            } else {
                evictableFrames.remove(frameId);
            }
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return evictableFrames.size();
        } finally {
            lock.unlock();
        }
    }
}
