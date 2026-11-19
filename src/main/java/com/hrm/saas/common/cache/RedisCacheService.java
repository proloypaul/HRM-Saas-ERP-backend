package com.hrm.saas.common.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrm.saas.config.RedisCacheProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisCacheProperties cacheProperties;
    private final ObjectMapper objectMapper;

    /**
     * Builds a fully qualified key with the configured application prefix.
     */
    public String buildKey(String key) {
        String prefix = cacheProperties.getKeyPrefix();
        if (prefix == null || prefix.isBlank()) {
            return key;
        }
        return prefix + ":" + key;
    }

    /**
     * Safely retrieves an item from Redis.
     * Returns Optional.empty() if not found, disabled, or if Redis is down.
     */
    public <T> Optional<T> get(String key, Class<T> targetClass) {
        if (!cacheProperties.isEnabled() || key == null) {
            return Optional.empty();
        }

        String fullKey = buildKey(key);
        try {
            Object raw = redisTemplate.opsForValue().get(fullKey);
            if (raw == null) {
                return Optional.empty();
            }

            if (targetClass.isInstance(raw)) {
                return Optional.of(targetClass.cast(raw));
            }

            // Convert via ObjectMapper if type differs (e.g. LinkedHashMap from Jackson deserialization)
            T mapped = objectMapper.convertValue(raw, targetClass);
            return Optional.ofNullable(mapped);
        } catch (Exception e) {
            log.warn("Redis connection failed on GET for key '{}'. Falling back. Reason: {}", fullKey, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Safely stores an item in Redis with specified TTL.
     * Fails silently with a warning log if Redis is down.
     */
    public boolean set(String key, Object value, Duration ttl) {
        if (!cacheProperties.isEnabled() || key == null || value == null) {
            return false;
        }

        String fullKey = buildKey(key);
        Duration effectiveTtl = ttl != null ? ttl : cacheProperties.getDefaultTtl();
        try {
            redisTemplate.opsForValue().set(fullKey, value, effectiveTtl);
            return true;
        } catch (Exception e) {
            log.warn("Redis connection failed on SET for key '{}'. Reason: {}", fullKey, e.getMessage());
            return false;
        }
    }

    /**
     * Safely stores an item in Redis with default configured TTL.
     */
    public boolean set(String key, Object value) {
        return set(key, value, cacheProperties.getDefaultTtl());
    }

    /**
     * Safely evicts a single key from Redis.
     */
    public boolean evict(String key) {
        if (key == null) {
            return false;
        }

        String fullKey = buildKey(key);
        try {
            return Boolean.TRUE.equals(redisTemplate.delete(fullKey));
        } catch (Exception e) {
            log.warn("Redis connection failed on EVICT for key '{}'. Reason: {}", fullKey, e.getMessage());
            return false;
        }
    }

    /**
     * Production-grade Cache-Aside pattern (Get or Fetch):
     * 1. Attempts to get value from Redis.
     * 2. If present in Redis, returns immediately.
     * 3. If missing in Redis OR if Redis is down, calls dbFallback.
     * 4. If dbFallback returns a value, safely populates Redis with the configured TTL.
     * 5. Returns the value.
     */
    public <T> Optional<T> getOrFetch(
            String key,
            Class<T> targetClass,
            Duration ttl,
            Supplier<Optional<T>> dbFallback
    ) {
        // Step 1: Try reading from cache
        Optional<T> cached = get(key, targetClass);
        if (cached.isPresent()) {
            log.debug("Cache HIT for key '{}'", buildKey(key));
            return cached;
        }

        log.debug("Cache MISS or Redis unavailable for key '{}'. Fetching from DB fallback...", buildKey(key));

        // Step 2: Fetch from database / origin
        Optional<T> freshData;
        try {
            freshData = dbFallback.get();
        } catch (Exception e) {
            log.error("Database fallback failed for key '{}': {}", key, e.getMessage(), e);
            throw e;
        }

        // Step 3: Populate Redis cache for subsequent calls
        freshData.ifPresent(data -> set(key, data, ttl));

        return freshData;
    }
}
