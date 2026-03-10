package com.learnapp.dto;

import com.learnapp.entities.UserFeedbackCategory;
import com.learnapp.entities.UserFeedbackStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record UserFeedbackResponse(
        UUID id,
        UserFeedbackCategory category,
        String title,
        String message,
        UserFeedbackStatus status,
        String sourceScreen,
        String appVersion,
        String deviceInfo,
        String locale,
        List<UserFeedbackAttachmentResponse> attachments,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
