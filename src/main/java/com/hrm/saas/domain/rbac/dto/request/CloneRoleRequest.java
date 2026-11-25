package com.hrm.saas.domain.rbac.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CloneRoleRequest {

    @NotBlank(message = "Target role code is required")
    @Pattern(regexp = "^[A-Z0-9_]{3,50}$", message = "Target role code must be uppercase alphanumeric with underscores (3-50 chars)")
    private String newCode;

    @NotBlank(message = "Target role name is required")
    @Size(min = 2, max = 100, message = "Target role name must be between 2 and 100 characters")
    private String newName;

    private String newDescription;
}
