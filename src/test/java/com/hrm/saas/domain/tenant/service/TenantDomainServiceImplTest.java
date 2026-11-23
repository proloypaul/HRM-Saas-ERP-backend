package com.hrm.saas.domain.tenant.service;

import com.hrm.saas.common.cache.RedisCacheService;
import com.hrm.saas.common.exception.AppException;
import com.hrm.saas.config.RedisCacheProperties;
import com.hrm.saas.domain.tenant.dto.request.RegisterDomainRequest;
import com.hrm.saas.domain.tenant.dto.response.TenantDomainResponse;
import com.hrm.saas.domain.tenant.dto.response.TenantSummaryDTO;
import com.hrm.saas.domain.tenant.entity.Tenant;
import com.hrm.saas.domain.tenant.entity.TenantDomain;
import com.hrm.saas.domain.tenant.enums.DomainType;
import com.hrm.saas.domain.tenant.enums.TenantStatus;
import com.hrm.saas.domain.tenant.mapper.TenantMapper;
import com.hrm.saas.domain.tenant.repository.TenantDomainRepository;
import com.hrm.saas.domain.tenant.repository.TenantRepository;
import com.hrm.saas.domain.tenant.service.impl.TenantDomainServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TenantDomainServiceImpl}.
 *
 * Covers: domain registration, auto-verification logic, DNS-based verification,
 * cache eviction, domain deletion, and resolveVerifiedDomain with Cache-Aside.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TenantDomainServiceImpl — Unit Tests")
class TenantDomainServiceImplTest {

    @Mock private TenantDomainRepository tenantDomainRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private TenantMapper tenantMapper;
    @Mock private DomainVerificationService domainVerificationService;
    @Mock private RedisCacheService redisCacheService;
    @Mock private RedisCacheProperties cacheProperties;

    @InjectMocks
    private TenantDomainServiceImpl tenantDomainService;

    private UUID tenantId;
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        tenant = Tenant.builder()
                .id(tenantId)
                .name("Acme")
                .slug("acme")
                .status(TenantStatus.ACTIVE)
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // registerDomain
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("registerDomain()")
    class RegisterDomainTests {

        @Test
        @DisplayName("*.localhost domains are auto-verified immediately")
        void shouldAutoVerifyLocalhostDomain() {
            RegisterDomainRequest request = RegisterDomainRequest.builder()
                    .domain("acme.localhost")
                    .domainType(DomainType.SUBDOMAIN)
                    .isPrimary(true)
                    .build();

            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
            when(tenantDomainRepository.existsByDomain("acme.localhost")).thenReturn(false);

            TenantDomain savedDomain = TenantDomain.builder()
                    .id(UUID.randomUUID())
                    .tenant(tenant)
                    .domain("acme.localhost")
                    .domainType(DomainType.SUBDOMAIN)
                    .isPrimary(true)
                    .isVerified(true)
                    .verifiedAt(Instant.now())
                    .build();
            when(tenantDomainRepository.save(any())).thenReturn(savedDomain);
            when(tenantMapper.toDomainResponse(savedDomain))
                    .thenReturn(TenantDomainResponse.builder().isVerified(true).build());

            TenantDomainResponse result = tenantDomainService.registerDomain(tenantId, request);

            assertTrue(result.isVerified());
            verify(redisCacheService).evict(contains("acme.localhost"));
        }

        @Test
        @DisplayName("SUBDOMAIN type is auto-verified regardless of host pattern")
        void shouldAutoVerifySubdomainType() {
            RegisterDomainRequest request = RegisterDomainRequest.builder()
                    .domain("acme.yourhrm.com")
                    .domainType(DomainType.SUBDOMAIN) // subdomain type → auto-verify
                    .isPrimary(true)
                    .build();

            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
            when(tenantDomainRepository.existsByDomain("acme.yourhrm.com")).thenReturn(false);

            TenantDomain savedDomain = TenantDomain.builder()
                    .id(UUID.randomUUID()).tenant(tenant)
                    .domain("acme.yourhrm.com").isVerified(true).build();
            when(tenantDomainRepository.save(any())).thenReturn(savedDomain);
            when(tenantMapper.toDomainResponse(any()))
                    .thenReturn(TenantDomainResponse.builder().isVerified(true).build());

            TenantDomainResponse result = tenantDomainService.registerDomain(tenantId, request);

            assertTrue(result.isVerified());
        }

