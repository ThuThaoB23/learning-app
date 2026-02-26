package com.learnapp.dto;

import jakarta.validation.constraints.Size;

public record ApproveVocabularyContributionRequest(
        @Size(max = 4000) String reviewNote
) {}
