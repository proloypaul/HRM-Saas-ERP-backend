package com.hrm.saas.domain.tenant.controller;

import com.hrm.saas.common.constants.ApiRoutes;
import com.hrm.saas.common.response.ApiResponse;
import com.hrm.saas.domain.tenant.dto.request.CreateTenantRequest;
import com.hrm.saas.domain.tenant.dto.request.UpdateTenantRequest;
import com.hrm.saas.domain.tenant.dto.response.TenantResponse;
import com.hrm.saas.domain.tenant.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(ApiRoutes.Platform.Tenants.BASE)
@RequiredArgsConstructor
@Tag(name = "Platform Tenant Management", description = "Endpoints for platform administrators to manage tenant lifecycles")
public class PlatformTenantController {

    private final TenantService tenantService;

    @PostMapping
    @Operation(summary = "Register a new tenant company")
    public ResponseEntity<ApiResponse<TenantResponse>> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        TenantResponse response = tenantService.createTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Tenant registered successfully"));
    }

    @GetMapping
    @Operation(summary = "List all tenants with pagination")
    public ResponseEntity<ApiResponse<Page<TenantResponse>>> listTenants(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        Page<TenantResponse> tenants = tenantService.listTenants(pageable);
        return ResponseEntity.ok(ApiResponse.success(tenants, "Tenants retrieved successfully"));
    }

    @GetMapping(ApiRoutes.Platform.Tenants.BY_ID)
    @Operation(summary = "Get tenant details by ID")
    public ResponseEntity<ApiResponse<TenantResponse>> getTenantById(@PathVariable UUID id) {
        TenantResponse tenant = tenantService.getTenantById(id);
        return ResponseEntity.ok(ApiResponse.success(tenant, "Tenant retrieved successfully"));
    }

    @GetMapping(ApiRoutes.Platform.Tenants.BY_SLUG)
    @Operation(summary = "Get tenant details by slug")
    public ResponseEntity<ApiResponse<TenantResponse>> getTenantBySlug(@PathVariable String slug) {
        TenantResponse tenant = tenantService.getTenantBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(tenant, "Tenant retrieved successfully"));
    }

    @PutMapping(ApiRoutes.Platform.Tenants.BY_ID)
    @Operation(summary = "Update tenant profile details")
    public ResponseEntity<ApiResponse<TenantResponse>> updateTenant(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTenantRequest request) {
        TenantResponse updated = tenantService.updateTenant(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Tenant updated successfully"));
    }

    @PostMapping(ApiRoutes.Platform.Tenants.SUSPEND)
    @Operation(summary = "Suspend a tenant account")
    public ResponseEntity<ApiResponse<TenantResponse>> suspendTenant(@PathVariable UUID id) {
        TenantResponse response = tenantService.suspendTenant(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Tenant suspended successfully"));
    }

    @PostMapping(ApiRoutes.Platform.Tenants.ACTIVATE)
    @Operation(summary = "Activate a suspended or pending tenant account")
    public ResponseEntity<ApiResponse<TenantResponse>> activateTenant(@PathVariable UUID id) {
        TenantResponse response = tenantService.activateTenant(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Tenant activated successfully"));
    }

    @DeleteMapping(ApiRoutes.Platform.Tenants.BY_ID)
    @Operation(summary = "Cancel and soft-delete a tenant account")
    public ResponseEntity<ApiResponse<Void>> deleteTenant(@PathVariable UUID id) {
        tenantService.deleteTenant(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Tenant deleted successfully"));
    }
}
