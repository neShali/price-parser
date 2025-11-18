package com.github.neshali.price_parser.web;

import com.github.neshali.price_parser.domain.ParsingTask;
import com.github.neshali.price_parser.domain.ParsingTaskStatus;
import com.github.neshali.price_parser.domain.Product;
import com.github.neshali.price_parser.repository.ParsingTaskRepository;
import com.github.neshali.price_parser.repository.ProductRepository;
import com.github.neshali.price_parser.web.dto.CreateParsingTaskRequest;
import com.github.neshali.price_parser.web.dto.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Базовый REST-контроллер:
 * - POST /parse    — добавить новый URL в очередь парсинга
 * - GET  /products — получить список товаров с пагинацией и сортировкой
 */
@RestController
@RequestMapping // без префикса, чтобы пути были ровно /parse и /products
public class PriceParserController {

    private final ParsingTaskRepository parsingTaskRepository;
    private final ProductRepository productRepository;

    public PriceParserController(ParsingTaskRepository parsingTaskRepository,
                                 ProductRepository productRepository) {
        this.parsingTaskRepository = parsingTaskRepository;
        this.productRepository = productRepository;
    }

    /**
     * Добавить новый URL в очередь парсинга.
     *
     * Пример запроса:
     * POST /parse
     * {
     *   "url": "https://example.com/product/123"
     * }
     */
    @PostMapping("/parse")
    public ResponseEntity<ParsingTask> createParsingTask(
            @RequestBody CreateParsingTaskRequest request
    ) {
        if (request == null || request.getUrl() == null || request.getUrl().isBlank()) {
            // минимальная валидация, потом можем заменить на @Valid
            return ResponseEntity.badRequest().build();
        }

        ParsingTask task = new ParsingTask();
        task.setUrl(request.getUrl().trim());
        task.setStatus(ParsingTaskStatus.NEW);

        ParsingTask saved = parsingTaskRepository.save(task);
        return ResponseEntity.ok(saved);
    }

    /**
     * Получить список товаров с пагинацией и сортировкой.
     *
     * Примеры:
     * GET /products
     * GET /products?page=0&size=10
     * GET /products?sort=price,asc
     * GET /products?page=1&size=5&sort=publicationDate,desc
     */
    @GetMapping("/products")
    public Page<ProductResponse> getProducts(
            @PageableDefault(
                    size = 20,
                    sort = "publicationDate",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        Page<Product> page = productRepository.findAll(pageable);

        List<ProductResponse> content = page.getContent().stream()
                .map(this::toProductResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, page.getTotalElements());
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
