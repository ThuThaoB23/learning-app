package com.learnapp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.learnapp.entities.UserRole;
import com.learnapp.entities.UserStatus;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponse(
        UUID id,
        String email,
        String username,
        String displayName,
        String avatarUrl,
        UserRole role,
        UserStatus status,
        String locale,
        String timeZone,
        Integer dailyGoal,
        Map<String, Object> preferences,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
