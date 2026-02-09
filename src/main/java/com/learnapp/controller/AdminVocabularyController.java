package com.learnapp.controller;

import com.learnapp.dto.UpdateVocabularyRequest;
import com.learnapp.dto.VocabularyResponse;
import com.learnapp.service.VocabularyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/vocab")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Vocabulary", description = "Admin vocab moderation APIs")
public class AdminVocabularyController {

    private final VocabularyService vocabularyService;

    public AdminVocabularyController(VocabularyService vocabularyService) {
        this.vocabularyService = vocabularyService;
    }

    /**
     * Approve a vocabulary contribution.
     */
    @Operation(summary = "Approve vocab", description = "Approve a pending vocabulary contribution.")
    @PatchMapping("/{id}/approve")
    public VocabularyResponse approve(@PathVariable UUID id) {
        return vocabularyService.approve(id);
    }

    /**
     * Reject a vocabulary contribution.
     */
    @Operation(summary = "Reject vocab", description = "Reject a pending vocabulary contribution.")
    @PatchMapping("/{id}/reject")
    public VocabularyResponse reject(@PathVariable UUID id) {
        return vocabularyService.reject(id);
    }

    /**
     * Update a vocabulary entry.
     */
    @Operation(summary = "Update vocab", description = "Update vocabulary fields.")
    @PatchMapping("/{id}")
    public VocabularyResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateVocabularyRequest request) {
        return vocabularyService.updateVocabulary(id, request);
    }

    /**
     * Delete a vocabulary entry (soft delete).
     */
    @Operation(summary = "Delete vocab", description = "Soft delete a vocabulary entry.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        vocabularyService.deleteVocabulary(id);
        return ResponseEntity.noContent().build();
    }
}
