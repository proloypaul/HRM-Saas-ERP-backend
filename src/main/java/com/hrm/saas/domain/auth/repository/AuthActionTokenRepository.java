package com.hrm.saas.domain.auth.repository;

import com.hrm.saas.domain.auth.entity.AuthActionToken;
import com.hrm.saas.domain.auth.enums.ActionTokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuthActionTokenRepository extends JpaRepository<AuthActionToken, UUID> {

    @Query("SELECT at FROM AuthActionToken at " +
            "LEFT JOIN FETCH at.user u " +
            "WHERE at.tokenHash = :tokenHash AND at.tokenType = :tokenType AND at.isDeleted = false")
    Optional<AuthActionToken> findByTokenHashAndTokenTypeWithUser(
            @Param("tokenHash") String tokenHash,
            @Param("tokenType") ActionTokenType tokenType
    );
}
