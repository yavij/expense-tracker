package expensetracker.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiter using sliding window approach.
 * Tracks requests per IP address with automatic cleanup of old entries.
 */
public class RateLimiter {

    private static class RequestWindow {
        final long windowStartTime;
        final AtomicInteger count;

        RequestWindow() {
            this.windowStartTime = System.currentTimeMillis();
            this.count = new AtomicInteger(0);
        }

        boolean isExpired(long windowDurationMs) {
            return System.currentTimeMillis() - windowStartTime > windowDurationMs;
        }
    }

    private final ConcurrentHashMap<String, RequestWindow> requestMap = new ConcurrentHashMap<>();
    private final int maxRequests;
    private final long windowDurationMs;

    /**
     * Creates a rate limiter with specified max requests per time window.
     *
     * @param maxRequests maximum number of requests allowed
     * @param windowDurationMs duration of the time window in milliseconds
     */
    public RateLimiter(int maxRequests, long windowDurationMs) {
        this.maxRequests = maxRequests;
        this.windowDurationMs = windowDurationMs;
    }

    /**
     * Checks if a request from the given IP is allowed.
     * Returns true if the request is within rate limits, false otherwise.
     * Automatically cleans up expired entries.
     *
     * @param ip the client IP address
     * @return true if the request is allowed, false if rate limit exceeded
     */
    public synchronized boolean isAllowed(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        // Cleanup expired entries
        cleanup();

        RequestWindow window = requestMap.get(ip);

        if (window == null || window.isExpired(windowDurationMs)) {
            // Create new window
            window = new RequestWindow();
            requestMap.put(ip, window);
        }

        int currentCount = window.count.get();
        if (currentCount >= maxRequests) {
            return false;
        }

        window.count.incrementAndGet();
        return true;
    }

    /**
     * Removes expired entries from the request map to prevent memory leaks.
     */
    private void cleanup() {
        requestMap.entrySet().removeIf(entry -> entry.getValue().isExpired(windowDurationMs));
    }

    /**
     * Resets the rate limiter state.
     */
    public void reset() {
        requestMap.clear();
    }

    /**
     * Gets the current request count for a specific IP (for testing/monitoring).
     *
     * @param ip the client IP address
     * @return the current request count or 0 if not found
     */
    public int getRequestCount(String ip) {
        RequestWindow window = requestMap.get(ip);
        if (window == null || window.isExpired(windowDurationMs)) {
            return 0;
        }
        return window.count.get();
    }
}
