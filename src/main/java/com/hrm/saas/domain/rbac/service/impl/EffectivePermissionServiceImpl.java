package com.hrm.saas.domain.rbac.service.impl;

import com.hrm.saas.domain.auth.entity.TenantMembership;
import com.hrm.saas.domain.auth.enums.MembershipRole;
import com.hrm.saas.domain.auth.enums.MembershipStatus;
import com.hrm.saas.domain.auth.repository.TenantMembershipRepository;
import com.hrm.saas.domain.rbac.constants.RbacPermissions;
import com.hrm.saas.domain.rbac.entity.Permission;
import com.hrm.saas.domain.rbac.entity.Role;
import com.hrm.saas.domain.rbac.entity.UserPermissionOverride;
import com.hrm.saas.domain.rbac.enums.OverrideEffect;
import com.hrm.saas.domain.rbac.repository.RoleRepository;
import com.hrm.saas.domain.rbac.repository.UserPermissionOverrideRepository;
import com.hrm.saas.domain.rbac.service.EffectivePermissionService;
import com.hrm.saas.domain.rbac.service.PermissionCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EffectivePermissionServiceImpl implements EffectivePermissionService {

    private final PermissionCacheService permissionCacheService;
    private final TenantMembershipRepository tenantMembershipRepository;
    private final RoleRepository roleRepository;
    private final UserPermissionOverrideRepository overrideRepository;

    @Override
    @Transactional(readOnly = true)
    public Set<String> getEffectivePermissions(UUID tenantId, UUID userId) {
        if (tenantId == null || userId == null) {
            return Set.of();
        }

        // 1. Try cache hit
        Set<String> cached = permissionCacheService.getCachedPermissions(tenantId, userId);
        if (cached != null) {
            return cached;
        }

        // 2. Cache miss: Compute permissions from DB
        TenantMembership membership = tenantMembershipRepository.findByTenantIdAndUserIdWithRoles(tenantId, userId)
                .orElse(null);

        if (membership == null || membership.getStatus() != MembershipStatus.ACTIVE) {
            return Set.of();
        }

        Set<String> effective = new HashSet<>();

        // (a) Legacy/Fallback Role mapping if membership.roles is empty
        if (membership.getRoles().isEmpty() && membership.getRole() != null) {
            mapLegacyRolePermissions(tenantId, membership.getRole(), effective);
        } else {
            // (b) Roles assigned via membership_roles
            for (Role role : membership.getRoles()) {
                if (Boolean.TRUE.equals(role.getIsActive())) {
                    for (Permission perm : role.getPermissions()) {
                        effective.add(perm.getCode());
                    }
                }
            }
        }

        // (c) Apply fine-grained user permission overrides
        List<UserPermissionOverride> overrides = overrideRepository.findByMembershipIdWithPermission(membership.getId());
        for (UserPermissionOverride override : overrides) {
            String code = override.getPermission().getCode();
            if (override.getEffect() == OverrideEffect.GRANT) {
                effective.add(code);
            } else if (override.getEffect() == OverrideEffect.REVOKE) {
                effective.remove(code);
            }
        }

        // 3. Cache the resolved effective permissions
        permissionCacheService.cachePermissions(tenantId, userId, effective);
        return effective;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPermission(UUID tenantId, UUID userId, String permissionCode) {
        Set<String> permissions = getEffectivePermissions(tenantId, userId);
        return permissions.contains(permissionCode);
    }

    private void mapLegacyRolePermissions(UUID tenantId, MembershipRole role, Set<String> target) {
        if (role == null) return;
        String roleCode = switch (role) {
            case OWNER, ADMIN -> RbacPermissions.ROLE_TENANT_ADMIN;
            case HR_MANAGER -> RbacPermissions.ROLE_HR_MANAGER;
            case EMPLOYEE -> RbacPermissions.ROLE_EMPLOYEE;
        };

        roleRepository.findByTenantIdAndCode(tenantId, roleCode)
                .ifPresentOrElse(
                        r -> r.getPermissions().forEach(p -> target.add(p.getCode())),
                        () -> {
                            Set<String> defaults = RbacPermissions.SYSTEM_ROLE_PERMISSIONS.get(roleCode);
                            if (defaults != null) {
                                target.addAll(defaults);
                            }
                        }
                );
    }
}
