package com.rinhabeckend.rinha.domain;

import java.math.BigDecimal;

public record PaymentResponse(boolean success, BigDecimal fee, String gateway) {
}
