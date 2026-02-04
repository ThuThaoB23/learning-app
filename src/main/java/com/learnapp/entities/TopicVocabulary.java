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
        name = "topic_vocabularies",
        indexes = {
            @Index(name = "idx_topic_vocab_topic_id", columnList = "topic_id"),
            @Index(name = "idx_topic_vocab_vocab_id", columnList = "vocabulary_id")
        }
)
@IdClass(TopicVocabularyId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicVocabulary {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "topic_id", nullable = false, length = 36)
    private UUID topicId;

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "vocabulary_id", nullable = false, length = 36)
    private UUID vocabularyId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
