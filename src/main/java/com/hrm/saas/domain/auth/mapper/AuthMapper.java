package com.hrm.saas.domain.auth.mapper;

import com.hrm.saas.domain.auth.dto.request.RegisterRequest;
import com.hrm.saas.domain.auth.dto.response.TenantMembershipResponse;
import com.hrm.saas.domain.auth.dto.response.UserResponse;
import com.hrm.saas.domain.auth.entity.TenantMembership;
import com.hrm.saas.domain.auth.entity.User;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AuthMapper {

    public User toEntity(RegisterRequest request, String encodedPassword) {
        if (request == null) {
            return null;
        }
        return User.builder()
                .email(request.getEmail().trim().toLowerCase())
                .passwordHash(encodedPassword)
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .isEmailVerified(false)
                .isActive(true)
                .failedLoginAttempts(0)
                .build();
    }

    public UserResponse toUserResponse(User user) {
        if (user == null) {
            return null;
        }
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .isEmailVerified(Boolean.TRUE.equals(user.getIsEmailVerified()))
                .isActive(Boolean.TRUE.equals(user.getIsActive()))
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public TenantMembershipResponse toMembershipResponse(TenantMembership membership) {
        if (membership == null) {
            return null;
        }
        List<String> roleCodes = (membership.getRoles() != null && !membership.getRoles().isEmpty())
                ? membership.getRoles().stream().map(com.hrm.saas.domain.rbac.entity.Role::getCode).collect(Collectors.toList())
                : List.of("EMPLOYEE");

        return TenantMembershipResponse.builder()
                .membershipId(membership.getId())
                .tenantId(membership.getTenant() != null ? membership.getTenant().getId() : null)
                .tenantName(membership.getTenant() != null ? membership.getTenant().getName() : null)
                .tenantSlug(membership.getTenant() != null ? membership.getTenant().getSlug() : null)
                .isOwner(Boolean.TRUE.equals(membership.getIsOwner()))
                .roles(roleCodes)
                .status(membership.getStatus())
                .joinedAt(membership.getJoinedAt())
                .build();
    }

    public List<TenantMembershipResponse> toMembershipResponseList(List<TenantMembership> memberships) {
        if (memberships == null || memberships.isEmpty()) {
            return Collections.emptyList();
        }
        return memberships.stream()
                .map(this::toMembershipResponse)
                .collect(Collectors.toList());
    }
}
