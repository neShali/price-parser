package com.github.neshali.price_parser.service;

import com.github.neshali.price_parser.domain.ParsingTask;
import com.github.neshali.price_parser.domain.ParsingTaskStatus;
import com.github.neshali.price_parser.domain.Product;
import com.github.neshali.price_parser.repository.ParsingTaskRepository;
import com.github.neshali.price_parser.repository.ProductRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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

    // Micrometer metrics
    private final MeterRegistry meterRegistry;
    private final Timer parsingDurationTimer;
    private final Counter parsingSuccessCounter;
    private final Counter parsingFailureCounter;
    private final Counter productsSavedCounter;

    public ParsingTaskProcessingService(
            ParsingTaskRepository parsingTaskRepository,
            ProductRepository productRepository,
            PriceParsingService priceParsingService,
            @Qualifier("parsingExecutorService") ExecutorService parsingExecutorService,
            @Value("${price-parser.parser.max-tasks-per-tick:10}") int maxTasksPerTick,
            MeterRegistry meterRegistry
    ) {
        this.parsingTaskRepository = parsingTaskRepository;
        this.productRepository = productRepository;
        this.priceParsingService = priceParsingService;
        this.parsingExecutorService = parsingExecutorService;
        this.maxTasksPerTick = maxTasksPerTick;

        this.meterRegistry = meterRegistry;

        // Timer: время обработки одного задания парсинга
        this.parsingDurationTimer = Timer.builder("price_parser.parsing.duration")
                .description("Duration of single parsing task processing")
                .publishPercentileHistogram()
                .register(meterRegistry);

        // Счётчики успешных / неуспешных парсингов
        this.parsingSuccessCounter = Counter.builder("price_parser.parsing.success.count")
                .description("Number of successfully processed parsing tasks")
                .register(meterRegistry);

        this.parsingFailureCounter = Counter.builder("price_parser.parsing.failure.count")
                .description("Number of failed parsing tasks")
                .register(meterRegistry);

        // Счётчик сохранённых записей в БД
        this.productsSavedCounter = Counter.builder("price_parser.products.saved.count")
                .description("Number of Product entities saved to DB")
                .register(meterRegistry);

        // Gauge: количество задач со статусом NEW (ожидающих обработки)
        Gauge.builder("price_parser.tasks.new.count", parsingTaskRepository, repo ->
                        repo.findByStatus(ParsingTaskStatus.NEW).size())
                .description("Number of NEW parsing tasks waiting for processing")
                .register(meterRegistry);

        // Gauge: количество задач со статусом IN_PROGRESS
        Gauge.builder("price_parser.tasks.in_progress.count", parsingTaskRepository, repo ->
                        repo.findByStatus(ParsingTaskStatus.IN_PROGRESS).size())
                .description("Number of IN_PROGRESS parsing tasks")
                .register(meterRegistry);
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

    public void processTask(Long taskId) {
        ParsingTask task = parsingTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalStateException("ParsingTask not found: " + taskId));

        // старт замера времени обработки конкретного задания
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            log.debug("Started processing task {} with URL {}", taskId, task.getUrl());

            Product product = priceParsingService.parseProduct(task.getUrl());
            productRepository.save(product);
            productsSavedCounter.increment();

            task.setStatus(ParsingTaskStatus.COMPLETED);
            task.setErrorMessage(null);

            parsingSuccessCounter.increment();

            log.info("Task {} completed successfully", taskId);
        } catch (Exception e) {
            task.setStatus(ParsingTaskStatus.FAILED);
            task.setErrorMessage(e.getMessage());
            parsingFailureCounter.increment();
            log.warn("Failed to process task {} for url {}: {}",
                    taskId, task.getUrl(), e.getMessage());
        } finally {
            // Останавливаем таймер независимо от результата
            sample.stop(parsingDurationTimer);
        }

        parsingTaskRepository.save(task);
    }
}
