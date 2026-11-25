package com.hrm.saas.domain.rbac.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
public class MemberRoleAssignmentResponse {
    private UUID membershipId;
    private UUID userId;
    private String email;
    private String fullName;
    private List<RoleResponse> assignedRoles;
    private Set<String> effectivePermissions;
}
