package com.hrm.saas.domain.tenant.resolver;

import com.hrm.saas.config.TenancyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(30)
@RequiredArgsConstructor
public class CustomDomainTenantResolver implements TenantResolver {

    private final TenancyProperties properties;

    @Override
    public boolean supports(String hostname) {
        if (hostname == null) {
            return false;
        }
        String host = hostname.toLowerCase();

        // Not bare localhost, not an IP address, and does not end with base domain
        if (host.equals("localhost") || host.equals("127.0.0.1") || host.endsWith(".localhost")) {
            return false;
        }

        String base = properties.getBaseDomain() != null ? properties.getBaseDomain().toLowerCase() : "";
        if (!base.isEmpty() && (host.equals(base) || host.endsWith("." + base))) {
            return false;
        }

        // Must look like a domain name with at least one dot
        return host.contains(".") && !host.startsWith(".") && !host.endsWith(".");
    }

    @Override
    public String resolveDomain(String hostname) {
        return hostname.toLowerCase();
    }
}
