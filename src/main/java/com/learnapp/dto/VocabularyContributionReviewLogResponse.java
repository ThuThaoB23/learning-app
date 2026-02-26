package com.learnapp.dto;

import com.learnapp.entities.VocabularyContributionReviewAction;
import java.time.LocalDateTime;
import java.util.UUID;

public record VocabularyContributionReviewLogResponse(
        UUID id,
        VocabularyContributionReviewAction action,
        UUID actorUserId,
        String actorDisplayName,
        String note,
        LocalDateTime createdAt
) {}
