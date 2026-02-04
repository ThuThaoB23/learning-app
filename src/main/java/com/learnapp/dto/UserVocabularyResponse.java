package com.learnapp.dto;

import com.learnapp.entities.UserVocabStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserVocabularyResponse(
        UUID vocabularyId,
        UserVocabStatus status,
        Integer progress,
        LocalDateTime lastReviewedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
