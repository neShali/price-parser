package com.github.neshali.price_parser.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Конфигурация WebClient для обращения к внешнему сервису с информацией о товарах.
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient externalProductInfoWebClient(
            @Value("${price-parser.external-service.base-url:http://localhost:8081}") String baseUrl
    ) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
