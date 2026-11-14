package com.csscholar.backend.config;

import com.csscholar.backend.config.RateLimitingFilter.EndpointRisk;
import com.csscholar.backend.config.RateLimitingFilter.RateCheckStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class RedisRateLimitAtomicityIntegrationTest {

    @Autowired
    private RateLimitingFilter rateLimitingFilter;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private String testPrefix;

    @BeforeEach
    void setUp() {
        testPrefix = "test_rate_" + UUID.randomUUID();
    }

    @AfterEach
    void cleanUp() {
        Set<String> keys = redisTemplate.keys("cs:ratelimit:*" + testPrefix + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Test
    @DisplayName("Redis Lua script atomically increments counter and sets key TTL")
    void shouldAtomicallyIncrementAndSetTtl() {
        String rateKey = testPrefix + ":single";
        long startMinute = System.currentTimeMillis() / 60000L;
        RateCheckStatus status = rateLimitingFilter.checkRateLimit(rateKey, 10, EndpointRisk.STANDARD);
        assertEquals(RateCheckStatus.ALLOWED, status);
        long endMinute = System.currentTimeMillis() / 60000L;

        String redisKey = "cs:ratelimit:" + rateKey + ":" + startMinute;
        String value = redisTemplate.opsForValue().get(redisKey);
        if (value == null && endMinute != startMinute) {
            redisKey = "cs:ratelimit:" + rateKey + ":" + endMinute;
            value = redisTemplate.opsForValue().get(redisKey);
        }
        assertEquals("1", value, "Initial counter value must be 1");

        Long ttl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
        assertNotNull(ttl, "Key must have an expiration TTL");
        assertTrue(ttl > 0 && ttl <= 120, "TTL must be positive and within 120 seconds, was: " + ttl);
    }

    @Test
    @DisplayName("Concurrent requests under Lua script execute atomically with precise limit enforcement and TTL preservation")
    void shouldAtomicallyEnforceLimitsUnderConcurrency() throws InterruptedException {
        String rateKey = testPrefix + ":concurrency";
        int totalRequests = 20;
        int maxAllowed = 10;

        ExecutorService executor = Executors.newFixedThreadPool(totalRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(totalRequests);

        AtomicInteger allowedCount = new AtomicInteger(0);
        AtomicInteger rateLimitedCount = new AtomicInteger(0);

        for (int i = 0; i < totalRequests; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    RateCheckStatus status = rateLimitingFilter.checkRateLimit(rateKey, maxAllowed, EndpointRisk.STANDARD);
                    if (status == RateCheckStatus.ALLOWED) {
                        allowedCount.incrementAndGet();
                    } else if (status == RateCheckStatus.RATE_LIMITED) {
                        rateLimitedCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // unexpected
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        // Fire all threads simultaneously
        startLatch.countDown();
        boolean completed = finishLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All concurrent requests should finish within 10 seconds");
        assertEquals(maxAllowed, allowedCount.get(), "Exactly " + maxAllowed + " requests should be allowed");
        assertEquals(totalRequests - maxAllowed, rateLimitedCount.get(),
                "Exactly " + (totalRequests - maxAllowed) + " requests should be rate limited");

        long currentMinute = System.currentTimeMillis() / 60000L;
        String redisKey = "cs:ratelimit:" + rateKey + ":" + currentMinute;
        Long ttl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
        assertNotNull(ttl);
        assertTrue(ttl > 0 && ttl <= 120, "TTL must be preserved under concurrency");
    }

    @Test
    @DisplayName("High-cost operations strictly fail closed when Redis is unavailable")
    void shouldFailClosedForHighCostEndpointsWhenRedisIsUnavailable() {
        // Construct filter with disconnected / failing Redis template
        RateLimitingFilter isolatedFilter = new RateLimitingFilter(
                true,
                "127.0.0.1",
                new StringRedisTemplate() {
                    @Override
                    public <T> T execute(org.springframework.data.redis.core.script.RedisScript<T> script,
                                         java.util.List<String> keys, Object... args) {
                        throw new RedisConnectionFailureException("Simulated Redis Outage");
                    }
                },
                new ObjectMapper()
        );

        String highCostKey = testPrefix + ":high_cost";
        RateCheckStatus highCostStatus = isolatedFilter.checkRateLimit(highCostKey, 15, EndpointRisk.HIGH_COST);
        assertEquals(RateCheckStatus.STORE_OUTAGE_FAIL_CLOSED, highCostStatus,
                "High-cost endpoints must fail closed with 503 during Redis outages");

        String standardKey = testPrefix + ":standard";
        RateCheckStatus standardStatus = isolatedFilter.checkRateLimit(standardKey, 10, EndpointRisk.STANDARD);
        assertEquals(RateCheckStatus.ALLOWED, standardStatus,
                "Standard endpoints should degrade to local sliding window counter");
    }
}

