package com.learnapp.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.UUID;

public record CreateTopicSessionRequest(
        @NotEmpty List<@NotNull UUID> topicIds,
        @Positive Integer totalItems
) {}
