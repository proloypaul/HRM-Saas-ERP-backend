package com.hrm.saas.domain.tenant.resolver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompositeTenantResolver {

    private final List<TenantResolver> resolvers;

    public Optional<String> resolve(String rawHostHeader) {
        if (rawHostHeader == null || rawHostHeader.isBlank()) {
            return Optional.empty();
        }

        // Clean host: strip port if present (e.g. acme.localhost:8080 -> acme.localhost)
        String host = rawHostHeader.trim();
        int portIndex = host.indexOf(':');
        if (portIndex != -1) {
            host = host.substring(0, portIndex);
        }
        host = host.toLowerCase();

        for (TenantResolver resolver : resolvers) {
            if (resolver.supports(host)) {
                String resolvedDomain = resolver.resolveDomain(host);
                log.debug("Resolved host '{}' via {} to domain '{}'", host, resolver.getClass().getSimpleName(), resolvedDomain);
                return Optional.of(resolvedDomain);
            }
        }

        log.debug("No tenant resolver supported host '{}'", host);
        return Optional.empty();
    }
}
