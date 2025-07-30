//package com.rinhabeckend.rinha.services.payment;
//
//import com.rinhabeckend.rinha.gateway.GatewayClient;
//import jakarta.annotation.PostConstruct;
//import org.slf4j.Logger;
//import org.springframework.stereotype.Component;
//import org.springframework.web.reactive.function.client.WebClient;
//
//import java.time.Duration;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.Executors;
//
//@Component
//public class HealthMonitor {
//
//    Logger log = org.slf4j.LoggerFactory.getLogger(HealthMonitor.class);
//    private final Map<String, Boolean> status = new ConcurrentHashMap<>();
//    private final WebClient client1 = WebClient.create("http://127.0.0.1:8001");
//    private final WebClient client2 = WebClient.create("http://127.0.0.1:8002");
//
//    @PostConstruct
//    public void init() {
//        schedule("G1", client1);
//        schedule("G2", client2);
//    }
//
////    private void schedule(String name, WebClient client) {
////        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
////            client.get()
////                    .uri("/payments/service-health")
////                    .retrieve()
////                    .toBodilessEntity()
////                    .timeout(Duration.ofMillis(300))
////                    .doOnError(ex -> log.warn("[{}] Falha healthcheck: {}", name, ex.getMessage()))
////                    .subscribe(
////                            response -> status.put(name, response.getStatusCode().is2xxSuccessful()),
////                            ex -> status.put(name, false)
////                    );
////        }, 0, 1, java.util.concurrent.);
////    }
//
//    public boolean isHealthy(String gateway) {
//        return status.getOrDefault(gateway, false);
//    }
//
//}
