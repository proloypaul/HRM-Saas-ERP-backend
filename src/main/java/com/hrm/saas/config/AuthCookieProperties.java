package com.hrm.saas.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.security.cookie")
public class AuthCookieProperties {
    private String name;
    private boolean httpOnly;
    private boolean secure;
    private String sameSite;
    private String domain;
    private String path;
    private Duration maxAge;

    public String getDomain() {
        return (domain != null && !domain.isBlank()) ? domain.trim() : null;
    }
}
