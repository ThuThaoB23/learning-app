package com.learnapp.controller;

import com.learnapp.dto.TopicResponse;
import com.learnapp.dto.VocabularyResponse;
import com.learnapp.entities.Topic;
import com.learnapp.entities.Vocabulary;
import com.learnapp.service.TopicService;
import com.learnapp.service.VocabularyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/topics")
@Tag(name = "Topics", description = "Topic browsing APIs")
public class TopicController {

    private final TopicService topicService;
    private final VocabularyService vocabularyService;

    public TopicController(TopicService topicService, VocabularyService vocabularyService) {
        this.topicService = topicService;
        this.vocabularyService = vocabularyService;
    }

    /**
     * List active topics.
     */
    @Operation(summary = "List topics", description = "List active vocabulary topics.")
    @GetMapping
    public Page<TopicResponse> listTopics(@ParameterObject Pageable pageable) {
        return topicService.listActive(pageable).map(this::toTopicResponse);
    }

    /**
     * Get a topic by id.
     */
    @Operation(summary = "Get topic", description = "Get an active topic by id.")
    @GetMapping("/{id}")
    public TopicResponse getTopic(@PathVariable UUID id) {
        return toTopicResponse(topicService.getActiveById(id));
    }

    /**
     * List approved vocabularies in a topic.
     */
    @Operation(summary = "List vocab in topic", description = "List approved vocabularies under a topic.")
    @GetMapping("/{id}/vocab")
    public Page<VocabularyResponse> listTopicVocabulary(
            @PathVariable UUID id,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String language,
            @ParameterObject Pageable pageable
    ) {
        return vocabularyService.searchApproved(query, id, language, pageable).map(this::toVocabularyResponse);
    }

    private TopicResponse toTopicResponse(Topic topic) {
        return new TopicResponse(
                topic.getId(),
                topic.getName(),
                topic.getSlug(),
                topic.getDescription(),
                topic.getCreatedAt()
        );
    }

    private VocabularyResponse toVocabularyResponse(Vocabulary vocabulary) {
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
