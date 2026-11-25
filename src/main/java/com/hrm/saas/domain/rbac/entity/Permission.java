package com.hrm.saas.domain.rbac.entity;

import com.hrm.saas.common.entity.BaseEntity;
import com.hrm.saas.domain.rbac.enums.PermissionModule;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "permissions",
        indexes = {
                @Index(name = "idx_permissions_module", columnList = "module"),
                @Index(name = "idx_permissions_code", columnList = "code", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 80)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "module", nullable = false, length = 50)
    private PermissionModule module;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_assignable", nullable = false)
    @Builder.Default
    private Boolean isAssignable = true;
}
