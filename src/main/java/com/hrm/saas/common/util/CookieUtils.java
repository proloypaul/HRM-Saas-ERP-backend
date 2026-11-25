package com.hrm.saas.common.util;

import com.hrm.saas.config.AuthCookieProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class CookieUtils {

    private final AuthCookieProperties cookieProperties;

    /**
     * Creates the HttpOnly Refresh Token Cookie (scoped to auth paths).
     */
    public ResponseCookie createRefreshTokenCookie(String tokenValue) {
        String path = cookieProperties.getRefreshPath() != null ? cookieProperties.getRefreshPath()
                : cookieProperties.getPath();
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(cookieProperties.getName(), tokenValue)
                .httpOnly(cookieProperties.isHttpOnly())
                .secure(cookieProperties.isSecure())
                .path(path)
                .maxAge(cookieProperties.getMaxAge())
                .sameSite(cookieProperties.getSameSite());

        if (cookieProperties.getDomain() != null && !cookieProperties.getDomain().isBlank()) {
            builder.domain(cookieProperties.getDomain());
        }

        return builder.build();
    }

    /**
     * Creates the HttpOnly Access Token Cookie (scoped to all API endpoints, with
     * 15m expiration).
     */
    public ResponseCookie createAccessTokenCookie(String tokenValue) {
        // Duration maxAge = duration != null ? duration : Duration.ofMinutes(15);
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie
                .from(cookieProperties.getAccessCookieName(), tokenValue)
                .httpOnly(cookieProperties.isHttpOnly())
                .secure(cookieProperties.isSecure())
                .path(cookieProperties.getPath())
                .maxAge(cookieProperties.getMaxAge())
                .sameSite(cookieProperties.getSameSite());

        if (cookieProperties.getDomain() != null && !cookieProperties.getDomain().isBlank()) {
            builder.domain(cookieProperties.getDomain());
        }

        return builder.build();
    }

    /**
     * Evicts the Refresh Token cookie on logout.
     */
    public ResponseCookie createCleanRefreshLogoutCookie() {
        String path = cookieProperties.getRefreshPath() != null ? cookieProperties.getRefreshPath()
                : cookieProperties.getPath();
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(cookieProperties.getName(), "")
                .httpOnly(cookieProperties.isHttpOnly())
                .secure(cookieProperties.isSecure())
                .path(path)
                .maxAge(0)
                .sameSite(cookieProperties.getSameSite());

        if (cookieProperties.getDomain() != null && !cookieProperties.getDomain().isBlank()) {
            builder.domain(cookieProperties.getDomain());
        }

        return builder.build();
    }

    /**
     * Evicts the Access Token cookie on logout.
     */
    public ResponseCookie createCleanAccessLogoutCookie() {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(cookieProperties.getAccessCookieName(), "")
                .httpOnly(cookieProperties.isHttpOnly())
                .secure(cookieProperties.isSecure())
                .path(cookieProperties.getPath())
                .maxAge(0)
                .sameSite(cookieProperties.getSameSite());

        if (cookieProperties.getDomain() != null && !cookieProperties.getDomain().isBlank()) {
            builder.domain(cookieProperties.getDomain());
        }

        return builder.build();
    }

    public ResponseCookie createCleanLogoutCookie() {
        return createCleanRefreshLogoutCookie();
    }

    public String getRefreshTokenCookieName() {
        return cookieProperties.getName();
    }

    public String getAccessTokenCookieName() {
        return cookieProperties.getAccessCookieName();
    }
}
