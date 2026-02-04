package com.learnapp.entities;

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
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "user_vocabularies",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_user_vocab_user_vocab",
                    columnNames = {"user_id", "vocabulary_id"}
            )
        },
        indexes = {
            @Index(name = "idx_user_vocab_user_id", columnList = "user_id"),
            @Index(name = "idx_user_vocab_vocab_id", columnList = "vocabulary_id"),
            @Index(name = "idx_user_vocab_status", columnList = "status"),
            @Index(name = "idx_user_vocab_last_reviewed", columnList = "last_reviewed_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVocabulary {

    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private UUID id;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "user_id", nullable = false, length = 36)
    private UUID userId;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "vocabulary_id", nullable = false, length = 36)
    private UUID vocabularyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private UserVocabStatus status = UserVocabStatus.NEW;

    @Column(name = "progress", nullable = false)
    @Builder.Default
    private Integer progress = 0;

    @Column(name = "last_reviewed_at")
    private LocalDateTime lastReviewedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
