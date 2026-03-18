package com.payment.payment_api.dto;

import com.payment.payment_api.entity.Payment;
import com.payment.payment_api.enums.PaymentStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class PaymentResponse implements Serializable {

    private Long id;
    private String orderId;
    private String customerName;
    private Long amount;
    private PaymentStatus status;
    private String pgTransactionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PaymentResponse(Payment payment) {
        this.id = payment.getId();
        this.orderId = payment.getOrderId();
        this.customerName = payment.getCustomerName();
        this.amount = payment.getAmount();
        this.status = payment.getStatus();
        this.pgTransactionId = payment.getPgTransactionId();
        this.createdAt = payment.getCreatedAt();
        this.updatedAt = payment.getUpdatedAt();
    }
}
