package com.hrm.saas.domain.system.service;

import com.hrm.saas.domain.system.dto.SystemEchoRequest;
import com.hrm.saas.domain.system.dto.SystemStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemServiceImpl implements SystemService {

    @Value("${spring.application.name:hrm-backend}")
    private String applicationName;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Override
    public SystemStatusResponse getSystemStatus() {
        log.info("Fetching system status check");
        return new SystemStatusResponse(
                applicationName,
                "v1.0.0",
                activeProfile,
                "HEALTHY",
                Instant.now()
        );
    }

    @Override
    public String echoMessage(SystemEchoRequest request) {
        log.info("Received system echo: {}", request.message());
        return "Echo received: " + request.message();
    }
}
