package com.hrm.saas.domain.rbac.service.impl;

import com.hrm.saas.common.cache.RedisCacheService;
import com.hrm.saas.domain.auth.entity.TenantMembership;
import com.hrm.saas.domain.auth.repository.TenantMembershipRepository;
import com.hrm.saas.domain.rbac.service.PermissionCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionCacheServiceImpl implements PermissionCacheService {

    private final RedisCacheService redisCacheService;
    private final TenantMembershipRepository tenantMembershipRepository;

    private static final Duration PERM_CACHE_TTL = Duration.ofMinutes(30);

    private String buildUserKey(UUID tenantId, UUID userId) {
        return "rbac:perms:" + tenantId + ":" + userId;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<String> getCachedPermissions(UUID tenantId, UUID userId) {
        if (tenantId == null || userId == null) {
            return null;
        }
        String key = buildUserKey(tenantId, userId);
        Optional<List> opt = redisCacheService.get(key, List.class);
        return opt.map(list -> (Set<String>) new HashSet<String>(list)).orElse(null);
    }

    @Override
    public void cachePermissions(UUID tenantId, UUID userId, Set<String> permissions) {
        if (tenantId == null || userId == null || permissions == null) {
            return;
        }
        String key = buildUserKey(tenantId, userId);
        redisCacheService.set(key, new ArrayList<>(permissions), PERM_CACHE_TTL);
    }

    @Override
    public void evictUserPermissions(UUID tenantId, UUID userId) {
        if (tenantId == null || userId == null) {
            return;
        }
        String key = buildUserKey(tenantId, userId);
        redisCacheService.evict(key);
        log.debug("Evicted permission cache for tenant={}, user={}", tenantId, userId);
    }

    @Override
    public void evictTenantPermissions(UUID tenantId) {
        if (tenantId == null) {
            return;
        }
        List<TenantMembership> memberships = tenantMembershipRepository.findByTenantIdWithRolesAndUser(tenantId);
        for (TenantMembership m : memberships) {
            evictUserPermissions(tenantId, m.getUser().getId());
        }
        log.info("Evicted permission cache for all members of tenant={}", tenantId);
    }

    @Override
    public void evictUsersWithRole(UUID tenantId, UUID roleId) {
        if (tenantId == null || roleId == null) {
            return;
        }
        List<TenantMembership> memberships = tenantMembershipRepository.findByTenantIdWithRolesAndUser(tenantId);
        for (TenantMembership m : memberships) {
            boolean hasRole = m.getRoles().stream().anyMatch(r -> r.getId().equals(roleId));
            if (hasRole) {
                evictUserPermissions(tenantId, m.getUser().getId());
            }
        }
        log.info("Evicted permission cache for members with role={} in tenant={}", roleId, tenantId);
    }
}
