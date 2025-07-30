package com.rinhabeckend.rinha.services.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rinhabeckend.rinha.gateway.GatewayRequest;
import com.rinhabeckend.rinha.gateway.GatewayStoreProcess;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Component
public class PaymentProcessor {

    public static final ObjectMapper mapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(MapperFeature.REQUIRE_HANDLERS_FOR_JAVA8_TIMES)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();
    final Logger log = org.slf4j.LoggerFactory.getLogger(PaymentProcessor.class);
    private final WebClient client1;
    private final WebClient client2;

    private final Retry backoffStrategy = Retry.backoff(5, Duration.ofMillis(200))
            .maxBackoff(Duration.ofSeconds(10))
            .jitter(0.5);

    public PaymentProcessor(@Qualifier("client1") WebClient client1, @Qualifier("client2") WebClient client2) {
        this.client1 = client1;
        this.client2 = client2;
    }

    public Mono<GatewayStoreProcess> process(GatewayRequest req) {



        return tryGateway(req, client1, PaymentQueue.G1, 5)
                .switchIfEmpty(tryGateway(req, client2, PaymentQueue.G2, 2));
//        return tryGateway(req, client1, PaymentQueue.G1, 4)
//                .onErrorResume(ex -> {
//                    log.error("[G1] Erro ao processar requisição: {}", ex.getMessage());
//                    return tryGateway(req, client2, PaymentQueue.G2, 2)
//                            .onErrorResume(ex2 -> {
//                                log.error("[G2] Erro ao processar requisição: {}", ex2.getMessage());
//                                return Mono.empty(); // falha total
//                            });
//                });
    }

    private Mono<GatewayStoreProcess> tryGateway(GatewayRequest req, WebClient gateway, String name, int attempt) {
        String requestMapper;
        try {
            requestMapper = mapper.writeValueAsString(req);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return gateway.post()
                .uri("/payments")
                .header("Content-Type", "application/json")
                .bodyValue(requestMapper)
                .retrieve()
                .toEntity(String.class)
//                .timeout(Duration.ofMillis(500))
                .retryWhen(backoffStrategy)
                .flatMap(response -> {
                    int status = response.getStatusCode().value();
                    if (status >= 200 && status < 300) {
                        return Mono.just(new GatewayStoreProcess(req.correlationId(), req.amount(), req.requestedAt(), name)); // sucesso real
                    } else if (status == 422) {
                        log.warn("Requisição duplicada detectada: {}", req.correlationId());
                        return Mono.just(new GatewayStoreProcess(req.correlationId(), req.amount(), req.requestedAt(), name)); // idempotente: consideramos processado!
                    } else {
                        return Mono.empty(); // falha lógica
                    }
                });


    }
}
