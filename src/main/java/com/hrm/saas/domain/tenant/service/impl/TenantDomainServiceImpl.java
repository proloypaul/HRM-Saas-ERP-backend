package com.hrm.saas.domain.tenant.service.impl;

import com.hrm.saas.common.cache.RedisCacheService;
import com.hrm.saas.common.exception.AppException;
import com.hrm.saas.config.RedisCacheProperties;
import com.hrm.saas.domain.tenant.dto.request.RegisterDomainRequest;
import com.hrm.saas.domain.tenant.dto.response.TenantDomainResponse;
import com.hrm.saas.domain.tenant.dto.response.TenantSummaryDTO;
import com.hrm.saas.domain.tenant.entity.Tenant;
import com.hrm.saas.domain.tenant.entity.TenantDomain;
import com.hrm.saas.domain.tenant.enums.DomainType;
import com.hrm.saas.domain.tenant.mapper.TenantMapper;
import com.hrm.saas.domain.tenant.repository.TenantDomainRepository;
import com.hrm.saas.domain.tenant.repository.TenantRepository;
import com.hrm.saas.domain.tenant.service.DomainVerificationService;
import com.hrm.saas.domain.tenant.service.TenantDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantDomainServiceImpl implements TenantDomainService {

    private static final String DOMAIN_CACHE_PREFIX = "tenant:domain:";

    private final TenantDomainRepository tenantDomainRepository;
    private final TenantRepository tenantRepository;
    private final TenantMapper tenantMapper;
    private final DomainVerificationService domainVerificationService;
    private final RedisCacheService redisCacheService;
    private final RedisCacheProperties cacheProperties;

    @Override
    @Transactional(readOnly = true)
    public Optional<TenantSummaryDTO> resolveVerifiedDomain(String host) {
        if (host == null || host.isBlank()) {
            return Optional.empty();
        }
        String normalizedHost = host.trim().toLowerCase();
        String cacheKey = DOMAIN_CACHE_PREFIX + normalizedHost;

        // Centralized Cache-Aside with fallback & safe repopulation
        return redisCacheService.getOrFetch(
                cacheKey,
                TenantSummaryDTO.class,
                cacheProperties.getDomainTtl(),
                () -> fetchDomainFromDatabase(normalizedHost));
    }

    private Optional<TenantSummaryDTO> fetchDomainFromDatabase(String normalizedHost) {
        return tenantDomainRepository.findByDomainAndIsVerifiedTrueWithTenant(normalizedHost)
                .map(domain -> {
                    Tenant tenant = domain.getTenant();
                    return TenantSummaryDTO.builder()
                            .tenantId(tenant.getId())
                            .name(tenant.getName())
                            .slug(tenant.getSlug())
                            .status(tenant.getStatus())
                            .domain(domain.getDomain())
                            .verified(domain.getIsVerified())
                            .build();
                });
    }

    @Override
    @Transactional
    public TenantDomainResponse registerDomain(UUID tenantId, RegisterDomainRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .filter(t -> !t.getIsDeleted())
                .orElseThrow(() -> new AppException("Tenant not found", HttpStatus.NOT_FOUND));

        String normalizedDomain = request.getDomain().trim().toLowerCase();
        if (tenantDomainRepository.existsByDomain(normalizedDomain)) {
            throw new AppException("Domain '" + normalizedDomain + "' is already registered", HttpStatus.CONFLICT);
        }

        String verificationToken = "hrm-verify-" + UUID.randomUUID();

        // Local development domains (*.localhost) are automatically verified
        boolean autoVerified = normalizedDomain.endsWith(".localhost")
                || request.getDomainType() == DomainType.SUBDOMAIN;

        TenantDomain domain = TenantDomain.builder()
                .tenant(tenant)
                .domain(normalizedDomain)
                .domainType(request.getDomainType())
                .isPrimary(request.isPrimary())
                .isVerified(autoVerified)
                .verificationToken(verificationToken)
                .verifiedAt(autoVerified ? Instant.now() : null)
                .build();

        TenantDomain saved = tenantDomainRepository.save(domain);
        evictDomainCache(normalizedDomain);

        return tenantMapper.toDomainResponse(saved);
    }

    @Override
    @Transactional
    public TenantDomainResponse verifyDomain(UUID domainId) {
        TenantDomain domain = tenantDomainRepository.findById(domainId)
                .filter(d -> !d.getIsDeleted())
                .orElseThrow(() -> new AppException("Domain record not found", HttpStatus.NOT_FOUND));

        if (domain.getIsVerified()) {
            return tenantMapper.toDomainResponse(domain);
        }

        boolean verified = domainVerificationService.verifyDomainOwnership(
                domain.getDomain(),
                domain.getVerificationToken());

        if (!verified) {
            throw new AppException("Domain ownership verification failed. Please check DNS TXT record.",
                    HttpStatus.BAD_REQUEST);
        }

        domain.setIsVerified(true);
        domain.setVerifiedAt(Instant.now());
        TenantDomain saved = tenantDomainRepository.save(domain);
        evictDomainCache(domain.getDomain());

        return tenantMapper.toDomainResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantDomainResponse> listDomainsByTenant(UUID tenantId) {
        return tenantDomainRepository.findByTenantIdAndIsDeletedFalse(tenantId).stream()
                .map(tenantMapper::toDomainResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteDomain(UUID domainId) {
        TenantDomain domain = tenantDomainRepository.findById(domainId)
                .filter(d -> !d.getIsDeleted())
                .orElseThrow(() -> new AppException("Domain record not found", HttpStatus.NOT_FOUND));

        domain.setIsDeleted(true);
        domain.setDeletedAt(Instant.now());
        tenantDomainRepository.save(domain);
        evictDomainCache(domain.getDomain());
    }

    @Override
    public void evictDomainCache(String domain) {
        if (domain == null)
            return;
        redisCacheService.evict(DOMAIN_CACHE_PREFIX + domain.trim().toLowerCase());
    }
}
