package com.learnapp.dto;

import com.learnapp.entities.UserVocabStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record FlashcardItemResponse(
        UUID userVocabularyId,
        UUID vocabularyId,
        FlashcardDeckBucket bucket,
        String term,
        String definition,
        String definitionVi,
        List<String> examples,
        List<VocabularyAudioResponse> audios,
        String phonetic,
        String partOfSpeech,
        String language,
        UserVocabStatus status,
        Integer progress,
        LocalDateTime lastReviewedAt,
        LocalDateTime nextDueAt
) {}
