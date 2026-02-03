package com.learnapp.dto;

import com.learnapp.entities.UserRole;
import com.learnapp.entities.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record AdminUpdateUserRequest(
        @Email @Size(min = 1, max = 255) String email,
        @Size(min = 1, max = 100) String username,
        @Size(min = 1, max = 100) String displayName,
        @Size(max = 500) String avatarUrl,
        UserRole role,
        UserStatus status,
        @Size(max = 10) String locale,
        @Size(max = 50) String timeZone,
        @Min(0) Integer dailyGoal
) {}
