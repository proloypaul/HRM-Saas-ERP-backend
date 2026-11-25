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
     * Authentication, Identity & Tenant Membership endpoints
     */
    public static final class Auth {
        private Auth() {}

        public static final String BASE = V1 + "/auth";
        public static final String REGISTER = "/register";
        public static final String LOGIN = "/login";
        public static final String LOGOUT = "/logout";
        public static final String REFRESH = "/refresh";
        public static final String FORGOT_PASSWORD = "/forgot-password";
        public static final String RESET_PASSWORD = "/reset-password";
        public static final String VERIFY_EMAIL = "/verify-email";
        public static final String ME = "/me";
        public static final String TENANTS = "/tenants";
        public static final String SWITCH_TENANT = "/switch-tenant";
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

    /**
     * RBAC, Role & Permission Management endpoints
     */
    public static final class Rbac {
        private Rbac() {}

        public static final String PERMISSIONS = V1 + "/permissions";
        public static final String ROLES = V1 + "/roles";
        public static final String ROLE_BY_ID = "/{id}";
        public static final String ROLE_CLONE = "/{id}/clone";

        public static final String MEMBERSHIPS = V1 + "/memberships";
        public static final String MEMBERSHIP_ROLES = "/{id}/roles";

        public static final String ME_PERMISSIONS = Auth.BASE + "/me/permissions";
    }
}
