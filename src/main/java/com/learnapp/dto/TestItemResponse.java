package com.learnapp.dto;

import com.learnapp.entities.QuestionType;
import com.learnapp.entities.TestItemStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record TestItemResponse(
        UUID id,
        QuestionType questionType,
        Object questionPayload,
        Integer position,
        TestItemStatus status,
        String expected,
        String userAnswer,
        LocalDateTime answeredAt,
        Integer timeMs
) {}
