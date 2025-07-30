package com.rinhabeckend.rinha.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rinhabeckend.rinha.domain.PaymentRequest;
import com.rinhabeckend.rinha.domain.PaymentResponse;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Component
public class GatewayClient {

    public static final ObjectMapper mapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(MapperFeature.REQUIRE_HANDLERS_FOR_JAVA8_TIMES)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();
    Logger logger = org.slf4j.LoggerFactory.getLogger(GatewayClient.class);
    private final WebClient client1;
    private final WebClient client2;

    // Injetando os WebClients configurados como Beans
    public GatewayClient(@Qualifier("client1") WebClient client1, @Qualifier("client2") WebClient client2) {
        this.client1 = client1;
        this.client2 = client2;
    }

    public Flux<PaymentResponse> sendToBestGatewayFlux(PaymentRequest req) {
        var reqWithCorrelationId = new GatewayRequest(
                UUID.randomUUID().toString().toLowerCase(),
                req.amount().doubleValue(),
                Instant.now()
        );

        // Lista dos gateways disponíveis para processamento
        List<GatewayProcessor> gateways = List.of(
                new GatewayProcessor(this::checkHealthG1, this::callGatewayG1),
                new GatewayProcessor(this::checkHealthG2, this::callGatewayG2)
        );

        return Flux.fromIterable(gateways)
                // Para cada gateway, verifica a saúde
                .flatMap(gateway -> gateway.healthChecker().get()
                        // Se estiver saudável, continua o fluxo com o gateway
                        .filter(isHealthy -> isHealthy)
                        .map(isHealthy -> gateway)
                )
                // Para os gateways saudáveis, faz a chamada de pagamento
                .flatMap(gateway -> gateway.paymentCaller().apply(reqWithCorrelationId))
                // Filtra apenas as respostas com sucesso
                .filter(PaymentResponse::success);
    }

