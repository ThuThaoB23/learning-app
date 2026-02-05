package com.learnapp.service;

import com.learnapp.dto.AdminResetPasswordRequest;
import com.learnapp.dto.AdminUpdateUserRequest;
import com.learnapp.dto.UpdateMeRequest;
import com.learnapp.dto.UserResponse;
import com.learnapp.entities.User;
import com.learnapp.entities.UserRole;
import com.learnapp.entities.UserStatus;
import com.learnapp.error.AppException;
import com.learnapp.repository.UserRepository;
import java.util.Locale;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserResponse getMe(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        ensureNotDeleted(user);
        return UserMapper.toResponse(user);
    }

    public UserResponse updateMe(UUID userId, UpdateMeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        ensureNotDeleted(user);

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

    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(
            String email,
            String username,
            String displayName,
            UserRole role,
            UserStatus status,
            Pageable pageable
    ) {
        return userRepository.searchUsers(
                normalizeSearch(email),
                normalizeSearch(username),
                normalizeSearch(displayName),
                role,
                status,
                pageable
        ).map(UserMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> exportUsers(
            String email,
            String username,
            String displayName,
            UserRole role,
            UserStatus status
    ) {
        return userRepository.searchUsers(
                normalizeSearch(email),
                normalizeSearch(username),
                normalizeSearch(displayName),
                role,
                status,
                Pageable.unpaged()
        ).map(UserMapper::toResponse).getContent();
    }

    public UserResponse updateUser(UUID userId, AdminUpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        ensureNotDeleted(user);

        if (request.email() != null) {
            String normalizedEmail = normalizeEmail(request.email());
            if (!normalizedEmail.equals(user.getEmail())) {
                userRepository.findByEmail(normalizedEmail).ifPresent(existing -> {
                    throw new AppException(HttpStatus.CONFLICT, "EMAIL_EXISTS", "Email already exists");
                });
                user.setEmail(normalizedEmail);
            }
        }

        if (request.username() != null) {
            user.setUsername(request.username().trim());
        }

        if (request.displayName() != null) {
            user.setDisplayName(request.displayName().trim());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl());
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        if (request.status() != null) {
            user.setStatus(request.status());
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

    public void deleteUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        ensureNotDeleted(user);
        user.setDeletedAt(java.time.LocalDateTime.now());
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
    }

    public void resetPassword(UUID userId, AdminResetPasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        ensureNotDeleted(user);
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    public UserResponse restoreUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        if (user.getDeletedAt() == null) {
            return UserMapper.toResponse(user);
        }
        user.setDeletedAt(null);
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);
        return UserMapper.toResponse(user);
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeSearch(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void ensureNotDeleted(User user) {
        if (user.getDeletedAt() != null) {
            throw new AppException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found");
        }
    }
}
