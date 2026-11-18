package com.hrm.saas.domain.tenant.service;

import com.hrm.saas.common.exception.AppException;
import com.hrm.saas.domain.tenant.dto.request.RegisterDomainRequest;
import com.hrm.saas.domain.tenant.dto.response.TenantDomainResponse;
import com.hrm.saas.domain.tenant.dto.response.TenantSummaryDTO;
import com.hrm.saas.domain.tenant.entity.Tenant;
import com.hrm.saas.domain.tenant.entity.TenantDomain;
import com.hrm.saas.domain.tenant.enums.DomainType;
import com.hrm.saas.domain.tenant.mapper.TenantMapper;
import com.hrm.saas.domain.tenant.repository.TenantDomainRepository;
import com.hrm.saas.domain.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantDomainService {

    private static final String REDIS_DOMAIN_CACHE_PREFIX = "hrm:tenant:domain:";
    private static final Duration DOMAIN_CACHE_TTL = Duration.ofMinutes(15);

    private final TenantDomainRepository tenantDomainRepository;
    private final TenantRepository tenantRepository;
    private final TenantMapper tenantMapper;
    private final DomainVerificationService domainVerificationService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional(readOnly = true)
    public Optional<TenantSummaryDTO> resolveVerifiedDomain(String host) {
        if (host == null || host.isBlank()) {
            return Optional.empty();
        }
        String normalizedHost = host.trim().toLowerCase();
        String cacheKey = REDIS_DOMAIN_CACHE_PREFIX + normalizedHost;

        // 1. Try Redis cache lookup
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof TenantSummaryDTO summary) {
                log.debug("Redis cache hit for domain '{}' -> tenant '{}'", normalizedHost, summary.getSlug());
                return Optional.of(summary);
            }
        } catch (Exception e) {
            log.warn("Redis domain cache query failed for host '{}': {}", normalizedHost, e.getMessage());
        }

        // 2. Query Database
        Optional<TenantDomain> domainOpt = tenantDomainRepository.findByDomainAndIsVerifiedTrueWithTenant(normalizedHost);
        if (domainOpt.isEmpty()) {
            return Optional.empty();
        }

        TenantDomain domain = domainOpt.get();
        Tenant tenant = domain.getTenant();

        TenantSummaryDTO summary = TenantSummaryDTO.builder()
                .tenantId(tenant.getId())
                .name(tenant.getName())
                .slug(tenant.getSlug())
                .status(tenant.getStatus())
                .domain(domain.getDomain())
                .verified(domain.getIsVerified())
                .build();

        // 3. Cache result in Redis
        try {
            redisTemplate.opsForValue().set(cacheKey, summary, DOMAIN_CACHE_TTL);
        } catch (Exception e) {
            log.warn("Failed to write domain '{}' to Redis cache: {}", normalizedHost, e.getMessage());
        }

        return Optional.of(summary);
    }

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
        boolean autoVerified = normalizedDomain.endsWith(".localhost") || request.getDomainType() == DomainType.SUBDOMAIN;

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
                domain.getVerificationToken()
        );

        if (!verified) {
            throw new AppException("Domain ownership verification failed. Please check DNS TXT record.", HttpStatus.BAD_REQUEST);
        }

        domain.setIsVerified(true);
        domain.setVerifiedAt(Instant.now());
        TenantDomain saved = tenantDomainRepository.save(domain);
        evictDomainCache(domain.getDomain());

        return tenantMapper.toDomainResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TenantDomainResponse> listDomainsByTenant(UUID tenantId) {
        return tenantDomainRepository.findByTenantIdAndIsDeletedFalse(tenantId).stream()
                .map(tenantMapper::toDomainResponse)
                .collect(Collectors.toList());
    }

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

    public void evictDomainCache(String domain) {
        if (domain == null) return;
        try {
            redisTemplate.delete(REDIS_DOMAIN_CACHE_PREFIX + domain.trim().toLowerCase());
        } catch (Exception e) {
            log.warn("Failed to evict Redis cache for domain '{}': {}", domain, e.getMessage());
        }
    }
}
