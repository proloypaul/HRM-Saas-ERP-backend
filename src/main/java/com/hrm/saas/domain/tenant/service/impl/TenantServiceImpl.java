package com.hrm.saas.domain.tenant.service.impl;

import com.hrm.saas.common.exception.AppException;
import com.hrm.saas.config.TenancyProperties;
import com.hrm.saas.domain.tenant.dto.request.CreateTenantRequest;
import com.hrm.saas.domain.tenant.dto.request.RegisterDomainRequest;
import com.hrm.saas.domain.tenant.dto.request.UpdateTenantRequest;
import com.hrm.saas.domain.tenant.dto.response.TenantResponse;
import com.hrm.saas.domain.tenant.entity.Tenant;
import com.hrm.saas.domain.tenant.entity.TenantSettings;
import com.hrm.saas.domain.tenant.enums.DomainType;
import com.hrm.saas.domain.tenant.enums.TenantStatus;
import com.hrm.saas.domain.tenant.mapper.TenantMapper;
import com.hrm.saas.domain.tenant.repository.TenantRepository;
import com.hrm.saas.domain.tenant.service.TenantDomainService;
import com.hrm.saas.domain.tenant.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {

    private final TenantRepository tenantRepository;
    private final TenantDomainService tenantDomainService;
    private final TenantMapper tenantMapper;
    private final TenancyProperties tenancyProperties;

    @Override
    @Transactional
    public TenantResponse createTenant(CreateTenantRequest request) {
        String slug = request.getSlug().trim().toLowerCase();

        if (tenantRepository.existsBySlug(slug)) {
            throw new AppException("Tenant with slug '" + slug + "' already exists", HttpStatus.CONFLICT);
        }

        Tenant tenant = tenantMapper.toEntity(request);

        // 1. Initialize default tenant settings
        TenantSettings settings = TenantSettings.builder()
                .tenant(tenant)
                .settings("{\"branding\":{\"primaryColor\":\"#2563EB\"},\"features\":{\"payroll\":true,\"attendance\":true}}")
                .build();
        tenant.setSettings(settings);

        Tenant savedTenant = tenantRepository.save(tenant);

        // 2. Automatically register default system subdomain (e.g. acme.localhost in dev or acme.yourhrm.com in prod)
        String defaultDomain = slug + "." + tenancyProperties.getBaseDomain();
        tenantDomainService.registerDomain(
                savedTenant.getId(),
                RegisterDomainRequest.builder()
                        .domain(defaultDomain)
                        .domainType(DomainType.SUBDOMAIN)
                        .isPrimary(true)
                        .build()
        );

        // Reload tenant with domains populated
        Tenant reloaded = tenantRepository.findByIdWithDomains(savedTenant.getId()).orElse(savedTenant);
        log.info("Tenant '{}' created with default domain '{}'", slug, defaultDomain);
        return tenantMapper.toResponse(reloaded);
    }

    @Override
    @Transactional(readOnly = true)
    public TenantResponse getTenantById(UUID id) {
        Tenant tenant = tenantRepository.findByIdWithDomains(id)
                .filter(t -> !t.getIsDeleted())
                .orElseThrow(() -> new AppException("Tenant not found", HttpStatus.NOT_FOUND));

        return tenantMapper.toResponse(tenant);
    }

    @Override
    @Transactional(readOnly = true)
    public TenantResponse getTenantBySlug(String slug) {
        Tenant tenant = tenantRepository.findBySlugAndIsDeletedFalse(slug.trim().toLowerCase())
                .orElseThrow(() -> new AppException("Tenant not found with slug: " + slug, HttpStatus.NOT_FOUND));

        return tenantMapper.toResponse(tenant);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TenantResponse> listTenants(Pageable pageable) {
        return tenantRepository.findAll(pageable)
                .map(tenantMapper::toResponse);
    }

    @Override
    @Transactional
    public TenantResponse updateTenant(UUID id, UpdateTenantRequest request) {
        Tenant tenant = tenantRepository.findById(id)
                .filter(t -> !t.getIsDeleted())
                .orElseThrow(() -> new AppException("Tenant not found", HttpStatus.NOT_FOUND));

        if (request.getName() != null && !request.getName().isBlank()) {
            tenant.setName(request.getName().trim());
        }
        if (request.getLegalName() != null) {
            tenant.setLegalName(request.getLegalName().trim());
        }
        if (request.getCompanyEmail() != null && !request.getCompanyEmail().isBlank()) {
            tenant.setCompanyEmail(request.getCompanyEmail().trim().toLowerCase());
        }
        if (request.getPhone() != null) {
            tenant.setPhone(request.getPhone().trim());
        }
        if (request.getTimezone() != null) {
            tenant.setTimezone(request.getTimezone().trim());
        }
        if (request.getCurrency() != null) {
            tenant.setCurrency(request.getCurrency().trim());
        }

        Tenant updated = tenantRepository.save(tenant);
        evictTenantDomains(updated);

        return tenantMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public TenantResponse suspendTenant(UUID id) {
        Tenant tenant = tenantRepository.findById(id)
                .filter(t -> !t.getIsDeleted())
                .orElseThrow(() -> new AppException("Tenant not found", HttpStatus.NOT_FOUND));

        tenant.setStatus(TenantStatus.SUSPENDED);
        Tenant saved = tenantRepository.save(tenant);
        evictTenantDomains(saved);

        log.warn("Tenant '{}' (id: {}) has been SUSPENDED", saved.getSlug(), saved.getId());
        return tenantMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TenantResponse activateTenant(UUID id) {
        Tenant tenant = tenantRepository.findById(id)
                .filter(t -> !t.getIsDeleted())
                .orElseThrow(() -> new AppException("Tenant not found", HttpStatus.NOT_FOUND));

        tenant.setStatus(TenantStatus.ACTIVE);
        Tenant saved = tenantRepository.save(tenant);
        evictTenantDomains(saved);

        log.info("Tenant '{}' (id: {}) has been ACTIVATED", saved.getSlug(), saved.getId());
        return tenantMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteTenant(UUID id) {
        Tenant tenant = tenantRepository.findById(id)
                .filter(t -> !t.getIsDeleted())
                .orElseThrow(() -> new AppException("Tenant not found", HttpStatus.NOT_FOUND));

        tenant.setIsDeleted(true);
        tenant.setDeletedAt(Instant.now());
        tenant.setStatus(TenantStatus.CANCELLED);
        Tenant saved = tenantRepository.save(tenant);
        evictTenantDomains(saved);

        log.warn("Tenant '{}' (id: {}) has been CANCELLED and soft-deleted", saved.getSlug(), saved.getId());
    }

    private void evictTenantDomains(Tenant tenant) {
        if (tenant.getDomains() != null) {
            tenant.getDomains().forEach(d -> tenantDomainService.evictDomainCache(d.getDomain()));
        }
    }
}
