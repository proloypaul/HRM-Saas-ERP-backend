package com.hrm.saas.domain.tenant.resolver;

import com.hrm.saas.config.TenancyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
@RequiredArgsConstructor
public class SubdomainTenantResolver implements TenantResolver {

    private final TenancyProperties properties;

    @Override
    public boolean supports(String hostname) {
        if (hostname == null || properties.getBaseDomain() == null) {
            return false;
        }
        String host = hostname.toLowerCase();
        String base = properties.getBaseDomain().toLowerCase();
        String suffix = "." + base;

        if (host.endsWith(suffix) && !host.equals(base)) {
            String prefix = host.substring(0, host.length() - suffix.length());
            // Exclude platform administrative subdomain e.g. platform.yourhrm.com
            return !prefix.equalsIgnoreCase(properties.getPlatformSubdomain());
        }
        return false;
    }

    @Override
    public String resolveDomain(String hostname) {
        return hostname.toLowerCase();
    }
}
