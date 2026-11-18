package com.hrm.saas.domain.tenant.resolver;

import com.hrm.saas.config.TenancyProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CompositeTenantResolverTest {

    private CompositeTenantResolver compositeTenantResolver;

    @BeforeEach
    void setUp() {
        TenancyProperties properties = new TenancyProperties();
        properties.setMode("LOCAL");
        properties.setBaseDomain("localhost");
        properties.setAllowLocalhostSubdomains(true);
        properties.setPlatformSubdomain("platform");

        LocalTenantResolver localResolver = new LocalTenantResolver(properties);
        SubdomainTenantResolver subdomainResolver = new SubdomainTenantResolver(properties);
        CustomDomainTenantResolver customDomainResolver = new CustomDomainTenantResolver(properties);

        compositeTenantResolver = new CompositeTenantResolver(List.of(localResolver, subdomainResolver, customDomainResolver));
    }

    @Test
    @DisplayName("Should resolve localhost subdomain with port")
    void shouldResolveLocalhostSubdomainWithPort() {
        Optional<String> domain = compositeTenantResolver.resolve("acme.localhost:8080");
        assertTrue(domain.isPresent());
        assertEquals("acme.localhost", domain.get());
    }

    @Test
    @DisplayName("Should resolve company localhost subdomain")
    void shouldResolveCompanyLocalhostSubdomain() {
        Optional<String> domain = compositeTenantResolver.resolve("company-b.localhost:9080");
        assertTrue(domain.isPresent());
        assertEquals("company-b.localhost", domain.get());
    }

    @Test
    @DisplayName("Should not resolve bare localhost")
    void shouldNotResolveBareLocalhost() {
        Optional<String> domain = compositeTenantResolver.resolve("localhost:8080");
        assertTrue(domain.isEmpty());
    }

    @Test
    @DisplayName("Should resolve custom vanity domain")
    void shouldResolveCustomDomain() {
        Optional<String> domain = compositeTenantResolver.resolve("hr.acme.corp:443");
        assertTrue(domain.isPresent());
        assertEquals("hr.acme.corp", domain.get());
    }
}
