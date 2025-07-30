package com.rinhabeckend.rinha.services.payment;

import com.rinhabeckend.rinha.domain.PaymentRequest;
import com.rinhabeckend.rinha.domain.PaymentResponse;
import com.rinhabeckend.rinha.gateway.GatewayClient;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private final GatewayClient gatewayClient;

    public PaymentService(GatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    public PaymentResponse process(PaymentRequest req) {
        return gatewayClient.sendToBestGateway(req).block();
    }

}
