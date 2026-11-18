package com.hrm.saas.domain.tenant.dto.response;

import com.hrm.saas.domain.tenant.enums.TenantStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantSummaryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID tenantId;
    private String name;
    private String slug;
    private TenantStatus status;
    private String domain;
    private boolean verified;
}
