package com.learnapp.dto;

import com.learnapp.entities.VocabularyStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record VocabularyResponse(
        UUID id,
        String term,
        String definition,
        String definitionVi,
        List<String> examples,
        String phonetic,
        String partOfSpeech,
        String language,
        VocabularyStatus status,
        Boolean inMyVocab,
        UUID createdBy,
        LocalDateTime createdAt
) {}
