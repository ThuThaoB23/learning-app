package com.learnapp.controller;

import com.learnapp.dto.VocabularyResponse;
import com.learnapp.entities.Vocabulary;
import com.learnapp.service.VocabularyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
        return toResponse(vocabularyService.approve(id));
    }

    /**
     * Reject a vocabulary contribution.
     */
    @Operation(summary = "Reject vocab", description = "Reject a pending vocabulary contribution.")
    @PatchMapping("/{id}/reject")
    public VocabularyResponse reject(@PathVariable UUID id) {
        return toResponse(vocabularyService.reject(id));
    }

    private VocabularyResponse toResponse(Vocabulary vocabulary) {
        return new VocabularyResponse(
                vocabulary.getId(),
                vocabulary.getTerm(),
                vocabulary.getDefinition(),
                vocabulary.getExample(),
                vocabulary.getPhonetic(),
                vocabulary.getPartOfSpeech(),
                vocabulary.getLanguage(),
                vocabulary.getStatus(),
                vocabulary.getCreatedBy(),
                vocabulary.getCreatedAt()
        );
    }
}
