package com.learnapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateVocabularyAudioRequest(
        @NotBlank @Size(max = 1000) String audioUrl,
        @Size(max = 20) String accent
) {}
