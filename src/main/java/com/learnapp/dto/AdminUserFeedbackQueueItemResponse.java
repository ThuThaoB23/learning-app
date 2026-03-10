package com.learnapp.dto;

import com.learnapp.entities.UserFeedbackCategory;
import com.learnapp.entities.UserFeedbackStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record AdminUserFeedbackQueueItemResponse(
        UUID id,
        UUID userId,
        String userDisplayName,
        UserFeedbackCategory category,
        String title,
        UserFeedbackStatus status,
        long attachmentCount,
        LocalDateTime createdAt
) {}