    public Mono<PaymentResponse> sendToBestGateway(PaymentRequest req) {
        var reqWithCorrelationId = new GatewayRequest(
                UUID.randomUUID().toString().toLowerCase(),
                req.amount().doubleValue(),
                Instant.now()
        );

        // Lista dos gateways disponíveis para processamento
        List<GatewayProcessor> gateways = List.of(
                new GatewayProcessor(this::checkHealthG1, this::callGatewayG1),
                new GatewayProcessor(this::checkHealthG2, this::callGatewayG2)
        );

        return Flux.fromIterable(gateways)
                // Para cada gateway, verifica a saúde
                .flatMap(gateway -> gateway.healthChecker().get()
                        // Se estiver saudável, continua o fluxo com o gateway
                        .filter(isHealthy -> isHealthy)
                        .map(isHealthy -> gateway)
                )
                // Para os gateways saudáveis, faz a chamada de pagamento
                .flatMap(gateway -> gateway.paymentCaller().apply(reqWithCorrelationId))
                // Filtra apenas as respostas com sucesso
                .filter(PaymentResponse::success)
                // Usando collectSortedList como uma alternativa robusta ao .min()
                // Isso coleta todas as respostas bem-sucedidas, ordena pela menor taxa,
                // e então pegamos o primeiro elemento.
                .collectSortedList(Comparator.comparing(PaymentResponse::fee))
                .flatMap(sortedList -> {
                    // Se a lista estiver vazia (nenhum sucesso), emite um Mono vazio
                    // para acionar o defaultIfEmpty abaixo.
                    if (sortedList.isEmpty()) {
                        return Mono.empty();
                    }
                    // Caso contrário, retorna o primeiro elemento, que é o de menor taxa.
                    return Mono.just(sortedList.get(0));
                })
                // Se nenhum gateway respondeu com sucesso, retorna uma resposta de falha padrão
                .defaultIfEmpty(new PaymentResponse(false, BigDecimal.ZERO, "Nenhum gateway disponível ou todos falharam."));
    }

//    public PaymentResponse  sendToBestGateway(PaymentRequest req) {
//
//        Mono<Boolean> health1 = checkHealth(client1, "G1");
//        Mono<Boolean> health2 = checkHealth(client2, "G2");
//
//        var reqWithCorrelationId = new GatewayRequest(
//                UUID.randomUUID().toString().toLowerCase(),
//                req.amount().doubleValue(),
//                Instant.now()
//        );
//
//        return Mono.zip(health1, health2)
//                .flatMap(tuple -> {
//                    List<Mono<PaymentResponse>> calls = new ArrayList<>();
//                    if (tuple.getT1()) calls.add(Mono.fromFuture(callGateway(client1, reqWithCorrelationId, "G1")));
//                    if (tuple.getT2()) calls.add(Mono.fromFuture(callGateway(client2, reqWithCorrelationId, "G2")));
//
//                    if (calls.isEmpty()) {
//                        return Mono.just(new PaymentResponse(false, BigDecimal.ZERO, "Nenhum gateway disponível"));
//                    }
//
//                    return Mono.zip(calls, results -> Arrays.stream(results)
//                            .map(r -> (PaymentResponse) r)
//                            .filter(PaymentResponse::success)
//                            .min(Comparator.comparing(PaymentResponse::fee))
//                            .orElse(new PaymentResponse(false, BigDecimal.ZERO, "Nenhum gateway válido"))
//                    );
//                })
//                .block();
//
//    }

//    private CompletableFuture<PaymentResponse> callGateway(WebClient client, GatewayRequest req, String name) {
//        String requestMapper = "";
//        try {
//            requestMapper = mapper.writeValueAsString(req);
//            logger.info("[{}] Requisição JSON: {}", name, requestMapper);
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException(e);
//        }
//        return client.post()
//                .uri("/payments")
//                .header("Content-Type", "application/json")
//                .bodyValue(requestMapper)
//                .exchangeToMono(response -> {
//                    if (response.statusCode().is2xxSuccessful()) {
//
//                        return response.bodyToMono(PaymentResponse.class)
//                                .doOnNext(res -> logger.info("[{}] Sucesso: {}", name, res));
//                    } else {
//                        logger.info("[{}] Erro HTTP {}: {}", name, response.statusCode(), response.bodyToMono(String.class));
//                        return response.bodyToMono(String.class)
//                                .doOnNext(errorBody -> logger.info("[{}] Erro HTTP {}: {}", name, response.statusCode(), errorBody))
//                                .thenReturn(new PaymentResponse(false, BigDecimal.ZERO, name));
//                    }
//                })
//                .timeout(Duration.ofMillis(300))
//                .onErrorResume(ex -> {
//                    logger.error("[{}] Erro de comunicação: {}", name, ex.getMessage());
//                    return Mono.just(new PaymentResponse(false, BigDecimal.ZERO, name));
//                })
//                .toFuture();
//    }
    private Mono<Boolean> checkHealth(WebClient client, String name) {
        return client.get()
                .uri("/payments/service-health")
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(5))
                .map(resp -> {
                    logger.info("[{}] Healthcheck OK", name);
                    return resp.getStatusCode().is2xxSuccessful();
                })
                .onErrorResume(ex -> {
                    logger.warn("[{}] Falha no healthcheck: {}", name, ex.getMessage());
                    return Mono.just(false);
                });
    }

    private Mono<PaymentResponse> callGateway(WebClient client, GatewayRequest req, String name) {
        String requestMapper;
        try {
            requestMapper = mapper.writeValueAsString(req);
            logger.info("[{}] Requisição JSON: {}", name, requestMapper);
        } catch (JsonProcessingException e) {
            return Mono.error(new RuntimeException(e));
        }

        return client.post()
                .uri("/payments")
                .header("Content-Type", "application/json")
                .bodyValue(requestMapper)
                .retrieve()
                .bodyToMono(PaymentResponse.class)
                .timeout(Duration.ofMillis(3000)) // Timeout da chamada de pagamento
                .doOnSuccess(res -> logger.info("[{}] Sucesso: {}", name, res))
                .onErrorResume(ex -> {
                    logger.error("[{}] Erro na chamada de pagamento: {}", name, ex.getMessage());
                    // Retorna uma resposta de falha específica para este gateway
                    return Mono.just(new PaymentResponse(false, BigDecimal.ZERO, "Falha no gateway " + name));
                });
    }

    private Mono<PaymentResponse> callGatewayG1(GatewayRequest req) {
        return callGateway(client1, req, "G1");
    }

    /**
     * Chama o Gateway 2.
     */
    private Mono<PaymentResponse> callGatewayG2(GatewayRequest req) {
        return callGateway(client2, req, "G2");
    }
    public Mono<Boolean> checkHealthG1() {
        logger.info("[G1] EXECUTANDO healthcheck (sem cache)...");
        return checkHealth(client1, "G1");
    }

    public Mono<Boolean> checkHealthG2() {
        logger.info("[G2] EXECUTANDO healthcheck (sem cache)...");
        return checkHealth(client2, "G2");
    }

    public Mono<Boolean> healthCheckFallback(Throwable ex) {
        logger.warn("Circuit Breaker fallback ativado. Causa: {}", ex.getMessage());
        return Mono.just(false);
    }

    // --- Records para organizar a lógica ---
    private record GatewayProcessor(java.util.function.Supplier<Mono<Boolean>> healthChecker, java.util.function.Function<GatewayRequest, Mono<PaymentResponse>> paymentCaller) {}
//    private record GatewayRequest(String correlationId, Double amount, Instant timestamp) {}

}
