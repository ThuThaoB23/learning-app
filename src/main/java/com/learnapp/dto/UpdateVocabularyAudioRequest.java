package com.learnapp.dto;

import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UpdateVocabularyAudioRequest(
        UUID id,
        @Size(min = 1, max = 1000) String audioUrl,
        @Size(max = 20) String accent
) {}
