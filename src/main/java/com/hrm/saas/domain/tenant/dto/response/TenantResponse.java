package com.hrm.saas.domain.tenant.dto.response;

import com.hrm.saas.domain.tenant.enums.TenantStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantResponse {
    private UUID id;
    private String name;
    private String slug;
    private String legalName;
    private String companyEmail;
    private String phone;
    private TenantStatus status;
    private String timezone;
    private String currency;
    private List<TenantDomainResponse> domains;
    private String settings;
    private Instant createdAt;
    private Instant updatedAt;
}
