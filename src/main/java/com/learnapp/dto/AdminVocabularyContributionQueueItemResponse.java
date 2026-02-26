package com.learnapp.dto;

import com.learnapp.entities.VocabularyContributionStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record AdminVocabularyContributionQueueItemResponse(
        UUID id,
        String term,
        String language,
        String partOfSpeech,
        UUID contributorUserId,
        String contributorDisplayName,
        VocabularyContributionStatus status,
        LocalDateTime createdAt
) {}
