package com.learnapp.controller;

import com.learnapp.dto.CreateUserFeedbackRequest;
import com.learnapp.dto.UserFeedbackResponse;
import com.learnapp.security.UserPrincipal;
import com.learnapp.service.UserFeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping
@Tag(name = "Feedback", description = "User feedback submission and history APIs")
public class FeedbackController {

    private final UserFeedbackService userFeedbackService;

    public FeedbackController(UserFeedbackService userFeedbackService) {
        this.userFeedbackService = userFeedbackService;
    }

    @Operation(summary = "Submit feedback", description = "Submit a new feedback entry with optional image attachments.")
    @PostMapping(value = "/feedback", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserFeedbackResponse submit(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @ModelAttribute CreateUserFeedbackRequest request
    ) {
        return userFeedbackService.create(principal.id(), request);
    }

    @Operation(summary = "List my feedback", description = "List feedback submitted by the current user.")
    @GetMapping("/me/feedback")
    public Page<UserFeedbackResponse> listMine(
            @AuthenticationPrincipal UserPrincipal principal,
            @ParameterObject Pageable pageable
    ) {
        return userFeedbackService.listMine(principal.id(), pageable);
    }
}
