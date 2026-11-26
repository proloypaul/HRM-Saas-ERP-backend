package com.hrm.saas.domain.rbac.repository;

import com.hrm.saas.domain.rbac.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    List<Role> findByTenantIdAndIsActiveTrueOrderByPriorityAscNameAsc(UUID tenantId);

    Optional<Role> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<Role> findByTenantIdAndCode(UUID tenantId, String code);

    boolean existsByTenantIdAndCode(UUID tenantId, String code);

    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.id = :id AND r.tenant.id = :tenantId")
    Optional<Role> findByIdAndTenantIdWithPermissions(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.tenant.id = :tenantId AND r.isActive = true ORDER BY r.priority ASC, r.name ASC")
    List<Role> findAllByTenantIdWithPermissions(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(m) FROM TenantMembership m JOIN m.roles r WHERE r.id = :roleId")
    long countAssignedMembers(@Param("roleId") UUID roleId);

    @Query("SELECT r.id, COUNT(m) FROM TenantMembership m JOIN m.roles r WHERE m.tenant.id = :tenantId GROUP BY r.id")
    List<Object[]> countMembersPerRoleByTenant(@Param("tenantId") UUID tenantId);
}
