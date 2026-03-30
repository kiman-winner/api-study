package com.payment.payment_api.service;

import com.payment.payment_api.client.PgClient;
import com.payment.payment_api.dto.PaymentRequest;
import com.payment.payment_api.dto.PaymentResponse;
import com.payment.payment_api.entity.Payment;
import com.payment.payment_api.enums.PaymentStatus;
import com.payment.payment_api.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    private PaymentRequest paymentRequest;

    @Mock
    private PgClient pgClient;

    @BeforeEach
    void setUp() {
        paymentRequest = new PaymentRequest();
        setField(paymentRequest, "orderId", "ORDER-001");
        setField(paymentRequest, "customerName", "홍길동");
        setField(paymentRequest, "amount", 10000L);
    }

    @Test
    @DisplayName("결제 요청 성공 - APPROVED 상태로 저장")
    void requestPayment_success() {
        // given
        given(paymentRepository.existsByOrderId("ORDER-001")).willReturn(false);
        given(paymentRepository.save(any(Payment.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(pgClient.approve(anyString(), anyLong())).willReturn("PG-MOCK-001");

        // when
        PaymentResponse response = paymentService.requestPayment(paymentRequest);

        // then
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(response.getOrderId()).isEqualTo("ORDER-001");
        assertThat(response.getPgTransactionId()).startsWith("PG-");
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("중복 결제 방지 - 동일 orderId 재요청 시 예외 발생")
    void requestPayment_duplicateOrder_throwsException() {
        // given
        given(paymentRepository.existsByOrderId("ORDER-001")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> paymentService.requestPayment(paymentRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 처리된 주문입니다");
    }

    @Test
    @DisplayName("결제 취소 성공 - APPROVED → CANCELLED")
    void cancelPayment_success() {
        // given
        Payment payment = Payment.builder()
                .orderId("ORDER-001")
                .customerName("홍길동")
                .amount(10000L)
                .build();
        payment.approve("PG-ABCD1234");

        given(paymentRepository.findById(1L)).willReturn(Optional.of(payment));

        // when
        PaymentResponse response = paymentService.cancelPayment(1L);

        // then
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
    }

    @Test
    @DisplayName("결제 취소 실패 - APPROVED 아닌 상태에서 취소 시 예외 발생")
    void cancelPayment_notApproved_throwsException() {
        // given
        Payment payment = Payment.builder()
                .orderId("ORDER-001")
                .customerName("홍길동")
                .amount(10000L)
                .build();
        // PENDING 상태 그대로

        given(paymentRepository.findById(1L)).willReturn(Optional.of(payment));

        // when & then
        assertThatThrownBy(() -> paymentService.cancelPayment(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("승인된 결제만 취소할 수 있습니다");
    }

    @Test
    @DisplayName("결제 조회 성공")
    void getPayment_success() {
        // given
        Payment payment = Payment.builder()
                .orderId("ORDER-001")
                .customerName("홍길동")
                .amount(10000L)
                .build();
        payment.approve("PG-ABCD1234");

        given(paymentRepository.findById(1L)).willReturn(Optional.of(payment));

        // when
        PaymentResponse response = paymentService.getPayment(1L);

        // then
        assertThat(response.getOrderId()).isEqualTo("ORDER-001");
        assertThat(response.getAmount()).isEqualTo(10000L);
    }

    @Test
    @DisplayName("결제 조회 실패 - 존재하지 않는 ID 조회 시 예외 발생")
    void getPayment_notFound_throwsException() {
        // given
        given(paymentRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> paymentService.getPayment(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("결제 정보를 찾을 수 없습니다");
    }

    // Lombok @NoArgsConstructor + private 필드 세팅용 헬퍼
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
