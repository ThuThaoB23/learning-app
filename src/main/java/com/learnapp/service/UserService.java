package com.learnapp.service;

import com.learnapp.dto.UpdateMeRequest;
import com.learnapp.dto.UserResponse;
import com.learnapp.entities.User;
import com.learnapp.error.AppException;
import com.learnapp.repository.UserRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getMe(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        return UserMapper.toResponse(user);
    }

    public UserResponse updateMe(UUID userId, UpdateMeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        if (request.username() != null) {
            user.setUsername(request.username().trim());
        }

        if (request.displayName() != null) {
            user.setDisplayName(request.displayName().trim());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl());
        }
        if (request.locale() != null) {
            user.setLocale(request.locale());
        }
        if (request.timeZone() != null) {
            user.setTimeZone(request.timeZone());
        }
        if (request.dailyGoal() != null) {
            user.setDailyGoal(request.dailyGoal());
        }

        user = userRepository.save(user);
        return UserMapper.toResponse(user);
    }
}
