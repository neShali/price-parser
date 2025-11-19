package com.github.neshali.price_parser.web;

import com.github.neshali.price_parser.domain.ParsingTask;
import com.github.neshali.price_parser.domain.ParsingTaskStatus;
import com.github.neshali.price_parser.repository.ParsingTaskRepository;
import com.github.neshali.price_parser.repository.ProductRepository;
import com.github.neshali.price_parser.service.ProductQueryService;
import com.github.neshali.price_parser.web.dto.ProductResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PriceParserController.class)
class PriceParserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ParsingTaskRepository parsingTaskRepository;

    @MockBean
    private ProductRepository productRepository;

    @MockBean
    private ProductQueryService productQueryService;

    @Test
    void createParsingTask_shouldReturnCreatedTask() throws Exception {
        ParsingTask saved = new ParsingTask();
        saved.setId(1L);
        saved.setUrl("https://example.com/product/1");
        saved.setStatus(ParsingTaskStatus.NEW);

        when(parsingTaskRepository.save(any(ParsingTask.class))).thenReturn(saved);

        mockMvc.perform(post("/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com/product/1\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.url", is("https://example.com/product/1")))
                .andExpect(jsonPath("$.status", is("NEW")));
    }

    @Test
    void createParsingTask_shouldReturnBadRequestWhenUrlMissing() throws Exception {
        mockMvc.perform(post("/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getFilteredProducts_shouldReturnListOfProducts() throws Exception {
        ProductResponse p1 = new ProductResponse();
        p1.setId(1L);
        p1.setName("Product 1");
        p1.setDescription("Desc 1");
        p1.setPrice(new BigDecimal("10.00"));
        p1.setPublicationDate(LocalDateTime.now());
        p1.setSourceUrl("https://example.com/product/1");

        ProductResponse p2 = new ProductResponse();
        p2.setId(2L);
        p2.setName("Product 2");
        p2.setDescription("Desc 2");
        p2.setPrice(new BigDecimal("20.00"));
        p2.setPublicationDate(LocalDateTime.now());
        p2.setSourceUrl("https://example.com/product/2");

        when(productQueryService.getFilteredProducts(any()))
                .thenReturn(List.of(p1, p2));

        mockMvc.perform(get("/products/filtered")
                        .param("q", "Product")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Product 1")))
                .andExpect(jsonPath("$[1].name", is("Product 2")));
    }
}
