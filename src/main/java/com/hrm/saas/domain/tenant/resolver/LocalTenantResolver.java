package com.hrm.saas.domain.tenant.resolver;

import com.hrm.saas.config.TenancyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
@RequiredArgsConstructor
public class LocalTenantResolver implements TenantResolver {

    private final TenancyProperties properties;

    @Override
    public boolean supports(String hostname) {
        if (!properties.isAllowLocalhostSubdomains() || hostname == null) {
            return false;
        }
        String host = hostname.toLowerCase();
        return host.endsWith(".localhost") && !host.equals("localhost");
    }

    @Override
    public String resolveDomain(String hostname) {
        return hostname.toLowerCase();
    }
}
