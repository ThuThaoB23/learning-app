package com.learnapp.controller;

import com.learnapp.dto.AdminUpdateUserRequest;
import com.learnapp.dto.AdminResetPasswordRequest;
import com.learnapp.dto.RegisterRequest;
import com.learnapp.dto.UserResponse;
import com.learnapp.entities.UserRole;
import com.learnapp.entities.UserStatus;
import com.learnapp.service.AuthService;
import com.learnapp.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
@Tag(name = "Admin Users", description = "Admin user management APIs")
public class AdminUserController {

    private final AuthService authService;
    private final UserService userService;

    public AdminUserController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    /**
     * List users. Admin-only.
     */
    @Operation(summary = "List users", description = "List non-deleted users.")
    @org.springframework.web.bind.annotation.GetMapping
    public Page<UserResponse> listUsers(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String email,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String username,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String displayName,
            @org.springframework.web.bind.annotation.RequestParam(required = false) UserRole role,
            @org.springframework.web.bind.annotation.RequestParam(required = false) UserStatus status,
            @ParameterObject Pageable pageable
    ) {
        return userService.listUsers(email, username, displayName, role, status, pageable);
    }

    /**
     * Export users to CSV. Admin-only.
     */
    @Operation(summary = "Export users", description = "Export users to CSV with the same filters as search.")
    @org.springframework.web.bind.annotation.GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<String> exportUsers(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String email,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String username,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String displayName,
            @org.springframework.web.bind.annotation.RequestParam(required = false) UserRole role,
            @org.springframework.web.bind.annotation.RequestParam(required = false) UserStatus status
    ) {
        String csv = buildCsv(userService.exportUsers(email, username, displayName, role, status));
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"users.csv\"")
                .header("Content-Type", "text/csv; charset=utf-8")
                .body(csv);
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

    private String buildCsv(java.util.List<UserResponse> users) {
        StringBuilder builder = new StringBuilder();
        // UTF-8 BOM for Excel compatibility
        builder.append('\uFEFF');
        builder.append("id,email,username,displayName,role,status,locale,timeZone,dailyGoal,lastLoginAt,createdAt,updatedAt")
                .append("\n");
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        for (UserResponse user : users) {
            builder.append(escapeCsv(valueOrEmpty(user.id()))).append(",")
                    .append(escapeCsv(user.email())).append(",")
                    .append(escapeCsv(user.username())).append(",")
                    .append(escapeCsv(user.displayName())).append(",")
                    .append(escapeCsv(valueOrEmpty(user.role()))).append(",")
                    .append(escapeCsv(valueOrEmpty(user.status()))).append(",")
                    .append(escapeCsv(user.locale())).append(",")
                    .append(escapeCsv(user.timeZone())).append(",")
                    .append(escapeCsv(valueOrEmpty(user.dailyGoal()))).append(",")
                    .append(escapeCsv(formatDate(user.lastLoginAt(), formatter))).append(",")
                    .append(escapeCsv(formatDate(user.createdAt(), formatter))).append(",")
                    .append(escapeCsv(formatDate(user.updatedAt(), formatter)))
                    .append("\n");
        }
        return builder.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\r")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private String valueOrEmpty(Object value) {
        return value == null ? "" : value.toString();
    }

    private String formatDate(java.time.LocalDateTime value, DateTimeFormatter formatter) {
        return value == null ? "" : formatter.format(value);
    }
}
