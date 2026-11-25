package com.hrm.saas.domain.rbac.repository;

import com.hrm.saas.domain.rbac.entity.UserPermissionOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserPermissionOverrideRepository extends JpaRepository<UserPermissionOverride, UUID> {

    @Query("SELECT o FROM UserPermissionOverride o JOIN FETCH o.permission WHERE o.membership.id = :membershipId")
    List<UserPermissionOverride> findByMembershipIdWithPermission(@Param("membershipId") UUID membershipId);

    Optional<UserPermissionOverride> findByMembershipIdAndPermissionId(UUID membershipId, UUID permissionId);

    void deleteByMembershipId(UUID membershipId);
}
