package com.learnapp.dto;

import com.learnapp.entities.VocabularyStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record VocabularyResponse(
        UUID id,
        String term,
        String definition,
        String example,
        String phonetic,
        String partOfSpeech,
        String language,
        VocabularyStatus status,
        UUID createdBy,
        LocalDateTime createdAt
) {}
