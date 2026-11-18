package com.hrm.saas.domain.tenant.repository;

import com.hrm.saas.domain.tenant.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findBySlugAndIsDeletedFalse(String slug);

    boolean existsBySlug(String slug);

    @Query("SELECT t FROM Tenant t LEFT JOIN FETCH t.domains WHERE t.id = :id AND t.isDeleted = false")
    Optional<Tenant> findByIdWithDomains(@Param("id") UUID id);
}
