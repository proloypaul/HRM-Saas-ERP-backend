package com.hrm.saas.domain.auth.dto.response;

import com.hrm.saas.domain.auth.enums.MembershipRole;
import com.hrm.saas.domain.auth.enums.MembershipStatus;
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
public class TenantMembershipResponse {
    private UUID membershipId;
    private UUID tenantId;
    private String tenantName;
    private String tenantSlug;
    private MembershipRole role;
    private MembershipStatus status;
    private Instant joinedAt;
}
