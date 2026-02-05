package com.learnapp.controller;

import com.learnapp.dto.CreateTopicRequest;
import com.learnapp.dto.TopicResponse;
import com.learnapp.dto.UpdateTopicRequest;
import com.learnapp.entities.TopicStatus;
import com.learnapp.service.TopicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/admin/topics")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Topics", description = "Admin topic management APIs")
public class AdminTopicController {

    private final TopicService topicService;

    public AdminTopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    /**
     * Search topics (non-deleted). Admin-only.
     */
    @Operation(summary = "Search topics", description = "Search topics by name/slug/status.")
    @GetMapping
    public Page<TopicResponse> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String slug,
            @RequestParam(required = false) TopicStatus status,
            @ParameterObject Pageable pageable
    ) {
        return topicService.searchTopics(name, slug, status, pageable);
    }

    /**
     * Export topics to CSV. Admin-only.
     */
    @Operation(summary = "Export topics", description = "Export topics to CSV with the same filters as search.")
    @GetMapping(value = "/export", produces = "text/csv")
    public org.springframework.http.ResponseEntity<String> export(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String slug,
            @RequestParam(required = false) TopicStatus status
    ) {
        String csv = buildCsv(topicService.exportTopics(name, slug, status));
        return org.springframework.http.ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"topics.csv\"")
                .header("Content-Type", "text/csv; charset=utf-8")
                .body(csv);
    }

    /**
     * Create a topic. Admin-only.
     */
    @Operation(summary = "Create topic", description = "Create a new topic.")
    @PostMapping
    public TopicResponse create(@Valid @RequestBody CreateTopicRequest request) {
        return topicService.createTopic(request);
    }

    /**
     * Update a topic. Admin-only.
     */
    @Operation(summary = "Update topic", description = "Update topic fields.")
    @PatchMapping("/{id}")
    public TopicResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateTopicRequest request) {
        return topicService.updateTopic(id, request);
    }

    /**
     * Delete a topic (soft delete). Admin-only.
     */
    @Operation(summary = "Delete topic", description = "Soft delete a topic.")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        topicService.deleteTopic(id);
    }

    private String buildCsv(java.util.List<TopicResponse> topics) {
        StringBuilder builder = new StringBuilder();
        // UTF-8 BOM for Excel compatibility
        builder.append('\uFEFF');
        builder.append("id,name,slug,description,createdAt").append("\n");
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        for (TopicResponse topic : topics) {
            builder.append(escapeCsv(valueOrEmpty(topic.id()))).append(",")
                    .append(escapeCsv(topic.name())).append(",")
                    .append(escapeCsv(topic.slug())).append(",")
                    .append(escapeCsv(topic.description())).append(",")
                    .append(escapeCsv(formatDate(topic.createdAt(), formatter)))
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
