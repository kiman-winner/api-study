package com.payment.payment_api.dto;

import com.payment.payment_api.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@NoArgsConstructor
public class PaymentSearchCondition {
    private PaymentStatus status;
    private Long minAmount;
    private Long maxAmount;

    @Schema(example = "20260101", description = "시작 날짜 (yyyyMMdd)")
    private String startDate;

    @Schema(example = "20261231", description = "종료 날짜 (yyyyMMdd)")
    private String endDate;

    private String customerName;

    @JsonIgnore
    public LocalDateTime getStartDateTime() {
        if (startDate == null || startDate.isBlank()) return null;
        return LocalDate.parse(startDate, DateTimeFormatter.ofPattern("yyyyMMdd")).atStartOfDay();
    }

    @JsonIgnore
    public LocalDateTime getEndDateTime() {
        if (endDate == null || endDate.isBlank()) return null;
        return LocalDate.parse(endDate, DateTimeFormatter.ofPattern("yyyyMMdd"))
                        .plusDays(1).atStartOfDay();
    }
}
