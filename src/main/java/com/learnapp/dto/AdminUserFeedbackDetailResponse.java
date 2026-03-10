package com.learnapp.dto;

import com.learnapp.entities.UserFeedbackCategory;
import com.learnapp.entities.UserFeedbackStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AdminUserFeedbackDetailResponse(
        UUID id,
        UUID userId,
        String userDisplayName,
        UserFeedbackCategory category,
        String title,
        String message,
        UserFeedbackStatus status,
        String sourceScreen,
        String appVersion,
        String deviceInfo,
        String locale,
        UUID readBy,
        String readByDisplayName,
        LocalDateTime readAt,
        UUID archivedBy,
        String archivedByDisplayName,
        LocalDateTime archivedAt,
        List<UserFeedbackAttachmentResponse> attachments,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
