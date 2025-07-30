package com.rinhabeckend.rinha.services.payment;

import com.rinhabeckend.rinha.gateway.GatewayRequest;
import com.rinhabeckend.rinha.gateway.GatewayStoreProcess;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Component
public class PaymentQueue {

    public static final String G1 = "G1";
    public static final String  G2 = "G2";
    Logger logger = org.slf4j.LoggerFactory.getLogger(PaymentQueue.class);

    private final Sinks.Many<GatewayRequest> sink = Sinks.many().unicast().onBackpressureBuffer();
    private final List<GatewayStoreProcess> processedRequests;

    private final  PaymentProperties props = new PaymentProperties();

    public PaymentQueue(PaymentProcessor processor) {
        this.processedRequests = Collections.synchronizedList(new LinkedList<>());

        sink.asFlux()
                .flatMap(req ->
                                processor.process(req)
                                        .doOnSuccess(processed -> store(processed, props.getMemoryBufferSize()))
                                        .onErrorResume(ex -> {
                                            logger.warn("Erro ao processar req {}: {}. Reenfileirando...", req, ex.getMessage());
                                            return Mono.delay(Duration.ofMillis(500))
                                                    .doOnNext(t -> enqueue(req))
                                                    .then(Mono.empty());
                                        }),
                        2 // nível de concorrência
                )
                .subscribe();
    }

    public void enqueue(GatewayRequest req) {
        sink.tryEmitNext(req);
    }

    private void store(GatewayStoreProcess req, int maxSize) {
//        logger.info("Armazenando requisição processada: {}", req);
//        logger.info("Tamanho atual da lista: {}", processedRequests.size());
        synchronized (processedRequests) {
            if (processedRequests.size() >= maxSize) {
                processedRequests.remove(0);
            }
            processedRequests.add(req);
        }
    }

    public List<GatewayStoreProcess> getProcessedRequests() {
        return Collections.unmodifiableList(processedRequests);
    }
}
