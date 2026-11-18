package com.hrm.saas.domain.tenant.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TenantContextTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should bind and clear TenantContext on ThreadLocal")
    void shouldBindAndClearContext() {
        UUID tenantId = UUID.randomUUID();
        String slug = "acme";
        String domain = "acme.localhost";

        TenantContext.setTenant(tenantId, slug, domain);

        assertTrue(TenantContext.hasTenant());
        assertEquals(tenantId, TenantContext.getTenantId());
        assertEquals(slug, TenantContext.getTenantSlug());
        assertEquals(domain, TenantContext.getTenant().getDomain());

        TenantContext.clear();

        assertFalse(TenantContext.hasTenant());
        assertNull(TenantContext.getTenantId());
        assertNull(TenantContext.getTenantSlug());
    }

    @Test
    @DisplayName("TenantAwareTaskDecorator propagates and clears context on worker thread")
    void shouldPropagateContextInTaskDecorator() throws InterruptedException {
        UUID tenantId = UUID.randomUUID();
        TenantContext.setTenant(tenantId, "techcorp", "techcorp.localhost");

        TenantAwareTaskDecorator decorator = new TenantAwareTaskDecorator();
        final UUID[] workerThreadTenantId = new UUID[1];

        Thread worker = new Thread(decorator.decorate(() -> {
            workerThreadTenantId[0] = TenantContext.getTenantId();
        }));

        worker.start();
        worker.join();

        assertEquals(tenantId, workerThreadTenantId[0]);
        // Main thread context remains intact
        assertEquals(tenantId, TenantContext.getTenantId());
    }
}
