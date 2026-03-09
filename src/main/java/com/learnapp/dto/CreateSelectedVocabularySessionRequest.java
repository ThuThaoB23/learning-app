package com.learnapp.dto;

import com.learnapp.entities.QuestionType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record CreateSelectedVocabularySessionRequest(
        @NotEmpty List<@NotNull UUID> vocabularyIds,
        @NotEmpty List<@NotNull QuestionType> questionTypes
) {}
