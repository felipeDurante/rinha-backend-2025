package com.rinhabeckend.rinha.controllers.payment;
import com.rinhabeckend.rinha.domain.PaymentRequest;
import com.rinhabeckend.rinha.gateway.GatewayRequest;
import com.rinhabeckend.rinha.gateway.GatewayStoreProcess;
import com.rinhabeckend.rinha.services.payment.PaymentQueue;
import com.rinhabeckend.rinha.services.payment.SummaryData;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@RestController
public class PaymentController {


    public PaymentController( PaymentQueue paymentQueue) {
        this.paymentQueue = paymentQueue;
    }

    private final PaymentQueue paymentQueue;

    @PostMapping("/payments")
    public Mono<ResponseEntity<String>> pagar(@RequestBody PaymentRequest req) {

        var gatewayRequest = new GatewayRequest(
                req.correlationId(),
                req.amount().doubleValue(),
                Instant.now()
        );
        return Mono.fromRunnable(() -> paymentQueue.enqueue(gatewayRequest))
                .thenReturn(ResponseEntity.accepted().body("Enfileirado"));
    }

    @GetMapping("/payments-summary")
    public ResponseEntity<PaymentsSummaryResponse> getPaymentsSummary(
            @RequestParam Instant from,
            @RequestParam Instant to) {

       var requests = paymentQueue.getProcessedRequests().stream()
                .filter(r -> !r.requestedAt().isBefore(from) && !r.requestedAt().isAfter(to))
                .toList();

        SummaryData defaultSummary = summarize(requests, "G1");
        SummaryData fallbackSummary = summarize(requests, "G2");

        PaymentsSummaryResponse response = new PaymentsSummaryResponse(defaultSummary, fallbackSummary);
        return ResponseEntity.ok(response);
    }
    private SummaryData summarize(List<GatewayStoreProcess> requests, String gateway) {
        long total = 0;
        double amount = 0.0;

        for (GatewayStoreProcess r : requests) {
            if (gateway.equalsIgnoreCase(r.gateway())) {
                total++;
                amount += r.amount();
            }
        }

        return new SummaryData(total, amount);
    }
}
