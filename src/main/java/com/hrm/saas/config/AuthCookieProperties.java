package com.hrm.saas.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.security.cookie")
public class AuthCookieProperties {
    private String name = "hrm_refresh_token";
    private boolean httpOnly = true;
    private boolean secure = false;
    private String sameSite = "Lax";
    private String domain;
    private String path = "/api/v1/auth";
    private Duration maxAge = Duration.ofDays(7);
}
