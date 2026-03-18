package com.payment.payment_api.enums;

public enum PaymentStatus {
    PENDING,    // 결제 요청
    APPROVED,   // 결제 승인
    CANCELLED,  // 결제 취소
    FAILED      // 결제 실패
}
