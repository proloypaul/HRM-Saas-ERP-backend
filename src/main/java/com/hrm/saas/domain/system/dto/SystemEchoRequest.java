package com.hrm.saas.domain.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for system echo testing")
public record SystemEchoRequest(
        @NotBlank(message = "Message cannot be blank")
        @Size(min = 2, max = 100, message = "Message length must be between 2 and 100 characters")
        @Schema(description = "Content to echo", example = "HRM SaaS System Test")
        String message
) {
}
