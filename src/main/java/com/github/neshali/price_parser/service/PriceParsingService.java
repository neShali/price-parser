package com.github.neshali.price_parser.service;

import com.github.neshali.price_parser.domain.Product;
import com.github.neshali.price_parser.integration.ExternalProductInfoClient;
import com.github.neshali.price_parser.integration.dto.ExternalProductInfoResponse;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.Random;

@Service
public class PriceParsingService {

    private static final Logger log = LoggerFactory.getLogger(PriceParsingService.class);

    private final ExternalProductInfoClient externalProductInfoClient;
    private final Tracer tracer;
    private final Random random = new Random();

    public PriceParsingService(ExternalProductInfoClient externalProductInfoClient, Tracer tracer) {
        this.externalProductInfoClient = externalProductInfoClient;
        this.tracer = tracer;
    }

    /**
     * "Парсит" товар по URL и возвращает заполненный объект Product.
     * Использует OpenTelemetry трейсинг для отслеживания этапов парсинга.
     */
    public Product parseProduct(String url) {
        Span span = tracer.nextSpan().name("parse.product").start();
        try (Tracer.SpanInScope scope = tracer.withSpan(span)) {
            span.tag("product.url", url);
            log.debug("Начало парсинга продукта: {}", url);

            Product product = new Product();

            // Этап 1: Извлечение базовой информации
            Span extractSpan = tracer.nextSpan().name("extract.basic.info").start();
            try (Tracer.SpanInScope extractScope = tracer.withSpan(extractSpan)) {
                product.setSourceUrl(url);
                product.setName(extractNameFromUrl(url));
                product.setDescription("Demo product parsed from " + url);

                BigDecimal price = BigDecimal
                        .valueOf(10 + random.nextInt(90)) // цена от 10 до 99
                        .setScale(2, RoundingMode.HALF_UP);
                product.setPrice(price);
                product.setPublicationDate(LocalDateTime.now());

                extractSpan.tag("product.price", price.toString());
                extractSpan.tag("product.name", product.getName());
            } finally {
                extractSpan.end();
            }

            // Этап 2: Обогащение данных через внешний сервис
            Span enrichSpan = tracer.nextSpan().name("enrich.external.info").start();
            try (Tracer.SpanInScope enrichScope = tracer.withSpan(enrichSpan)) {
                ExternalProductInfoResponse externalInfo = externalProductInfoClient.fetchAdditionalInfo(url);
                if (externalInfo != null) {
                    String extra = String.format(" [external category=%s, rating=%s, currency=%s]",
                            externalInfo.getCategory(),
                            externalInfo.getRating(),
                            externalInfo.getCurrency());
                    product.setDescription(product.getDescription() + extra);
                    log.debug("Обогащен продукт {} внешней информацией: {}", url, extra);
                    enrichSpan.tag("external.enriched", "true");
                    enrichSpan.tag("external.category",
                            externalInfo.getCategory() != null ? externalInfo.getCategory() : "unknown");
                } else {
                    enrichSpan.tag("external.enriched", "false");
                }
            } finally {
                enrichSpan.end();
            }

            log.debug("Парсинг продукта завершен: {}", url);
            return product;
        } finally {
            span.end();
        }
    }

    /**
     * Извлечение "имени товара" из URL.
     */
    private String extractNameFromUrl(String url) {
        try {
            URI uri = new URI(url);
            String path = uri.getPath();

            if (path == null || path.isBlank()) {
                return "Product from " + uri.getHost();
            }

            String[] segments = path.split("/");
            String lastSegment = segments[segments.length - 1];

            if (lastSegment.isBlank()) {
                return "Product from " + uri.getHost();
            }

            String normalized = lastSegment
                    .replace('-', ' ')
                    .replace('_', ' ');

            return normalized.substring(0, 1).toUpperCase() + normalized.substring(1);
        } catch (URISyntaxException e) {
            return "Product from URL";
        }
    }
}
