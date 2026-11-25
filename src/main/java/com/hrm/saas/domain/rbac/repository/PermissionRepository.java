package com.hrm.saas.domain.rbac.repository;

import com.hrm.saas.domain.rbac.entity.Permission;
import com.hrm.saas.domain.rbac.enums.PermissionModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    Optional<Permission> findByCode(String code);

    List<Permission> findByModule(PermissionModule module);

    List<Permission> findByIsAssignableTrueOrderByModuleAscDisplayNameAsc();

    Set<Permission> findByCodeIn(Set<String> codes);
}
