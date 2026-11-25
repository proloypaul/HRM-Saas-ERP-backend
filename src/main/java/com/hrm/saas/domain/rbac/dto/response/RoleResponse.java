package com.hrm.saas.domain.rbac.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
public class RoleResponse {
    private UUID id;
    private UUID tenantId;
    private String code;
    private String name;
    private String description;
    private Boolean isSystem;
    private Boolean isActive;
    private Integer priority;
    private Set<String> permissions;
    private Long memberCount;
    private Instant createdAt;
    private Instant updatedAt;
}
