package com.payment.payment_api.service;

import com.payment.payment_api.dto.PaymentPageResponse;
import com.payment.payment_api.dto.PaymentRequest;
import com.payment.payment_api.dto.PaymentResponse;
import com.payment.payment_api.entity.Payment;
import com.payment.payment_api.enums.PaymentStatus;
import com.payment.payment_api.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    // 결제 요청
    @Transactional
    public PaymentResponse requestPayment(PaymentRequest request) {
        if (paymentRepository.existsByOrderId(request.getOrderId())) {
            throw new IllegalStateException("이미 처리된 주문입니다: " + request.getOrderId());
        }

        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .customerName(request.getCustomerName())
                .amount(request.getAmount())
                .build();

        try {
            String pgTransactionId = mockPgApprove(payment);
            payment.approve(pgTransactionId);
        } catch (Exception e) {
            payment.fail();
            paymentRepository.save(payment);
            throw new RuntimeException("PG사 승인 실패: " + e.getMessage());
        }

        return new PaymentResponse(paymentRepository.save(payment));
    }

    // 결제 취소
    @CacheEvict(value = "payment", key = "#id")
    @Transactional
    public PaymentResponse cancelPayment(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다: " + id));

        if (payment.getStatus() != PaymentStatus.APPROVED) {
            throw new IllegalStateException("승인된 결제만 취소할 수 있습니다.");
        }

        payment.cancel();
        return new PaymentResponse(payment);
    }

    // 결제 단건 조회
    @Cacheable(value = "payment", key = "#id")
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다: " + id));
        return new PaymentResponse(payment);
    }

    // 결제 목록 조회 (페이징)
    @Transactional(readOnly = true)
    public PaymentPageResponse getPayments(PaymentStatus status, Pageable pageable) {
        if (status != null) {
            return PaymentPageResponse.of(
                    paymentRepository.findByStatus(status, pageable)
                            .map(PaymentResponse::new)
            );
        }
        return PaymentPageResponse.of(
                paymentRepository.findAll(pageable)
                        .map(PaymentResponse::new)
        );
    }

    // Mock PG사 승인 (실제 PG 연동 대신 UUID 발급)
    private String mockPgApprove(Payment payment) {
        return "PG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
