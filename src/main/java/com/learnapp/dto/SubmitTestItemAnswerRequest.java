package com.learnapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record SubmitTestItemAnswerRequest(
        @NotBlank String answer,
        @NotNull @PositiveOrZero Integer timeMs
) {}
