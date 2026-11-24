package com.hrm.saas.domain.auth.repository;

import com.hrm.saas.domain.auth.entity.TenantMembership;
import com.hrm.saas.domain.auth.enums.MembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantMembershipRepository extends JpaRepository<TenantMembership, UUID> {

    @Query("SELECT tm FROM TenantMembership tm " +
            "LEFT JOIN FETCH tm.tenant t " +
            "WHERE tm.user.id = :userId AND tm.isDeleted = false")
    List<TenantMembership> findAllByUserIdWithTenant(@Param("userId") UUID userId);

    @Query("SELECT tm FROM TenantMembership tm " +
            "LEFT JOIN FETCH tm.tenant t " +
            "WHERE tm.user.id = :userId AND tm.status = :status AND tm.isDeleted = false")
    List<TenantMembership> findAllByUserIdAndStatusWithTenant(@Param("userId") UUID userId, @Param("status") MembershipStatus status);

    @Query("SELECT tm FROM TenantMembership tm " +
            "LEFT JOIN FETCH tm.tenant t " +
            "WHERE tm.user.id = :userId AND tm.tenant.id = :tenantId AND tm.isDeleted = false")
    Optional<TenantMembership> findByUserIdAndTenantIdWithTenant(@Param("userId") UUID userId, @Param("tenantId") UUID tenantId);

    boolean existsByUserIdAndTenantIdAndIsDeletedFalse(UUID userId, UUID tenantId);
}
