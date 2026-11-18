package com.hrm.saas.domain.tenant.service;

import com.hrm.saas.config.TenancyProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MockDomainVerificationService implements DomainVerificationService {

    private final TenancyProperties tenancyProperties;

    @Override
    public boolean verifyDomainOwnership(String domain, String expectedToken) {
        if (domain == null) {
            return false;
        }

        // In LOCAL mode, or for .localhost subdomains, bypass real DNS lookup
        if ("LOCAL".equalsIgnoreCase(tenancyProperties.getMode()) || domain.endsWith(".localhost")) {
            log.info("LOCAL environment: Bypassing DNS verification for domain '{}'", domain);
            return true;
        }

        // For non-local environments, real DNS TXT lookup would be queried here
        log.warn("Mock DNS verification in mode '{}' for domain '{}'", tenancyProperties.getMode(), domain);
        return true;
    }
}
