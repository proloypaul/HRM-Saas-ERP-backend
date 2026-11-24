package com.hrm.saas.domain.tenant.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrm.saas.common.response.ApiResponse;
import com.hrm.saas.domain.tenant.context.TenantContext;
import com.hrm.saas.domain.tenant.dto.response.TenantSummaryDTO;
import com.hrm.saas.domain.tenant.enums.TenantStatus;
import com.hrm.saas.domain.tenant.resolver.CompositeTenantResolver;
import com.hrm.saas.domain.tenant.service.TenantDomainService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
@RequiredArgsConstructor
public class TenantResolutionFilter extends OncePerRequestFilter {

    private final CompositeTenantResolver compositeTenantResolver;
    private final TenantDomainService tenantDomainService;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        // Bypass platform management endpoints, auth endpoints, public documentation, and health actuators
        return path.startsWith("/api/platform/") ||
                path.startsWith("/api/v1/platform/") ||
                path.startsWith("/api/v1/auth/") ||
                path.startsWith("/api/v1/system/") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String hostHeader = request.getHeader("Host");
            Optional<String> domainCandidate = compositeTenantResolver.resolve(hostHeader);

            if (domainCandidate.isEmpty()) {
                // If it's the root platform host (e.g. localhost:8080 or yourhrm.com) without a subdomain, allow through if not tenant-specific
                log.debug("No tenant domain detected for host '{}'", hostHeader);
                filterChain.doFilter(request, response);
                return;
            }

            String domain = domainCandidate.get();
            Optional<TenantSummaryDTO> summaryOpt = tenantDomainService.resolveVerifiedDomain(domain);

            if (summaryOpt.isEmpty()) {
                log.warn("Access attempt with unrecognized/unverified domain: {}", domain);
                sendErrorResponse(response, HttpStatus.NOT_FOUND, "Tenant domain not recognized or not verified: " + domain);
                return;
            }

            TenantSummaryDTO tenantSummary = summaryOpt.get();

            // Validate Tenant Status
            if (tenantSummary.getStatus() == TenantStatus.SUSPENDED) {
                log.warn("Access blocked for suspended tenant '{}'", tenantSummary.getSlug());
                sendErrorResponse(response, HttpStatus.FORBIDDEN, "Tenant account is currently suspended. Please contact platform support.");
                return;
            }

            if (tenantSummary.getStatus() == TenantStatus.CANCELLED) {
                log.warn("Access blocked for cancelled tenant '{}'", tenantSummary.getSlug());
                sendErrorResponse(response, HttpStatus.FORBIDDEN, "Tenant account has been cancelled.");
                return;
            }

            if (tenantSummary.getStatus() != TenantStatus.ACTIVE) {
                log.warn("Access blocked for inactive tenant '{}' with status: {}", tenantSummary.getSlug(), tenantSummary.getStatus());
                sendErrorResponse(response, HttpStatus.FORBIDDEN, "Tenant account is not active.");
                return;
            }

            // Set context for the current request
            TenantContext.setTenant(tenantSummary.getTenantId(), tenantSummary.getSlug(), domain);
            log.debug("TenantContext bound: id='{}', slug='{}', domain='{}'",
                    tenantSummary.getTenantId(), tenantSummary.getSlug(), domain);

            filterChain.doFilter(request, response);

        } finally {
            // Guarantee ThreadLocal context clean-up in all scenarios
            TenantContext.clear();
        }
    }

    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<Void> apiResponse = ApiResponse.error(message);
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
