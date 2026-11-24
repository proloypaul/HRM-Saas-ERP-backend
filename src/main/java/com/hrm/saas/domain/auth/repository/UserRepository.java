package com.hrm.saas.domain.auth.repository;

import com.hrm.saas.domain.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCaseAndIsDeletedFalse(String email);

    boolean existsByEmailIgnoreCaseAndIsDeletedFalse(String email);

    @Query("SELECT u FROM User u " +
            "LEFT JOIN FETCH u.memberships m " +
            "LEFT JOIN FETCH m.tenant " +
            "WHERE u.id = :id AND u.isDeleted = false")
    Optional<User> findByIdWithMemberships(@Param("id") UUID id);

    @Query("SELECT u FROM User u " +
            "LEFT JOIN FETCH u.memberships m " +
            "LEFT JOIN FETCH m.tenant " +
            "WHERE LOWER(u.email) = LOWER(:email) AND u.isDeleted = false")
    Optional<User> findByEmailWithMemberships(@Param("email") String email);
}
