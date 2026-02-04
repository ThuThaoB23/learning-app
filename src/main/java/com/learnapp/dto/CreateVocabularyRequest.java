package com.learnapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CreateVocabularyRequest(
        @NotBlank @Size(max = 255) String term,
        @NotBlank String definition,
        @Size(max = 2000) String example,
        @Size(max = 100) String phonetic,
        @Size(max = 50) String partOfSpeech,
        @NotBlank @Size(max = 10) String language,
        List<UUID> topicIds
) {}
