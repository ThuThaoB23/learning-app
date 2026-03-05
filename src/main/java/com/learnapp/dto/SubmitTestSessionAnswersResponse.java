package com.learnapp.dto;

import com.learnapp.entities.TestSessionStatus;
import java.util.List;
import java.util.UUID;

public record SubmitTestSessionAnswersResponse(
        UUID sessionId,
        TestSessionStatus sessionStatus,
        Integer totalItems,
        Integer correctCount,
        Integer wrongCount,
        Integer skippedCount,
        Integer score,
        List<SubmitTestItemAnswerResponse> results
) {}
