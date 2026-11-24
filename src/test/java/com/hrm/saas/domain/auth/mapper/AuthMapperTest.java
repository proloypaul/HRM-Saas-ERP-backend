package com.hrm.saas.domain.auth.mapper;

import com.hrm.saas.domain.auth.dto.request.RegisterRequest;
import com.hrm.saas.domain.auth.dto.response.TenantMembershipResponse;
import com.hrm.saas.domain.auth.dto.response.UserResponse;
import com.hrm.saas.domain.auth.entity.TenantMembership;
import com.hrm.saas.domain.auth.entity.User;
import com.hrm.saas.domain.auth.enums.MembershipRole;
import com.hrm.saas.domain.auth.enums.MembershipStatus;
import com.hrm.saas.domain.tenant.entity.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuthMapper — Unit Tests")
class AuthMapperTest {

    private AuthMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new AuthMapper();
    }

    @Test
    @DisplayName("Should map RegisterRequest to User entity")
    void shouldMapRegisterRequestToEntity() {
        RegisterRequest request = RegisterRequest.builder()
                .email("  USER@Company.com  ")
                .firstName("  Alice ")
                .lastName(" Smith  ")
                .phone("+1 234 567")
                .build();

        User user = mapper.toEntity(request, "encoded-hash");

        assertNotNull(user);
        assertEquals("user@company.com", user.getEmail());
        assertEquals("Alice", user.getFirstName());
        assertEquals("Smith", user.getLastName());
        assertEquals("+1 234 567", user.getPhone());
        assertEquals("encoded-hash", user.getPasswordHash());
        assertFalse(user.getIsEmailVerified());
        assertTrue(user.getIsActive());
    }

    @Test
    @DisplayName("Should map User entity to UserResponse")
    void shouldMapUserToResponse() {
        UUID id = UUID.randomUUID();
        User user = User.builder()
                .id(id)
                .email("user@example.com")
                .firstName("Bob")
                .lastName("Jones")
                .phone("+1 555-0100")
                .isEmailVerified(true)
                .isActive(true)
                .lastLoginAt(Instant.now())
                .build();

        UserResponse response = mapper.toUserResponse(user);

        assertNotNull(response);
        assertEquals(id, response.getId());
        assertEquals("user@example.com", response.getEmail());
        assertEquals("Bob Jones", response.getFullName());
        assertTrue(response.isEmailVerified());
        assertTrue(response.isActive());
    }

    @Test
    @DisplayName("Should map TenantMembership to TenantMembershipResponse")
    void shouldMapTenantMembershipToResponse() {
        UUID membershipId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        Tenant tenant = Tenant.builder()
                .id(tenantId)
                .name("Acme Corp")
                .slug("acme")
                .build();

        TenantMembership membership = TenantMembership.builder()
                .id(membershipId)
                .tenant(tenant)
                .role(MembershipRole.ADMIN)
                .status(MembershipStatus.ACTIVE)
                .joinedAt(Instant.now())
                .build();

        TenantMembershipResponse response = mapper.toMembershipResponse(membership);

        assertNotNull(response);
        assertEquals(membershipId, response.getMembershipId());
        assertEquals(tenantId, response.getTenantId());
        assertEquals("Acme Corp", response.getTenantName());
        assertEquals("acme", response.getTenantSlug());
        assertEquals(MembershipRole.ADMIN, response.getRole());
        assertEquals(MembershipStatus.ACTIVE, response.getStatus());
    }
}
