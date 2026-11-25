package com.hrm.saas.domain.rbac.service;

import java.util.Set;
import java.util.UUID;

public interface EffectivePermissionService {

    Set<String> getEffectivePermissions(UUID tenantId, UUID userId);

    boolean hasPermission(UUID tenantId, UUID userId, String permissionCode);
}
