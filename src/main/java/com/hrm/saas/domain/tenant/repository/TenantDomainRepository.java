package com.hrm.saas.domain.tenant.repository;

import com.hrm.saas.domain.tenant.entity.TenantDomain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantDomainRepository extends JpaRepository<TenantDomain, UUID> {

    @Query("SELECT td FROM TenantDomain td JOIN FETCH td.tenant t WHERE td.domain = :domain AND td.isVerified = true AND td.isDeleted = false AND t.isDeleted = false")
    Optional<TenantDomain> findByDomainAndIsVerifiedTrueWithTenant(@Param("domain") String domain);

    Optional<TenantDomain> findByDomainAndIsDeletedFalse(String domain);

    boolean existsByDomain(String domain);

    List<TenantDomain> findByTenantIdAndIsDeletedFalse(UUID tenantId);
}
