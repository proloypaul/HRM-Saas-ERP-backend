package com.hrm.saas.domain.auth.controller;

import com.hrm.saas.common.constants.ApiRoutes;
import com.hrm.saas.common.response.ApiResponse;
import com.hrm.saas.common.util.CookieUtils;
import com.hrm.saas.domain.auth.dto.request.*;
import com.hrm.saas.domain.auth.dto.response.AuthResponse;
import com.hrm.saas.domain.auth.dto.response.AuthResult;
import com.hrm.saas.domain.auth.dto.response.TenantMembershipResponse;
import com.hrm.saas.domain.auth.dto.response.UserResponse;
import com.hrm.saas.domain.auth.security.UserPrincipal;
import com.hrm.saas.domain.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiRoutes.Auth.BASE)
@RequiredArgsConstructor
@Tag(name = "Authentication & Identity", description = "Endpoints for user authentication, registration, tenant switching, and credential recovery")
public class AuthController {

    private final AuthService authService;
    private final CookieUtils cookieUtils;

    @PostMapping(ApiRoutes.Auth.REGISTER)
    @Operation(summary = "Register user and optional company workspace")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest) {
        String userAgent = servletRequest.getHeader("User-Agent");
        String ipAddress = servletRequest.getRemoteAddr();

        AuthResult result = authService.register(request, userAgent, ipAddress);
        ResponseCookie refreshCookie = cookieUtils.createRefreshTokenCookie(result.getRawRefreshToken());
        ResponseCookie accessCookie = cookieUtils.createAccessTokenCookie(result.getAuthResponse().getAccessToken(),
                null);

        // Access token is kept only in HttpOnly cookie; remove it from API payload
        AuthResponse responseBody = result.getAuthResponse();
        responseBody.setAccessToken(null);

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .body(ApiResponse.success(responseBody, "User registered successfully"));
    }

    @PostMapping(ApiRoutes.Auth.LOGIN)
    @Operation(summary = "Authenticate user and issue tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest) {
        String userAgent = servletRequest.getHeader("User-Agent");
        String ipAddress = servletRequest.getRemoteAddr();

        AuthResult result = authService.login(request, userAgent, ipAddress);
        ResponseCookie refreshCookie = cookieUtils.createRefreshTokenCookie(result.getRawRefreshToken());
        ResponseCookie accessCookie = cookieUtils.createAccessTokenCookie(result.getAuthResponse().getAccessToken(),
                null);

        // Access token is kept only in HttpOnly cookie; remove it from API payload
        AuthResponse responseBody = result.getAuthResponse();
        responseBody.setAccessToken(null);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .body(ApiResponse.success(responseBody, "Authentication successful"));
    }

    @PostMapping(ApiRoutes.Auth.REFRESH)
    @Operation(summary = "Rotate refresh token and issue new access token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @CookieValue(name = "${app.security.cookie.name:hrm_refresh_token}", required = false) String cookieRefreshToken,
            HttpServletRequest servletRequest) {
        String tokenToUse = cookieRefreshToken;

        // Fallback: Check request header if cookie was not passed
        if (tokenToUse == null || tokenToUse.isBlank()) {
            tokenToUse = servletRequest.getHeader("X-Refresh-Token");
        }

        String userAgent = servletRequest.getHeader("User-Agent");
        String ipAddress = servletRequest.getRemoteAddr();

        AuthResult result = authService.refresh(tokenToUse, userAgent, ipAddress);
        // ResponseCookie refreshCookie =
        // cookieUtils.createRefreshTokenCookie(result.getRawRefreshToken());
        ResponseCookie accessCookie = cookieUtils.createAccessTokenCookie(result.getAuthResponse().getAccessToken(),
                null);

        AuthResponse responseBody = result.getAuthResponse();
        responseBody.setAccessToken(null);

        return ResponseEntity.ok()
                // .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .body(ApiResponse.success(responseBody, "Tokens refreshed successfully"));
    }

    @PostMapping(ApiRoutes.Auth.LOGOUT)
    @Operation(summary = "Revoke session and clear refresh cookie")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = "${app.security.cookie.name:hrm_refresh_token}", required = false) String cookieRefreshToken,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID userId = principal != null ? principal.getUserId() : null;
        authService.logout(cookieRefreshToken, userId);

        ResponseCookie cleanRefreshCookie = cookieUtils.createCleanRefreshLogoutCookie();
        ResponseCookie cleanAccessCookie = cookieUtils.createCleanAccessLogoutCookie();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cleanRefreshCookie.toString())
                .header(HttpHeaders.SET_COOKIE, cleanAccessCookie.toString())
                .body(ApiResponse.success(null, "Logged out successfully"));
    }

    @PostMapping(ApiRoutes.Auth.SWITCH_TENANT)
    @Operation(summary = "Switch active tenant workspace")
    public ResponseEntity<ApiResponse<AuthResponse>> switchTenant(
            @Valid @RequestBody SwitchTenantRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest servletRequest) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthenticated"));
        }

        String userAgent = servletRequest.getHeader("User-Agent");
        String ipAddress = servletRequest.getRemoteAddr();

        AuthResult result = authService.switchTenant(principal.getUserId(), request.getTenantId(), userAgent,
                ipAddress);
        ResponseCookie refreshCookie = cookieUtils.createRefreshTokenCookie(result.getRawRefreshToken());
        ResponseCookie accessCookie = cookieUtils.createAccessTokenCookie(result.getAuthResponse().getAccessToken(),
                null);

        AuthResponse responseBody = result.getAuthResponse();
        responseBody.setAccessToken(null);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .body(ApiResponse.success(responseBody, "Switched tenant successfully"));
    }

    @GetMapping(ApiRoutes.Auth.ME)
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthenticated"));
        }
        UserResponse response = authService.getCurrentUser(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response, "Current user fetched"));
    }

    @GetMapping(ApiRoutes.Auth.TENANTS)
    @Operation(summary = "Get available tenant memberships for current user")
    public ResponseEntity<ApiResponse<List<TenantMembershipResponse>>> getUserTenants(
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthenticated"));
        }
        List<TenantMembershipResponse> response = authService.getUserTenants(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response, "User tenant memberships fetched"));
    }

    @PostMapping(ApiRoutes.Auth.FORGOT_PASSWORD)
    @Operation(summary = "Request password reset token")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success(null,
                "If your email is registered, you will receive password reset instructions."));
    }

    @PostMapping(ApiRoutes.Auth.RESET_PASSWORD)
    @Operation(summary = "Reset password using reset token")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Password reset successfully. You can now login."));
    }

    @PostMapping(ApiRoutes.Auth.VERIFY_EMAIL)
    @Operation(summary = "Verify email address using verification token")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Email verified successfully"));
    }
}
