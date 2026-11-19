package com.hrm.saas.common.constants;

/**
 * Centralized API route definitions and version management.
 * All controllers, security matchers, and client contracts reference these routes.
 */
public final class ApiRoutes {

    private ApiRoutes() {
        // Prevent instantiation
    }

    public static final String API_PREFIX = "/api";
    public static final String V1 = API_PREFIX + "/v1";
    public static final String V2 = API_PREFIX + "/v2";

    /**
     * System & health check endpoints
     */
    public static final class System {
        private System() {}

        public static final String BASE = V1 + "/system";
        public static final String STATUS = "/status";
        public static final String ECHO = "/echo";
    }

    /**
     * Platform administration & tenant lifecycle endpoints
     */
    public static final class Platform {
        private Platform() {}

        public static final String BASE = V1 + "/platform";

        public static final class Tenants {
            private Tenants() {}

            public static final String BASE = Platform.BASE + "/tenants";
            public static final String BY_ID = "/{id}";
            public static final String BY_SLUG = "/slug/{slug}";
            public static final String SUSPEND = "/{id}/suspend";
            public static final String ACTIVATE = "/{id}/activate";

            public static final class Domains {
                private Domains() {}

                public static final String BASE = Tenants.BASE + "/{tenantId}/domains";
                public static final String BY_ID = "/{domainId}";
                public static final String VERIFY = "/{domainId}/verify";
            }
        }
    }
}
