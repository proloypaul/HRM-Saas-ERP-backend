package com.hrm.saas.domain.tenant.service;

public interface DomainVerificationService {

    /**
     * Checks if domain ownership is verified (e.g. via DNS TXT record or local bypass).
     */
    boolean verifyDomainOwnership(String domain, String expectedToken);
}
