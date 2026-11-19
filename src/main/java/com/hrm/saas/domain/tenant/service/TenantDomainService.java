package com.hrm.saas.domain.tenant.service;

import com.hrm.saas.domain.tenant.dto.request.RegisterDomainRequest;
import com.hrm.saas.domain.tenant.dto.response.TenantDomainResponse;
import com.hrm.saas.domain.tenant.dto.response.TenantSummaryDTO;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantDomainService {

    Optional<TenantSummaryDTO> resolveVerifiedDomain(String host);

    TenantDomainResponse registerDomain(UUID tenantId, RegisterDomainRequest request);

    TenantDomainResponse verifyDomain(UUID domainId);

    List<TenantDomainResponse> listDomainsByTenant(UUID tenantId);

    void deleteDomain(UUID domainId);

    void evictDomainCache(String domain);
}
