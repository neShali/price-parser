package com.github.neshali.price_parser.service;

import com.github.neshali.price_parser.domain.Product;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.Random;

@Service
public class PriceParsingService {

    private final Random random = new Random();

    /**
     * "Парсит" товар по URL и возвращает заполненный объект Product.
     * Объект НЕ сохраняется в БД — этим займётся отдельный сервис,
     * который будет обрабатывать ParsingTask.
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

        return product;
    }

    /**
     * Примитивное извлечение "имени товара" из URL.
     * Например:
     *   https://example.com/product/super-phone-3000  -> "Super phone 3000"
     */
    private String extractNameFromUrl(String url) {
        try {
            URI uri = new URI(url);
            String path = uri.getPath(); // например, "/product/999" или "/product/super-phone-3000"

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

            // Первая буква заглавная, остальное как есть
            return normalized.substring(0, 1).toUpperCase() + normalized.substring(1);
        } catch (URISyntaxException e) {
            return "Product from URL";
        }
    }
}
