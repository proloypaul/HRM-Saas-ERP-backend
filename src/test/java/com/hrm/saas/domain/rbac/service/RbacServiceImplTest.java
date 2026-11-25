package com.hrm.saas.domain.rbac.service;

import com.hrm.saas.common.exception.AppException;
import com.hrm.saas.domain.auth.entity.TenantMembership;
import com.hrm.saas.domain.auth.entity.User;
import com.hrm.saas.domain.auth.repository.TenantMembershipRepository;
import com.hrm.saas.domain.rbac.constants.RbacPermissions;
import com.hrm.saas.domain.rbac.dto.request.AssignMemberRolesRequest;
import com.hrm.saas.domain.rbac.dto.request.CreateRoleRequest;
import com.hrm.saas.domain.rbac.dto.response.RoleResponse;
import com.hrm.saas.domain.rbac.entity.Permission;
import com.hrm.saas.domain.rbac.entity.Role;
import com.hrm.saas.domain.rbac.enums.PermissionModule;
import com.hrm.saas.domain.rbac.repository.PermissionRepository;
import com.hrm.saas.domain.rbac.repository.RoleRepository;
import com.hrm.saas.domain.rbac.service.impl.EffectivePermissionServiceImpl;
import com.hrm.saas.domain.rbac.service.impl.RbacServiceImpl;
import com.hrm.saas.domain.tenant.entity.Tenant;
import com.hrm.saas.domain.tenant.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RbacServiceImplTest {

    @Mock
    private PermissionRepository permissionRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private TenantMembershipRepository membershipRepository;
    @Mock
    private EffectivePermissionService effectivePermissionService;
    @Mock
    private PermissionCacheService permissionCacheService;

    @InjectMocks
    private RbacServiceImpl rbacService;

    private UUID tenantAId;
    private UUID tenantBId;
    private UUID adminUserId;
    private UUID hrUserId;
    private Tenant tenantA;
    private Tenant tenantB;
    private Permission payrollViewPerm;
    private Permission payrollApprovePerm;

    @BeforeEach
    void setUp() {
        tenantAId = UUID.randomUUID();
        tenantBId = UUID.randomUUID();
        adminUserId = UUID.randomUUID();
        hrUserId = UUID.randomUUID();

        tenantA = Tenant.builder().id(tenantAId).name("Company Alpha").slug("alpha").build();
        tenantB = Tenant.builder().id(tenantBId).name("Company Beta").slug("beta").build();

        payrollViewPerm = Permission.builder()
                .id(UUID.randomUUID())
                .code(RbacPermissions.PAYROLL_VIEW)
                .module(PermissionModule.PAYROLL)
                .displayName("View Payroll")
                .isAssignable(true)
                .build();

        payrollApprovePerm = Permission.builder()
                .id(UUID.randomUUID())
                .code(RbacPermissions.PAYROLL_APPROVE)
                .module(PermissionModule.PAYROLL)
                .displayName("Approve Payroll")
                .isAssignable(true)
                .build();
    }

    @Test
    @DisplayName("Tenant A creates Accountant role: Tenant isolation check")
    void testTenantCreatesCustomRole() {
        when(tenantRepository.findById(tenantAId)).thenReturn(Optional.of(tenantA));
        when(roleRepository.existsByTenantIdAndCode(tenantAId, "ACCOUNTANT")).thenReturn(false);
        when(effectivePermissionService.getEffectivePermissions(tenantAId, adminUserId))
                .thenReturn(Set.of(RbacPermissions.PAYROLL_VIEW, RbacPermissions.ROLE_CREATE));
        when(permissionRepository.findByCodeIn(Set.of(RbacPermissions.PAYROLL_VIEW)))
                .thenReturn(Set.of(payrollViewPerm));

        Role savedRole = Role.builder()
                .id(UUID.randomUUID())
                .tenant(tenantA)
                .code("ACCOUNTANT")
                .name("Accountant")
                .isSystem(false)
                .permissions(Set.of(payrollViewPerm))
                .build();

        when(roleRepository.save(any(Role.class))).thenReturn(savedRole);

        CreateRoleRequest request = new CreateRoleRequest();
        request.setCode("ACCOUNTANT");
        request.setName("Accountant");
        request.setPermissions(Set.of(RbacPermissions.PAYROLL_VIEW));

        RoleResponse response = rbacService.createRole(tenantAId, adminUserId, request);

        assertThat(response).isNotNull();
        assertThat(response.getCode()).isEqualTo("ACCOUNTANT");
        assertThat(response.getPermissions()).contains(RbacPermissions.PAYROLL_VIEW);
    }

    @Test
    @DisplayName("Tenant isolation: Tenant B cannot access Tenant A custom role")
    void testTenantCannotAccessOtherTenantRole() {
        UUID roleAId = UUID.randomUUID();
        when(roleRepository.findByIdAndTenantIdWithPermissions(roleAId, tenantBId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> rbacService.getRole(tenantBId, roleAId))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Role not found");
    }

    @Test
    @DisplayName("Delegation Restriction: Granter cannot grant permissions they do not possess")
    void testDelegationRestrictionPreventsPrivilegeEscalation() {
        when(tenantRepository.findById(tenantAId)).thenReturn(Optional.of(tenantA));
        when(roleRepository.existsByTenantIdAndCode(tenantAId, "SUPER_ACCOUNTANT")).thenReturn(false);

        // HR User only has payroll.view, but attempts to grant payroll.approve
        when(effectivePermissionService.getEffectivePermissions(tenantAId, hrUserId))
                .thenReturn(Set.of(RbacPermissions.PAYROLL_VIEW));

        CreateRoleRequest request = new CreateRoleRequest();
        request.setCode("SUPER_ACCOUNTANT");
        request.setName("Super Accountant");
        request.setPermissions(Set.of(RbacPermissions.PAYROLL_VIEW, RbacPermissions.PAYROLL_APPROVE));

        assertThatThrownBy(() -> rbacService.createRole(tenantAId, hrUserId, request))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Privilege Escalation Detected");

        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    @DisplayName("System roles cannot be deleted")
    void testSystemRoleProtection() {
        UUID systemRoleId = UUID.randomUUID();
        Role systemRole = Role.builder()
                .id(systemRoleId)
                .tenant(tenantA)
                .code(RbacPermissions.ROLE_TENANT_ADMIN)
                .name("Tenant Administrator")
                .isSystem(true)
                .build();

        when(roleRepository.findByIdAndTenantId(systemRoleId, tenantAId))
                .thenReturn(Optional.of(systemRole));

        assertThatThrownBy(() -> rbacService.deleteRole(tenantAId, systemRoleId))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("protected and cannot be deleted");

        verify(roleRepository, never()).delete(any(Role.class));
    }

    @Test
    @DisplayName("Role assignment triggers real-time Redis cache invalidation")
    void testRoleAssignmentInvalidatesUserPermissionCache() {
        UUID membershipId = UUID.randomUUID();
        User targetUser = User.builder().id(UUID.randomUUID()).email("alice@company.com").firstName("Alice").lastName("Smith").build();
        TenantMembership membership = TenantMembership.builder()
                .id(membershipId)
                .user(targetUser)
                .tenant(tenantA)
                .roles(new HashSet<>())
                .build();

        UUID accountantRoleId = UUID.randomUUID();
        Role accountantRole = Role.builder()
                .id(accountantRoleId)
                .tenant(tenantA)
                .code("ACCOUNTANT")
                .name("Accountant")
                .permissions(Set.of(payrollViewPerm))
                .build();

        when(membershipRepository.findById(membershipId)).thenReturn(Optional.of(membership));
        when(roleRepository.findByIdAndTenantIdWithPermissions(accountantRoleId, tenantAId))
                .thenReturn(Optional.of(accountantRole));
        when(effectivePermissionService.getEffectivePermissions(tenantAId, adminUserId))
                .thenReturn(Set.of(RbacPermissions.PAYROLL_VIEW, RbacPermissions.ROLE_ASSIGN));

        AssignMemberRolesRequest req = new AssignMemberRolesRequest();
        req.setRoleIds(Set.of(accountantRoleId));

        rbacService.assignRolesToMember(tenantAId, adminUserId, membershipId, req);

        // Verify Redis permission cache was immediately evicted for target user
        verify(permissionCacheService).evictUserPermissions(tenantAId, targetUser.getId());
        verify(membershipRepository).save(membership);
    }
}
