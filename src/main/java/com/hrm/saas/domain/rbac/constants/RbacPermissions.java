package com.hrm.saas.domain.rbac.constants;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RbacPermissions {

    private RbacPermissions() {}

    // EMPLOYEE MODULE
    public static final String EMPLOYEE_VIEW = "employee.view";
    public static final String EMPLOYEE_CREATE = "employee.create";
    public static final String EMPLOYEE_UPDATE = "employee.update";
    public static final String EMPLOYEE_DELETE = "employee.delete";

    // ATTENDANCE MODULE
    public static final String ATTENDANCE_VIEW = "attendance.view";
    public static final String ATTENDANCE_CREATE = "attendance.create";
    public static final String ATTENDANCE_APPROVE = "attendance.approve";

    // LEAVE MODULE
    public static final String LEAVE_VIEW = "leave.view";
    public static final String LEAVE_APPLY = "leave.apply";
    public static final String LEAVE_APPROVE = "leave.approve";

    // PAYROLL MODULE
    public static final String PAYROLL_VIEW = "payroll.view";
    public static final String PAYROLL_CALCULATE = "payroll.calculate";
    public static final String PAYROLL_APPROVE = "payroll.approve";

    // ROLE MODULE
    public static final String ROLE_VIEW = "role.view";
    public static final String ROLE_CREATE = "role.create";
    public static final String ROLE_UPDATE = "role.update";
    public static final String ROLE_DELETE = "role.delete";
    public static final String ROLE_ASSIGN = "role.assign";

    // SYSTEM MODULE
    public static final String TENANT_SETTINGS_VIEW = "tenant.settings.view";
    public static final String TENANT_SETTINGS_UPDATE = "tenant.settings.update";

    // System Role Codes
    public static final String ROLE_TENANT_ADMIN = "TENANT_ADMIN";
    public static final String ROLE_HR_MANAGER = "HR_MANAGER";
    public static final String ROLE_MANAGER = "MANAGER";
    public static final String ROLE_EMPLOYEE = "EMPLOYEE";

    // Custom Role Template suggestions
    public static final String ROLE_ACCOUNTANT = "ACCOUNTANT";
    public static final String ROLE_PAYROLL_OFFICER = "PAYROLL_OFFICER";
    public static final String ROLE_RECRUITMENT_MANAGER = "RECRUITMENT_MANAGER";
    public static final String ROLE_ATTENDANCE_SUPERVISOR = "ATTENDANCE_SUPERVISOR";

    public static final Set<String> SYSTEM_ROLE_CODES = Set.of(
            ROLE_TENANT_ADMIN,
            ROLE_HR_MANAGER,
            ROLE_MANAGER,
            ROLE_EMPLOYEE
    );

    // Initial permission definitions for system roles
    public static final Map<String, Set<String>> SYSTEM_ROLE_PERMISSIONS = Map.of(
            ROLE_TENANT_ADMIN, Set.of(
                    EMPLOYEE_VIEW, EMPLOYEE_CREATE, EMPLOYEE_UPDATE, EMPLOYEE_DELETE,
                    ATTENDANCE_VIEW, ATTENDANCE_CREATE, ATTENDANCE_APPROVE,
                    LEAVE_VIEW, LEAVE_APPLY, LEAVE_APPROVE,
                    PAYROLL_VIEW, PAYROLL_CALCULATE, PAYROLL_APPROVE,
                    ROLE_VIEW, ROLE_CREATE, ROLE_UPDATE, ROLE_DELETE, ROLE_ASSIGN,
                    TENANT_SETTINGS_VIEW, TENANT_SETTINGS_UPDATE
            ),
            ROLE_HR_MANAGER, Set.of(
                    EMPLOYEE_VIEW, EMPLOYEE_CREATE, EMPLOYEE_UPDATE,
                    ATTENDANCE_VIEW, ATTENDANCE_CREATE, ATTENDANCE_APPROVE,
                    LEAVE_VIEW, LEAVE_APPLY, LEAVE_APPROVE,
                    PAYROLL_VIEW,
                    ROLE_VIEW, ROLE_ASSIGN
            ),
            ROLE_MANAGER, Set.of(
                    EMPLOYEE_VIEW,
                    ATTENDANCE_VIEW, ATTENDANCE_APPROVE,
                    LEAVE_VIEW, LEAVE_APPLY, LEAVE_APPROVE,
                    ROLE_VIEW
            ),
            ROLE_EMPLOYEE, Set.of(
                    EMPLOYEE_VIEW,
                    ATTENDANCE_VIEW, ATTENDANCE_CREATE,
                    LEAVE_VIEW, LEAVE_APPLY
            )
    );
}
