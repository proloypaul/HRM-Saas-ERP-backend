package com.hrm.saas.domain.tenant.service;

import com.hrm.saas.common.exception.AppException;
import com.hrm.saas.config.TenancyProperties;
import com.hrm.saas.domain.tenant.dto.request.CreateTenantRequest;
import com.hrm.saas.domain.tenant.dto.request.RegisterDomainRequest;
import com.hrm.saas.domain.tenant.dto.request.UpdateTenantRequest;
import com.hrm.saas.domain.tenant.dto.response.TenantDomainResponse;
import com.hrm.saas.domain.tenant.dto.response.TenantResponse;
import com.hrm.saas.domain.tenant.entity.Tenant;
import com.hrm.saas.domain.tenant.entity.TenantDomain;
import com.hrm.saas.domain.tenant.entity.TenantSettings;
import com.hrm.saas.domain.tenant.enums.DomainType;
import com.hrm.saas.domain.tenant.enums.TenantStatus;
import com.hrm.saas.domain.tenant.mapper.TenantMapper;
import com.hrm.saas.domain.tenant.repository.TenantRepository;
import com.hrm.saas.domain.tenant.service.impl.TenantServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TenantServiceImpl}.
 *
 * Uses Mockito to isolate the service from the database.
 * Every public method is tested including: happy path, slug conflict,
 * not-found, state transitions, and N+1 fix verification.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TenantServiceImpl — Unit Tests")
class TenantServiceImplTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private TenantDomainService tenantDomainService;
    @Mock private TenantMapper tenantMapper;
    @Mock private TenancyProperties tenancyProperties;

    @InjectMocks
    private TenantServiceImpl tenantService;

    // ── shared fixtures ──────────────────────────────────────────
    private UUID tenantId;
    private Tenant activeTenant;
    private TenantResponse tenantResponse;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();

        activeTenant = Tenant.builder()
                .id(tenantId)
                .name("Acme Corporation")
                .slug("acme")
                .companyEmail("admin@acme.com")
                .status(TenantStatus.ACTIVE)
                .timezone("UTC")
                .currency("USD")
                .domains(new ArrayList<>())   // mutable — tests can call .add()
                .build();

        tenantResponse = TenantResponse.builder()
                .id(tenantId)
                .name("Acme Corporation")
                .slug("acme")
                .companyEmail("admin@acme.com")
                .status(TenantStatus.ACTIVE)
                .domains(List.of())
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // createTenant
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("createTenant()")
    class CreateTenantTests {

        private CreateTenantRequest validRequest;

        @BeforeEach
        void setUpRequest() {
            validRequest = CreateTenantRequest.builder()
                    .name("Acme Corporation")
                    .slug("acme")
                    .companyEmail("admin@acme.com")
                    .timezone("UTC")
                    .currency("USD")
                    .build();
        }

        @Test
        @DisplayName("Happy path: creates tenant, registers default domain, returns response")
        void shouldCreateTenantSuccessfully() {
            when(tenantRepository.existsBySlug("acme")).thenReturn(false);
            when(tenantMapper.toEntity(validRequest)).thenReturn(activeTenant);
            when(tenantRepository.save(any(Tenant.class))).thenReturn(activeTenant);
            when(tenancyProperties.getBaseDomain()).thenReturn("localhost");

            TenantDomainResponse domainResponse = TenantDomainResponse.builder()
                    .id(UUID.randomUUID()).domain("acme.localhost").build();
            when(tenantDomainService.registerDomain(eq(tenantId), any(RegisterDomainRequest.class)))
                    .thenReturn(domainResponse);

            when(tenantRepository.findByIdWithDomainsAndSettings(tenantId))
                    .thenReturn(Optional.of(activeTenant));
            when(tenantMapper.toResponse(activeTenant)).thenReturn(tenantResponse);

            TenantResponse result = tenantService.createTenant(validRequest);

            assertNotNull(result);
            assertEquals("acme", result.getSlug());

            // Verify default domain registration with subdomain type
            ArgumentCaptor<RegisterDomainRequest> domainCaptor =
                    ArgumentCaptor.forClass(RegisterDomainRequest.class);
            verify(tenantDomainService).registerDomain(eq(tenantId), domainCaptor.capture());

            RegisterDomainRequest capturedDomain = domainCaptor.getValue();
            assertEquals("acme.localhost", capturedDomain.getDomain());
            assertEquals(DomainType.SUBDOMAIN, capturedDomain.getDomainType());
            assertTrue(capturedDomain.isPrimary());
        }

        @Test
        @DisplayName("Should trim and lowercase slug before persisting")
        void shouldNormalizeSlug() {
            CreateTenantRequest requestWithUpperSlug = CreateTenantRequest.builder()
                    .name("Acme")
                    .slug("  ACME-Corp  ")
                    .companyEmail("a@a.com")
                    .build();

            when(tenantRepository.existsBySlug("acme-corp")).thenReturn(false);
            when(tenantMapper.toEntity(requestWithUpperSlug)).thenReturn(activeTenant);
            when(tenantRepository.save(any())).thenReturn(activeTenant);
            when(tenancyProperties.getBaseDomain()).thenReturn("localhost");
            when(tenantDomainService.registerDomain(any(), any())).thenReturn(mock(TenantDomainResponse.class));
            when(tenantRepository.findByIdWithDomainsAndSettings(tenantId))
                    .thenReturn(Optional.of(activeTenant));
            when(tenantMapper.toResponse(any())).thenReturn(tenantResponse);

            tenantService.createTenant(requestWithUpperSlug);

            // Slug passed to existsBySlug must be normalised
            verify(tenantRepository).existsBySlug("acme-corp");
        }

        @Test
        @DisplayName("Should throw CONFLICT when slug already exists")
        void shouldThrowConflictWhenSlugExists() {
            when(tenantRepository.existsBySlug("acme")).thenReturn(true);

            AppException ex = assertThrows(AppException.class,
                    () -> tenantService.createTenant(validRequest));

            assertEquals(HttpStatus.CONFLICT, ex.getStatus());
            assertTrue(ex.getMessage().contains("already exists"));
            verify(tenantRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should use findByIdWithDomainsAndSettings (N+1 fix) to reload created tenant")
        void shouldUseEagerQueryOnReload() {
            when(tenantRepository.existsBySlug("acme")).thenReturn(false);
            when(tenantMapper.toEntity(validRequest)).thenReturn(activeTenant);
            when(tenantRepository.save(any())).thenReturn(activeTenant);
            when(tenancyProperties.getBaseDomain()).thenReturn("localhost");
            when(tenantDomainService.registerDomain(any(), any())).thenReturn(mock(TenantDomainResponse.class));
            when(tenantRepository.findByIdWithDomainsAndSettings(tenantId))
                    .thenReturn(Optional.of(activeTenant));
            when(tenantMapper.toResponse(any())).thenReturn(tenantResponse);

            tenantService.createTenant(validRequest);

            // Must use the eager-fetch query, NOT findById
            verify(tenantRepository).findByIdWithDomainsAndSettings(tenantId);
            verify(tenantRepository, never()).findById(any());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // getTenantById
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getTenantById()")
    class GetTenantByIdTests {

        @Test
        @DisplayName("Should return tenant response for existing active tenant")
        void shouldReturnTenantById() {
            when(tenantRepository.findByIdWithDomainsAndSettings(tenantId))
                    .thenReturn(Optional.of(activeTenant));
            when(tenantMapper.toResponse(activeTenant)).thenReturn(tenantResponse);

            TenantResponse result = tenantService.getTenantById(tenantId);

            assertNotNull(result);
            assertEquals(tenantId, result.getId());
        }

        @Test
        @DisplayName("Should throw NOT_FOUND when tenant does not exist")
        void shouldThrowNotFoundWhenTenantMissing() {
            when(tenantRepository.findByIdWithDomainsAndSettings(tenantId))
                    .thenReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class,
                    () -> tenantService.getTenantById(tenantId));

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        }

        @Test
        @DisplayName("Should use eager-fetch query (N+1 fix) — never raw findById")
        void shouldUseEagerFetchQuery() {
            when(tenantRepository.findByIdWithDomainsAndSettings(tenantId))
                    .thenReturn(Optional.of(activeTenant));
            when(tenantMapper.toResponse(any())).thenReturn(tenantResponse);

            tenantService.getTenantById(tenantId);

            verify(tenantRepository).findByIdWithDomainsAndSettings(tenantId);
            verify(tenantRepository, never()).findById(any());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // getTenantBySlug
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getTenantBySlug()")
    class GetTenantBySlugTests {

        @Test
        @DisplayName("Should trim and lowercase slug before lookup")
        void shouldNormalizeSlugForLookup() {
            when(tenantRepository.findBySlugWithDomainsAndSettings("acme"))
                    .thenReturn(Optional.of(activeTenant));
            when(tenantMapper.toResponse(any())).thenReturn(tenantResponse);

            tenantService.getTenantBySlug("  ACME  ");

            verify(tenantRepository).findBySlugWithDomainsAndSettings("acme");
        }

        @Test
        @DisplayName("Should throw NOT_FOUND when slug does not match")
        void shouldThrowNotFoundForUnknownSlug() {
            when(tenantRepository.findBySlugWithDomainsAndSettings("unknown"))
                    .thenReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class,
                    () -> tenantService.getTenantBySlug("unknown"));

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
            assertTrue(ex.getMessage().contains("unknown"));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // listTenants — N+1 Fix verification
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("listTenants() — N+1 Query Fix")
    class ListTenantsTests {

        @Test
        @DisplayName("Should delegate to findAllActiveWithDomainsAndSettings — not findAll")
        void shouldUseEagerFetchPaginationQuery() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Tenant> page = new PageImpl<>(List.of(activeTenant), pageable, 1);

            when(tenantRepository.findAllActiveWithDomainsAndSettings(pageable)).thenReturn(page);
            when(tenantMapper.toResponse(activeTenant)).thenReturn(tenantResponse);

            Page<TenantResponse> result = tenantService.listTenants(pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());

            // Core assertion: N+1 fix — must NOT use the base findAll()
            verify(tenantRepository).findAllActiveWithDomainsAndSettings(pageable);
            verify(tenantRepository, never()).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("Should return empty page when no active tenants exist")
        void shouldReturnEmptyPage() {
            Pageable pageable = PageRequest.of(0, 10);
            when(tenantRepository.findAllActiveWithDomainsAndSettings(pageable))
                    .thenReturn(Page.empty());

            Page<TenantResponse> result = tenantService.listTenants(pageable);

            assertNotNull(result);
            assertEquals(0, result.getTotalElements());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // updateTenant
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("updateTenant()")
    class UpdateTenantTests {

        @Test
        @DisplayName("Should update only the provided non-null fields")
        void shouldUpdateProvidedFields() {
            when(tenantRepository.findByIdWithDomainsAndSettings(tenantId))
                    .thenReturn(Optional.of(activeTenant));
            when(tenantRepository.save(any())).thenReturn(activeTenant);
            when(tenantMapper.toResponse(any())).thenReturn(tenantResponse);

            UpdateTenantRequest request = UpdateTenantRequest.builder()
                    .name("New Name")
                    .currency("GBP")
                    .build();

            tenantService.updateTenant(tenantId, request);

            assertEquals("New Name", activeTenant.getName());
            assertEquals("GBP", activeTenant.getCurrency());
            // Not-provided fields remain unchanged
            assertEquals("admin@acme.com", activeTenant.getCompanyEmail());
        }

        @Test
        @DisplayName("Should use eager-fetch query (N+1 fix) — never raw findById")
        void shouldUseEagerFetchQueryOnUpdate() {
            when(tenantRepository.findByIdWithDomainsAndSettings(tenantId))
                    .thenReturn(Optional.of(activeTenant));
            when(tenantRepository.save(any())).thenReturn(activeTenant);
            when(tenantMapper.toResponse(any())).thenReturn(tenantResponse);

            tenantService.updateTenant(tenantId, UpdateTenantRequest.builder().name("X").build());

            verify(tenantRepository).findByIdWithDomainsAndSettings(tenantId);
            verify(tenantRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Should throw NOT_FOUND for non-existent tenant")
        void shouldThrowNotFoundOnUpdate() {
            when(tenantRepository.findByIdWithDomainsAndSettings(tenantId))
                    .thenReturn(Optional.empty());

            assertThrows(AppException.class,
                    () -> tenantService.updateTenant(tenantId, UpdateTenantRequest.builder().build()));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // suspendTenant
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("suspendTenant()")
    class SuspendTenantTests {

        @Test
        @DisplayName("Should set status to SUSPENDED and evict domain cache")
        void shouldSuspendTenantAndEvictCache() {
            TenantDomain domain = TenantDomain.builder()
                    .id(UUID.randomUUID()).domain("acme.localhost").build();
            activeTenant.getDomains().add(domain);

            when(tenantRepository.findByIdWithDomainsAndSettings(tenantId))
                    .thenReturn(Optional.of(activeTenant));
            when(tenantRepository.save(any())).thenReturn(activeTenant);

            TenantResponse suspended = TenantResponse.builder()
                    .id(tenantId).status(TenantStatus.SUSPENDED).build();
            when(tenantMapper.toResponse(any())).thenReturn(suspended);

            TenantResponse result = tenantService.suspendTenant(tenantId);

            assertEquals(TenantStatus.SUSPENDED, activeTenant.getStatus());
            assertEquals(TenantStatus.SUSPENDED, result.getStatus());
            verify(tenantDomainService).evictDomainCache("acme.localhost");
        }

        @Test
        @DisplayName("Should use eager-fetch query (N+1 fix) — never raw findById")
        void shouldUseEagerFetchQueryOnSuspend() {
            when(tenantRepository.findByIdWithDomainsAndSettings(tenantId))
                    .thenReturn(Optional.of(activeTenant));
            when(tenantRepository.save(any())).thenReturn(activeTenant);
            when(tenantMapper.toResponse(any())).thenReturn(tenantResponse);

            tenantService.suspendTenant(tenantId);

            verify(tenantRepository).findByIdWithDomainsAndSettings(tenantId);
            verify(tenantRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Should throw NOT_FOUND for non-existent tenant")
        void shouldThrowNotFoundOnSuspend() {
            when(tenantRepository.findByIdWithDomainsAndSettings(tenantId))
                    .thenReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class,
                    () -> tenantService.suspendTenant(tenantId));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // activateTenant
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("activateTenant()")
    class ActivateTenantTests {

        @Test
        @DisplayName("Should set status to ACTIVE")
        void shouldActivateTenant() {
            activeTenant.setStatus(TenantStatus.SUSPENDED);
            when(tenantRepository.findByIdWithDomainsAndSettings(tenantId))
                    .thenReturn(Optional.of(activeTenant));
            when(tenantRepository.save(any())).thenReturn(activeTenant);
            when(tenantMapper.toResponse(any())).thenReturn(tenantResponse);

            tenantService.activateTenant(tenantId);

            assertEquals(TenantStatus.ACTIVE, activeTenant.getStatus());
        }

        @Test
        @DisplayName("Should use eager-fetch query (N+1 fix) — never raw findById")
        void shouldUseEagerFetchQueryOnActivate() {
            when(tenantRepository.findByIdWithDomainsAndSettings(tenantId))
                    .thenReturn(Optional.of(activeTenant));
            when(tenantRepository.save(any())).thenReturn(activeTenant);
            when(tenantMapper.toResponse(any())).thenReturn(tenantResponse);

            tenantService.activateTenant(tenantId);

            verify(tenantRepository).findByIdWithDomainsAndSettings(tenantId);
            verify(tenantRepository, never()).findById(any());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // deleteTenant
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("deleteTenant()")
    class DeleteTenantTests {

        @Test
        @DisplayName("Should soft-delete tenant: isDeleted=true, status=CANCELLED, deletedAt set")
        void shouldSoftDeleteTenant() {
            when(tenantRepository.findByIdWithDomainsAndSettings(tenantId))
                    .thenReturn(Optional.of(activeTenant));
            when(tenantRepository.save(any())).thenReturn(activeTenant);

            tenantService.deleteTenant(tenantId);

            assertTrue(activeTenant.getIsDeleted());
            assertEquals(TenantStatus.CANCELLED, activeTenant.getStatus());
            assertNotNull(activeTenant.getDeletedAt());
        }

        @Test
        @DisplayName("Should use eager-fetch query (N+1 fix) — never raw findById")
        void shouldUseEagerFetchQueryOnDelete() {
            when(tenantRepository.findByIdWithDomainsAndSettings(tenantId))
                    .thenReturn(Optional.of(activeTenant));
            when(tenantRepository.save(any())).thenReturn(activeTenant);

            tenantService.deleteTenant(tenantId);

            verify(tenantRepository).findByIdWithDomainsAndSettings(tenantId);
            verify(tenantRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Should throw NOT_FOUND for non-existent tenant on delete")
        void shouldThrowNotFoundOnDelete() {
            when(tenantRepository.findByIdWithDomainsAndSettings(tenantId))
                    .thenReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class,
                    () -> tenantService.deleteTenant(tenantId));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        }
    }
}
