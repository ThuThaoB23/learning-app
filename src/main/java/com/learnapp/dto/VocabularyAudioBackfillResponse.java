package com.learnapp.dto;

import com.learnapp.entities.VocabularyStatus;

public record VocabularyAudioBackfillResponse(
        String language,
        VocabularyStatus status,
        boolean forceRefresh,
        int batchSize,
        Integer limit,
        long processed,
        long updated,
        long skipped,
        long failed
) {}
