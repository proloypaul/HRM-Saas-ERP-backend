package com.hrm.saas.domain.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "System status and health information")
public record SystemStatusResponse(
        @Schema(description = "Service Name", example = "HRM SaaS Backend")
        String serviceName,
        @Schema(description = "Application Version", example = "v1.0.0")
        String version,
        @Schema(description = "Active Profile", example = "dev")
        String environment,
        @Schema(description = "System Operational Status", example = "HEALTHY")
        String status,
        @Schema(description = "Current Server Time")
        Instant serverTime
) {
}
