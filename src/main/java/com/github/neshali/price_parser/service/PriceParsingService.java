package com.github.neshali.price_parser.service;

import com.github.neshali.price_parser.domain.Product;
import com.github.neshali.price_parser.integration.ExternalProductInfoClient;
import com.github.neshali.price_parser.integration.dto.ExternalProductInfoResponse;
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
    private final Random random = new Random();

    public PriceParsingService(ExternalProductInfoClient externalProductInfoClient) {
        this.externalProductInfoClient = externalProductInfoClient;
    }

    /**
     * "Парсит" товар по URL и возвращает заполненный объект Product.
     * Объект НЕ сохраняется в БД — этим занимается ParsingTaskProcessingService.
     */
    public Product parseProduct(String url) {
        Product product = new Product();

        product.setSourceUrl(url);
        product.setName(extractNameFromUrl(url));
        product.setDescription("Demo product parsed from " + url);

        BigDecimal price = BigDecimal
                .valueOf(10 + random.nextInt(90)) // цена от 10 до 99
                .setScale(2, RoundingMode.HALF_UP);
        product.setPrice(price);

        product.setPublicationDate(LocalDateTime.now());

        // Попробуем обогатить данные через внешний сервис
        ExternalProductInfoResponse externalInfo = externalProductInfoClient.fetchAdditionalInfo(url);
        if (externalInfo != null) {
            String extra = String.format(" [external category=%s, rating=%s, currency=%s]",
                    externalInfo.getCategory(),
                    externalInfo.getRating(),
                    externalInfo.getCurrency());
            product.setDescription(product.getDescription() + extra);
            log.debug("Enriched product {} with external info: {}", url, extra);
        }

        return product;
    }

    /**
     * Примитивное извлечение "имени товара" из URL.
     * Например:
     *   https://example.com/product/super-phone-3000 -> "Super phone 3000"
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
