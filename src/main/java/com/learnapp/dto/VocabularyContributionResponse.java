package com.learnapp.dto;

import com.learnapp.entities.VocabularyContributionRejectReason;
import com.learnapp.entities.VocabularyContributionStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record VocabularyContributionResponse(
        UUID id,
        UUID contributorUserId,
        String contributorDisplayName,
        String term,
        String definition,
        String definitionVi,
        List<String> examples,
        String phonetic,
        String partOfSpeech,
        String language,
        List<UUID> topicIds,
        VocabularyContributionStatus status,
        String reviewNote,
        VocabularyContributionRejectReason rejectReason,
        UUID approvedVocabularyId,
        UUID reviewedBy,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
