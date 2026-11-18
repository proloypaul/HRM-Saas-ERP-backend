package com.hrm.saas.domain.tenant.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTenantRequest {

    @NotBlank(message = "Company name is mandatory")
    @Size(min = 2, max = 100, message = "Company name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Tenant slug is mandatory")
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug must contain only lowercase alphanumeric characters and hyphens")
    @Size(min = 3, max = 63, message = "Slug must be between 3 and 63 characters")
    private String slug;

    @Size(max = 150, message = "Legal name cannot exceed 150 characters")
    private String legalName;

    @NotBlank(message = "Company email is mandatory")
    @Email(message = "Company email must be a valid email address")
    @Size(max = 150, message = "Email cannot exceed 150 characters")
    private String companyEmail;

    @Size(max = 30, message = "Phone cannot exceed 30 characters")
    private String phone;

    @Builder.Default
    private String timezone = "UTC";

    @Builder.Default
    private String currency = "USD";
}
