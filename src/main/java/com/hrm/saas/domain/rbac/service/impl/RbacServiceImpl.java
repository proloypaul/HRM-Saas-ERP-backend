package com.hrm.saas.domain.rbac.service.impl;

import com.hrm.saas.common.exception.AppException;
import com.hrm.saas.domain.auth.entity.TenantMembership;
import com.hrm.saas.domain.auth.repository.TenantMembershipRepository;
import com.hrm.saas.domain.rbac.constants.RbacPermissions;
import com.hrm.saas.domain.rbac.dto.request.AssignMemberRolesRequest;
import com.hrm.saas.domain.rbac.dto.request.CloneRoleRequest;
import com.hrm.saas.domain.rbac.dto.request.CreateRoleRequest;
import com.hrm.saas.domain.rbac.dto.request.UpdateRoleRequest;
import com.hrm.saas.domain.rbac.dto.response.MemberRoleAssignmentResponse;
import com.hrm.saas.domain.rbac.dto.response.ModulePermissionsResponse;
import com.hrm.saas.domain.rbac.dto.response.PermissionResponse;
import com.hrm.saas.domain.rbac.dto.response.RoleResponse;
import com.hrm.saas.domain.rbac.entity.Permission;
import com.hrm.saas.domain.rbac.entity.Role;
import com.hrm.saas.domain.rbac.enums.PermissionModule;
import com.hrm.saas.domain.rbac.repository.PermissionRepository;
import com.hrm.saas.domain.rbac.repository.RoleRepository;
import com.hrm.saas.domain.rbac.service.EffectivePermissionService;
import com.hrm.saas.domain.rbac.service.PermissionCacheService;
import com.hrm.saas.domain.rbac.service.RbacService;
import com.hrm.saas.domain.tenant.entity.Tenant;
import com.hrm.saas.domain.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RbacServiceImpl implements RbacService {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;
    private final EffectivePermissionService effectivePermissionService;
    private final PermissionCacheService permissionCacheService;

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> getAllPermissions() {
        return permissionRepository.findByIsAssignableTrueOrderByModuleAscDisplayNameAsc().stream()
                .map(this::mapPermissionResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModulePermissionsResponse> getPermissionsGroupedByModule() {
        List<Permission> permissions = permissionRepository.findByIsAssignableTrueOrderByModuleAscDisplayNameAsc();
        Map<PermissionModule, List<PermissionResponse>> grouped = permissions.stream()
                .map(this::mapPermissionResponse)
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

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getRoles(UUID tenantId) {
        validateTenantExists(tenantId);
        List<Role> roles = roleRepository.findAllByTenantIdWithPermissions(tenantId);
        return roles.stream().map(this::mapRoleResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getRole(UUID tenantId, UUID roleId) {
        Role role = roleRepository.findByIdAndTenantIdWithPermissions(roleId, tenantId)
                .orElseThrow(() -> new AppException("Role not found with ID: " + roleId, HttpStatus.NOT_FOUND));
        return mapRoleResponse(role);
    }

    @Override
    @Transactional
    public RoleResponse createRole(UUID tenantId, UUID currentUserId, CreateRoleRequest request) {
        Tenant tenant = validateTenantExists(tenantId);

        if (roleRepository.existsByTenantIdAndCode(tenantId, request.getCode().toUpperCase())) {
            throw new AppException("A role with code '" + request.getCode() + "' already exists in this workspace");
        }

        // Delegation restriction: Granter cannot grant permissions they do not possess
        validateDelegationAuthority(tenantId, currentUserId, request.getPermissions());

        Set<Permission> permissions = resolvePermissions(request.getPermissions());

        Role role = Role.builder()
                .tenant(tenant)
                .code(request.getCode().toUpperCase())
                .name(request.getName().trim())
                .description(request.getDescription())
                .priority(request.getPriority() != null ? request.getPriority() : 50)
                .isSystem(false)
                .isActive(true)
                .permissions(permissions)
                .build();

        Role saved = roleRepository.save(role);
        log.info("Created custom role '{}' for tenant {}", saved.getCode(), tenantId);
        return mapRoleResponse(saved);
    }

    @Override
    @Transactional
    public RoleResponse updateRole(UUID tenantId, UUID currentUserId, UUID roleId, UpdateRoleRequest request) {
        Role role = roleRepository.findByIdAndTenantIdWithPermissions(roleId, tenantId)
                .orElseThrow(() -> new AppException("Role not found with ID: " + roleId, HttpStatus.NOT_FOUND));

        if (Boolean.TRUE.equals(role.getIsSystem())) {
            // System roles cannot be disabled or renamed to keep baseline integrity
            if (request.getIsActive() != null && !request.getIsActive()) {
                throw new AppException("System roles cannot be deactivated");
            }
        }

        // Delegation restriction
        validateDelegationAuthority(tenantId, currentUserId, request.getPermissions());

        Set<Permission> permissions = resolvePermissions(request.getPermissions());

        role.setName(request.getName().trim());
        role.setDescription(request.getDescription());
        if (request.getPriority() != null) {
            role.setPriority(request.getPriority());
        }
        if (request.getIsActive() != null && !Boolean.TRUE.equals(role.getIsSystem())) {
            role.setIsActive(request.getIsActive());
        }
        role.setPermissions(permissions);

        Role updated = roleRepository.save(role);

        // Instant Redis Cache Invalidation for all users holding this role
        permissionCacheService.evictUsersWithRole(tenantId, roleId);
        log.info("Updated role '{}' ({}) and evicted active user permission caches", updated.getCode(), roleId);

        return mapRoleResponse(updated);
    }

    @Override
    @Transactional
    public void deleteRole(UUID tenantId, UUID roleId) {
        Role role = roleRepository.findByIdAndTenantId(roleId, tenantId)
                .orElseThrow(() -> new AppException("Role not found with ID: " + roleId, HttpStatus.NOT_FOUND));

        if (Boolean.TRUE.equals(role.getIsSystem())) {
            throw new AppException("System roles (" + role.getCode() + ") are protected and cannot be deleted");
        }

        long assignedCount = roleRepository.countAssignedMembers(roleId);
        if (assignedCount > 0) {
            throw new AppException("Cannot delete role '" + role.getName() + "'. It is actively assigned to " + assignedCount + " user(s). Reassign them first.");
        }

        roleRepository.delete(role);
        permissionCacheService.evictUsersWithRole(tenantId, roleId);
        log.info("Deleted custom role '{}' from tenant {}", role.getCode(), tenantId);
    }

    @Override
    @Transactional
    public RoleResponse cloneRole(UUID tenantId, UUID currentUserId, UUID sourceRoleId, CloneRoleRequest request) {
        Role sourceRole = roleRepository.findByIdAndTenantIdWithPermissions(sourceRoleId, tenantId)
                .orElseThrow(() -> new AppException("Source role not found with ID: " + sourceRoleId, HttpStatus.NOT_FOUND));

        CreateRoleRequest createReq = new CreateRoleRequest();
        createReq.setCode(request.getNewCode());
        createReq.setName(request.getNewName());
        createReq.setDescription(request.getNewDescription() != null ? request.getNewDescription() : "Cloned from " + sourceRole.getName());
        createReq.setPriority(sourceRole.getPriority() + 5);
        createReq.setPermissions(sourceRole.getPermissions().stream().map(Permission::getCode).collect(Collectors.toSet()));

        return createRole(tenantId, currentUserId, createReq);
    }

    @Override
    @Transactional(readOnly = true)
    public MemberRoleAssignmentResponse getMemberRoles(UUID tenantId, UUID membershipId) {
        TenantMembership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new AppException("Membership not found with ID: " + membershipId, HttpStatus.NOT_FOUND));

        if (!membership.getTenant().getId().equals(tenantId)) {
            throw new AppException("Membership does not belong to this tenant");
        }

        Set<String> effective = effectivePermissionService.getEffectivePermissions(tenantId, membership.getUser().getId());
        List<RoleResponse> assigned = membership.getRoles().stream().map(this::mapRoleResponse).toList();

        return MemberRoleAssignmentResponse.builder()
                .membershipId(membership.getId())
                .userId(membership.getUser().getId())
                .email(membership.getUser().getEmail())
                .fullName(membership.getUser().getFirstName() + " " + membership.getUser().getLastName())
                .assignedRoles(assigned)
                .effectivePermissions(effective)
                .build();
    }

    @Override
    @Transactional
    public MemberRoleAssignmentResponse assignRolesToMember(UUID tenantId, UUID currentUserId, UUID membershipId, AssignMemberRolesRequest request) {
        TenantMembership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new AppException("Membership not found with ID: " + membershipId, HttpStatus.NOT_FOUND));

        if (!membership.getTenant().getId().equals(tenantId)) {
            throw new AppException("Membership does not belong to this tenant");
        }

        Set<Role> rolesToAssign = new HashSet<>();
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            for (UUID rId : request.getRoleIds()) {
                Role role = roleRepository.findByIdAndTenantIdWithPermissions(rId, tenantId)
                        .orElseThrow(() -> new AppException("Role not found with ID: " + rId, HttpStatus.NOT_FOUND));
                
                // Delegation check: Granter cannot assign roles with permissions they don't have
                Set<String> requiredPerms = role.getPermissions().stream().map(Permission::getCode).collect(Collectors.toSet());
                validateDelegationAuthority(tenantId, currentUserId, requiredPerms);

                rolesToAssign.add(role);
            }
        }

        membership.setRoles(rolesToAssign);
        membershipRepository.save(membership);

        // Invalidate Redis permission cache for the target user immediately
        permissionCacheService.evictUserPermissions(tenantId, membership.getUser().getId());
        log.info("Assigned {} role(s) to membership {} (user={}) in tenant {}",
                rolesToAssign.size(), membershipId, membership.getUser().getId(), tenantId);

        return getMemberRoles(tenantId, membershipId);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getCurrentUserEffectivePermissions(UUID tenantId, UUID currentUserId) {
        return effectivePermissionService.getEffectivePermissions(tenantId, currentUserId);
    }

    private void validateDelegationAuthority(UUID tenantId, UUID currentUserId, Set<String> requestedPermissions) {
        if (currentUserId == null || requestedPermissions == null || requestedPermissions.isEmpty()) {
            return;
        }

        Set<String> granterPermissions = effectivePermissionService.getEffectivePermissions(tenantId, currentUserId);

        // If granter has full TENANT_ADMIN / all perms, pass
        if (granterPermissions.containsAll(requestedPermissions)) {
            return;
        }

        Set<String> missingPermissions = new HashSet<>(requestedPermissions);
        missingPermissions.removeAll(granterPermissions);

        if (!missingPermissions.isEmpty()) {
            log.warn("Privilege escalation prevented! User {} in tenant {} attempted to delegate permissions not held: {}",
                    currentUserId, tenantId, missingPermissions);
            throw new AppException("Privilege Escalation Detected: You cannot grant permissions you do not possess: " + missingPermissions);
        }
    }

    private Set<Permission> resolvePermissions(Set<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Permission> found = permissionRepository.findByCodeIn(codes);
        if (found.size() != codes.size()) {
            Set<String> foundCodes = found.stream().map(Permission::getCode).collect(Collectors.toSet());
            Set<String> missing = new HashSet<>(codes);
            missing.removeAll(foundCodes);
            throw new AppException("Invalid permission codes: " + missing);
        }
        return found;
    }

    private Tenant validateTenantExists(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new AppException("Tenant not found with ID: " + tenantId, HttpStatus.NOT_FOUND));
    }

    private PermissionResponse mapPermissionResponse(Permission p) {
        return PermissionResponse.builder()
                .id(p.getId())
                .code(p.getCode())
                .module(p.getModule())
                .displayName(p.getDisplayName())
                .description(p.getDescription())
                .isAssignable(p.getIsAssignable())
                .build();
    }

    private RoleResponse mapRoleResponse(Role r) {
        long count = roleRepository.countAssignedMembers(r.getId());
        Set<String> permCodes = r.getPermissions() != null
                ? r.getPermissions().stream().map(Permission::getCode).collect(Collectors.toSet())
                : Collections.emptySet();

        return RoleResponse.builder()
                .id(r.getId())
                .tenantId(r.getTenant() != null ? r.getTenant().getId() : null)
                .code(r.getCode())
                .name(r.getName())
                .description(r.getDescription())
                .isSystem(r.getIsSystem())
                .isActive(r.getIsActive())
                .priority(r.getPriority())
                .permissions(permCodes)
                .memberCount(count)
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }

    private String formatModuleName(PermissionModule module) {
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
