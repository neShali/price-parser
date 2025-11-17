package com.github.neshali.price_parser.domain;

/**
 * Статус задачи парсинга.
 * NEW        - только что создана, ещё не бралась в работу
 * IN_PROGRESS - сейчас обрабатывается каким-то потоком
 * COMPLETED   - успешно спарсили данные
 * FAILED      - произошла ошибка при парсинге
 */
public enum ParsingTaskStatus {
    NEW,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}
