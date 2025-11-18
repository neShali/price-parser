package com.github.neshali.price_parser;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PriceParserApplication {

	public static void main(String[] args) {
		SpringApplication.run(PriceParserApplication.class, args);
	}
}