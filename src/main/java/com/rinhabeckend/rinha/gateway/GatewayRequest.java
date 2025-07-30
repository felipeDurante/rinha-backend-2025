package com.rinhabeckend.rinha.gateway;

import java.math.BigDecimal;
import java.time.Instant;

public record GatewayRequest(String correlationId, double amount, Instant requestedAt) {
}
