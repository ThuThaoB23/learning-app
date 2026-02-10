package com.learnapp.dto;

import com.learnapp.entities.VocabularyStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record UpdateVocabularyRequest(
        @Size(min = 1, max = 255) String term,
        @Size(min = 1) String definition,
        @Size(max = 4000) String definitionVi,
        List<@Valid UpdateVocabularyExampleRequest> examples,
        @Size(max = 100) String phonetic,
        @Size(max = 50) String partOfSpeech,
        @Size(min = 1, max = 10) String language,
        VocabularyStatus status,
        List<UUID> topicIds
) {}
