package com.github.neshali.price_parser.service;

import com.github.neshali.price_parser.domain.Product;
import com.github.neshali.price_parser.integration.ExternalProductInfoClient;
import com.github.neshali.price_parser.integration.dto.ExternalProductInfoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Юнит-тесты для сервиса парсинга цен.
 */
@ExtendWith(MockitoExtension.class)
class PriceParsingServiceTest {

    @Mock
    private ExternalProductInfoClient externalProductInfoClient;

    @InjectMocks
    private PriceParsingService priceParsingService;

    private final String url = "https://example.com/product/super-phone-3000";

    @BeforeEach
    void setUp() {
        // По умолчанию внешний сервис ничего не возвращает
        when(externalProductInfoClient.fetchAdditionalInfo(anyString()))
                .thenReturn(null);
    }

    @Test
    void parseProduct_shouldFillBasicFields() {
        Product product = priceParsingService.parseProduct(url);

        assertThat(product).isNotNull();
        assertThat(product.getSourceUrl()).isEqualTo(url);
        assertThat(product.getName()).isEqualTo("Super phone 3000");
        assertThat(product.getDescription()).contains("Demo product parsed from");

        BigDecimal price = product.getPrice();
        assertThat(price).isNotNull();
        assertThat(price).isBetween(BigDecimal.TEN, new BigDecimal("100"));
    }

    @Test
    void parseProduct_shouldEnrichDescriptionWithExternalInfo_whenAvailable() {
        ExternalProductInfoResponse external = new ExternalProductInfoResponse();
        external.setCategory("electronics");
        external.setCurrency("USD");
        external.setRating(4.5);

        when(externalProductInfoClient.fetchAdditionalInfo(url))
                .thenReturn(external);

        Product product = priceParsingService.parseProduct(url);

        assertThat(product.getDescription())
                .contains("external category=electronics")
                .contains("rating=4.5")
                .contains("currency=USD");
    }
}
