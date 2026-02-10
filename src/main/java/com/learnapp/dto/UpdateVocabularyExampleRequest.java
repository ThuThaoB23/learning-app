package com.learnapp.dto;

import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UpdateVocabularyExampleRequest(
        UUID id,
        @Size(min = 1) String value
) {}
