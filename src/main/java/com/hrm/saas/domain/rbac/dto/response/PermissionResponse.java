package com.hrm.saas.domain.rbac.dto.response;

import com.hrm.saas.domain.rbac.enums.PermissionModule;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class PermissionResponse {
    private UUID id;
    private String code;
    private PermissionModule module;
    private String displayName;
    private String description;
    private Boolean isAssignable;
}
