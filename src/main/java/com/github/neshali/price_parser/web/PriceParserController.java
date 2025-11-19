package com.github.neshali.price_parser.web;

import com.github.neshali.price_parser.domain.ParsingTask;
import com.github.neshali.price_parser.domain.ParsingTaskStatus;
import com.github.neshali.price_parser.domain.Product;
import com.github.neshali.price_parser.repository.ParsingTaskRepository;
import com.github.neshali.price_parser.repository.ProductRepository;
import com.github.neshali.price_parser.service.ProductFilterCriteria;
import com.github.neshali.price_parser.service.ProductQueryService;
import com.github.neshali.price_parser.service.ProductSortBy;
import com.github.neshali.price_parser.service.SortDirection;
import com.github.neshali.price_parser.web.dto.CreateParsingTaskRequest;
import com.github.neshali.price_parser.web.dto.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping
public class PriceParserController {

    private final ParsingTaskRepository parsingTaskRepository;
    private final ProductRepository productRepository;
    private final ProductQueryService productQueryService;

    public PriceParserController(ParsingTaskRepository parsingTaskRepository,
                                 ProductRepository productRepository,
                                 ProductQueryService productQueryService) {
        this.parsingTaskRepository = parsingTaskRepository;
        this.productRepository = productRepository;
        this.productQueryService = productQueryService;
    }

    @PostMapping("/parse")
    public ResponseEntity<ParsingTask> createParsingTask(
            @RequestBody CreateParsingTaskRequest request
    ) {
        if (request == null || request.getUrl() == null || request.getUrl().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        ParsingTask task = new ParsingTask();
        task.setUrl(request.getUrl().trim());
        task.setStatus(ParsingTaskStatus.NEW);

        ParsingTask saved = parsingTaskRepository.save(task);
        return ResponseEntity.ok(saved);
    }

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

    /**
     * Параллельная фильтрация и сортировка товаров.
     *
     * Примеры:
     * GET /products/filtered?minPrice=50&maxPrice=90
     * GET /products/filtered?q=101&sortBy=PRICE&direction=DESC
     * GET /products/filtered?page=1&size=5&sortBy=PUBLICATION_DATE&direction=ASC
     */
    @GetMapping("/products/filtered")
    public List<ProductResponse> getFilteredProducts(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(name = "sortBy", defaultValue = "PRICE") ProductSortBy sortBy,
            @RequestParam(name = "direction", defaultValue = "DESC") SortDirection direction,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        ProductFilterCriteria criteria = new ProductFilterCriteria();
        criteria.setQuery(query);
        criteria.setMinPrice(minPrice);
        criteria.setMaxPrice(maxPrice);
        criteria.setSortBy(sortBy);
        criteria.setDirection(direction);
        criteria.setPage(page);
        criteria.setSize(size);

        return productQueryService.getFilteredProducts(criteria);
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
