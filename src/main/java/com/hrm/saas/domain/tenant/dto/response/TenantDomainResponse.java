package com.hrm.saas.domain.tenant.dto.response;

import com.hrm.saas.domain.tenant.enums.DomainType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantDomainResponse {
    private UUID id;
    private UUID tenantId;
    private String domain;
    private DomainType domainType;
    private boolean isPrimary;
    private boolean isVerified;
    private String verificationToken;
    private Instant verifiedAt;
    private Instant createdAt;
}
