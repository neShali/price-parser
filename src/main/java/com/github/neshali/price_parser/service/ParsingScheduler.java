package com.github.neshali.price_parser.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ParsingScheduler {

    private static final Logger log = LoggerFactory.getLogger(ParsingScheduler.class);

    private final ParsingTaskProcessingService processingService;

    public ParsingScheduler(ParsingTaskProcessingService processingService) {
        this.processingService = processingService;
    }

    @Scheduled(fixedDelayString = "${price-parser.scheduler.delay-ms:10000}")
    public void scheduleParsingCycle() {
        log.debug("ParsingScheduler tick: checking for new tasks");
        processingService.submitNewTasksForParsing();
    }
}
