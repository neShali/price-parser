package com.github.neshali.price_parser.repository;

import com.github.neshali.price_parser.domain.ParsingTask;
import com.github.neshali.price_parser.domain.ParsingTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для задач парсинга.
 */
@Repository
public interface ParsingTaskRepository extends JpaRepository<ParsingTask, Long> {

    /**
     * Найти самую старую задачу в указанном статусе.
     */
    Optional<ParsingTask> findFirstByStatusOrderByCreatedAtAsc(ParsingTaskStatus status);

    /**
     * Найти все задачи с указанным статусом.
     */
    List<ParsingTask> findByStatus(ParsingTaskStatus status);
}

