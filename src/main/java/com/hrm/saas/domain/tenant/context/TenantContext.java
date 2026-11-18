package com.hrm.saas.domain.tenant.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

public final class TenantContext {

    private static final ThreadLocal<TenantInfo> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenant(UUID tenantId, String slug, String domain) {
        CURRENT_TENANT.set(TenantInfo.builder()
                .tenantId(tenantId)
                .slug(slug)
                .domain(domain)
                .build());
    }

    public static void setTenant(TenantInfo tenantInfo) {
        CURRENT_TENANT.set(tenantInfo);
    }

    public static TenantInfo getTenant() {
        return CURRENT_TENANT.get();
    }

    public static UUID getTenantId() {
        TenantInfo info = CURRENT_TENANT.get();
        return info != null ? info.getTenantId() : null;
    }

    public static String getTenantSlug() {
        TenantInfo info = CURRENT_TENANT.get();
        return info != null ? info.getSlug() : null;
    }

    public static boolean hasTenant() {
        return CURRENT_TENANT.get() != null;
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TenantInfo {
        private UUID tenantId;
        private String slug;
        private String domain;
    }
}
