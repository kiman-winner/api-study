package com.payment.payment_api.client;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PgClient {
    public String approve(String orderId, Long amount) {
        // 실제 API 호출 로직 (생략)
        return "PG-" + UUID.randomUUID().toString().substring(0, 8);
    }
}