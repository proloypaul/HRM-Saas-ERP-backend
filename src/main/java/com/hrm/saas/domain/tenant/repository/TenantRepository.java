package com.hrm.saas.domain.tenant.repository;

import com.hrm.saas.domain.tenant.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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

    /**
     * Eagerly fetches domains AND settings in a single JOIN to avoid N+1 on list pages.
     * Uses @EntityGraph so Spring Data handles the count query separately (required for pagination).
     */
    @EntityGraph(attributePaths = {"domains", "settings"})
    @Query("SELECT t FROM Tenant t WHERE t.isDeleted = false")
    Page<Tenant> findAllActiveWithDomainsAndSettings(Pageable pageable);

    /**
     * Fetches a single tenant with its domains and settings eagerly — used for all
     * single-entity operations (get, update, suspend, activate, delete) so that
     * toResponse() never triggers a lazy-load.
     */
    @Query("SELECT t FROM Tenant t LEFT JOIN FETCH t.domains LEFT JOIN FETCH t.settings WHERE t.id = :id AND t.isDeleted = false")
    Optional<Tenant> findByIdWithDomainsAndSettings(@Param("id") UUID id);

    /**
     * @deprecated Use {@link #findByIdWithDomainsAndSettings(UUID)} to also load settings.
     * Kept for backwards compatibility with TenantDomainServiceImpl.
     */
    @Deprecated
    @Query("SELECT t FROM Tenant t LEFT JOIN FETCH t.domains WHERE t.id = :id AND t.isDeleted = false")
    Optional<Tenant> findByIdWithDomains(@Param("id") UUID id);

    /**
     * Fetches by slug with domains AND settings eagerly — prevents N+1 in getTenantBySlug.
     */
    @Query("SELECT t FROM Tenant t LEFT JOIN FETCH t.domains LEFT JOIN FETCH t.settings WHERE t.slug = :slug AND t.isDeleted = false")
    Optional<Tenant> findBySlugWithDomainsAndSettings(@Param("slug") String slug);
}
