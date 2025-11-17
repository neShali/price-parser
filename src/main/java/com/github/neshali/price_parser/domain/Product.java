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
@Table(name = "products")
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
     * Название товара (как на странице магазина).
     */
    @Column(nullable = false)
    private String name;

    /**
     * Описание товара (может быть длинным, делаем побольше длину).
     */
    @Column(length = 4000)
    private String description;

    /**
     * Цена товара. BigDecimal используем, чтобы не терять точность.
     */
    @Column(precision = 19, scale = 2)
    private BigDecimal price;

    /**
     * Дата/время публикации товара (или последнего обновления цены).
     */
    @Column(name = "publication_date")
    private LocalDateTime publicationDate;

    /**
     * URL страницы, откуда спарсили данные.
     */
    @Column(name = "source_url", nullable = false, length = 1000)
    private String sourceUrl;
}
