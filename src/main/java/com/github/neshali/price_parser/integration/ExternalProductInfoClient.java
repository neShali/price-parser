package com.github.neshali.price_parser.integration;

import com.github.neshali.price_parser.integration.dto.ExternalProductInfoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * Клиент для обращения к внешнему сервису с использованием WebClient.
 *
 * Важно:
 * - по умолчанию вызовы отключены (enabled = false), чтобы приложение
 *   работало без любого внешнего сервиса;
 * - при включении (enabled = true) клиент делает GET-запрос вида:
 *   GET {baseUrl}/api/product-info?url={productUrl}
 */
@Service
public class ExternalProductInfoClient {

    private static final Logger log = LoggerFactory.getLogger(ExternalProductInfoClient.class);

    private final WebClient webClient;
    private final boolean enabled;
    private final long timeoutMs;

    public ExternalProductInfoClient(
            @Qualifier("externalProductInfoWebClient") WebClient webClient,
            @Value("${price-parser.external-service.enabled:false}") boolean enabled,
            @Value("${price-parser.external-service.timeout-ms:1000}") long timeoutMs
    ) {
        this.webClient = webClient;
        this.enabled = enabled;
        this.timeoutMs = timeoutMs;
    }

    public ExternalProductInfoResponse fetchAdditionalInfo(String productUrl) {
        if (!enabled) {
            log.debug("External product info service is disabled, skipping call");
            return null;
        }

        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/product-info")
                            .queryParam("url", productUrl)
                            .build())
                    .retrieve()
                    .bodyToMono(ExternalProductInfoResponse.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .block();
        } catch (Exception e) {
            log.warn("Failed to fetch external info for {}: {}", productUrl, e.getMessage());
            return null;
        }
    }
}
