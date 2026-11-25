package com.hrm.saas.domain.rbac.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

@Data
public class AssignMemberRolesRequest {

    @NotNull(message = "Role IDs set cannot be null")
    private Set<UUID> roleIds;
}
