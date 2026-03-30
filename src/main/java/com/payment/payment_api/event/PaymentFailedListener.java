package com.payment.payment_api.event;

import com.payment.payment_api.entity.Payment;
import com.payment.payment_api.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PaymentFailedListener {

    private final PaymentRepository paymentRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentFailed(PaymentFailedEvent event) {
        Payment failPayment = Payment.builder()
                .orderId(event.orderId())
                .customerName(event.customerName())
                .amount(event.amount())
                .build();
        failPayment.fail(event.reason());
        paymentRepository.save(failPayment);
    }
}