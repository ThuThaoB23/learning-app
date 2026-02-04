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
        name = "vocabularies",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_vocab_term_language",
                    columnNames = {"term_normalized", "language"}
            )
        },
        indexes = {
            @Index(name = "idx_vocab_term", columnList = "term_normalized"),
            @Index(name = "idx_vocab_language", columnList = "language"),
            @Index(name = "idx_vocab_status", columnList = "status"),
            @Index(name = "idx_vocab_deleted_at", columnList = "deleted_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vocabulary {

    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private UUID id;

    @Column(name = "term", nullable = false, length = 255)
    private String term;

    @Column(name = "term_normalized", nullable = false, length = 255)
    private String termNormalized;

    @Column(name = "definition", nullable = false, columnDefinition = "text")
    private String definition;

    @Column(name = "example", columnDefinition = "text")
    private String example;

    @Column(name = "phonetic", length = 100)
    private String phonetic;

    @Column(name = "part_of_speech", length = 50)
    private String partOfSpeech;

    @Column(name = "language", nullable = false, length = 10)
    private String language;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private VocabularyStatus status = VocabularyStatus.PENDING;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "created_by", length = 36)
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
