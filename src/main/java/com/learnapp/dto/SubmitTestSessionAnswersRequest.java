package com.learnapp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import java.util.UUID;

public record SubmitTestSessionAnswersRequest(
        @NotNull List<@Valid ItemAnswer> answers
) {
    public record ItemAnswer(
            @NotNull UUID itemId,
            @NotBlank String answer,
            @NotNull @PositiveOrZero Integer timeMs
    ) {}
}
