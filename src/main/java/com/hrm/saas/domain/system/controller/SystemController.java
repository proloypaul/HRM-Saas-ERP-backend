package com.hrm.saas.domain.system.controller;

import com.hrm.saas.common.constants.ApiRoutes;
import com.hrm.saas.common.response.ApiResponse;
import com.hrm.saas.domain.system.dto.SystemEchoRequest;
import com.hrm.saas.domain.system.dto.SystemStatusResponse;
import com.hrm.saas.domain.system.service.SystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiRoutes.System.BASE)
@RequiredArgsConstructor
@Tag(name = "System", description = "System health, connectivity & test endpoints")
public class SystemController {

    private final SystemService systemService;

    @GetMapping(ApiRoutes.System.STATUS)
    @Operation(summary = "Get system operational status", description = "Returns active profile and service health info")
    public ResponseEntity<ApiResponse<SystemStatusResponse>> getStatus() {
        SystemStatusResponse response = systemService.getSystemStatus();
        return ResponseEntity.ok(ApiResponse.success(response, "System status retrieved successfully"));
    }

    @PostMapping(ApiRoutes.System.ECHO)
    @Operation(summary = "Validate and echo test message", description = "Tests validation, JSON serialization and request pipeline")
    public ResponseEntity<ApiResponse<String>> echo(@Valid @RequestBody SystemEchoRequest request) {
        String result = systemService.echoMessage(request);
        return ResponseEntity.ok(ApiResponse.success(result, "Echo processed successfully"));
    }
}
