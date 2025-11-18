package com.github.neshali.price_parser.service;

import com.github.neshali.price_parser.domain.ParsingTask;
import com.github.neshali.price_parser.domain.ParsingTaskStatus;
import com.github.neshali.price_parser.domain.Product;
import com.github.neshali.price_parser.repository.ParsingTaskRepository;
import com.github.neshali.price_parser.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutorService;

@Service
public class ParsingTaskProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ParsingTaskProcessingService.class);

    private final ParsingTaskRepository parsingTaskRepository;
    private final ProductRepository productRepository;
    private final PriceParsingService priceParsingService;
    private final ExecutorService parsingExecutorService;
    private final int maxTasksPerTick;

    public ParsingTaskProcessingService(
            ParsingTaskRepository parsingTaskRepository,
            ProductRepository productRepository,
            PriceParsingService priceParsingService,
            @Qualifier("parsingExecutorService") ExecutorService parsingExecutorService,
            @Value("${price-parser.parser.max-tasks-per-tick:10}") int maxTasksPerTick
    ) {
        this.parsingTaskRepository = parsingTaskRepository;
        this.productRepository = productRepository;
        this.priceParsingService = priceParsingService;
        this.parsingExecutorService = parsingExecutorService;
        this.maxTasksPerTick = maxTasksPerTick;
    }

    public void submitNewTasksForParsing() {
        List<ParsingTask> newTasks = parsingTaskRepository.findByStatus(ParsingTaskStatus.NEW);

        if (newTasks.isEmpty()) {
            log.debug("No NEW parsing tasks found");
            return;
        }

        List<ParsingTask> tasksToProcess = newTasks.stream()
                .limit(maxTasksPerTick)
                .toList();

        log.info("Submitting {} parsing tasks for processing", tasksToProcess.size());

        for (ParsingTask task : tasksToProcess) {
            Long taskId = task.getId();

            task.setStatus(ParsingTaskStatus.IN_PROGRESS);
            task.setErrorMessage(null);
            parsingTaskRepository.save(task); // статус IN_PROGRESS сохранили

            parsingExecutorService.submit(() -> {
                try {
                    processTask(taskId);
                } catch (Exception ex) {
                    log.error("Unexpected error while processing task {}", taskId, ex);
                }
            });
        }
    }

    /**
     * Обработка одной задачи парсинга в отдельном потоке.
     * Здесь мы явно сохраняем задачу после изменения статуса.
     */
    public void processTask(Long taskId) {
        ParsingTask task = parsingTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalStateException("ParsingTask not found: " + taskId));

        try {
            log.debug("Started processing task {} with URL {}", taskId, task.getUrl());

            Product product = priceParsingService.parseProduct(task.getUrl());
            productRepository.save(product);

            task.setStatus(ParsingTaskStatus.COMPLETED);
            task.setErrorMessage(null);

            log.info("Task {} completed successfully", taskId);
        } catch (Exception e) {
            task.setStatus(ParsingTaskStatus.FAILED);
            task.setErrorMessage(e.getMessage());
            log.warn("Failed to process task {} for url {}: {}",
                    taskId, task.getUrl(), e.getMessage());
        }

        // КЛЮЧЕВАЯ СТРОКА: сохраняем обновлённый статус в БД
        parsingTaskRepository.save(task);
    }
}
