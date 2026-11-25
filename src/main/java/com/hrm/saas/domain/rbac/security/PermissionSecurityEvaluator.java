package com.hrm.saas.domain.rbac.security;

import com.hrm.saas.domain.auth.security.UserPrincipal;
import com.hrm.saas.domain.rbac.service.EffectivePermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

/**
 * SpEL Security component accessible via `@perm.has('permission.code')` in Spring Security annotations.
 * Provides sub-millisecond evaluation backed by Redis and real-time cache eviction.
 */
@Slf4j
@Component("perm")
@RequiredArgsConstructor
public class PermissionSecurityEvaluator {

    private final EffectivePermissionService effectivePermissionService;

    public boolean has(String permissionCode) {
        UserPrincipal principal = getCurrentPrincipal();
        if (principal == null || principal.getTenantId() == null) {
            return false;
        }

        return effectivePermissionService.hasPermission(principal.getTenantId(), principal.getUserId(), permissionCode);
    }

    public boolean hasAny(String... permissionCodes) {
        UserPrincipal principal = getCurrentPrincipal();
        if (principal == null || principal.getTenantId() == null) {
            return false;
        }

        Set<String> permissions = effectivePermissionService.getEffectivePermissions(principal.getTenantId(), principal.getUserId());
        return Arrays.stream(permissionCodes).anyMatch(permissions::contains);
    }

    public boolean hasAll(String... permissionCodes) {
        UserPrincipal principal = getCurrentPrincipal();
        if (principal == null || principal.getTenantId() == null) {
            return false;
        }

        Set<String> permissions = effectivePermissionService.getEffectivePermissions(principal.getTenantId(), principal.getUserId());
        return Arrays.stream(permissionCodes).allMatch(permissions::contains);
    }

    public boolean isTenantMember(UUID tenantId) {
        UserPrincipal principal = getCurrentPrincipal();
        return principal != null && principal.getTenantId() != null && principal.getTenantId().equals(tenantId);
    }

    private UserPrincipal getCurrentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        return null;
    }
}
