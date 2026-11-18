package com.hrm.saas.domain.tenant.mapper;

import com.hrm.saas.domain.tenant.dto.request.CreateTenantRequest;
import com.hrm.saas.domain.tenant.dto.response.TenantDomainResponse;
import com.hrm.saas.domain.tenant.dto.response.TenantResponse;
import com.hrm.saas.domain.tenant.entity.Tenant;
import com.hrm.saas.domain.tenant.entity.TenantDomain;
import com.hrm.saas.domain.tenant.enums.TenantStatus;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TenantMapper {

    public Tenant toEntity(CreateTenantRequest request) {
        if (request == null) {
            return null;
        }
        return Tenant.builder()
                .name(request.getName().trim())
                .slug(request.getSlug().trim().toLowerCase())
                .legalName(request.getLegalName() != null ? request.getLegalName().trim() : null)
                .companyEmail(request.getCompanyEmail().trim().toLowerCase())
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .timezone(request.getTimezone() != null ? request.getTimezone() : "UTC")
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .status(TenantStatus.ACTIVE)
                .build();
    }

    public TenantResponse toResponse(Tenant tenant) {
        if (tenant == null) {
            return null;
        }
        List<TenantDomainResponse> domainResponses = tenant.getDomains() != null
                ? tenant.getDomains().stream().map(this::toDomainResponse).collect(Collectors.toList())
                : Collections.emptyList();

        String settings = tenant.getSettings() != null ? tenant.getSettings().getSettings() : "{}";

        return TenantResponse.builder()
                .id(tenant.getId())
                .name(tenant.getName())
                .slug(tenant.getSlug())
                .legalName(tenant.getLegalName())
                .companyEmail(tenant.getCompanyEmail())
                .phone(tenant.getPhone())
                .status(tenant.getStatus())
                .timezone(tenant.getTimezone())
                .currency(tenant.getCurrency())
                .domains(domainResponses)
                .settings(settings)
                .createdAt(tenant.getCreatedAt())
                .updatedAt(tenant.getUpdatedAt())
                .build();
    }

    public TenantDomainResponse toDomainResponse(TenantDomain domain) {
        if (domain == null) {
            return null;
        }
        return TenantDomainResponse.builder()
                .id(domain.getId())
                .tenantId(domain.getTenant() != null ? domain.getTenant().getId() : null)
                .domain(domain.getDomain())
                .domainType(domain.getDomainType())
                .isPrimary(Boolean.TRUE.equals(domain.getIsPrimary()))
                .isVerified(Boolean.TRUE.equals(domain.getIsVerified()))
                .verificationToken(domain.getVerificationToken())
                .verifiedAt(domain.getVerifiedAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
