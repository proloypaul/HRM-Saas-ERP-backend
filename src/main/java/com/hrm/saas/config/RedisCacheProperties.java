package com.hrm.saas.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.cache.redis")
public class RedisCacheProperties {

    /**
     * Master switch to enable or disable Redis caching
     */
    private boolean enabled;;

    /**
     * Default TTL for cached items
     */
    private Duration defaultTtl;;

    /**
     * TTL for resolved tenant domain lookups
     */
    private Duration domainTtl;;

    /**
     * Prefix for all application Redis keys to prevent collisions
     */
    private String keyPrefix;
}
