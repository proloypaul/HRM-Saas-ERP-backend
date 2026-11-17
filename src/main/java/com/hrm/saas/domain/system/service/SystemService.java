package com.hrm.saas.domain.system.service;

import com.hrm.saas.domain.system.dto.SystemEchoRequest;
import com.hrm.saas.domain.system.dto.SystemStatusResponse;

public interface SystemService {
    SystemStatusResponse getSystemStatus();
    String echoMessage(SystemEchoRequest request);
}
