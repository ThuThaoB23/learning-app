package com.learnapp.dto;

import com.learnapp.entities.TopicStatus;
import jakarta.validation.constraints.Size;

public record UpdateTopicRequest(
        @Size(min = 1, max = 100) String name,
        @Size(max = 500) String description,
        TopicStatus status
) {}
