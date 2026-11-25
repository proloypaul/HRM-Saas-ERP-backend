package com.hrm.saas.domain.rbac.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class CreateRoleRequest {

    @NotBlank(message = "Role code is required")
    @Pattern(regexp = "^[A-Z0-9_]{3,50}$", message = "Role code must be uppercase alphanumeric with underscores (3-50 chars)")
    private String code;

    @NotBlank(message = "Role name is required")
    @Size(min = 2, max = 100, message = "Role name must be between 2 and 100 characters")
    private String name;

    private String description;

    private Integer priority = 50;

    @NotNull(message = "Permissions list cannot be null")
    private Set<String> permissions;
}
