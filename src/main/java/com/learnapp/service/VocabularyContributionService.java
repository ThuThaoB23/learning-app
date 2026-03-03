package com.learnapp.service;

import com.learnapp.dto.AdminVocabularyContributionDetailResponse;
import com.learnapp.dto.AdminVocabularyContributionQueueItemResponse;
import com.learnapp.dto.CreateVocabularyRequest;
import com.learnapp.dto.RejectVocabularyContributionRequest;
import com.learnapp.dto.VocabularyContributionResponse;
import com.learnapp.dto.VocabularyContributionReviewLogResponse;
import com.learnapp.entities.Topic;
import com.learnapp.entities.TopicStatus;
import com.learnapp.entities.TopicVocabulary;
import com.learnapp.entities.User;
import com.learnapp.entities.Vocabulary;
import com.learnapp.entities.VocabularyContribution;
import com.learnapp.entities.VocabularyContributionExample;
import com.learnapp.entities.VocabularyContributionRejectReason;
import com.learnapp.entities.VocabularyContributionReviewAction;
import com.learnapp.entities.VocabularyContributionReviewLog;
import com.learnapp.entities.VocabularyContributionStatus;
import com.learnapp.entities.VocabularyContributionTopic;
import com.learnapp.entities.VocabularyStatus;
import com.learnapp.entities.VocabularyExample;
import com.learnapp.error.AppException;
import com.learnapp.repository.TopicRepository;
import com.learnapp.repository.TopicVocabularyRepository;
import com.learnapp.repository.UserRepository;
import com.learnapp.repository.VocabularyContributionExampleRepository;
import com.learnapp.repository.VocabularyContributionRepository;
import com.learnapp.repository.VocabularyContributionReviewLogRepository;
import com.learnapp.repository.VocabularyContributionTopicRepository;
import com.learnapp.repository.VocabularyExampleRepository;
import com.learnapp.repository.VocabularyRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
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
public class VocabularyContributionService {

    private final VocabularyContributionRepository vocabularyContributionRepository;
    private final VocabularyContributionExampleRepository vocabularyContributionExampleRepository;
    private final VocabularyContributionTopicRepository vocabularyContributionTopicRepository;
    private final VocabularyContributionReviewLogRepository vocabularyContributionReviewLogRepository;
    private final VocabularyRepository vocabularyRepository;
    private final VocabularyExampleRepository vocabularyExampleRepository;
    private final TopicRepository topicRepository;
    private final TopicVocabularyRepository topicVocabularyRepository;
    private final UserRepository userRepository;
    private final UserActivityLogService userActivityLogService;
    private final VocabularyAudioService vocabularyAudioService;

    public VocabularyContributionService(
            VocabularyContributionRepository vocabularyContributionRepository,
            VocabularyContributionExampleRepository vocabularyContributionExampleRepository,
            VocabularyContributionTopicRepository vocabularyContributionTopicRepository,
            VocabularyContributionReviewLogRepository vocabularyContributionReviewLogRepository,
            VocabularyRepository vocabularyRepository,
            VocabularyExampleRepository vocabularyExampleRepository,
            TopicRepository topicRepository,
            TopicVocabularyRepository topicVocabularyRepository,
            UserRepository userRepository,
            UserActivityLogService userActivityLogService,
            VocabularyAudioService vocabularyAudioService
    ) {
        this.vocabularyContributionRepository = vocabularyContributionRepository;
        this.vocabularyContributionExampleRepository = vocabularyContributionExampleRepository;
        this.vocabularyContributionTopicRepository = vocabularyContributionTopicRepository;
        this.vocabularyContributionReviewLogRepository = vocabularyContributionReviewLogRepository;
        this.vocabularyRepository = vocabularyRepository;
        this.vocabularyExampleRepository = vocabularyExampleRepository;
        this.topicRepository = topicRepository;
        this.topicVocabularyRepository = topicVocabularyRepository;
        this.userRepository = userRepository;
        this.userActivityLogService = userActivityLogService;
        this.vocabularyAudioService = vocabularyAudioService;
    }

