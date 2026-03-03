package com.learnapp.dto;

import java.util.UUID;

public record VocabularyAudioResponse(
        UUID id,
        String audioUrl,
        String accent,
        Integer position
) {}
