package com.github.neshali.price_parser.service;

import com.github.neshali.price_parser.domain.Product;
import com.github.neshali.price_parser.repository.ProductRepository;
import com.github.neshali.price_parser.web.dto.ProductResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Сервис получения товаров с параллельной фильтрацией/сортировкой.
 */
@Service
public class ProductQueryService {

    private final ProductRepository productRepository;

    public ProductQueryService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Возвращает отфильтрованный и отсортированный список товаров.
     * ВАЖНО: здесь используется parallelStream(), чтобы показать параллельную обработку.
     */
    public List<ProductResponse> getFilteredProducts(ProductFilterCriteria criteria) {
        List<Product> allProducts = productRepository.findAll();

        Stream<Product> stream = allProducts.parallelStream(); // параллельная обработка

        // Фильтрация по подстроке в названии
        if (criteria.getQuery() != null && !criteria.getQuery().isBlank()) {
            String q = criteria.getQuery().toLowerCase(Locale.ROOT);
            stream = stream.filter(p ->
                    p.getName() != null && p.getName().toLowerCase(Locale.ROOT).contains(q)
            );
        }

        // Фильтрация по цене
        BigDecimal minPrice = criteria.getMinPrice();
        BigDecimal maxPrice = criteria.getMaxPrice();

        if (minPrice != null) {
            stream = stream.filter(p ->
                    p.getPrice() != null && p.getPrice().compareTo(minPrice) >= 0
            );
        }
        if (maxPrice != null) {
            stream = stream.filter(p ->
                    p.getPrice() != null && p.getPrice().compareTo(maxPrice) <= 0
            );
        }

        // Сортировка
        ProductSortBy sortBy = criteria.getSortBy() != null
                ? criteria.getSortBy()
                : ProductSortBy.PRICE;

        SortDirection direction = criteria.getDirection() != null
                ? criteria.getDirection()
                : SortDirection.DESC;

        Comparator<Product> comparator = buildComparator(sortBy, direction);
        stream = stream.sorted(comparator);

        int page = criteria.getPage();
        int size = criteria.getSize();

        return stream
                .skip((long) page * size)
                .limit(size)
                .map(this::toProductResponse)
                .collect(Collectors.toList());
    }

    private Comparator<Product> buildComparator(ProductSortBy sortBy, SortDirection direction) {
        Comparator<Product> comparator;

        switch (sortBy) {
            case NAME:
                comparator = Comparator.comparing(
                        p -> p.getName() == null ? "" : p.getName(),
                        String.CASE_INSENSITIVE_ORDER
                );
                break;
            case PUBLICATION_DATE:
                comparator = Comparator.comparing(
                        Product::getPublicationDate,
                        Comparator.nullsLast(Comparator.naturalOrder())
                );
                break;
            case PRICE:
            default:
                comparator = Comparator.comparing(
                        Product::getPrice,
                        Comparator.nullsLast(Comparator.naturalOrder())
                );
                break;
        }

        if (direction == SortDirection.DESC) {
            comparator = comparator.reversed();
        }
        return comparator;
    }

    private ProductResponse toProductResponse(Product product) {
        ProductResponse dto = new ProductResponse();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setPublicationDate(product.getPublicationDate());
        dto.setSourceUrl(product.getSourceUrl());
        return dto;
    }
}
