package com.learnapp.dto;

import com.learnapp.entities.UserVocabStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateUserVocabularyRequest(
        UserVocabStatus status,
        @Min(0) @Max(100) Integer progress
) {}
