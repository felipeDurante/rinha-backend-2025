package com.rinhabeckend.rinha.services.payment;
import java.time.Duration;

class PaymentProperties {

    private int memoryBufferSize = 100000;
    private final Duration retryDelay = Duration.ofSeconds(1);
//    private final int maxAttempts = 3;
    public int getMemoryBufferSize() {
        return memoryBufferSize;
    }

    public void setMemoryBufferSize(int memoryBufferSize) {
        this.memoryBufferSize = memoryBufferSize;
    }

    public Duration getRetryDelay() {
        return retryDelay;
    }


}
