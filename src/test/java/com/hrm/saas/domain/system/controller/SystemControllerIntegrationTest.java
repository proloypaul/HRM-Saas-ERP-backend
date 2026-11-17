package com.hrm.saas.domain.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrm.saas.domain.system.dto.SystemEchoRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SystemControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/v1/system/status returns 200 with standard envelope")
    void shouldReturnSystemStatus() throws Exception {
        mockMvc.perform(get("/api/v1/system/status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("successfully")))
                .andExpect(jsonPath("$.data.status", is("HEALTHY")))
                .andExpect(jsonPath("$.data.serviceName", is("hrm-backend")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/system/echo returns 200 on valid payload")
    void shouldEchoValidMessage() throws Exception {
        SystemEchoRequest request = new SystemEchoRequest("HRM Connectivity Check");

        mockMvc.perform(post("/api/v1/system/echo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", containsString("HRM Connectivity Check")));
    }

    @Test
    @DisplayName("POST /api/v1/system/echo returns 400 on blank or short payload")
    void shouldFailValidationOnBlankMessage() throws Exception {
        SystemEchoRequest request = new SystemEchoRequest("x"); // min length is 2

        mockMvc.perform(post("/api/v1/system/echo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].field", is("message")));
    }
}
