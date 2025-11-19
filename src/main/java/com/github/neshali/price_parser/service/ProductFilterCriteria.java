package com.github.neshali.price_parser.service;

import java.math.BigDecimal;

/**
 * Критерии фильтрации/сортировки товаров.
 */
public class ProductFilterCriteria {

    private String query;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private ProductSortBy sortBy;
    private SortDirection direction;
    private int page;
    private int size;

    public ProductFilterCriteria() {
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    public ProductSortBy getSortBy() {
        return sortBy;
    }

    public void setSortBy(ProductSortBy sortBy) {
        this.sortBy = sortBy;
    }

    public SortDirection getDirection() {
        return direction;
    }

    public void setDirection(SortDirection direction) {
        this.direction = direction;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = Math.max(page, 0);
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size <= 0 ? 20 : size;
    }
}
