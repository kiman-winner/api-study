package com.payment.payment_api.entity;

import com.payment.payment_api.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderId;       // 중복 결제 방지용 unique key

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    private String pgTransactionId;  // Mock PG사 트랜잭션 ID

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Builder
    public Payment(String orderId, String customerName, Long amount) {
        this.orderId = orderId;
        this.customerName = customerName;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void approve(String pgTransactionId) {
        this.status = PaymentStatus.APPROVED;
        this.pgTransactionId = pgTransactionId;
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = PaymentStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    public void fail() {
        this.status = PaymentStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }
}
