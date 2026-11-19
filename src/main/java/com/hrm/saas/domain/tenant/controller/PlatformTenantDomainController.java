package com.hrm.saas.domain.tenant.controller;

import com.hrm.saas.common.constants.ApiRoutes;
import com.hrm.saas.common.response.ApiResponse;
import com.hrm.saas.domain.tenant.dto.request.RegisterDomainRequest;
import com.hrm.saas.domain.tenant.dto.response.TenantDomainResponse;
import com.hrm.saas.domain.tenant.service.TenantDomainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiRoutes.Platform.Tenants.Domains.BASE)
@RequiredArgsConstructor
@Tag(name = "Platform Tenant Domains", description = "Endpoints for managing custom domains and subdomains per tenant")
public class PlatformTenantDomainController {

    private final TenantDomainService tenantDomainService;

    @GetMapping
    @Operation(summary = "List all domains registered to a tenant")
    public ResponseEntity<ApiResponse<List<TenantDomainResponse>>> listDomains(@PathVariable UUID tenantId) {
        List<TenantDomainResponse> domains = tenantDomainService.listDomainsByTenant(tenantId);
        return ResponseEntity.ok(ApiResponse.success(domains, "Domains retrieved successfully"));
    }

    @PostMapping
    @Operation(summary = "Register a new subdomain or custom domain for a tenant")
    public ResponseEntity<ApiResponse<TenantDomainResponse>> registerDomain(
            @PathVariable UUID tenantId,
            @Valid @RequestBody RegisterDomainRequest request) {
        TenantDomainResponse response = tenantDomainService.registerDomain(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Domain registered successfully"));
    }

    @PostMapping(ApiRoutes.Platform.Tenants.Domains.VERIFY)
    @Operation(summary = "Verify domain ownership")
    public ResponseEntity<ApiResponse<TenantDomainResponse>> verifyDomain(
            @PathVariable UUID tenantId,
            @PathVariable UUID domainId) {
        TenantDomainResponse response = tenantDomainService.verifyDomain(domainId);
        return ResponseEntity.ok(ApiResponse.success(response, "Domain verified successfully"));
    }

    @DeleteMapping(ApiRoutes.Platform.Tenants.Domains.BY_ID)
    @Operation(summary = "Remove a domain from a tenant")
    public ResponseEntity<ApiResponse<Void>> deleteDomain(
            @PathVariable UUID tenantId,
            @PathVariable UUID domainId) {
        tenantDomainService.deleteDomain(domainId);
        return ResponseEntity.ok(ApiResponse.success(null, "Domain deleted successfully"));
    }
}
