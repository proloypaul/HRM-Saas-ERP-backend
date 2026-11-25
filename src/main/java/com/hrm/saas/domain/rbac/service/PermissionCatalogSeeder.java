package com.hrm.saas.domain.rbac.service;

import com.hrm.saas.domain.rbac.constants.RbacPermissions;
import com.hrm.saas.domain.rbac.entity.Permission;
import com.hrm.saas.domain.rbac.entity.Role;
import com.hrm.saas.domain.rbac.enums.PermissionModule;
import com.hrm.saas.domain.rbac.repository.PermissionRepository;
import com.hrm.saas.domain.rbac.repository.RoleRepository;
import com.hrm.saas.domain.tenant.entity.Tenant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionCatalogSeeder {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedCatalog() {
        log.info("Checking and seeding RBAC permission catalog...");
        List<PermissionDefinition> definitions = List.of(
                // EMPLOYEE
                new PermissionDefinition(RbacPermissions.EMPLOYEE_VIEW, PermissionModule.EMPLOYEE, "View Employees", "View employee profiles and organizational directory"),
                new PermissionDefinition(RbacPermissions.EMPLOYEE_CREATE, PermissionModule.EMPLOYEE, "Create Employee", "Onboard and register new employees"),
                new PermissionDefinition(RbacPermissions.EMPLOYEE_UPDATE, PermissionModule.EMPLOYEE, "Update Employee", "Modify employee job details, departments, and profile"),
                new PermissionDefinition(RbacPermissions.EMPLOYEE_DELETE, PermissionModule.EMPLOYEE, "Delete Employee", "Archive or remove employee records"),

                // ATTENDANCE
                new PermissionDefinition(RbacPermissions.ATTENDANCE_VIEW, PermissionModule.ATTENDANCE, "View Attendance", "View daily punches, biometric logs, and monthly timesheets"),
                new PermissionDefinition(RbacPermissions.ATTENDANCE_CREATE, PermissionModule.ATTENDANCE, "Clock-in / Regularize Attendance", "Submit attendance punches and regularization requests"),
                new PermissionDefinition(RbacPermissions.ATTENDANCE_APPROVE, PermissionModule.ATTENDANCE, "Approve Attendance", "Approve or reject team attendance regularization requests"),

                // LEAVE
                new PermissionDefinition(RbacPermissions.LEAVE_VIEW, PermissionModule.LEAVE, "View Leave", "View leave quotas, balances, and public holiday calendars"),
                new PermissionDefinition(RbacPermissions.LEAVE_APPLY, PermissionModule.LEAVE, "Apply for Leave", "Submit casual, sick, or annual leave applications"),
                new PermissionDefinition(RbacPermissions.LEAVE_APPROVE, PermissionModule.LEAVE, "Approve Leave", "Authorize or reject employee leave requests"),

                // PAYROLL
                new PermissionDefinition(RbacPermissions.PAYROLL_VIEW, PermissionModule.PAYROLL, "View Payroll", "View salary sheets, allowances, deductions, and payslips"),
                new PermissionDefinition(RbacPermissions.PAYROLL_CALCULATE, PermissionModule.PAYROLL, "Calculate Payroll", "Run monthly payroll calculation formulas and generate reports"),
                new PermissionDefinition(RbacPermissions.PAYROLL_APPROVE, PermissionModule.PAYROLL, "Approve Payroll", "Finalize payroll disbursements and release payment vouchers"),

                // ROLE
                new PermissionDefinition(RbacPermissions.ROLE_VIEW, PermissionModule.ROLE, "View Roles", "View role catalog and permission allocations in current tenant"),
                new PermissionDefinition(RbacPermissions.ROLE_CREATE, PermissionModule.ROLE, "Create Custom Roles", "Configure new custom roles from permission catalog"),
                new PermissionDefinition(RbacPermissions.ROLE_UPDATE, PermissionModule.ROLE, "Update Roles", "Modify permissions and details for tenant roles"),
                new PermissionDefinition(RbacPermissions.ROLE_DELETE, PermissionModule.ROLE, "Delete Custom Roles", "Delete unassigned custom roles"),
                new PermissionDefinition(RbacPermissions.ROLE_ASSIGN, PermissionModule.ROLE, "Assign Roles to Members", "Grant or change assigned roles for workspace users"),

                // SYSTEM
                new PermissionDefinition(RbacPermissions.TENANT_SETTINGS_VIEW, PermissionModule.SYSTEM, "View Tenant Settings", "View company configuration and workspace settings"),
                new PermissionDefinition(RbacPermissions.TENANT_SETTINGS_UPDATE, PermissionModule.SYSTEM, "Update Tenant Settings", "Update workspace branding, domains, and global defaults")
        );

        for (PermissionDefinition def : definitions) {
            permissionRepository.findByCode(def.code).orElseGet(() -> {
                Permission p = Permission.builder()
                        .code(def.code)
                        .module(def.module)
                        .displayName(def.displayName)
                        .description(def.description)
                        .isAssignable(true)
                        .build();
                return permissionRepository.save(p);
            });
        }
        log.info("RBAC permission catalog initialized with {} permissions.", definitions.size());
    }

    /**
     * Seeds system roles for a newly registered or existing tenant workspace.
     */
    @Transactional
    public List<Role> seedSystemRolesForTenant(Tenant tenant) {
        log.info("Seeding system roles for tenant: {}", tenant.getId());
        List<Role> seededRoles = new ArrayList<>();

        Map<String, Permission> permMap = new HashMap<>();
        permissionRepository.findAll().forEach(p -> permMap.put(p.getCode(), p));

        // 1. TENANT_ADMIN
        Role admin = createSystemRole(tenant, RbacPermissions.ROLE_TENANT_ADMIN, "Tenant Administrator",
                "Full operational and administrative authority across the entire workspace", 10,
                RbacPermissions.SYSTEM_ROLE_PERMISSIONS.get(RbacPermissions.ROLE_TENANT_ADMIN), permMap);
        seededRoles.add(admin);

        // 2. HR_MANAGER
        Role hrManager = createSystemRole(tenant, RbacPermissions.ROLE_HR_MANAGER, "HR Manager",
                "Manages employee profiles, leave, attendance, and basic payroll visibility", 30,
                RbacPermissions.SYSTEM_ROLE_PERMISSIONS.get(RbacPermissions.ROLE_HR_MANAGER), permMap);
        seededRoles.add(hrManager);

        // 3. MANAGER
        Role manager = createSystemRole(tenant, RbacPermissions.ROLE_MANAGER, "Line Manager",
                "Approves team attendance and leave, with visibility into team member details", 50,
                RbacPermissions.SYSTEM_ROLE_PERMISSIONS.get(RbacPermissions.ROLE_MANAGER), permMap);
        seededRoles.add(manager);

        // 4. EMPLOYEE
        Role employee = createSystemRole(tenant, RbacPermissions.ROLE_EMPLOYEE, "Employee",
                "Self-service portal for personal profile, clock-in, leave application, and timesheet", 90,
                RbacPermissions.SYSTEM_ROLE_PERMISSIONS.get(RbacPermissions.ROLE_EMPLOYEE), permMap);
        seededRoles.add(employee);

        return seededRoles;
    }

    private Role createSystemRole(Tenant tenant, String code, String name, String description,
                                  int priority, Set<String> permCodes, Map<String, Permission> permMap) {
        return roleRepository.findByTenantIdAndCode(tenant.getId(), code).orElseGet(() -> {
            Set<Permission> perms = new HashSet<>();
            if (permCodes != null) {
                for (String pCode : permCodes) {
                    Permission p = permMap.get(pCode);
                    if (p != null) {
                        perms.add(p);
                    }
                }
            }
            Role role = Role.builder()
                    .tenant(tenant)
                    .code(code)
                    .name(name)
                    .description(description)
                    .isSystem(true)
                    .isActive(true)
                    .priority(priority)
                    .permissions(perms)
                    .build();
            return roleRepository.save(role);
        });
    }

    private record PermissionDefinition(String code, PermissionModule module, String displayName, String description) {}
}
