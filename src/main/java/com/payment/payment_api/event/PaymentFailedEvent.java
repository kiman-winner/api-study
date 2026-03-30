package com.payment.payment_api.event;

public record PaymentFailedEvent(String orderId, String customerName, Long amount, String reason) {}