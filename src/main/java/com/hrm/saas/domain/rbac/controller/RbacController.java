package com.hrm.saas.domain.rbac.controller;

import com.hrm.saas.common.constants.ApiRoutes;
import com.hrm.saas.common.response.ApiResponse;
import com.hrm.saas.domain.auth.security.UserPrincipal;
import com.hrm.saas.domain.rbac.constants.RbacPermissions;
import com.hrm.saas.domain.rbac.dto.request.AssignMemberRolesRequest;
import com.hrm.saas.domain.rbac.dto.request.CloneRoleRequest;
import com.hrm.saas.domain.rbac.dto.request.CreateRoleRequest;
import com.hrm.saas.domain.rbac.dto.request.UpdateRoleRequest;
import com.hrm.saas.domain.rbac.dto.response.MemberRoleAssignmentResponse;
import com.hrm.saas.domain.rbac.dto.response.ModulePermissionsResponse;
import com.hrm.saas.domain.rbac.dto.response.PermissionResponse;
import com.hrm.saas.domain.rbac.dto.response.RoleResponse;
import com.hrm.saas.domain.rbac.service.RbacService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "RBAC & Dynamic Permissions", description = "Role-based access control and tenant permission management")
public class RbacController {

    private final RbacService rbacService;

    // 1. Permission Catalog Endpoints
    @GetMapping(ApiRoutes.Rbac.PERMISSIONS)
    @Operation(summary = "Get all available permissions (flat list)")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getAllPermissions() {
        List<PermissionResponse> permissions = rbacService.getAllPermissions();
        return ResponseEntity.ok(ApiResponse.success(permissions));
    }

    @GetMapping(ApiRoutes.Rbac.PERMISSIONS + "/grouped")
    @Operation(summary = "Get permissions grouped by functional module")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ModulePermissionsResponse>>> getGroupedPermissions() {
        List<ModulePermissionsResponse> grouped = rbacService.getPermissionsGroupedByModule();
        return ResponseEntity.ok(ApiResponse.success(grouped));
    }

    // 2. Roles Management Endpoints
    @GetMapping(ApiRoutes.Rbac.ROLES)
    @Operation(summary = "Get all roles for active tenant")
    @PreAuthorize("@perm.has('" + RbacPermissions.ROLE_VIEW + "')")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getRoles(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<RoleResponse> roles = rbacService.getRoles(principal.getTenantId());
        return ResponseEntity.ok(ApiResponse.success(roles));
    }

    @GetMapping(ApiRoutes.Rbac.ROLES + ApiRoutes.Rbac.ROLE_BY_ID)
    @Operation(summary = "Get role details and allocated permissions")
    @PreAuthorize("@perm.has('" + RbacPermissions.ROLE_VIEW + "')")
    public ResponseEntity<ApiResponse<RoleResponse>> getRole(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        RoleResponse role = rbacService.getRole(principal.getTenantId(), id);
        return ResponseEntity.ok(ApiResponse.success(role));
    }

    @PostMapping(ApiRoutes.Rbac.ROLES)
    @Operation(summary = "Create custom role for active tenant")
    @PreAuthorize("@perm.has('" + RbacPermissions.ROLE_CREATE + "')")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(
            @Valid @RequestBody CreateRoleRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        RoleResponse created = rbacService.createRole(principal.getTenantId(), principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Role created successfully"));
    }

    @PutMapping(ApiRoutes.Rbac.ROLES + ApiRoutes.Rbac.ROLE_BY_ID)
    @Operation(summary = "Update role details and permissions")
    @PreAuthorize("@perm.has('" + RbacPermissions.ROLE_UPDATE + "')")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRole(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoleRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        RoleResponse updated = rbacService.updateRole(principal.getTenantId(), principal.getUserId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Role updated successfully"));
    }

    @DeleteMapping(ApiRoutes.Rbac.ROLES + ApiRoutes.Rbac.ROLE_BY_ID)
    @Operation(summary = "Delete custom role")
    @PreAuthorize("@perm.has('" + RbacPermissions.ROLE_DELETE + "')")
    public ResponseEntity<ApiResponse<Void>> deleteRole(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        rbacService.deleteRole(principal.getTenantId(), id);
        return ResponseEntity.ok(ApiResponse.success(null, "Role deleted successfully"));
    }

    @PostMapping(ApiRoutes.Rbac.ROLES + ApiRoutes.Rbac.ROLE_CLONE)
    @Operation(summary = "Clone existing role into a new custom role")
    @PreAuthorize("@perm.has('" + RbacPermissions.ROLE_CREATE + "')")
    public ResponseEntity<ApiResponse<RoleResponse>> cloneRole(
            @PathVariable UUID id,
            @Valid @RequestBody CloneRoleRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        RoleResponse cloned = rbacService.cloneRole(principal.getTenantId(), principal.getUserId(), id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(cloned, "Role cloned successfully"));
    }

    // 3. User Membership Role Assignments
    @GetMapping(ApiRoutes.Rbac.MEMBERSHIPS + ApiRoutes.Rbac.MEMBERSHIP_ROLES)
    @Operation(summary = "Get roles assigned to a tenant member")
    @PreAuthorize("@perm.has('" + RbacPermissions.ROLE_VIEW + "')")
    public ResponseEntity<ApiResponse<MemberRoleAssignmentResponse>> getMemberRoles(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        MemberRoleAssignmentResponse response = rbacService.getMemberRoles(principal.getTenantId(), id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping(ApiRoutes.Rbac.MEMBERSHIPS + ApiRoutes.Rbac.MEMBERSHIP_ROLES)
    @Operation(summary = "Assign roles to a tenant member")
    @PreAuthorize("@perm.has('" + RbacPermissions.ROLE_ASSIGN + "')")
    public ResponseEntity<ApiResponse<MemberRoleAssignmentResponse>> assignMemberRoles(
            @PathVariable UUID id,
            @Valid @RequestBody AssignMemberRolesRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        MemberRoleAssignmentResponse response = rbacService.assignRolesToMember(
                principal.getTenantId(), principal.getUserId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Member roles updated successfully"));
    }

    // 4. Effective Current User Permissions
    @GetMapping(ApiRoutes.Rbac.ME_PERMISSIONS)
    @Operation(summary = "Get current authenticated user's effective permissions for active tenant")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Set<String>>> getMyEffectivePermissions(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal.getTenantId() == null) {
            return ResponseEntity.ok(ApiResponse.success(Set.of()));
        }
        Set<String> permissions = rbacService.getCurrentUserEffectivePermissions(
                principal.getTenantId(), principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(permissions));
    }
}
