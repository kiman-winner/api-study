package com.payment.payment_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.payment_api.client.PgClient;
import com.payment.payment_api.dto.LoginRequest;
import com.payment.payment_api.dto.PaymentRequest;
import com.payment.payment_api.entity.Payment;
import com.payment.payment_api.enums.PaymentStatus;
import com.payment.payment_api.repository.PaymentRepository;
import com.payment.payment_api.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentControllerFailedIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private PaymentRepository paymentRepository;

    private String accessToken;

    @MockitoBean
    private PgClient pgClient;

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
    @DisplayName("결제 요청 PG 예외 발생 - DB 저장 검증")
    void payment_fail_event_test() throws Exception {
        PaymentRequest request = new PaymentRequest();
        setField(request, "orderId", "ORDER-TEST-005");
        setField(request, "customerName", "홍길동");
        setField(request, "amount", 1000L);

        // Given: pgClient 호출 시 RuntimeException 발생 설정
        BDDMockito.given(pgClient.approve(request.getOrderId(), request.getAmount()))
                .willThrow(new RuntimeException("PG사 점검"));

        // When & Then
        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError()); // 500 에러를 기대할 때

        Payment savedPayment = paymentRepository.findByOrderId("ORDER-TEST-005")
                .orElseThrow();
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(savedPayment.getFailureReason()).contains("PG사 점검");
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
