package com.hrm.saas.domain.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrm.saas.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles unauthenticated requests or expired/invalid tokens.
 * Returns HTTP 401 Unauthorized so frontend clients (Axios interceptor) can trigger silent refresh.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        String tokenError = (String) request.getAttribute("jwt_token_error");
        String message = (tokenError != null && !tokenError.isBlank())
                ? tokenError
                : "Full authentication is required to access this resource";

        log.debug("Unauthorized access to '{}': {}", request.getRequestURI(), message);

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiResponse<Void> body = ApiResponse.error(message);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
