package com.learnapp.error;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(
        Instant timestamp,
        String path,
        String errorCode,
        String message,
        Map<String, Object> details
) {}