    public VocabularyContributionResponse submit(UUID contributorUserId, CreateVocabularyRequest request) {
        String term = trimToNull(request.term());
        String definition = trimToNull(request.definition());
        String language = normalizeLanguage(request.language());
        String termNormalized = normalizeTerm(term);

        if (termNormalized == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_TERM", "Term is required");
        }
        if (definition == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_DEFINITION", "Definition is required");
        }
        if (language == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_LANGUAGE", "Language is required");
        }
        ensureUserExists(contributorUserId);

        List<UUID> topicIds = sanitizeTopicIds(request.topicIds());
        validateActiveTopics(topicIds);

        VocabularyContribution contribution = VocabularyContribution.builder()
                .contributorUserId(contributorUserId)
                .term(term)
                .termNormalized(termNormalized)
                .definition(definition)
                .definitionVi(trimToNull(request.definitionVi()))
                .phonetic(trimToNull(request.phonetic()))
                .partOfSpeech(trimToNull(request.partOfSpeech()))
                .language(language)
                .status(VocabularyContributionStatus.SUBMITTED)
                .build();
        contribution = vocabularyContributionRepository.save(contribution);

        saveContributionExamples(contribution.getId(), request.examples());
        saveContributionTopics(contribution.getId(), topicIds);
        saveReviewLog(contribution.getId(), VocabularyContributionReviewAction.SUBMIT, contributorUserId, null);
        userActivityLogService.logSubmitVocabContribution(
                contribution,
                countNonBlank(request.examples()),
                topicIds.size()
        );

        return toContributionResponse(contribution);
    }

    @Transactional(readOnly = true)
    public Page<VocabularyContributionResponse> listMine(
            UUID contributorUserId,
            VocabularyContributionStatus status,
            Pageable pageable
    ) {
        Page<VocabularyContribution> page = status == null
                ? vocabularyContributionRepository.findByContributorUserId(contributorUserId, pageable)
                : vocabularyContributionRepository.findByContributorUserIdAndStatus(contributorUserId, status, pageable);
        return page.map(this::toContributionResponse);
    }

    @Transactional(readOnly = true)
    public Page<AdminVocabularyContributionQueueItemResponse> searchForAdmin(
            String query,
            String language,
            VocabularyContributionStatus status,
            Pageable pageable
    ) {
        String normalizedQuery = normalizeSearch(query);
        String normalizedLanguage = normalizeLanguage(language);
        Page<VocabularyContribution> page = vocabularyContributionRepository.searchForAdmin(
                normalizedQuery,
                normalizedLanguage,
                status,
                pageable
        );

        Map<UUID, String> userNames = loadUserDisplayNames(page.stream().map(VocabularyContribution::getContributorUserId).toList());
        return page.map(contribution -> new AdminVocabularyContributionQueueItemResponse(
                contribution.getId(),
                contribution.getTerm(),
                contribution.getLanguage(),
                contribution.getPartOfSpeech(),
                contribution.getContributorUserId(),
                userNames.get(contribution.getContributorUserId()),
                contribution.getStatus(),
                contribution.getCreatedAt()
        ));
    }

    @Transactional(readOnly = true)
    public AdminVocabularyContributionDetailResponse getDetailForAdmin(UUID contributionId) {
        VocabularyContribution contribution = findContribution(contributionId);
        List<VocabularyContributionReviewLog> logs =
                vocabularyContributionReviewLogRepository.findByContributionIdOrderByCreatedAtAsc(contributionId);
        return new AdminVocabularyContributionDetailResponse(
                toContributionResponse(contribution),
                toReviewLogResponses(logs)
        );
    }

    public VocabularyContributionResponse approve(UUID contributionId, UUID adminUserId, String reviewNote) {
        VocabularyContribution contribution = findContribution(contributionId);
        ensureReviewable(contribution);

        String normalizedTerm = normalizeTerm(contribution.getTerm());
        String normalizedLanguage = normalizeLanguage(contribution.getLanguage());
        if (normalizedTerm == null || normalizedLanguage == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_CONTRIBUTION", "Contribution content is invalid");
        }

        vocabularyRepository.findByTermNormalizedAndLanguageAndDeletedAtIsNull(normalizedTerm, normalizedLanguage)
                .ifPresent(existing -> {
                    throw new AppException(HttpStatus.CONFLICT, "VOCAB_EXISTS", "Vocabulary already exists");
                });

        Vocabulary vocabulary = Vocabulary.builder()
                .term(contribution.getTerm())
                .termNormalized(normalizedTerm)
                .definition(contribution.getDefinition())
                .definitionVi(trimToNull(contribution.getDefinitionVi()))
                .phonetic(trimToNull(contribution.getPhonetic()))
                .partOfSpeech(trimToNull(contribution.getPartOfSpeech()))
                .language(normalizedLanguage)
                .status(VocabularyStatus.APPROVED)
                .createdBy(contribution.getContributorUserId())
                .build();
        vocabulary = vocabularyRepository.save(vocabulary);

        List<VocabularyContributionExample> contributionExamples =
                vocabularyContributionExampleRepository.findByContributionIdOrderByPositionAscCreatedAtAsc(contributionId);
        saveVocabularyExamples(vocabulary.getId(), contributionExamples);

        List<VocabularyContributionTopic> contributionTopics = vocabularyContributionTopicRepository.findByContributionId(contributionId);
        saveVocabularyTopics(vocabulary.getId(), contributionTopics);
        vocabularyAudioService.populateAudios(vocabulary);

        contribution.setStatus(VocabularyContributionStatus.APPROVED);
        contribution.setReviewedBy(adminUserId);
        contribution.setReviewedAt(LocalDateTime.now());
        contribution.setReviewNote(trimToNull(reviewNote));
        contribution.setRejectReason(null);
        contribution.setApprovedVocabularyId(vocabulary.getId());
        contribution = vocabularyContributionRepository.save(contribution);

        saveReviewLog(contributionId, VocabularyContributionReviewAction.APPROVE, adminUserId, reviewNote);
        userActivityLogService.logApproveVocabContribution(adminUserId, contribution);

        return toContributionResponse(contribution);
    }

