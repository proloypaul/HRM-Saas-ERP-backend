package com.hrm.saas.domain.auth.service;

import com.hrm.saas.domain.auth.dto.request.ForgotPasswordRequest;
import com.hrm.saas.domain.auth.dto.request.LoginRequest;
import com.hrm.saas.domain.auth.dto.request.RegisterRequest;
import com.hrm.saas.domain.auth.dto.request.ResetPasswordRequest;
import com.hrm.saas.domain.auth.dto.request.VerifyEmailRequest;
import com.hrm.saas.domain.auth.dto.response.AuthResult;
import com.hrm.saas.domain.auth.dto.response.TenantMembershipResponse;
import com.hrm.saas.domain.auth.dto.response.UserResponse;

import java.util.List;
import java.util.UUID;

public interface AuthService {

    AuthResult register(RegisterRequest request, String userAgent, String ipAddress);

    AuthResult login(LoginRequest request, String userAgent, String ipAddress);

    AuthResult refresh(String rawRefreshToken, String userAgent, String ipAddress);

    void logout(String rawRefreshToken, UUID currentUserId);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    void verifyEmail(VerifyEmailRequest request);

    UserResponse getCurrentUser(UUID userId);

    List<TenantMembershipResponse> getUserTenants(UUID userId);

    AuthResult switchTenant(UUID userId, UUID targetTenantId, String userAgent, String ipAddress);
}
