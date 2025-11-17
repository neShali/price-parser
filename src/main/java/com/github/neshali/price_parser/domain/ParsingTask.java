package com.github.neshali.price_parser.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "parsing_tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParsingTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * URL, который нужно спарсить.
     */
    @Column(name = "target_url", nullable = false, length = 1000)
    private String url;

    /**
     * Текущий статус задачи.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ParsingTaskStatus status;

    /**
     * Последняя ошибка при парсинге (если была).
     */
    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    /**
     * Когда задача была создана.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Когда задача последний раз изменялась (например, смена статуса).
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = ParsingTaskStatus.NEW;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
