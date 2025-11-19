package com.github.neshali.price_parser.service;

import com.github.neshali.price_parser.domain.ParsingTask;
import com.github.neshali.price_parser.domain.ParsingTaskStatus;
import com.github.neshali.price_parser.domain.Product;
import com.github.neshali.price_parser.repository.ParsingTaskRepository;
import com.github.neshali.price_parser.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Юнит-тесты для многопоточного сервиса обработки задач.
 *
 * В тестах используем "синхронный" ExecutorService, который запускает задачи
 * в текущем потоке, чтобы избежать гонок и не ждать завершения потоков.
 */
@ExtendWith(MockitoExtension.class)
class ParsingTaskProcessingServiceTest {

    @Mock
    private ParsingTaskRepository parsingTaskRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PriceParsingService priceParsingService;

    private final ExecutorService directExecutorService = new DirectExecutorService();

    @AfterEach
    void tearDown() {
        directExecutorService.shutdownNow();
    }

    private ParsingTaskProcessingService createService(int maxTasksPerTick) {
        return new ParsingTaskProcessingService(
                parsingTaskRepository,
                productRepository,
                priceParsingService,
                directExecutorService,
                maxTasksPerTick
        );
    }

    @Test
    void processTask_shouldParseProductAndMarkTaskCompleted() {
        Long taskId = 1L;
        String url = "https://example.com/product/1";

        ParsingTask task = new ParsingTask();
        task.setId(taskId);
        task.setUrl(url);
        task.setStatus(ParsingTaskStatus.IN_PROGRESS);

        when(parsingTaskRepository.findById(taskId)).thenReturn(Optional.of(task));

        Product product = new Product();
        product.setSourceUrl(url);
        when(priceParsingService.parseProduct(url)).thenReturn(product);

        ParsingTaskProcessingService service = createService(10);

        service.processTask(taskId);

        // Проверяем, что продукт сохраняется
        verify(productRepository).save(product);

        // Проверяем, что статус задачи обновился и задача сохранена
        ArgumentCaptor<ParsingTask> taskCaptor = ArgumentCaptor.forClass(ParsingTask.class);
        verify(parsingTaskRepository, atLeastOnce()).save(taskCaptor.capture());

        ParsingTask savedTask = taskCaptor.getValue();
        assertThat(savedTask.getStatus()).isEqualTo(ParsingTaskStatus.COMPLETED);
        assertThat(savedTask.getErrorMessage()).isNull();
    }

    @Test
    void submitNewTasksForParsing_shouldSetInProgressAndSubmitToExecutor() {
        ParsingTask task = new ParsingTask();
        task.setId(2L);
        task.setUrl("https://example.com/product/2");
        task.setStatus(ParsingTaskStatus.NEW);

        when(parsingTaskRepository.findByStatus(ParsingTaskStatus.NEW))
                .thenReturn(List.of(task));
        when(parsingTaskRepository.findById(2L))
                .thenReturn(Optional.of(task));

        Product product = new Product();
        product.setSourceUrl(task.getUrl());
        when(priceParsingService.parseProduct(task.getUrl())).thenReturn(product);

        ParsingTaskProcessingService service = createService(10);

        service.submitNewTasksForParsing();

        // Сначала задача должна быть помечена IN_PROGRESS и сохранена
        verify(parsingTaskRepository, atLeastOnce()).save(task);
        assertThat(task.getStatus()).isIn(ParsingTaskStatus.IN_PROGRESS, ParsingTaskStatus.COMPLETED);

        // Так как DirectExecutorService выполняет задачу сразу,
        // в итоге processTask тоже отработает и ещё раз сохранит задачу.
        verify(productRepository).save(product);

        verify(parsingTaskRepository, atLeast(2)).save(any(ParsingTask.class));
    }

    @Test
    void submitNewTasksForParsing_shouldDoNothingWhenNoNewTasks() {
        when(parsingTaskRepository.findByStatus(ParsingTaskStatus.NEW))
                .thenReturn(Collections.emptyList());

        ParsingTaskProcessingService service = createService(10);

        service.submitNewTasksForParsing();

        verify(parsingTaskRepository, never()).save(any());
        verifyNoInteractions(productRepository);
    }

    /**
     * Простой ExecutorService, выполняющий задачи синхронно в текущем потоке.
     */
    private static class DirectExecutorService extends AbstractExecutorService {

        private volatile boolean terminated = false;

        @Override
        public void shutdown() {
            terminated = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            terminated = true;
            return Collections.emptyList();
        }

        @Override
        public boolean isShutdown() {
            return terminated;
        }

        @Override
        public boolean isTerminated() {
            return terminated;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return terminated;
        }

        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }
}
