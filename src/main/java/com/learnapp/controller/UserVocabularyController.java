package com.learnapp.controller;

import com.learnapp.dto.AddUserVocabularyRequest;
import com.learnapp.dto.UpdateUserVocabularyRequest;
import com.learnapp.dto.UserVocabularyResponse;
import com.learnapp.entities.UserVocabStatus;
import com.learnapp.entities.UserVocabulary;
import com.learnapp.security.UserPrincipal;
import com.learnapp.service.UserVocabularyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me/vocab")
@Tag(name = "User Vocabulary", description = "Personal vocabulary list APIs")
public class UserVocabularyController {

    private final UserVocabularyService userVocabularyService;

    public UserVocabularyController(UserVocabularyService userVocabularyService) {
        this.userVocabularyService = userVocabularyService;
    }

    /**
     * List vocabularies in current user's learning list.
     */
    @Operation(summary = "List my vocab", description = "List vocabularies in the user's learning list.")
    @GetMapping
    public Page<UserVocabularyResponse> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) UserVocabStatus status,
            @ParameterObject Pageable pageable
    ) {
        return userVocabularyService.list(principal.id(), status, pageable).map(this::toResponse);
    }

    /**
     * Add a vocabulary to the current user's learning list.
     */
    @Operation(summary = "Add to my vocab", description = "Add a vocabulary to the user's learning list.")
    @PostMapping
    public UserVocabularyResponse add(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddUserVocabularyRequest request
    ) {
        return toResponse(userVocabularyService.add(principal.id(), request.vocabularyId()));
    }

    /**
     * Update learning status/progress for a vocabulary in the user's list.
     */
    @Operation(summary = "Update my vocab", description = "Update status/progress for a vocabulary in the user's list.")
    @PatchMapping("/{vocabularyId}")
    public UserVocabularyResponse update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID vocabularyId,
            @Valid @RequestBody UpdateUserVocabularyRequest request
    ) {
        return toResponse(userVocabularyService.update(
                principal.id(),
                vocabularyId,
                request.status(),
                request.progress(),
                LocalDateTime.now()
        ));
    }

    /**
     * Remove a vocabulary from the user's learning list.
     */
    @Operation(summary = "Remove from my vocab", description = "Remove a vocabulary from the user's list.")
    @DeleteMapping("/{vocabularyId}")
    public void remove(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID vocabularyId
    ) {
        userVocabularyService.remove(principal.id(), vocabularyId);
    }

    private UserVocabularyResponse toResponse(UserVocabulary userVocabulary) {
        return new UserVocabularyResponse(
                userVocabulary.getVocabularyId(),
                userVocabulary.getStatus(),
                userVocabulary.getProgress(),
                userVocabulary.getLastReviewedAt(),
                userVocabulary.getCreatedAt(),
                userVocabulary.getUpdatedAt()
        );
    }
}
