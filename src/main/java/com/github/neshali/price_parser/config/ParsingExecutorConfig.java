package com.github.neshali.price_parser.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Configuration
public class ParsingExecutorConfig {


    @Bean(destroyMethod = "shutdown")
    public ExecutorService parsingExecutorService(
            @Value("${price-parser.parser.pool-size:4}") int poolSize
    ) {
        return Executors.newFixedThreadPool(poolSize);
    }
}
