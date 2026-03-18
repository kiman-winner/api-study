package com.payment.payment_api.controller;

import com.payment.payment_api.dto.PaymentRequest;
import com.payment.payment_api.dto.PaymentResponse;
import com.payment.payment_api.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // 결제 요청
    @PostMapping
    public ResponseEntity<PaymentResponse> requestPayment(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(paymentService.requestPayment(request));
    }

    // 결제 취소
    @PostMapping("/{id}/cancel")
    public ResponseEntity<PaymentResponse> cancelPayment(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.cancelPayment(id));
    }

    // 결제 조회
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPayment(id));
    }
}
