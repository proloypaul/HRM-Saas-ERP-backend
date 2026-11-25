package com.hrm.saas.domain.rbac.entity;

import com.hrm.saas.common.entity.BaseEntity;
import com.hrm.saas.domain.auth.entity.TenantMembership;
import com.hrm.saas.domain.rbac.enums.OverrideEffect;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "user_permission_overrides",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_membership_perm_override", columnNames = {"membership_id", "permission_id"})
        },
        indexes = {
                @Index(name = "idx_override_membership", columnList = "membership_id"),
                @Index(name = "idx_override_permission", columnList = "permission_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPermissionOverride extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membership_id", nullable = false)
    private TenantMembership membership;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;

    @Enumerated(EnumType.STRING)
    @Column(name = "effect", nullable = false, length = 20)
    private OverrideEffect effect;

    @Column(name = "reason", length = 255)
    private String reason;
}
