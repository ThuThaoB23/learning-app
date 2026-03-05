package com.learnapp.dto;

import com.learnapp.entities.UserVocabStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record UserVocabularyResponse(
        UUID vocabularyId,
        String term,
        List<VocabularyAudioResponse> audios,
        UserVocabStatus status,
        Integer progress,
        LocalDateTime lastReviewedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
