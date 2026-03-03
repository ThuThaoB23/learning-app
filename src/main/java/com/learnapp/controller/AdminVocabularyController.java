package com.learnapp.controller;

import com.learnapp.dto.UpdateVocabularyRequest;
import com.learnapp.dto.VocabularyAudioBackfillResponse;
import com.learnapp.dto.VocabularyDetailResponse;
import com.learnapp.dto.VocabularyImportResultResponse;
import com.learnapp.dto.VocabularyResponse;
import com.learnapp.entities.VocabularyStatus;
import com.learnapp.service.VocabularyAudioService;
import com.learnapp.service.VocabularyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/vocab")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Vocabulary", description = "Admin vocab moderation APIs")
public class AdminVocabularyController {

    private static final Logger log = LoggerFactory.getLogger(AdminVocabularyController.class);

    private final VocabularyService vocabularyService;
    private final VocabularyAudioService vocabularyAudioService;

    public AdminVocabularyController(VocabularyService vocabularyService, VocabularyAudioService vocabularyAudioService) {
        this.vocabularyService = vocabularyService;
        this.vocabularyAudioService = vocabularyAudioService;
    }

    /**
     * Search vocabularies for moderation.
     */
    @Operation(summary = "Search vocab", description = "Search vocabularies by query, topic, language, and status.")
    @GetMapping
    public Page<VocabularyResponse> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) UUID topicId,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) VocabularyStatus status,
            @ParameterObject Pageable pageable
    ) {
        return vocabularyService.searchApproved(query, topicId, language, status, pageable);
    }

    /**
     * Export vocabularies to CSV.
     */
    @Operation(summary = "Export vocab", description = "Export vocabularies to CSV with the same filters as search.")
    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<String> export(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) UUID topicId,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) VocabularyStatus status
    ) {
        String csv = buildCsv(vocabularyService.exportVocabularies(query, topicId, language, status));
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"vocabularies.csv\"")
                .header("Content-Type", "text/csv; charset=utf-8")
                .body(csv);
    }

    /**
     * Get vocabulary detail.
     */
    @Operation(summary = "Get vocab detail", description = "Get vocabulary detail by id with linked topic ids.")
    @GetMapping("/{id}")
    public VocabularyDetailResponse getById(@PathVariable UUID id) {
        return vocabularyService.getDetail(id);
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
     * Refresh audio for a vocabulary entry.
     */
    @Operation(summary = "Refresh vocab audio", description = "Fetch and update audio for a single existing vocabulary.")
    @PostMapping("/{id}/audio/refresh")
    public VocabularyResponse refreshAudio(@PathVariable UUID id) {
        log.info("Admin refresh vocabulary audio: id={}", id);
        return vocabularyService.refreshVocabularyAudio(id);
    }

    @Operation(summary = "Delete vocab audio", description = "Delete a single audio file from an existing vocabulary.")
    @DeleteMapping("/{id}/audio/{audioId}")
    public ResponseEntity<Void> deleteAudio(@PathVariable UUID id, @PathVariable UUID audioId) {
        log.info("Admin delete vocabulary audio: vocabularyId={}, audioId={}", id, audioId);
        vocabularyService.deleteVocabularyAudio(id, audioId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Update a vocabulary entry.
     */
    @Operation(summary = "Update vocab", description = "Update vocabulary fields.")
    @PatchMapping("/{id}")
    public VocabularyResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateVocabularyRequest request) {
        log.info("Admin update vocabulary request: id={}, body={}", id, request);
        return vocabularyService.updateVocabulary(id, request);
    }

    /**
     * Import vocabularies from CSV.
     */
    @Operation(summary = "Import vocab CSV", description = "Import vocabularies from a CSV file.")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VocabularyImportResultResponse importCsv(@RequestParam("file") MultipartFile file) {
        log.info("Admin import vocabulary CSV: filename={}, size={}", file.getOriginalFilename(), file.getSize());
        return vocabularyService.importFromCsv(file);
    }

    /**
     * Backfill audio for old vocabularies.
     */
    @Operation(summary = "Backfill vocab audio", description = "Fetch and store audio for existing vocabularies in batches.")
    @PostMapping("/audio/backfill")
    public VocabularyAudioBackfillResponse backfillAudio(
            @RequestParam(defaultValue = "en") String language,
            @RequestParam(required = false) VocabularyStatus status,
            @RequestParam(defaultValue = "false") boolean forceRefresh,
            @RequestParam(defaultValue = "100") Integer batchSize,
            @RequestParam(required = false) Integer limit
    ) {
        log.info(
                "Admin backfill vocabulary audio: language={}, status={}, forceRefresh={}, batchSize={}, limit={}",
                language,
                status,
                forceRefresh,
                batchSize,
                limit
        );
        return vocabularyAudioService.backfillAudios(language, status, forceRefresh, batchSize, limit);
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

    private String buildCsv(java.util.List<VocabularyResponse> vocabularies) {
        StringBuilder builder = new StringBuilder();
        // UTF-8 BOM for Excel compatibility
        builder.append('\uFEFF');
        builder.append("id,term,definition,definitionVi,examples,phonetic,partOfSpeech,language,status,createdBy,createdAt")
                .append("\n");
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        for (VocabularyResponse vocabulary : vocabularies) {
            String examples = vocabulary.examples() == null ? "" : String.join("|", vocabulary.examples());
            builder.append(escapeCsv(valueOrEmpty(vocabulary.id()))).append(",")
                    .append(escapeCsv(vocabulary.term())).append(",")
                    .append(escapeCsv(vocabulary.definition())).append(",")
                    .append(escapeCsv(vocabulary.definitionVi())).append(",")
                    .append(escapeCsv(examples)).append(",")
                    .append(escapeCsv(vocabulary.phonetic())).append(",")
                    .append(escapeCsv(vocabulary.partOfSpeech())).append(",")
                    .append(escapeCsv(vocabulary.language())).append(",")
                    .append(escapeCsv(valueOrEmpty(vocabulary.status()))).append(",")
                    .append(escapeCsv(valueOrEmpty(vocabulary.createdBy()))).append(",")
                    .append(escapeCsv(formatDate(vocabulary.createdAt(), formatter)))
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
