package com.rinhabeckend.rinha.domain;

import java.math.BigDecimal;

public record PaymentRequest(String correlationId, BigDecimal amount) {

    public PaymentRequest {
        if (correlationId == null || correlationId.isBlank()) {
            throw new IllegalArgumentException("Correlation ID cannot be null or blank");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

    }

}
