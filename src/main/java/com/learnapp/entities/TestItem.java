package com.learnapp.entities;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "test_items",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_test_items_session_position",
                    columnNames = {"test_session_id", "position"}
            )
        },
        indexes = {
            @Index(name = "idx_test_items_session_id", columnList = "test_session_id"),
            @Index(name = "idx_test_items_status", columnList = "status"),
            @Index(name = "idx_test_items_user_vocab_id", columnList = "user_vocab_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestItem {

    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private UUID id;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "test_session_id", nullable = false, length = 36)
    private UUID testSessionId;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "user_vocab_id", nullable = false, length = 36)
    private UUID userVocabId;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 40)
    private QuestionType questionType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "question_payload", nullable = false, columnDefinition = "json")
    private JsonNode questionPayload;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TestItemStatus status = TestItemStatus.PENDING;

    @Column(name = "user_answer", columnDefinition = "text")
    private String userAnswer;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @Column(name = "time_ms")
    private Integer timeMs;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
