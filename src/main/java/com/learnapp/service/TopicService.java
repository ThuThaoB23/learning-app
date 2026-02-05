package com.learnapp.service;

import com.learnapp.dto.CreateTopicRequest;
import com.learnapp.dto.TopicResponse;
import com.learnapp.dto.UpdateTopicRequest;
import com.learnapp.entities.Topic;
import com.learnapp.entities.TopicStatus;
import com.learnapp.error.AppException;
import com.learnapp.repository.TopicRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TopicService {

    private final TopicRepository topicRepository;

    public TopicService(TopicRepository topicRepository) {
        this.topicRepository = topicRepository;
    }

    @Transactional(readOnly = true)
    public Page<Topic> listActive(Pageable pageable) {
        return topicRepository.findByStatusAndDeletedAtIsNull(TopicStatus.ACTIVE, pageable);
    }

    @Transactional(readOnly = true)
    public Topic getActiveById(UUID id) {
        Topic topic = topicRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "TOPIC_NOT_FOUND", "Topic not found"));
        if (topic.getStatus() != TopicStatus.ACTIVE) {
            throw new AppException(HttpStatus.NOT_FOUND, "TOPIC_NOT_FOUND", "Topic not found");
        }
        return topic;
    }

    @Transactional(readOnly = true)
    public Page<TopicResponse> searchTopics(
            String name,
            String slug,
            TopicStatus status,
            Pageable pageable
    ) {
        return topicRepository.searchTopics(
                normalizeSearch(name),
                normalizeSearch(slug),
                status,
                pageable
        ).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<TopicResponse> exportTopics(String name, String slug, TopicStatus status) {
        return topicRepository.searchTopics(
                normalizeSearch(name),
                normalizeSearch(slug),
                status,
                Pageable.unpaged()
        ).map(this::toResponse).getContent();
    }

    public TopicResponse createTopic(CreateTopicRequest request) {
        String name = request.name().trim();
        if (topicRepository.existsByNameIgnoreCase(name)) {
            throw new AppException(HttpStatus.CONFLICT, "TOPIC_NAME_EXISTS", "Name already exists");
        }
        String slug = generateUniqueSlug(name, null);
        Topic topic = Topic.builder()
                .name(name)
                .slug(slug)
                .description(request.description() == null ? null : request.description().trim())
                .status(TopicStatus.ACTIVE)
                .build();
        topic = topicRepository.save(topic);
        return toResponse(topic);
    }

    public TopicResponse updateTopic(UUID id, UpdateTopicRequest request) {
        Topic topic = topicRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "TOPIC_NOT_FOUND", "Topic not found"));

        if (request.name() != null) {
            String name = request.name().trim();
            if (!name.equalsIgnoreCase(topic.getName()) && topicRepository.existsByNameIgnoreCase(name)) {
                throw new AppException(HttpStatus.CONFLICT, "TOPIC_NAME_EXISTS", "Name already exists");
            }
            topic.setName(name);
            topic.setSlug(generateUniqueSlug(name, topic.getId()));
        }
        if (request.description() != null) {
            topic.setDescription(request.description().trim());
        }
        if (request.status() != null) {
            topic.setStatus(request.status());
        }

        topic = topicRepository.save(topic);
        return toResponse(topic);
    }

    public void deleteTopic(UUID id) {
        Topic topic = topicRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "TOPIC_NOT_FOUND", "Topic not found"));
        topic.setDeletedAt(LocalDateTime.now());
        topicRepository.save(topic);
    }

    private TopicResponse toResponse(Topic topic) {
        return new TopicResponse(
                topic.getId(),
                topic.getName(),
                topic.getSlug(),
                topic.getDescription(),
                topic.getCreatedAt()
        );
    }

    private String generateUniqueSlug(String name, UUID currentId) {
        String base = normalizeSlugFromName(name);
        String slug = base;
        int suffix = 1;
        while (topicRepository.existsBySlug(slug)) {
            if (currentId != null) {
                Topic existing = topicRepository.findBySlugAndDeletedAtIsNull(slug).orElse(null);
                if (existing != null && existing.getId().equals(currentId)) {
                    return slug;
                }
            }
            slug = base + "-" + suffix;
            suffix += 1;
        }
        return slug;
    }

    private String normalizeSlugFromName(String name) {
        String normalized = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("[^a-z0-9]+", "-");
        normalized = normalized.replaceAll("(^-|-$)", "");
        if (normalized.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_NAME", "Name is required");
        }
        return normalized;
    }

    private String normalizeSearch(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        return trimmed.isEmpty() ? null : trimmed;
    }
}
