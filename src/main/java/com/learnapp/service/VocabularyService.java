package com.learnapp.service;

import com.learnapp.dto.UpdateVocabularyRequest;
import com.learnapp.dto.VocabularyResponse;
import com.learnapp.entities.Topic;
import com.learnapp.entities.TopicStatus;
import com.learnapp.entities.TopicVocabulary;
import com.learnapp.entities.Vocabulary;
import com.learnapp.entities.VocabularyExample;
import com.learnapp.entities.VocabularyStatus;
import com.learnapp.error.AppException;
import com.learnapp.repository.TopicRepository;
import com.learnapp.repository.TopicVocabularyRepository;
import com.learnapp.repository.VocabularyExampleRepository;
import com.learnapp.repository.VocabularyRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
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
    private final VocabularyExampleRepository vocabularyExampleRepository;

    public VocabularyService(
            VocabularyRepository vocabularyRepository,
            TopicRepository topicRepository,
            TopicVocabularyRepository topicVocabularyRepository,
            VocabularyExampleRepository vocabularyExampleRepository
    ) {
        this.vocabularyRepository = vocabularyRepository;
        this.topicRepository = topicRepository;
        this.topicVocabularyRepository = topicVocabularyRepository;
        this.vocabularyExampleRepository = vocabularyExampleRepository;
    }

    @Transactional(readOnly = true)
    public Page<VocabularyResponse> searchApproved(
            String query,
            UUID topicId,
            String language,
            VocabularyStatus status,
            Pageable pageable
    ) {
        String normalizedQuery = normalizeTerm(query);
        String normalizedLanguage = normalizeLanguage(language);

        if (topicId != null) {
            Page<Vocabulary> page = vocabularyRepository.searchByTopic(
                    topicId,
                    status,
                    normalizedLanguage,
                    normalizedQuery,
                    pageable
            );
            return toResponses(page);
        }

        if (status != null && normalizedQuery != null && normalizedLanguage != null) {
            Page<Vocabulary> page = vocabularyRepository.findByStatusAndDeletedAtIsNullAndLanguageAndTermNormalizedContainingIgnoreCase(
                    status,
                    normalizedLanguage,
                    normalizedQuery,
                    pageable
            );
            return toResponses(page);
        }

        if (status != null && normalizedQuery != null) {
            Page<Vocabulary> page = vocabularyRepository.findByStatusAndDeletedAtIsNullAndTermNormalizedContainingIgnoreCase(
                    status,
                    normalizedQuery,
                    pageable
            );
            return toResponses(page);
        }

        if (status != null && normalizedLanguage != null) {
            Page<Vocabulary> page = vocabularyRepository.findByStatusAndDeletedAtIsNullAndLanguage(
                    status,
                    normalizedLanguage,
                    pageable
            );
            return toResponses(page);
        }

        if (status != null) {
            Page<Vocabulary> page = vocabularyRepository.findByStatusAndDeletedAtIsNull(status, pageable);
            return toResponses(page);
        }

        if (normalizedQuery != null && normalizedLanguage != null) {
            Page<Vocabulary> page = vocabularyRepository.findByDeletedAtIsNullAndLanguageAndTermNormalizedContainingIgnoreCase(
                    normalizedLanguage,
                    normalizedQuery,
                    pageable
            );
            return toResponses(page);
        }

        if (normalizedQuery != null) {
            Page<Vocabulary> page = vocabularyRepository.findByDeletedAtIsNullAndTermNormalizedContainingIgnoreCase(
                    normalizedQuery,
                    pageable
            );
            return toResponses(page);
        }

        if (normalizedLanguage != null) {
            Page<Vocabulary> page = vocabularyRepository.findByDeletedAtIsNullAndLanguage(
                    normalizedLanguage,
                    pageable
            );
            return toResponses(page);
        }

        return toResponses(vocabularyRepository.findByDeletedAtIsNull(pageable));
    }

    @Transactional(readOnly = true)
    public VocabularyResponse getApproved(UUID id) {
        Vocabulary vocabulary = vocabularyRepository.findByIdAndStatusAndDeletedAtIsNull(id, VocabularyStatus.APPROVED)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "VOCAB_NOT_FOUND", "Vocabulary not found"));
        return toResponse(vocabulary, loadExamples(vocabulary.getId()));
    }

    public VocabularyResponse createContribution(
            UUID userId,
            String term,
            String definition,
            String definitionVi,
            List<String> examples,
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
                .definitionVi(trimToNull(definitionVi))
                .phonetic(phonetic == null ? null : phonetic.trim())
                .partOfSpeech(partOfSpeech == null ? null : partOfSpeech.trim())
                .language(normalizedLanguage)
                .status(VocabularyStatus.PENDING)
                .createdBy(userId)
                .build();

        vocabulary = vocabularyRepository.save(vocabulary);

        if (examples != null && !examples.isEmpty()) {
            List<VocabularyExample> exampleEntities = new ArrayList<>();
            for (String example : examples) {
                if (example == null || example.trim().isEmpty()) {
                    continue;
                }
                exampleEntities.add(VocabularyExample.builder()
                        .vocabularyId(vocabulary.getId())
                        .example(example.trim())
                        .build());
            }
            if (!exampleEntities.isEmpty()) {
                vocabularyExampleRepository.saveAll(exampleEntities);
            }
        }

        if (topicIds != null && !topicIds.isEmpty()) {
            List<TopicVocabulary> links = buildTopicLinks(vocabulary.getId(), topicIds);
            topicVocabularyRepository.saveAll(links);
        }

        return toResponse(vocabulary, loadExamples(vocabulary.getId()));
    }

    public VocabularyResponse approve(UUID id) {
        Vocabulary vocabulary = vocabularyRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "VOCAB_NOT_FOUND", "Vocabulary not found"));
        vocabulary.setStatus(VocabularyStatus.APPROVED);
        vocabulary = vocabularyRepository.save(vocabulary);
        return toResponse(vocabulary, loadExamples(vocabulary.getId()));
    }

    public VocabularyResponse reject(UUID id) {
        Vocabulary vocabulary = vocabularyRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "VOCAB_NOT_FOUND", "Vocabulary not found"));
        vocabulary.setStatus(VocabularyStatus.REJECTED);
        vocabulary = vocabularyRepository.save(vocabulary);
        return toResponse(vocabulary, loadExamples(vocabulary.getId()));
    }

    public VocabularyResponse updateVocabulary(UUID id, UpdateVocabularyRequest request) {
        Vocabulary vocabulary = vocabularyRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "VOCAB_NOT_FOUND", "Vocabulary not found"));

        String updatedTermNormalized = vocabulary.getTermNormalized();
        String updatedLanguage = vocabulary.getLanguage();

        if (request.term() != null) {
            updatedTermNormalized = normalizeTerm(request.term());
            if (updatedTermNormalized == null) {
                throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_TERM", "Term is required");
            }
            vocabulary.setTerm(request.term().trim());
            vocabulary.setTermNormalized(updatedTermNormalized);
        }

        if (request.language() != null) {
            updatedLanguage = normalizeLanguage(request.language());
            if (updatedLanguage == null) {
                throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_LANGUAGE", "Language is required");
            }
            vocabulary.setLanguage(updatedLanguage);
        }

        if (request.term() != null || request.language() != null) {
            Vocabulary existing = vocabularyRepository.findByTermNormalizedAndLanguageAndDeletedAtIsNull(
                    updatedTermNormalized,
                    updatedLanguage
            ).orElse(null);
            if (existing != null && !existing.getId().equals(vocabulary.getId())) {
                throw new AppException(HttpStatus.CONFLICT, "VOCAB_EXISTS", "Vocabulary already exists");
            }
        }

        if (request.definition() != null) {
            String definition = request.definition().trim();
            if (definition.isEmpty()) {
                throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_DEFINITION", "Definition is required");
            }
            vocabulary.setDefinition(definition);
        }
        if (request.definitionVi() != null) {
            vocabulary.setDefinitionVi(trimToNull(request.definitionVi()));
        }
        if (request.phonetic() != null) {
            vocabulary.setPhonetic(trimToNull(request.phonetic()));
        }
        if (request.partOfSpeech() != null) {
            vocabulary.setPartOfSpeech(trimToNull(request.partOfSpeech()));
        }
        if (request.status() != null) {
            vocabulary.setStatus(request.status());
        }

        vocabulary = vocabularyRepository.save(vocabulary);
        return toResponse(vocabulary, loadExamples(vocabulary.getId()));
    }

    public void deleteVocabulary(UUID id) {
        Vocabulary vocabulary = vocabularyRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "VOCAB_NOT_FOUND", "Vocabulary not found"));
        vocabulary.setDeletedAt(LocalDateTime.now());
        vocabularyRepository.save(vocabulary);
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

    private Page<VocabularyResponse> toResponses(Page<Vocabulary> page) {
        List<UUID> ids = page.stream().map(Vocabulary::getId).toList();
        Map<UUID, List<String>> examplesByVocab = loadExamples(ids);
        return page.map(vocab -> toResponse(vocab, examplesByVocab.getOrDefault(vocab.getId(), List.of())));
    }

    private VocabularyResponse toResponse(Vocabulary vocabulary, List<String> examples) {
        return new VocabularyResponse(
                vocabulary.getId(),
                vocabulary.getTerm(),
                vocabulary.getDefinition(),
                vocabulary.getDefinitionVi(),
                examples,
                vocabulary.getPhonetic(),
                vocabulary.getPartOfSpeech(),
                vocabulary.getLanguage(),
                vocabulary.getStatus(),
                vocabulary.getCreatedBy(),
                vocabulary.getCreatedAt()
        );
    }

    private List<String> loadExamples(UUID vocabularyId) {
        return vocabularyExampleRepository.findByVocabularyId(vocabularyId).stream()
                .map(VocabularyExample::getExample)
                .toList();
    }

    private Map<UUID, List<String>> loadExamples(List<UUID> vocabularyIds) {
        Map<UUID, List<String>> result = new HashMap<>();
        if (vocabularyIds.isEmpty()) {
            return result;
        }
        List<VocabularyExample> examples = vocabularyExampleRepository.findByVocabularyIdIn(vocabularyIds);
        for (VocabularyExample example : examples) {
            result.computeIfAbsent(example.getVocabularyId(), key -> new ArrayList<>())
                    .add(example.getExample());
        }
        return result;
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

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
