package com.hrm.saas.domain.auth.dto.response;

import com.hrm.saas.domain.auth.enums.MembershipStatus;
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

public class TenantMembershipResponse {
    private UUID membershipId;
    private UUID tenantId;
    private String tenantName;
    private String tenantSlug;
    private Boolean isOwner;
    private List<String> roles;
    private MembershipStatus status;
    private Instant joinedAt;
}
