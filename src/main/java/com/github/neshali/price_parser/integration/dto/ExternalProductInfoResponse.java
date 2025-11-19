package com.github.neshali.price_parser.integration.dto;


public class ExternalProductInfoResponse {

    private String category;
    private String currency;
    private Double rating;

    public ExternalProductInfoResponse() {
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }
}
