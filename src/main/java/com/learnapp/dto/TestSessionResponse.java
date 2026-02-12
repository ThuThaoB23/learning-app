package com.learnapp.dto;

import com.learnapp.entities.TestSessionStatus;
import com.learnapp.entities.TestSessionType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TestSessionResponse(
        UUID id,
        TestSessionType type,
        TestSessionStatus status,
        String title,
        LocalDate scheduleDate,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        Integer totalItems,
        Integer correctCount,
        Integer wrongCount,
        Integer skippedCount,
        Integer score,
        List<TestItemResponse> items
) {}
