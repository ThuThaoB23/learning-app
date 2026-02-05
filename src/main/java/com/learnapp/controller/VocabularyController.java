package com.learnapp.controller;

import com.learnapp.dto.CreateVocabularyRequest;
import com.learnapp.dto.VocabularyResponse;
import com.learnapp.entities.Vocabulary;
import com.learnapp.entities.VocabularyStatus;
import com.learnapp.security.UserPrincipal;
import com.learnapp.service.VocabularyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/vocab")
@Tag(name = "Vocabulary", description = "Vocabulary search and contribution APIs")
public class VocabularyController {

    private final VocabularyService vocabularyService;

    public VocabularyController(VocabularyService vocabularyService) {
        this.vocabularyService = vocabularyService;
    }

    /**
     * Search approved vocabularies.
     */
    @Operation(summary = "Search vocab", description = "Search approved vocabularies by query, topic, or language.")
    @GetMapping
    public Page<VocabularyResponse> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) UUID topicId,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) VocabularyStatus status,
            @ParameterObject Pageable pageable
    ) {
        return vocabularyService.searchApproved(query, topicId, language, status, pageable).map(this::toResponse);
    }

    /**
     * Get an approved vocabulary by id.
     */
    @Operation(summary = "Get vocab", description = "Get a single approved vocabulary by id.")
    @GetMapping("/{id}")
    public VocabularyResponse getById(@PathVariable UUID id) {
        return toResponse(vocabularyService.getApproved(id));
    }

    /**
     * Submit a new vocabulary contribution.
     */
    @Operation(summary = "Contribute vocab", description = "Submit a new vocabulary entry for review.")
    @PostMapping("/contributions")
    public VocabularyResponse contribute(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateVocabularyRequest request
    ) {
        Vocabulary vocabulary = vocabularyService.createContribution(
                principal.id(),
                request.term(),
                request.definition(),
                request.example(),
                request.phonetic(),
                request.partOfSpeech(),
                request.language(),
                request.topicIds()
        );
        return toResponse(vocabulary);
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
