package com.learnapp.controller;

import com.learnapp.dto.AdminUserFeedbackDetailResponse;
import com.learnapp.dto.AdminUserFeedbackQueueItemResponse;
import com.learnapp.entities.UserFeedbackCategory;
import com.learnapp.entities.UserFeedbackStatus;
import com.learnapp.security.UserPrincipal;
import com.learnapp.service.UserFeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/admin/feedback")
@Tag(name = "Admin Feedback", description = "Admin inbox for user feedback")
public class AdminFeedbackController {

    private final UserFeedbackService userFeedbackService;

    public AdminFeedbackController(UserFeedbackService userFeedbackService) {
        this.userFeedbackService = userFeedbackService;
    }

    @Operation(summary = "Search feedback inbox", description = "List feedback items for the admin inbox.")
    @GetMapping
    public Page<AdminUserFeedbackQueueItemResponse> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) UserFeedbackStatus status,
            @RequestParam(required = false) UserFeedbackCategory category,
            @ParameterObject Pageable pageable
    ) {
        return userFeedbackService.searchForAdmin(query, status, category, pageable);
    }

    @Operation(summary = "Get feedback detail", description = "Get full detail for an inbox feedback item.")
    @GetMapping("/{id}")
    public AdminUserFeedbackDetailResponse getDetail(@PathVariable UUID id) {
        return userFeedbackService.getDetailForAdmin(id);
    }

    @Operation(summary = "Mark feedback as read", description = "Mark a feedback item as read in the admin inbox.")
    @PatchMapping("/{id}/read")
    public AdminUserFeedbackDetailResponse markRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        return userFeedbackService.markRead(id, principal.id());
    }

    @Operation(summary = "Archive feedback", description = "Archive a feedback item in the admin inbox.")
    @PatchMapping("/{id}/archive")
    public AdminUserFeedbackDetailResponse archive(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        return userFeedbackService.archive(id, principal.id());
    }
}
