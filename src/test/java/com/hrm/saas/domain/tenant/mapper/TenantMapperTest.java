package com.hrm.saas.domain.tenant.mapper;

import com.hrm.saas.domain.tenant.dto.request.CreateTenantRequest;
import com.hrm.saas.domain.tenant.dto.response.TenantDomainResponse;
import com.hrm.saas.domain.tenant.dto.response.TenantResponse;
import com.hrm.saas.domain.tenant.entity.Tenant;
import com.hrm.saas.domain.tenant.entity.TenantDomain;
import com.hrm.saas.domain.tenant.entity.TenantSettings;
import com.hrm.saas.domain.tenant.enums.DomainType;
import com.hrm.saas.domain.tenant.enums.TenantStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TenantMapper}.
 *
 * No Spring context — pure POJO instantiation, fastest possible test.
 * Covers: toEntity(), toResponse(), toDomainResponse(), null-safety, and field transformations.
 */
@DisplayName("TenantMapper — Unit Tests")
class TenantMapperTest {

    private TenantMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TenantMapper();
    }

    // ─────────────────────────────────────────────────────────────
    // toEntity()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("toEntity(CreateTenantRequest)")
    class ToEntityTests {

        @Test
        @DisplayName("Should correctly map all fields from request to entity")
        void shouldMapAllFieldsCorrectly() {
            CreateTenantRequest request = CreateTenantRequest.builder()
                    .name("  Acme Corporation  ")
                    .slug("  Acme-Corp  ")
                    .legalName("  Acme Global Inc.  ")
                    .companyEmail("  Admin@Acme.COM  ")
                    .phone("+1 555-0199")
                    .timezone("America/New_York")
                    .currency("EUR")
                    .build();

            Tenant entity = mapper.toEntity(request);

            assertNotNull(entity);
            assertEquals("Acme Corporation", entity.getName()); // trimmed
            assertEquals("acme-corp", entity.getSlug());        // trimmed + lowercased
            assertEquals("Acme Global Inc.", entity.getLegalName());
            assertEquals("admin@acme.com", entity.getCompanyEmail()); // trimmed + lowercased
            assertEquals("+1 555-0199", entity.getPhone());
            assertEquals("America/New_York", entity.getTimezone());
            assertEquals("EUR", entity.getCurrency());
            assertEquals(TenantStatus.ACTIVE, entity.getStatus()); // always ACTIVE on creation
        }

        @Test
        @DisplayName("Should use defaults for nullable timezone and currency")
        void shouldUseDefaultsForNullTimezoneAndCurrency() {
            CreateTenantRequest request = CreateTenantRequest.builder()
                    .name("Test Co")
                    .slug("test-co")
                    .companyEmail("test@test.com")
                    .timezone(null)
                    .currency(null)
                    .build();

            Tenant entity = mapper.toEntity(request);

            assertEquals("UTC", entity.getTimezone());
            assertEquals("USD", entity.getCurrency());
        }

        @Test
        @DisplayName("Should return null when request is null")
        void shouldReturnNullOnNullRequest() {
            assertNull(mapper.toEntity(null));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // toResponse()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("toResponse(Tenant)")
    class ToResponseTests {

        @Test
        @DisplayName("Should correctly map tenant with domains and settings")
        void shouldMapTenantWithDomainsAndSettings() {
            UUID tenantId = UUID.randomUUID();
            Instant now = Instant.now();

            TenantDomain domain = TenantDomain.builder()
                    .id(UUID.randomUUID())
                    .domain("acme.localhost")
                    .domainType(DomainType.SUBDOMAIN)
                    .isPrimary(true)
                    .isVerified(true)
                    .verificationToken("hrm-verify-abc")
                    .verifiedAt(now)
                    .build();

            TenantSettings settings = TenantSettings.builder()
                    .settings("{\"branding\":{\"primaryColor\":\"#2563EB\"}}")
                    .build();

            Tenant tenant = Tenant.builder()
                    .id(tenantId)
                    .name("Acme Corporation")
                    .slug("acme")
                    .legalName("Acme Global Inc.")
                    .companyEmail("admin@acme.com")
                    .phone("+1-555")
                    .status(TenantStatus.ACTIVE)
                    .timezone("UTC")
                    .currency("USD")
                    .domains(List.of(domain))
                    .settings(settings)
                    .build();
            domain.setTenant(tenant);

            TenantResponse response = mapper.toResponse(tenant);

            assertNotNull(response);
            assertEquals(tenantId, response.getId());
            assertEquals("Acme Corporation", response.getName());
            assertEquals("acme", response.getSlug());
            assertEquals("Acme Global Inc.", response.getLegalName());
            assertEquals("admin@acme.com", response.getCompanyEmail());
            assertEquals(TenantStatus.ACTIVE, response.getStatus());
            assertEquals("{\"branding\":{\"primaryColor\":\"#2563EB\"}}", response.getSettings());
            assertNotNull(response.getDomains());
            assertEquals(1, response.getDomains().size());
            assertTrue(response.getDomains().get(0).isVerified());
        }

        @Test
        @DisplayName("Should return empty domains list when tenant has no domains")
        void shouldReturnEmptyDomainsListWhenNoDomains() {
            Tenant tenant = Tenant.builder()
                    .id(UUID.randomUUID())
                    .name("Empty Co")
                    .slug("empty-co")
                    .companyEmail("e@e.com")
                    .status(TenantStatus.ACTIVE)
                    .domains(null)   // null collection
                    .build();

            TenantResponse response = mapper.toResponse(tenant);

            assertNotNull(response.getDomains());
            assertTrue(response.getDomains().isEmpty());
        }

        @Test
        @DisplayName("Should return '{}' as settings when settings entity is null")
        void shouldReturnEmptyJsonWhenSettingsIsNull() {
            Tenant tenant = Tenant.builder()
                    .id(UUID.randomUUID())
                    .name("No Settings Co")
                    .slug("no-settings")
                    .companyEmail("ns@e.com")
                    .status(TenantStatus.ACTIVE)
                    .domains(List.of())
                    .settings(null)  // no settings
                    .build();

            TenantResponse response = mapper.toResponse(tenant);

            assertEquals("{}", response.getSettings());
        }

        @Test
        @DisplayName("Should return null when tenant is null")
        void shouldReturnNullOnNullTenant() {
            assertNull(mapper.toResponse(null));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // toDomainResponse()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("toDomainResponse(TenantDomain)")
    class ToDomainResponseTests {

        @Test
        @DisplayName("Should correctly map domain fields")
        void shouldMapDomainCorrectly() {
            UUID domainId = UUID.randomUUID();
            UUID tenantId = UUID.randomUUID();
            Instant verifiedAt = Instant.now();

            Tenant tenant = Tenant.builder().id(tenantId).build();
            TenantDomain domain = TenantDomain.builder()
                    .id(domainId)
                    .tenant(tenant)
                    .domain("acme.localhost")
                    .domainType(DomainType.SUBDOMAIN)
                    .isPrimary(true)
                    .isVerified(true)
                    .verificationToken("hrm-verify-xyz")
                    .verifiedAt(verifiedAt)
                    .build();

            TenantDomainResponse response = mapper.toDomainResponse(domain);

            assertNotNull(response);
            assertEquals(domainId, response.getId());
            assertEquals(tenantId, response.getTenantId());
            assertEquals("acme.localhost", response.getDomain());
            assertEquals(DomainType.SUBDOMAIN, response.getDomainType());
            assertTrue(response.isPrimary());
            assertTrue(response.isVerified());
            assertEquals("hrm-verify-xyz", response.getVerificationToken());
            assertEquals(verifiedAt, response.getVerifiedAt());
        }

        @Test
        @DisplayName("Should handle null tenant reference in domain gracefully")
        void shouldHandleNullTenantInDomain() {
            TenantDomain domain = TenantDomain.builder()
                    .id(UUID.randomUUID())
                    .tenant(null)
                    .domain("orphan.example.com")
                    .domainType(DomainType.CUSTOM)
                    .build();

            TenantDomainResponse response = mapper.toDomainResponse(domain);

            assertNotNull(response);
            assertNull(response.getTenantId()); // gracefully returns null
        }

        @Test
        @DisplayName("Should return null when domain is null")
        void shouldReturnNullOnNullDomain() {
            assertNull(mapper.toDomainResponse(null));
        }
    }
}
