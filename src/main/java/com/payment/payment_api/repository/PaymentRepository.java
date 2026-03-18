package com.payment.payment_api.repository;

import com.payment.payment_api.entity.Payment;
import com.payment.payment_api.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(String orderId);
    boolean existsByOrderId(String orderId);
    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);
}
