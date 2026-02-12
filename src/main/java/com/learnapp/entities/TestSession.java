package com.learnapp.entities;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;
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
        name = "test_sessions",
        indexes = {
            @Index(name = "idx_test_sessions_user_id", columnList = "user_id"),
            @Index(name = "idx_test_sessions_type", columnList = "type"),
            @Index(name = "idx_test_sessions_status", columnList = "status"),
            @Index(name = "idx_test_sessions_schedule_date", columnList = "schedule_date")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestSession {

    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private UUID id;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "user_id", nullable = false, length = 36)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private TestSessionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private TestSessionStatus status = TestSessionStatus.ACTIVE;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "schedule_date")
    private LocalDate scheduleDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private TestSessionSourceType sourceType;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "source_ref_id", length = 36)
    private UUID sourceRefId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_params", columnDefinition = "json")
    private JsonNode sourceParams;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "total_items", nullable = false)
    @Builder.Default
    private Integer totalItems = 0;

    @Column(name = "correct_count", nullable = false)
    @Builder.Default
    private Integer correctCount = 0;

    @Column(name = "wrong_count", nullable = false)
    @Builder.Default
    private Integer wrongCount = 0;

    @Column(name = "skipped_count", nullable = false)
    @Builder.Default
    private Integer skippedCount = 0;

    @Column(name = "score", nullable = false)
    @Builder.Default
    private Integer score = 0;
}