        @Test
        @DisplayName("CUSTOM domain type is NOT auto-verified — requires DNS check")
        void shouldNotAutoVerifyCustomDomain() {
            RegisterDomainRequest request = RegisterDomainRequest.builder()
                    .domain("hr.acme.corp")
                    .domainType(DomainType.CUSTOM)
                    .isPrimary(false)
                    .build();

            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
            when(tenantDomainRepository.existsByDomain("hr.acme.corp")).thenReturn(false);

            // Capture what is saved — should have isVerified=false
            TenantDomain unverifiedDomain = TenantDomain.builder()
                    .id(UUID.randomUUID()).tenant(tenant)
                    .domain("hr.acme.corp").isVerified(false).build();
            when(tenantDomainRepository.save(any())).thenReturn(unverifiedDomain);
            when(tenantMapper.toDomainResponse(any()))
                    .thenReturn(TenantDomainResponse.builder().isVerified(false).build());

            TenantDomainResponse result = tenantDomainService.registerDomain(tenantId, request);

            assertFalse(result.isVerified());
        }

        @Test
        @DisplayName("Should throw CONFLICT when domain already registered")
        void shouldThrowConflictOnDuplicateDomain() {
            RegisterDomainRequest request = RegisterDomainRequest.builder()
                    .domain("acme.localhost")
                    .domainType(DomainType.SUBDOMAIN)
                    .build();

            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
            when(tenantDomainRepository.existsByDomain("acme.localhost")).thenReturn(true);

            AppException ex = assertThrows(AppException.class,
                    () -> tenantDomainService.registerDomain(tenantId, request));

            assertEquals(HttpStatus.CONFLICT, ex.getStatus());
            assertTrue(ex.getMessage().contains("already registered"));
        }

        @Test
        @DisplayName("Should throw NOT_FOUND for non-existent tenant")
        void shouldThrowNotFoundForMissingTenant() {
            when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

            assertThrows(AppException.class, () ->
                    tenantDomainService.registerDomain(tenantId, RegisterDomainRequest.builder()
                            .domain("x.localhost").domainType(DomainType.SUBDOMAIN).build()));
        }

