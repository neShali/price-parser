package com.github.neshali.price_parser.config;

import com.github.neshali.price_parser.domain.ParsingTask;
import com.github.neshali.price_parser.domain.ParsingTaskStatus;
import com.github.neshali.price_parser.repository.ParsingTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Инициализация тестовых задач парсинга при старте приложения.
 *
 * При первом запуске добавляем несколько URL в таблицу parsing_tasks
 * со статусом NEW, чтобы было что парсить.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final ParsingTaskRepository parsingTaskRepository;

    public DataInitializer(ParsingTaskRepository parsingTaskRepository) {
        this.parsingTaskRepository = parsingTaskRepository;
    }

    @Override
    public void run(String... args) {
        long existingCount = parsingTaskRepository.count();
        if (existingCount > 0) {
            log.info("Parsing tasks already exist in DB ({}), skipping initialization", existingCount);
            return;
        }

        List<String> demoUrls = List.of(
                "https://example.com/product/1",
                "https://example.com/product/2",
                "https://example.com/product/3",
                "https://example.org/item/42",
                "https://shop.example.net/goods/100500"
        );

        for (String url : demoUrls) {
            ParsingTask task = new ParsingTask();
            task.setUrl(url);
            task.setStatus(ParsingTaskStatus.NEW);
            // createdAt / updatedAt заполнятся в @PrePersist
            parsingTaskRepository.save(task);
        }

        log.info("Initialized {} parsing tasks with demo URLs", demoUrls.size());
    }
}
