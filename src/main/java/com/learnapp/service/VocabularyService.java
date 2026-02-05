package com.learnapp.service;

import com.learnapp.entities.Topic;
import com.learnapp.entities.TopicStatus;
import com.learnapp.entities.TopicVocabulary;
import com.learnapp.entities.Vocabulary;
import com.learnapp.entities.VocabularyStatus;
import com.learnapp.error.AppException;
import com.learnapp.repository.TopicRepository;
import com.learnapp.repository.TopicVocabularyRepository;
import com.learnapp.repository.VocabularyRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VocabularyService {

    private final VocabularyRepository vocabularyRepository;
    private final TopicRepository topicRepository;
    private final TopicVocabularyRepository topicVocabularyRepository;

    public VocabularyService(
            VocabularyRepository vocabularyRepository,
            TopicRepository topicRepository,
            TopicVocabularyRepository topicVocabularyRepository
    ) {
        this.vocabularyRepository = vocabularyRepository;
        this.topicRepository = topicRepository;
        this.topicVocabularyRepository = topicVocabularyRepository;
    }

    @Transactional(readOnly = true)
    public Page<Vocabulary> searchApproved(
            String query,
            UUID topicId,
            String language,
            VocabularyStatus status,
            Pageable pageable
    ) {
        String normalizedQuery = normalizeTerm(query);
        String normalizedLanguage = normalizeLanguage(language);

        if (topicId != null) {
            return vocabularyRepository.searchByTopic(
                    topicId,
                    status,
                    normalizedLanguage,
                    normalizedQuery,
                    pageable
            );
        }

        if (status != null && normalizedQuery != null && normalizedLanguage != null) {
            return vocabularyRepository.findByStatusAndDeletedAtIsNullAndLanguageAndTermNormalizedContainingIgnoreCase(
                    status,
                    normalizedLanguage,
                    normalizedQuery,
                    pageable
            );
        }

        if (status != null && normalizedQuery != null) {
            return vocabularyRepository.findByStatusAndDeletedAtIsNullAndTermNormalizedContainingIgnoreCase(
                    status,
                    normalizedQuery,
                    pageable
            );
        }

        if (status != null && normalizedLanguage != null) {
            return vocabularyRepository.findByStatusAndDeletedAtIsNullAndLanguage(
                    status,
                    normalizedLanguage,
                    pageable
            );
        }

        if (status != null) {
            return vocabularyRepository.findByStatusAndDeletedAtIsNull(status, pageable);
        }

        if (normalizedQuery != null && normalizedLanguage != null) {
            return vocabularyRepository.findByDeletedAtIsNullAndLanguageAndTermNormalizedContainingIgnoreCase(
                    normalizedLanguage,
                    normalizedQuery,
                    pageable
            );
        }

        if (normalizedQuery != null) {
            return vocabularyRepository.findByDeletedAtIsNullAndTermNormalizedContainingIgnoreCase(
                    normalizedQuery,
                    pageable
            );
        }

        if (normalizedLanguage != null) {
            return vocabularyRepository.findByDeletedAtIsNullAndLanguage(
                    normalizedLanguage,
                    pageable
            );
        }

        return vocabularyRepository.findByDeletedAtIsNull(pageable);
    }

    @Transactional(readOnly = true)
    public Vocabulary getApproved(UUID id) {
        return vocabularyRepository.findByIdAndStatusAndDeletedAtIsNull(id, VocabularyStatus.APPROVED)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "VOCAB_NOT_FOUND", "Vocabulary not found"));
    }

    public Vocabulary createContribution(
            UUID userId,
            String term,
            String definition,
            String example,
            String phonetic,
            String partOfSpeech,
            String language,
            List<UUID> topicIds
    ) {
        String normalizedTerm = normalizeTerm(term);
        if (normalizedTerm == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_TERM", "Term is required");
        }
        String normalizedLanguage = normalizeLanguage(language);
        if (normalizedLanguage == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_LANGUAGE", "Language is required");
        }
        if (definition == null || definition.trim().isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_DEFINITION", "Definition is required");
        }

        vocabularyRepository.findByTermNormalizedAndLanguageAndDeletedAtIsNull(normalizedTerm, normalizedLanguage)
                .ifPresent(existing -> {
                    throw new AppException(HttpStatus.CONFLICT, "VOCAB_EXISTS", "Vocabulary already exists");
                });

        Vocabulary vocabulary = Vocabulary.builder()
                .term(term.trim())
                .termNormalized(normalizedTerm)
                .definition(definition.trim())
                .example(example == null ? null : example.trim())
                .phonetic(phonetic == null ? null : phonetic.trim())
                .partOfSpeech(partOfSpeech == null ? null : partOfSpeech.trim())
                .language(normalizedLanguage)
                .status(VocabularyStatus.PENDING)
                .createdBy(userId)
                .build();

        vocabulary = vocabularyRepository.save(vocabulary);

        if (topicIds != null && !topicIds.isEmpty()) {
            List<TopicVocabulary> links = buildTopicLinks(vocabulary.getId(), topicIds);
            topicVocabularyRepository.saveAll(links);
        }

        return vocabulary;
    }

    public Vocabulary approve(UUID id) {
        Vocabulary vocabulary = vocabularyRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "VOCAB_NOT_FOUND", "Vocabulary not found"));
        vocabulary.setStatus(VocabularyStatus.APPROVED);
        return vocabularyRepository.save(vocabulary);
    }

    public Vocabulary reject(UUID id) {
        Vocabulary vocabulary = vocabularyRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "VOCAB_NOT_FOUND", "Vocabulary not found"));
        vocabulary.setStatus(VocabularyStatus.REJECTED);
        return vocabularyRepository.save(vocabulary);
    }

    private List<TopicVocabulary> buildTopicLinks(UUID vocabularyId, List<UUID> topicIds) {
        Set<UUID> uniqueTopicIds = new HashSet<>(topicIds);
        List<TopicVocabulary> links = new ArrayList<>();
        for (UUID topicId : uniqueTopicIds) {
            Topic topic = topicRepository.findByIdAndDeletedAtIsNull(topicId)
                    .orElseThrow(() -> new AppException(
                            HttpStatus.NOT_FOUND,
                            "TOPIC_NOT_FOUND",
                            "Topic not found"
                    ));
            if (topic.getStatus() != TopicStatus.ACTIVE) {
                throw new AppException(HttpStatus.BAD_REQUEST, "TOPIC_INACTIVE", "Topic is inactive");
            }
            links.add(TopicVocabulary.builder()
                    .topicId(topicId)
                    .vocabularyId(vocabularyId)
                    .build());
        }
        return links;
    }

    private String normalizeTerm(String term) {
        if (term == null) {
            return null;
        }
        String trimmed = term.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private String normalizeLanguage(String language) {
        if (language == null) {
            return null;
        }
        String trimmed = language.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }
}