    public VocabularyContributionResponse reject(
            UUID contributionId,
            UUID adminUserId,
            RejectVocabularyContributionRequest request
    ) {
        VocabularyContribution contribution = findContribution(contributionId);
        ensureReviewable(contribution);

        contribution.setStatus(VocabularyContributionStatus.REJECTED);
        contribution.setReviewedBy(adminUserId);
        contribution.setReviewedAt(LocalDateTime.now());
        contribution.setReviewNote(trimToNull(request.reviewNote()));
        contribution.setRejectReason(request.rejectReason());
        contribution.setApprovedVocabularyId(null);
        contribution = vocabularyContributionRepository.save(contribution);

        String note = request.rejectReason() + (trimToNull(request.reviewNote()) == null ? "" : (": " + trimToNull(request.reviewNote())));
        saveReviewLog(contributionId, VocabularyContributionReviewAction.REJECT, adminUserId, note);
        userActivityLogService.logRejectVocabContribution(adminUserId, contribution);

        return toContributionResponse(contribution);
    }

    private VocabularyContribution findContribution(UUID contributionId) {
        return vocabularyContributionRepository.findById(contributionId)
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        "VOCAB_CONTRIBUTION_NOT_FOUND",
                        "Vocabulary contribution not found"
                ));
    }

    private void ensureReviewable(VocabularyContribution contribution) {
        if (contribution.getStatus() == VocabularyContributionStatus.APPROVED
                || contribution.getStatus() == VocabularyContributionStatus.REJECTED
                || contribution.getStatus() == VocabularyContributionStatus.CANCELED) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "VOCAB_CONTRIBUTION_NOT_REVIEWABLE",
                    "Contribution is not reviewable"
            );
        }
    }

    private void ensureUserExists(UUID userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
    }

    private List<UUID> sanitizeTopicIds(List<UUID> topicIds) {
        if (topicIds == null || topicIds.isEmpty()) {
            return List.of();
        }
        Set<UUID> unique = new LinkedHashSet<>();
        for (UUID topicId : topicIds) {
            if (topicId == null) {
                throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_TOPIC_ID", "Topic id is required");
            }
            unique.add(topicId);
        }
        return List.copyOf(unique);
    }

    private void validateActiveTopics(List<UUID> topicIds) {
        for (UUID topicId : topicIds) {
            Topic topic = topicRepository.findByIdAndDeletedAtIsNull(topicId)
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "TOPIC_NOT_FOUND", "Topic not found"));
            if (topic.getStatus() != TopicStatus.ACTIVE) {
                throw new AppException(HttpStatus.BAD_REQUEST, "TOPIC_INACTIVE", "Topic is inactive");
            }
        }
    }

    private void saveContributionExamples(UUID contributionId, List<String> examples) {
        if (examples == null || examples.isEmpty()) {
            return;
        }
        List<VocabularyContributionExample> entities = new ArrayList<>();
        int position = 1;
        for (String raw : examples) {
            String value = trimToNull(raw);
            if (value == null) {
                continue;
            }
            entities.add(VocabularyContributionExample.builder()
                    .contributionId(contributionId)
                    .example(value)
                    .position(position++)
                    .build());
        }
        if (!entities.isEmpty()) {
            vocabularyContributionExampleRepository.saveAll(entities);
        }
    }

    private void saveContributionTopics(UUID contributionId, List<UUID> topicIds) {
        if (topicIds == null || topicIds.isEmpty()) {
            return;
        }
        List<VocabularyContributionTopic> links = new ArrayList<>();
        for (UUID topicId : topicIds) {
            links.add(VocabularyContributionTopic.builder()
                    .contributionId(contributionId)
                    .topicId(topicId)
                    .build());
        }
        vocabularyContributionTopicRepository.saveAll(links);
    }

    private void saveVocabularyExamples(UUID vocabularyId, List<VocabularyContributionExample> contributionExamples) {
        if (contributionExamples == null || contributionExamples.isEmpty()) {
            return;
        }
        List<VocabularyExample> examples = new ArrayList<>();
        for (VocabularyContributionExample contributionExample : contributionExamples) {
            String value = trimToNull(contributionExample.getExample());
            if (value == null) {
                continue;
            }
            examples.add(VocabularyExample.builder()
                    .vocabularyId(vocabularyId)
                    .example(value)
                    .build());
        }
        if (!examples.isEmpty()) {
            vocabularyExampleRepository.saveAll(examples);
        }
    }

    private void saveVocabularyTopics(UUID vocabularyId, List<VocabularyContributionTopic> contributionTopics) {
        if (contributionTopics == null || contributionTopics.isEmpty()) {
            return;
        }
        Set<UUID> seen = new HashSet<>();
        List<TopicVocabulary> topicLinks = new ArrayList<>();
        for (VocabularyContributionTopic contributionTopic : contributionTopics) {
            if (contributionTopic == null || contributionTopic.getTopicId() == null) {
                continue;
            }
            if (!seen.add(contributionTopic.getTopicId())) {
                continue;
            }
            topicLinks.add(TopicVocabulary.builder()
                    .topicId(contributionTopic.getTopicId())
                    .vocabularyId(vocabularyId)
                    .build());
        }
        if (!topicLinks.isEmpty()) {
            topicVocabularyRepository.saveAll(topicLinks);
        }
    }

    private void saveReviewLog(
            UUID contributionId,
            VocabularyContributionReviewAction action,
            UUID actorUserId,
            String note
    ) {
        vocabularyContributionReviewLogRepository.save(VocabularyContributionReviewLog.builder()
                .contributionId(contributionId)
                .action(action)
                .actorUserId(actorUserId)
                .note(trimToNull(note))
                .build());
    }

    private int countNonBlank(List<String> values) {
        if (values == null || values.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String value : values) {
            if (trimToNull(value) != null) {
                count++;
            }
        }
        return count;
    }

    private VocabularyContributionResponse toContributionResponse(VocabularyContribution contribution) {
        List<UUID> userIds = new ArrayList<>();
        if (contribution.getContributorUserId() != null) {
            userIds.add(contribution.getContributorUserId());
        }
        if (contribution.getReviewedBy() != null) {
            userIds.add(contribution.getReviewedBy());
        }
        Map<UUID, String> userNames = loadUserDisplayNames(userIds);
        List<String> examples = vocabularyContributionExampleRepository
                .findByContributionIdOrderByPositionAscCreatedAtAsc(contribution.getId())
                .stream()
                .map(VocabularyContributionExample::getExample)
                .toList();
        List<UUID> topicIds = vocabularyContributionTopicRepository.findByContributionId(contribution.getId())
                .stream()
                .map(VocabularyContributionTopic::getTopicId)
                .distinct()
                .toList();

        return new VocabularyContributionResponse(
                contribution.getId(),
                contribution.getContributorUserId(),
                userNames.get(contribution.getContributorUserId()),
                contribution.getTerm(),
                contribution.getDefinition(),
                contribution.getDefinitionVi(),
                examples,
                contribution.getPhonetic(),
                contribution.getPartOfSpeech(),
                contribution.getLanguage(),
                topicIds,
                contribution.getStatus(),
                contribution.getReviewNote(),
                contribution.getRejectReason(),
                contribution.getApprovedVocabularyId(),
                contribution.getReviewedBy(),
                contribution.getReviewedAt(),
                contribution.getCreatedAt(),
                contribution.getUpdatedAt()
        );
    }

    private List<VocabularyContributionReviewLogResponse> toReviewLogResponses(List<VocabularyContributionReviewLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return List.of();
        }
        Map<UUID, String> userNames = loadUserDisplayNames(logs.stream()
                .map(VocabularyContributionReviewLog::getActorUserId)
                .toList());
        return logs.stream()
                .map(log -> new VocabularyContributionReviewLogResponse(
                        log.getId(),
                        log.getAction(),
                        log.getActorUserId(),
                        userNames.get(log.getActorUserId()),
                        log.getNote(),
                        log.getCreatedAt()
                ))
                .toList();
    }

    private Map<UUID, String> loadUserDisplayNames(List<UUID> userIds) {
        Map<UUID, String> result = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return result;
        }
        Set<UUID> uniqueIds = new HashSet<>();
        for (UUID userId : userIds) {
            if (userId != null) {
                uniqueIds.add(userId);
            }
        }
        if (uniqueIds.isEmpty()) {
            return result;
        }
        for (User user : userRepository.findAllById(uniqueIds)) {
            result.put(user.getId(), user.getDisplayName());
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

    private String normalizeSearch(String query) {
        String normalized = trimToNull(query);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
