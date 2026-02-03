package com.learnapp.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateMeRequest(
        @Size(min = 1, max = 100) String username,
        @Size(min = 1, max = 100) String displayName,
        @Size(max = 500) String avatarUrl,
        @Size(max = 10) String locale,
        @Size(max = 50) String timeZone,
        @Min(0) Integer dailyGoal
) {}
