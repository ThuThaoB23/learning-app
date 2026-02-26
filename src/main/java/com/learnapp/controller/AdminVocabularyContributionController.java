package com.learnapp.controller;

import com.learnapp.dto.AdminVocabularyContributionDetailResponse;
import com.learnapp.dto.AdminVocabularyContributionQueueItemResponse;
import com.learnapp.dto.ApproveVocabularyContributionRequest;
import com.learnapp.dto.RejectVocabularyContributionRequest;
import com.learnapp.dto.VocabularyContributionResponse;
import com.learnapp.entities.VocabularyContributionStatus;
import com.learnapp.security.UserPrincipal;
import com.learnapp.service.VocabularyContributionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/vocab-contributions")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Vocabulary Contributions", description = "Admin review queue for user vocabulary contributions")
public class AdminVocabularyContributionController {

    private final VocabularyContributionService vocabularyContributionService;

    public AdminVocabularyContributionController(VocabularyContributionService vocabularyContributionService) {
        this.vocabularyContributionService = vocabularyContributionService;
    }

    @Operation(summary = "Search contribution queue", description = "List vocabulary contributions for admin review.")
    @GetMapping
    public Page<AdminVocabularyContributionQueueItemResponse> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) VocabularyContributionStatus status,
            @ParameterObject Pageable pageable
    ) {
        return vocabularyContributionService.searchForAdmin(query, language, status, pageable);
    }

    @Operation(summary = "Get contribution detail", description = "Get a vocabulary contribution with examples, topics, and review logs.")
    @GetMapping("/{id}")
    public AdminVocabularyContributionDetailResponse getDetail(@PathVariable UUID id) {
        return vocabularyContributionService.getDetailForAdmin(id);
    }

    @Operation(summary = "Approve contribution", description = "Approve a contribution and create an approved vocabulary entry.")
    @PatchMapping("/{id}/approve")
    public VocabularyContributionResponse approve(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) ApproveVocabularyContributionRequest request
    ) {
        String reviewNote = request == null ? null : request.reviewNote();
        return vocabularyContributionService.approve(id, principal.id(), reviewNote);
    }

    @Operation(summary = "Reject contribution", description = "Reject a contribution with reason/note.")
    @PatchMapping("/{id}/reject")
    public VocabularyContributionResponse reject(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody RejectVocabularyContributionRequest request
    ) {
        return vocabularyContributionService.reject(id, principal.id(), request);
    }
}
