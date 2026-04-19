package expensetracker.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RateLimiter Tests")
class RateLimiterTest {

    private RateLimiter limiter;

    @BeforeEach
    void setUp() {
        // 5 requests per 60 seconds
        limiter = new RateLimiter(5, 60_000);
    }

    @Test
    @DisplayName("should allow requests within rate limit")
    void allowsRequestsWithinLimit() {
        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.isAllowed("192.168.1.1"),
                    "Request " + (i + 1) + " should be allowed");
        }
    }

    @Test
    @DisplayName("should block requests exceeding rate limit")
    void blocksExceedingRequests() {
        // Exhaust the limit
        for (int i = 0; i < 5; i++) {
            limiter.isAllowed("192.168.1.1");
        }

        // 6th request should be blocked
        assertFalse(limiter.isAllowed("192.168.1.1"),
                "6th request should be blocked");
    }

    @Test
    @DisplayName("should track different IPs independently")
    void tracksDifferentIPsIndependently() {
        // Exhaust limit for IP1
        for (int i = 0; i < 5; i++) {
            limiter.isAllowed("192.168.1.1");
        }

        // IP2 should still be allowed
        assertTrue(limiter.isAllowed("192.168.1.2"),
                "Different IP should not be affected");
    }

    @Test
    @DisplayName("should reject null or empty IP")
    void handlesNullIP() {
        // Implementation returns false for null/empty IP
        assertFalse(limiter.isAllowed(null));
        assertFalse(limiter.isAllowed(""));
    }

    @Test
    @DisplayName("should allow requests after time window resets")
    void allowsAfterWindowResets() throws InterruptedException {
        // Use a very short window (100ms)
        RateLimiter shortLimiter = new RateLimiter(2, 100);

        assertTrue(shortLimiter.isAllowed("10.0.0.1"));
        assertTrue(shortLimiter.isAllowed("10.0.0.1"));
        assertFalse(shortLimiter.isAllowed("10.0.0.1"));

        // Wait for window to expire
        Thread.sleep(150);

        assertTrue(shortLimiter.isAllowed("10.0.0.1"),
                "Should allow after time window expires");
    }
}