        @Test
        @DisplayName("Should normalize domain to lowercase before checking and saving")
        void shouldNormalizeDomainToLowercase() {
            RegisterDomainRequest request = RegisterDomainRequest.builder()
                    .domain("  ACME.Localhost  ")
                    .domainType(DomainType.SUBDOMAIN)
                    .build();

            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
            when(tenantDomainRepository.existsByDomain("acme.localhost")).thenReturn(false);
            when(tenantDomainRepository.save(any())).thenReturn(
                    TenantDomain.builder().id(UUID.randomUUID()).tenant(tenant)
                            .domain("acme.localhost").isVerified(true).build());
            when(tenantMapper.toDomainResponse(any()))
                    .thenReturn(TenantDomainResponse.builder().build());

            tenantDomainService.registerDomain(tenantId, request);

            verify(tenantDomainRepository).existsByDomain("acme.localhost");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // verifyDomain
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("verifyDomain()")
    class VerifyDomainTests {

        @Test
        @DisplayName("Should mark domain verified when DNS check passes")
        void shouldVerifyDomainOnDnsSuccess() {
            UUID domainId = UUID.randomUUID();
            TenantDomain unverified = TenantDomain.builder()
                    .id(domainId)
                    .domain("hr.acme.corp")
                    .isVerified(false)
                    .verificationToken("hrm-verify-abc123")
                    .build();

            when(tenantDomainRepository.findById(domainId)).thenReturn(Optional.of(unverified));
            when(domainVerificationService.verifyDomainOwnership("hr.acme.corp", "hrm-verify-abc123"))
                    .thenReturn(true);
            when(tenantDomainRepository.save(any())).thenReturn(unverified);
            when(tenantMapper.toDomainResponse(any()))
                    .thenReturn(TenantDomainResponse.builder().isVerified(true).build());

            TenantDomainResponse result = tenantDomainService.verifyDomain(domainId);

            assertTrue(result.isVerified());
            assertTrue(unverified.getIsVerified());
            assertNotNull(unverified.getVerifiedAt());
        }

        @Test
        @DisplayName("Should return early without DB call if domain already verified")
        void shouldReturnEarlyIfAlreadyVerified() {
            UUID domainId = UUID.randomUUID();
            TenantDomain alreadyVerified = TenantDomain.builder()
                    .id(domainId).domain("acme.localhost")
                    .isVerified(true).verifiedAt(Instant.now()).build();

            when(tenantDomainRepository.findById(domainId)).thenReturn(Optional.of(alreadyVerified));
            when(tenantMapper.toDomainResponse(alreadyVerified))
                    .thenReturn(TenantDomainResponse.builder().isVerified(true).build());

            tenantDomainService.verifyDomain(domainId);

            // DNS service must NOT be called
            verify(domainVerificationService, never()).verifyDomainOwnership(any(), any());
            verify(tenantDomainRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw BAD_REQUEST when DNS TXT check fails")
        void shouldThrowBadRequestOnDnsFailure() {
            UUID domainId = UUID.randomUUID();
            TenantDomain unverified = TenantDomain.builder()
                    .id(domainId).domain("hr.fail.corp")
                    .isVerified(false).verificationToken("hrm-verify-fail").build();

            when(tenantDomainRepository.findById(domainId)).thenReturn(Optional.of(unverified));
            when(domainVerificationService.verifyDomainOwnership("hr.fail.corp", "hrm-verify-fail"))
                    .thenReturn(false);

            AppException ex = assertThrows(AppException.class,
                    () -> tenantDomainService.verifyDomain(domainId));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertTrue(ex.getMessage().contains("DNS TXT"));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // listDomainsByTenant
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("listDomainsByTenant()")
    class ListDomainsTests {

        @Test
        @DisplayName("Should return mapped domain list for a tenant")
        void shouldReturnDomainList() {
            TenantDomain d1 = TenantDomain.builder().id(UUID.randomUUID())
                    .tenant(tenant).domain("acme.localhost").build();
            TenantDomain d2 = TenantDomain.builder().id(UUID.randomUUID())
                    .tenant(tenant).domain("hr.acme.corp").build();

            when(tenantDomainRepository.findByTenantIdAndIsDeletedFalse(tenantId))
                    .thenReturn(List.of(d1, d2));
            when(tenantMapper.toDomainResponse(d1))
                    .thenReturn(TenantDomainResponse.builder().domain("acme.localhost").build());
            when(tenantMapper.toDomainResponse(d2))
                    .thenReturn(TenantDomainResponse.builder().domain("hr.acme.corp").build());

            List<TenantDomainResponse> result = tenantDomainService.listDomainsByTenant(tenantId);

            assertEquals(2, result.size());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // deleteDomain
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("deleteDomain()")
    class DeleteDomainTests {

        @Test
        @DisplayName("Should soft-delete domain and evict cache")
        void shouldSoftDeleteAndEvictCache() {
            UUID domainId = UUID.randomUUID();
            // isDeleted lives in BaseEntity — not in builder; set via setter
            TenantDomain domain = TenantDomain.builder()
                    .id(domainId).domain("acme.localhost").build();
            domain.setIsDeleted(false);

            when(tenantDomainRepository.findById(domainId)).thenReturn(Optional.of(domain));
            when(tenantDomainRepository.save(any())).thenReturn(domain);

            tenantDomainService.deleteDomain(domainId);

            assertTrue(domain.getIsDeleted());
            assertNotNull(domain.getDeletedAt());
            verify(redisCacheService).evict(contains("acme.localhost"));
        }

        @Test
        @DisplayName("Should throw NOT_FOUND for already-deleted domain")
        void shouldThrowNotFoundForDeletedDomain() {
            UUID domainId = UUID.randomUUID();
            // isDeleted lives in BaseEntity — not in builder; set via setter
            TenantDomain alreadyDeleted = TenantDomain.builder()
                    .id(domainId).domain("gone.localhost").build();
            alreadyDeleted.setIsDeleted(true);

            when(tenantDomainRepository.findById(domainId)).thenReturn(Optional.of(alreadyDeleted));

            AppException ex = assertThrows(AppException.class,
                    () -> tenantDomainService.deleteDomain(domainId));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // resolveVerifiedDomain — Cache-Aside
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("resolveVerifiedDomain() — Cache-Aside")
    class ResolveVerifiedDomainTests {

        @Test
        @DisplayName("Should return empty when host is null or blank")
        void shouldReturnEmptyForBlankHost() {
            assertTrue(tenantDomainService.resolveVerifiedDomain(null).isEmpty());
            assertTrue(tenantDomainService.resolveVerifiedDomain("   ").isEmpty());
        }

        @Test
        @DisplayName("Should delegate to redisCacheService.getOrFetch with normalized host")
        void shouldDelegateToCache() {
            when(cacheProperties.getDomainTtl()).thenReturn(Duration.ofMinutes(15));
            TenantSummaryDTO summary = TenantSummaryDTO.builder()
                    .tenantId(tenantId).slug("acme").build();
            when(redisCacheService.getOrFetch(
                    eq("tenant:domain:acme.localhost"),
                    eq(TenantSummaryDTO.class),
                    eq(Duration.ofMinutes(15)),
                    any())).thenReturn(Optional.of(summary));

            Optional<TenantSummaryDTO> result =
                    tenantDomainService.resolveVerifiedDomain("  ACME.localhost  ");

            assertTrue(result.isPresent());
            assertEquals("acme", result.get().getSlug());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // evictDomainCache
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("evictDomainCache()")
    class EvictDomainCacheTests {

        @Test
        @DisplayName("Should call redisCacheService.evict with correct key")
        void shouldEvictWithCorrectKey() {
            tenantDomainService.evictDomainCache("acme.localhost");
            verify(redisCacheService).evict("tenant:domain:acme.localhost");
        }

        @Test
        @DisplayName("Should do nothing when domain is null")
        void shouldSkipEvictionOnNullDomain() {
            tenantDomainService.evictDomainCache(null);
            verify(redisCacheService, never()).evict(any());
        }
    }
}
