package com.learnapp.controller;

import com.learnapp.dto.UserActivityLogResponse;
import com.learnapp.entities.UserActivityTargetType;
import com.learnapp.entities.UserActivityType;
import com.learnapp.dto.UpdateMeRequest;
import com.learnapp.dto.UserResponse;
import com.learnapp.security.UserPrincipal;
import com.learnapp.service.UserActivityLogService;
import com.learnapp.service.UserService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.Operation;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class UserController {

    private final UserService userService;
    private final UserActivityLogService userActivityLogService;

    public UserController(UserService userService, UserActivityLogService userActivityLogService) {
        this.userService = userService;
        this.userActivityLogService = userActivityLogService;
    }

    /**
     * Get the current authenticated user's profile.
     */
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return userService.getMe(principal.id());
    }

    /**
     * Update the current authenticated user's profile fields.
     */
    @PatchMapping("/me")
    public UserResponse updateMe(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateMeRequest request
    ) {
        return userService.updateMe(principal.id(), request);
    }

    @Operation(summary = "Update my avatar", description = "Upload avatar image for current user.")
    @PatchMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserResponse updateMyAvatar(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file
    ) {
        return userService.updateMyAvatar(principal.id(), file);
    }

    /**
     * Get current user's activity history.
     */
    @GetMapping("/me/activity-logs")
    public Page<UserActivityLogResponse> myActivityLogs(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) UserActivityType activityType,
            @RequestParam(required = false) UserActivityTargetType targetType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @ParameterObject Pageable pageable
    ) {
        return userActivityLogService.listMyActivities(principal.id(), activityType, targetType, from, to, pageable);
    }
}
