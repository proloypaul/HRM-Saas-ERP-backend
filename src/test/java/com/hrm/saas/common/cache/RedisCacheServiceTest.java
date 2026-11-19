package com.hrm.saas.common.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrm.saas.config.RedisCacheProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private RedisCacheProperties properties;
    private RedisCacheService redisCacheService;

    @BeforeEach
    void setUp() {
        properties = new RedisCacheProperties();
        properties.setEnabled(true);
        properties.setDefaultTtl(Duration.ofMinutes(30));
        properties.setDomainTtl(Duration.ofMinutes(15));
        properties.setKeyPrefix("hrm");

        ObjectMapper objectMapper = new ObjectMapper();
        redisCacheService = new RedisCacheService(redisTemplate, properties, objectMapper);
    }

    @Test
    @DisplayName("Should return cached value on cache HIT without calling DB fallback")
    void shouldReturnCachedValueOnHit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("hrm:test-key")).thenReturn("cached-data");

        AtomicBoolean dbFallbackCalled = new AtomicBoolean(false);
        Optional<String> result = redisCacheService.getOrFetch(
                "test-key",
                String.class,
                Duration.ofMinutes(10),
                () -> {
                    dbFallbackCalled.set(true);
                    return Optional.of("db-data");
                }
        );

        assertTrue(result.isPresent());
        assertEquals("cached-data", result.get());
        assertFalse(dbFallbackCalled.get(), "Database fallback must not be invoked on cache hit");
    }

    @Test
    @DisplayName("Should call DB fallback on cache MISS and repopulate Redis cache")
    void shouldFetchFromDbAndRepopulateOnMiss() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("hrm:test-key")).thenReturn(null);

        Optional<String> result = redisCacheService.getOrFetch(
                "test-key",
                String.class,
                Duration.ofMinutes(10),
                () -> Optional.of("db-data")
        );

        assertTrue(result.isPresent());
        assertEquals("db-data", result.get());
        verify(valueOperations).set("hrm:test-key", "db-data", Duration.ofMinutes(10));
    }

    @Test
    @DisplayName("Should handle Redis failure gracefully: log warning, call DB, and return data without throwing exception")
    void shouldHandleRedisFailureGracefully() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("hrm:test-key")).thenThrow(new RedisConnectionFailureException("Connection refused"));

        Optional<String> result = redisCacheService.getOrFetch(
                "test-key",
                String.class,
                Duration.ofMinutes(10),
                () -> Optional.of("db-data")
        );

        assertTrue(result.isPresent());
        assertEquals("db-data", result.get());
    }
}
