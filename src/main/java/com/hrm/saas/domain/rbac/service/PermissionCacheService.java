package com.hrm.saas.domain.rbac.service;

import java.util.Set;
import java.util.UUID;

public interface PermissionCacheService {

    Set<String> getCachedPermissions(UUID tenantId, UUID userId);

    void cachePermissions(UUID tenantId, UUID userId, Set<String> permissions);

    void evictUserPermissions(UUID tenantId, UUID userId);

    void evictTenantPermissions(UUID tenantId);

    void evictUsersWithRole(UUID tenantId, UUID roleId);
}
