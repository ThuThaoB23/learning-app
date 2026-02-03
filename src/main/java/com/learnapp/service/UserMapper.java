package com.learnapp.service;

import com.learnapp.dto.UserResponse;
import com.learnapp.entities.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getRole(),
                user.getStatus(),
                user.getLocale(),
                user.getTimeZone(),
                user.getDailyGoal(),
                user.getPreferences(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
