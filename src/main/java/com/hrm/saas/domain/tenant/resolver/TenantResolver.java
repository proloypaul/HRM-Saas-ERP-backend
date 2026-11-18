package com.hrm.saas.domain.tenant.resolver;

public interface TenantResolver {

    /**
     * Determines if this resolver can handle the given hostname.
     */
    boolean supports(String hostname);

    /**
     * Resolves the canonical domain to query against the database.
     */
    String resolveDomain(String hostname);
}
