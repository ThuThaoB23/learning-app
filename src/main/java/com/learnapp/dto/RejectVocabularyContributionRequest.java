package com.learnapp.dto;

import com.learnapp.entities.VocabularyContributionRejectReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RejectVocabularyContributionRequest(
        @NotNull VocabularyContributionRejectReason rejectReason,
        @Size(max = 4000) String reviewNote
) {}
