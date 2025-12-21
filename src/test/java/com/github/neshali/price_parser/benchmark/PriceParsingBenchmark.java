package com.github.neshali.price_parser.benchmark;

import com.github.neshali.price_parser.service.PriceParsingService;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Пример JMH-бенчмарка для сравнения разных стратегий парсинга URL-ов.
 *
 * Запуск из Maven:
 * mvn -DskipTests=false
 * -Dtest=com.github.neshali.price_parser.benchmark.PriceParsingBenchmark test
 *
 * или через отдельный JMH-плагин/launcher (по желанию).
 *
 * Также можно запустить main метод для быстрого тестирования.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class PriceParsingBenchmark {

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(PriceParsingBenchmark.class.getSimpleName())
                .forks(0) // Отключаем форки чтобы избежать проблем с classpath
                .warmupIterations(1)
                .measurementIterations(2)
                .build();

        new Runner(opt).run();
    }

    private ConfigurableApplicationContext context;
    private PriceParsingService priceParsingService;

    private List<String> urls;

    @Setup(Level.Trial)
    public void setup() {
        // Поднимаем Spring-контекст один раз на весь прогон бенчмарка
        this.context = SpringApplication.run(com.github.neshali.price_parser.PriceParserApplication.class);
        this.priceParsingService = context.getBean(PriceParsingService.class);

        this.urls = List.of(
                "https://example.com/product/cheap-item-1",
                "https://example.com/product/cheap-item-2",
                "https://example.com/product/cheap-item-3",
                "https://example.com/product/cheap-item-4",
                "https://example.com/product/cheap-item-5",
                "https://example.com/product/cheap-item-6",
                "https://example.com/product/cheap-item-7",
                "https://example.com/product/cheap-item-8",
                "https://example.com/product/cheap-item-9",
                "https://example.com/product/cheap-item-10");
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    /**
     * Обычный цикл for.
     */
    @Benchmark
    public void parseWithForLoop() {
        for (String url : urls) {
            priceParsingService.parseProduct(url);
        }
    }

    /**
     * Stream API (последовательный).
     */
    @Benchmark
    public void parseWithStream() {
        urls.stream()
                .map(priceParsingService::parseProduct)
                .forEach(p -> {
                });
    }

    /**
     * parallelStream — может быть быстрее или медленнее в зависимости от
     * нагрузки/окружения.
     */
    @Benchmark
    public void parseWithParallelStream() {
        urls.parallelStream()
                .map(priceParsingService::parseProduct)
                .forEach(p -> {
                });
    }
}
