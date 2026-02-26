package com.learnapp.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
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
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "vocabulary_contribution_topics",
        indexes = {
            @Index(name = "idx_vocab_contrib_topic_contrib_id", columnList = "contribution_id"),
            @Index(name = "idx_vocab_contrib_topic_topic_id", columnList = "topic_id")
        }
)
@IdClass(VocabularyContributionTopicId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyContributionTopic {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "contribution_id", nullable = false, length = 36)
    private UUID contributionId;

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "topic_id", nullable = false, length = 36)
    private UUID topicId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
