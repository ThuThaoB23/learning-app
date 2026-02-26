package com.learnapp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.learnapp.entities.UserActivityTargetType;
import com.learnapp.entities.UserActivityType;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserActivityLogResponse(
        UUID id,
        UUID userId,
        String userDisplayName,
        UserActivityType activityType,
        UserActivityTargetType targetType,
        UUID targetId,
        Map<String, Object> metadata,
        LocalDateTime createdAt
) {}
