package com.hrm.saas.domain.tenant.entity;

import com.hrm.saas.common.entity.BaseEntity;
import com.hrm.saas.domain.tenant.enums.DomainType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "tenant_domains",
        indexes = {
                @Index(name = "idx_tenant_domains_domain", columnList = "domain", unique = true),
                @Index(name = "idx_tenant_domains_lookup", columnList = "domain, is_verified")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantDomain extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "domain", nullable = false, unique = true, length = 255)
    private String domain;

    @Enumerated(EnumType.STRING)
    @Column(name = "domain_type", nullable = false, length = 20)
    @Builder.Default
    private DomainType domainType = DomainType.SUBDOMAIN;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "verification_token", length = 100)
    private String verificationToken;

    @Column(name = "verified_at")
    private Instant verifiedAt;
}
