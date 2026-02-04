package com.learnapp.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddUserVocabularyRequest(
        @NotNull UUID vocabularyId
) {}
