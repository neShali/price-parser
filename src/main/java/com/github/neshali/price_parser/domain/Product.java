package com.github.neshali.price_parser.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_product_publication_date", columnList = "publication_date"),
        @Index(name = "idx_product_price", columnList = "price"),
        @Index(name = "idx_product_source_url", columnList = "source_url")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Название товара.
     */
    @Column(nullable = false)
    private String name;

    /**
     * Описание товара.
     */
    @Column(length = 4000)
    private String description;

    /**
     * Цена товара.
     */
    @Column(precision = 19, scale = 2)
    private BigDecimal price;

    /**
     * Дата/время публикации товара.
     */
    @Column(name = "publication_date")
    private LocalDateTime publicationDate;

    /**
     * URL страницы, откуда спарсили данные.
     */
    @Column(name = "source_url", nullable = false, length = 1000)
    private String sourceUrl;
}
