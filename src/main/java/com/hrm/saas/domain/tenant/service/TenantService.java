package com.hrm.saas.domain.tenant.service;

import com.hrm.saas.domain.tenant.dto.request.CreateTenantRequest;
import com.hrm.saas.domain.tenant.dto.request.UpdateTenantRequest;
import com.hrm.saas.domain.tenant.dto.response.TenantResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TenantService {

    TenantResponse createTenant(CreateTenantRequest request);

    TenantResponse getTenantById(UUID id);

    TenantResponse getTenantBySlug(String slug);

    Page<TenantResponse> listTenants(Pageable pageable);

    TenantResponse updateTenant(UUID id, UpdateTenantRequest request);

    TenantResponse suspendTenant(UUID id);

    TenantResponse activateTenant(UUID id);

    void deleteTenant(UUID id);
}
