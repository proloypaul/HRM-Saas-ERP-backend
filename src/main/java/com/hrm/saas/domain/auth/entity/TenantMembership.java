package com.hrm.saas.domain.auth.entity;

import com.hrm.saas.common.entity.BaseEntity;
import com.hrm.saas.domain.auth.enums.MembershipStatus;
import com.hrm.saas.domain.rbac.entity.Role;
import com.hrm.saas.domain.tenant.entity.Tenant;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "tenant_memberships", uniqueConstraints = {
                @UniqueConstraint(name = "uq_tenant_user", columnNames = { "tenant_id", "user_id" })
}, indexes = {
                @Index(name = "idx_memberships_user", columnList = "user_id"),
                @Index(name = "idx_memberships_tenant", columnList = "tenant_id"),
                @Index(name = "idx_memberships_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantMembership extends BaseEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        @Column(name = "id", updatable = false, nullable = false)
        private UUID id;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "user_id", nullable = false)
        private User user;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "tenant_id", nullable = false)
        private Tenant tenant;

        @Column(name = "is_owner", nullable = false)
        @Builder.Default
        private Boolean isOwner = false;

        @ManyToMany(fetch = FetchType.LAZY)
        @JoinTable(name = "membership_roles", joinColumns = @JoinColumn(name = "membership_id", referencedColumnName = "id"), inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"), indexes = {
                        @Index(name = "idx_mem_roles_mem", columnList = "membership_id"),
                        @Index(name = "idx_mem_roles_role", columnList = "role_id")
        })
        @Builder.Default
        private Set<Role> roles = new HashSet<>();

        @Enumerated(EnumType.STRING)
        @Column(name = "status", nullable = false, length = 20)
        @Builder.Default
        private MembershipStatus status = MembershipStatus.ACTIVE;

        @Column(name = "joined_at", nullable = false)
        @Builder.Default
        private Instant joinedAt = Instant.now();
}
