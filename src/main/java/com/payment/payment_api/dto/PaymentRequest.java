package com.payment.payment_api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PaymentRequest {

    @NotBlank(message = "주문 ID는 필수입니다.")
    private String orderId;

    @NotBlank(message = "고객명은 필수입니다.")
    private String customerName;

    @NotNull(message = "결제 금액은 필수입니다.")
    @Min(value = 100, message = "결제 금액은 100원 이상이어야 합니다.")
    private Long amount;
}
