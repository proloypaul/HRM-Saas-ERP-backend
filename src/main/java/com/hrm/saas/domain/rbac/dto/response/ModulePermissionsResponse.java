package com.hrm.saas.domain.rbac.dto.response;

import com.hrm.saas.domain.rbac.enums.PermissionModule;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ModulePermissionsResponse {
    private PermissionModule module;
    private String moduleName;
    private List<PermissionResponse> permissions;
}
