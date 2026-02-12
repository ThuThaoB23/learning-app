package com.learnapp.dto;

import com.learnapp.entities.TestItemStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record SubmitTestItemAnswerResponse(
        UUID itemId,
        TestItemStatus status,
        boolean correct,
        String expected,
        String feedback,
        Integer process,
        LocalDateTime nextDueAt,
        Integer streak,
        Integer rightCount,
        Integer wrongCount
) {}
