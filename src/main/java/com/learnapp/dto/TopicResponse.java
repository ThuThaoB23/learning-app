package com.learnapp.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record TopicResponse(
        UUID id,
        String name,
        String slug,
        String description,
        LocalDateTime createdAt
) {}
