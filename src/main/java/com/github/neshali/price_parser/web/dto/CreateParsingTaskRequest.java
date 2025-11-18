package com.github.neshali.price_parser.web.dto;

/**
 * DTO для запроса на постановку нового URL в очередь парсинга.
 */
public class CreateParsingTaskRequest {

    private String url;

    public CreateParsingTaskRequest() {
    }

    public CreateParsingTaskRequest(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
