package com.payment.payment_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.payment_api.dto.LoginRequest;
import com.payment.payment_api.dto.PaymentRequest;
import com.payment.payment_api.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class PaymentControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        // 회원가입
        LoginRequest signUpRequest = new LoginRequest();
        setField(signUpRequest, "username", "testuser");
        setField(signUpRequest, "password", "password123");
        authService.signUp(signUpRequest);

        // 로그인 → 토큰 발급
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        accessToken = objectMapper.readTree(response).get("accessToken").asText();
    }

    @Test
    @DisplayName("결제 요청 성공")
    void requestPayment_success() throws Exception {
        PaymentRequest request = new PaymentRequest();
        setField(request, "orderId", "ORDER-TEST-001");
        setField(request, "customerName", "홍길동");
        setField(request, "amount", 10000L);

        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("ORDER-TEST-001"))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.pgTransactionId").isNotEmpty());
    }

    @Test
    @DisplayName("토큰 없이 결제 요청 시 401")
    void requestPayment_withoutToken_401() throws Exception {
        PaymentRequest request = new PaymentRequest();
        setField(request, "orderId", "ORDER-TEST-002");
        setField(request, "customerName", "홍길동");
        setField(request, "amount", 10000L);

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("중복 결제 방지 - 409 응답")
    void requestPayment_duplicate_409() throws Exception {
        PaymentRequest request = new PaymentRequest();
        setField(request, "orderId", "ORDER-TEST-003");
        setField(request, "customerName", "홍길동");
        setField(request, "amount", 10000L);

        // 첫 번째 결제
        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // 두 번째 결제 (중복)
        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    @DisplayName("결제 금액 유효성 검증 - 100원 미만 400 응답")
    void requestPayment_invalidAmount_400() throws Exception {
        PaymentRequest request = new PaymentRequest();
        setField(request, "orderId", "ORDER-TEST-004");
        setField(request, "customerName", "홍길동");
        setField(request, "amount", 50L);  // 100원 미만

        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
