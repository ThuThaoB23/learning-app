package com.learnapp.dto;

import java.util.List;

public record AdminVocabularyContributionDetailResponse(
        VocabularyContributionResponse contribution,
        List<VocabularyContributionReviewLogResponse> reviewLogs
) {}
