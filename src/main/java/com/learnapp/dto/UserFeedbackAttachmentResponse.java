package com.learnapp.dto;

import java.util.UUID;

public record UserFeedbackAttachmentResponse(
        UUID id,
        String fileName,
        String contentType,
        Long fileSize,
        Integer position,
        String fileUrl
) {}
