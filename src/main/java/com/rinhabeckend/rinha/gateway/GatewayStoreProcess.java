package com.rinhabeckend.rinha.gateway;

import java.time.Instant;

public record GatewayStoreProcess(String correlationId, double amount, Instant requestedAt, String gateway) {
    public GatewayStoreProcess {
        if (correlationId == null || correlationId.isBlank()) {
            throw new IllegalArgumentException("Correlation ID cannot be null or blank");
        }
        if (amount < 0 || amount == 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        if (requestedAt == null) {
            throw new IllegalArgumentException("Requested at cannot be null");
        }
        if (gateway == null || gateway.isBlank()) {
            throw new IllegalArgumentException("Gateway cannot be null or blank");
        }
    }
}
