package com.hrm.saas.domain.tenant.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTenantRequest {

    @Size(min = 2, max = 100, message = "Company name must be between 2 and 100 characters")
    private String name;

    @Size(max = 150, message = "Legal name cannot exceed 150 characters")
    private String legalName;

    @Email(message = "Company email must be a valid email address")
    @Size(max = 150, message = "Email cannot exceed 150 characters")
    private String companyEmail;

    @Size(max = 30, message = "Phone cannot exceed 30 characters")
    private String phone;

    private String timezone;

    private String currency;
}
