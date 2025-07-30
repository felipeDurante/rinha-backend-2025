package com.rinhabeckend.rinha;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
@EnableCaching
public class RinhaApplication {

	public static void main(String[] args) {
		SpringApplication.run(RinhaApplication.class, args);
	}

	@Bean
	public WebClient client1() {
		return WebClient.create("http://127.0.0.1:8001");
	}

	// Cria um bean para o WebClient do Gateway 2
	@Bean
	public WebClient client2() {
		return WebClient.create("http://127.0.0.1:8002");
	}

}
