package com.hrm.saas.domain.rbac.mapper;

import com.hrm.saas.domain.auth.entity.TenantMembership;
import com.hrm.saas.domain.rbac.dto.request.CreateRoleRequest;
import com.hrm.saas.domain.rbac.dto.response.MemberRoleAssignmentResponse;
import com.hrm.saas.domain.rbac.dto.response.ModulePermissionsResponse;
import com.hrm.saas.domain.rbac.dto.response.PermissionResponse;
import com.hrm.saas.domain.rbac.dto.response.RoleResponse;
import com.hrm.saas.domain.rbac.entity.Permission;
import com.hrm.saas.domain.rbac.entity.Role;
import com.hrm.saas.domain.rbac.enums.PermissionModule;
import com.hrm.saas.domain.tenant.entity.Tenant;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class RbacMapper {

    /**
     * Converts CreateRoleRequest DTO to Role entity
     */
    public Role toEntity(CreateRoleRequest request, Tenant tenant, Set<Permission> permissions) {
        if (request == null) {
            return null;
        }
        return Role.builder()
                .tenant(tenant)
                .code(request.getCode().trim().toUpperCase())
                .name(request.getName().trim())
                .description(request.getDescription())
                .priority(request.getPriority() != null ? request.getPriority() : 50)
                .isSystem(false)
                .isActive(true)
                .permissions(permissions != null ? permissions : new HashSet<>())
                .build();
    }

    /**
     * Converts Permission entity to PermissionResponse DTO
     */
    public PermissionResponse toPermissionResponse(Permission permission) {
        if (permission == null) {
            return null;
        }
        return PermissionResponse.builder()
                .id(permission.getId())
                .code(permission.getCode())
                .module(permission.getModule())
                .displayName(permission.getDisplayName())
                .description(permission.getDescription())
                .isAssignable(permission.getIsAssignable())
                .build();
    }

    /**
     * Groups a list of Permission entities into ModulePermissionsResponse DTOs
     */
    public List<ModulePermissionsResponse> toGroupedModuleResponses(List<Permission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return Collections.emptyList();
        }

        Map<PermissionModule, List<PermissionResponse>> grouped = permissions.stream()
                .map(this::toPermissionResponse)
                .collect(Collectors.groupingBy(PermissionResponse::getModule, LinkedHashMap::new, Collectors.toList()));

        List<ModulePermissionsResponse> result = new ArrayList<>();
        for (PermissionModule module : PermissionModule.values()) {
            List<PermissionResponse> modulePerms = grouped.getOrDefault(module, List.of());
            if (!modulePerms.isEmpty()) {
                result.add(ModulePermissionsResponse.builder()
                        .module(module)
                        .moduleName(formatModuleName(module))
                        .permissions(modulePerms)
                        .build());
            }
        }
        return result;
    }

    /**
     * Converts Role entity to RoleResponse DTO
     */
    public RoleResponse toRoleResponse(Role role, long memberCount) {
        if (role == null) {
            return null;
        }

        Set<String> permCodes = role.getPermissions() != null
                ? role.getPermissions().stream().map(Permission::getCode).collect(Collectors.toSet())
                : Collections.emptySet();

        return RoleResponse.builder()
                .id(role.getId())
                .tenantId(role.getTenant() != null ? role.getTenant().getId() : null)
                .code(role.getCode())
                .name(role.getName())
                .description(role.getDescription())
                .isSystem(role.getIsSystem())
                .isActive(role.getIsActive())
                .priority(role.getPriority())
                .permissions(permCodes)
                .memberCount(memberCount)
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .build();
    }

    /**
     * Converts TenantMembership with assigned roles and effective permissions to MemberRoleAssignmentResponse
     */
    public MemberRoleAssignmentResponse toMemberRoleAssignmentResponse(
            TenantMembership membership,
            List<RoleResponse> assignedRoles,
            Set<String> effectivePermissions
    ) {
        if (membership == null) {
            return null;
        }

        String fullName = (membership.getUser() != null)
                ? (membership.getUser().getFirstName() + " " + membership.getUser().getLastName()).trim()
                : "";

        return MemberRoleAssignmentResponse.builder()
                .membershipId(membership.getId())
                .userId(membership.getUser() != null ? membership.getUser().getId() : null)
                .email(membership.getUser() != null ? membership.getUser().getEmail() : null)
                .fullName(fullName)
                .assignedRoles(assignedRoles != null ? assignedRoles : Collections.emptyList())
                .effectivePermissions(effectivePermissions != null ? effectivePermissions : Collections.emptySet())
                .build();
    }

    public String formatModuleName(PermissionModule module) {
        if (module == null) return "";
        return switch (module) {
            case EMPLOYEE -> "Employee Directory";
            case ATTENDANCE -> "Attendance & Timesheet";
            case LEAVE -> "Leave Management";
            case PAYROLL -> "Payroll & Compensation";
            case ROLE -> "Role & Access Control";
            case SYSTEM -> "System & Settings";
        };
    }
}
