package com.hrm.saas.domain.tenant.entity;

import com.hrm.saas.common.entity.BaseEntity;
import com.hrm.saas.domain.tenant.enums.TenantStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "tenants",
        indexes = {
                @Index(name = "idx_tenants_slug", columnList = "slug", unique = true),
                @Index(name = "idx_tenants_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 63)
    private String slug;

    @Column(name = "legal_name", length = 150)
    private String legalName;

    @Column(name = "company_email", nullable = false, length = 150)
    private String companyEmail;

    @Column(name = "phone", length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TenantStatus status = TenantStatus.PENDING;

    @Column(name = "timezone", nullable = false, length = 50)
    @Builder.Default
    private String timezone = "UTC";

    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "USD";

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TenantDomain> domains = new ArrayList<>();

    @OneToOne(mappedBy = "tenant", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private TenantSettings settings;

    public void addDomain(TenantDomain domain) {
        domains.add(domain);
        domain.setTenant(this);
    }

    public void removeDomain(TenantDomain domain) {
        domains.remove(domain);
        domain.setTenant(null);
    }
}
