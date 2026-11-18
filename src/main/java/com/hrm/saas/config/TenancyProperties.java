package com.hrm.saas.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.tenancy")
public class TenancyProperties {

    /**
     * Environment mode: LOCAL, STAGING, PRODUCTION
     */
    private String mode = "LOCAL";

    /**
     * Base domain for tenant subdomains (e.g., localhost in dev, yourhrm.com in prod)
     */
    private String baseDomain = "localhost";

    /**
     * Whether *.localhost subdomains are permitted
     */
    private boolean allowLocalhostSubdomains = true;

    /**
     * Subdomain reserved for platform administration
     */
    private String platformSubdomain = "platform";
}
