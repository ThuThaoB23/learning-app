package com.learnapp.controller;

import com.learnapp.dto.UserActivityLogResponse;
import com.learnapp.entities.UserActivityTargetType;
import com.learnapp.entities.UserActivityType;
import com.learnapp.service.UserActivityLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/activity-logs")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Activity Logs", description = "Admin APIs for viewing user activity logs")
public class AdminActivityLogController {

    private final UserActivityLogService userActivityLogService;

    public AdminActivityLogController(UserActivityLogService userActivityLogService) {
        this.userActivityLogService = userActivityLogService;
    }

    @Operation(summary = "List all activity logs", description = "List activity logs across all users with optional filters.")
    @GetMapping
    public Page<UserActivityLogResponse> listAll(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) UserActivityType activityType,
            @RequestParam(required = false) UserActivityTargetType targetType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @ParameterObject Pageable pageable
    ) {
        return userActivityLogService.listActivitiesForAdmin(userId, activityType, targetType, from, to, pageable);
    }
}
