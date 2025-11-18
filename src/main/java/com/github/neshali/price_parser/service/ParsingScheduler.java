package com.github.neshali.price_parser.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

@Component
public class ParsingScheduler {

    private static final Logger log = LoggerFactory.getLogger(ParsingScheduler.class);

    private final ExecutorService parsingExecutorService;
    private final long delayMs;

    public ParsingScheduler(
            @Qualifier("parsingExecutorService") ExecutorService parsingExecutorService,
            @Value("${price-parser.scheduler.delay-ms:10000}") long delayMs
    ) {
        this.parsingExecutorService = parsingExecutorService;
        this.delayMs = delayMs;
    }


    @Scheduled(fixedDelayString = "${price-parser.scheduler.delay-ms:10000}")
    public void scheduleParsingCycle() {
        log.debug("ParsingScheduler tick: submitting parsing cycle task to executor");

        parsingExecutorService.submit(() -> {
            String threadName = Thread.currentThread().getName();
            log.debug("Parsing cycle stub is running in thread {}", threadName);


        });
    }
}
