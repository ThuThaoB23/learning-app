package com.learnapp.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
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
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "vocabulary_contributions",
        indexes = {
            @Index(name = "idx_vocab_contrib_status", columnList = "status"),
            @Index(name = "idx_vocab_contrib_created_at", columnList = "created_at"),
            @Index(name = "idx_vocab_contrib_contributor", columnList = "contributor_user_id"),
            @Index(name = "idx_vocab_contrib_term_lang", columnList = "term_normalized, language")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyContribution {

    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private UUID id;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "contributor_user_id", nullable = false, length = 36)
    private UUID contributorUserId;

    @Column(name = "term", nullable = false, length = 255)
    private String term;

    @Column(name = "term_normalized", nullable = false, length = 255)
    private String termNormalized;

    @Column(name = "definition", nullable = false, columnDefinition = "text")
    private String definition;

    @Column(name = "definition_vi", columnDefinition = "text")
    private String definitionVi;

    @Column(name = "phonetic", length = 100)
    private String phonetic;

    @Column(name = "part_of_speech", length = 50)
    private String partOfSpeech;

    @Column(name = "language", nullable = false, length = 10)
    private String language;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private VocabularyContributionStatus status = VocabularyContributionStatus.SUBMITTED;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "reviewed_by", length = 36)
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_note", columnDefinition = "text")
    private String reviewNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "reject_reason", length = 50)
    private VocabularyContributionRejectReason rejectReason;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "approved_vocabulary_id", length = 36)
    private UUID approvedVocabularyId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
