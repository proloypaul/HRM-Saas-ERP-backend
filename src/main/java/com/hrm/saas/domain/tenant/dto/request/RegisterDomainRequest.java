package com.hrm.saas.domain.tenant.dto.request;

import com.hrm.saas.domain.tenant.enums.DomainType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class RegisterDomainRequest {

    @NotBlank(message = "Domain name is mandatory")
    @Pattern(
            regexp = "^(?=.{1,253}$)(?!-)[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*$",
            message = "Invalid domain name format"
    )
    @Size(max = 255, message = "Domain cannot exceed 255 characters")
    private String domain;

    @NotNull(message = "Domain type is mandatory")
    private DomainType domainType;

    private boolean isPrimary;
}
