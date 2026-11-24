package com.hrm.saas.domain.auth.dto.response;

import com.hrm.saas.domain.auth.enums.TokenType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private TokenType tokenType;
    private long expiresIn;
    private UserResponse user;
    private TenantMembershipResponse activeTenant;
    private List<TenantMembershipResponse> availableTenants;
}
