package com.learnapp.controller;

import com.learnapp.dto.AdminUpdateUserRequest;
import com.learnapp.dto.AdminResetPasswordRequest;
import com.learnapp.dto.RegisterRequest;
import com.learnapp.dto.UserResponse;
import com.learnapp.service.AuthService;
import com.learnapp.service.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AuthService authService;
    private final UserService userService;

    public AdminUserController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    /**
     * Create a new user account. Admin-only.
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update an existing user. Admin-only.
     */
    @PatchMapping("/{userId}")
    public UserResponse updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody AdminUpdateUserRequest request
    ) {
        return userService.updateUser(userId, request);
    }

    /**
     * Delete a user. Admin-only.
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reset a user's password. Admin-only.
     */
    @PostMapping("/{userId}/reset-password")
    public ResponseEntity<Void> resetPassword(
            @PathVariable UUID userId,
            @Valid @RequestBody AdminResetPasswordRequest request
    ) {
        userService.resetPassword(userId, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Restore a soft-deleted user. Admin-only.
     */
    @PostMapping("/{userId}/restore")
    public UserResponse restoreUser(@PathVariable UUID userId) {
        return userService.restoreUser(userId);
    }
}
