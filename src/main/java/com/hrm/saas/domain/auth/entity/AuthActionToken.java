package com.hrm.saas.domain.auth.entity;

import com.hrm.saas.common.entity.BaseEntity;
import com.hrm.saas.domain.auth.enums.ActionTokenType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "auth_action_tokens",
        indexes = {
                @Index(name = "idx_action_tokens_hash", columnList = "token_hash", unique = true),
                @Index(name = "idx_action_tokens_user_type", columnList = "user_id, token_type")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthActionToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_type", nullable = false, length = 30)
    private ActionTokenType tokenType;

    @Column(name = "is_used", nullable = false)
    @Builder.Default
    private Boolean isUsed = false;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }

    public boolean isValid() {
        return !isUsed && !isExpired() && !Boolean.TRUE.equals(getIsDeleted());
    }
}
