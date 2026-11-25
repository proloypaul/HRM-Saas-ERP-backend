package com.hrm.saas.domain.rbac.service;

import com.hrm.saas.domain.rbac.dto.request.AssignMemberRolesRequest;
import com.hrm.saas.domain.rbac.dto.request.CloneRoleRequest;
import com.hrm.saas.domain.rbac.dto.request.CreateRoleRequest;
import com.hrm.saas.domain.rbac.dto.request.UpdateRoleRequest;
import com.hrm.saas.domain.rbac.dto.response.MemberRoleAssignmentResponse;
import com.hrm.saas.domain.rbac.dto.response.ModulePermissionsResponse;
import com.hrm.saas.domain.rbac.dto.response.PermissionResponse;
import com.hrm.saas.domain.rbac.dto.response.RoleResponse;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface RbacService {

    List<PermissionResponse> getAllPermissions();

    List<ModulePermissionsResponse> getPermissionsGroupedByModule();

    List<RoleResponse> getRoles(UUID tenantId);

    RoleResponse getRole(UUID tenantId, UUID roleId);

    RoleResponse createRole(UUID tenantId, UUID currentUserId, CreateRoleRequest request);

    RoleResponse updateRole(UUID tenantId, UUID currentUserId, UUID roleId, UpdateRoleRequest request);

    void deleteRole(UUID tenantId, UUID roleId);

    RoleResponse cloneRole(UUID tenantId, UUID currentUserId, UUID sourceRoleId, CloneRoleRequest request);

    MemberRoleAssignmentResponse getMemberRoles(UUID tenantId, UUID membershipId);

    MemberRoleAssignmentResponse assignRolesToMember(UUID tenantId, UUID currentUserId, UUID membershipId, AssignMemberRolesRequest request);

    Set<String> getCurrentUserEffectivePermissions(UUID tenantId, UUID currentUserId);
}
