package com.learnapp.service;

import com.learnapp.dto.UserVocabularyResponse;
import com.learnapp.entities.User;
import com.learnapp.entities.UserVocabStatus;
import com.learnapp.entities.UserVocabulary;
import com.learnapp.entities.Vocabulary;
import com.learnapp.entities.VocabularyStatus;
import com.learnapp.error.AppException;
import com.learnapp.repository.UserRepository;
import com.learnapp.repository.UserVocabularyRepository;
import com.learnapp.repository.VocabularyRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserVocabularyService {

    private final UserVocabularyRepository userVocabularyRepository;
    private final VocabularyRepository vocabularyRepository;
    private final UserRepository userRepository;
    private final SpacedRepetitionService spacedRepetitionService;

    public UserVocabularyService(
            UserVocabularyRepository userVocabularyRepository,
            VocabularyRepository vocabularyRepository,
            UserRepository userRepository,
            SpacedRepetitionService spacedRepetitionService
    ) {
        this.userVocabularyRepository = userVocabularyRepository;
        this.vocabularyRepository = vocabularyRepository;
        this.userRepository = userRepository;
        this.spacedRepetitionService = spacedRepetitionService;
    }

    @Transactional(readOnly = true)
    public Page<UserVocabulary> list(UUID userId, UserVocabStatus status, Pageable pageable) {
        ensureUserNotDeleted(userId);
        if (status == null) {
            return userVocabularyRepository.findByUserId(userId, pageable);
        }
        return userVocabularyRepository.findByUserIdAndStatus(userId, status, pageable);
    }

    @Transactional(readOnly = true)
    public Page<UserVocabularyResponse> listResponses(UUID userId, UserVocabStatus status, Pageable pageable) {
        Page<UserVocabulary> page = list(userId, status, pageable);
        Map<UUID, String> termsByVocabId = loadTerms(page.stream().map(UserVocabulary::getVocabularyId).toList());
        return page.map(userVocabulary -> toResponse(userVocabulary, termsByVocabId.get(userVocabulary.getVocabularyId())));
    }

    public UserVocabulary add(UUID userId, UUID vocabularyId) {
        ensureUserNotDeleted(userId);
        Vocabulary vocabulary = getApprovedVocabulary(vocabularyId);

        if (userVocabularyRepository.existsByUserIdAndVocabularyId(userId, vocabulary.getId())) {
            throw new AppException(HttpStatus.CONFLICT, "USER_VOCAB_EXISTS", "Vocabulary already added");
        }

        UserVocabulary userVocabulary = UserVocabulary.builder()
                .userId(userId)
                .vocabularyId(vocabulary.getId())
                .status(UserVocabStatus.NEW)
                .process(0)
                .build();

        return userVocabularyRepository.save(userVocabulary);
    }

    @Transactional(readOnly = true)
    public UserVocabularyResponse toResponse(UserVocabulary userVocabulary) {
        String term = vocabularyRepository.findByIdAndDeletedAtIsNull(userVocabulary.getVocabularyId())
                .map(Vocabulary::getTerm)
                .orElse(null);
        return toResponse(userVocabulary, term);
    }

    public UserVocabulary update(
            UUID userId,
            UUID vocabularyId,
            UserVocabStatus status,
            Integer process,
            LocalDateTime lastReviewedAt
    ) {
        ensureUserNotDeleted(userId);
        UserVocabulary userVocabulary = userVocabularyRepository.findByUserIdAndVocabularyId(userId, vocabularyId)
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        "USER_VOCAB_NOT_FOUND",
                        "User vocabulary not found"
                ));

        if (status != null) {
            userVocabulary.setStatus(status);
        }

        if (process != null) {
            validateProcess(process);
            userVocabulary.setProcess(process);
            userVocabulary.setNextDueAt(lastReviewedAt == null
                    ? null
                    : lastReviewedAt.plusDays(spacedRepetitionService.intervalDays(process)));
        }

        if (lastReviewedAt != null) {
            userVocabulary.setLastReviewedAt(lastReviewedAt);
        }

        return userVocabularyRepository.save(userVocabulary);
    }

    public void remove(UUID userId, UUID vocabularyId) {
        ensureUserNotDeleted(userId);
        UserVocabulary userVocabulary = userVocabularyRepository.findByUserIdAndVocabularyId(userId, vocabularyId)
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        "USER_VOCAB_NOT_FOUND",
                        "User vocabulary not found"
                ));
        userVocabularyRepository.delete(userVocabulary);
    }

    public UserVocabulary recordAttempt(UUID userId, UUID vocabularyId, boolean isCorrect, LocalDateTime attemptedAt) {
        ensureUserNotDeleted(userId);
        UserVocabulary userVocabulary = userVocabularyRepository.findByUserIdAndVocabularyId(userId, vocabularyId)
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        "USER_VOCAB_NOT_FOUND",
                        "User vocabulary not found"
                ));
        spacedRepetitionService.applyAttempt(userVocabulary, isCorrect, attemptedAt);
        syncStatusByProcess(userVocabulary);
        return userVocabularyRepository.save(userVocabulary);
    }

    private Vocabulary getApprovedVocabulary(UUID vocabularyId) {
        return vocabularyRepository.findByIdAndStatusAndDeletedAtIsNull(vocabularyId, VocabularyStatus.APPROVED)
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        "VOCAB_NOT_FOUND",
                        "Vocabulary not found"
                ));
    }

    private void ensureUserNotDeleted(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        if (user.getDeletedAt() != null) {
            throw new AppException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found");
        }
    }

    private void validateProcess(int process) {
        if (process < 0 || process > 100) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_PROGRESS", "Progress must be between 0 and 100");
        }
    }

    private void syncStatusByProcess(UserVocabulary userVocabulary) {
        int process = userVocabulary.getProcess() == null ? 0 : userVocabulary.getProcess();
        if (process >= 90) {
            userVocabulary.setStatus(UserVocabStatus.MASTERED);
            return;
        }
        if (process >= 1) {
            userVocabulary.setStatus(UserVocabStatus.LEARNING);
            return;
        }
        userVocabulary.setStatus(UserVocabStatus.NEW);
    }

    private Map<UUID, String> loadTerms(List<UUID> vocabularyIds) {
        if (vocabularyIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, String> termsById = new HashMap<>();
        vocabularyRepository.findAllById(vocabularyIds)
                .forEach(vocabulary -> termsById.put(vocabulary.getId(), vocabulary.getTerm()));
        return termsById;
    }

    private UserVocabularyResponse toResponse(UserVocabulary userVocabulary, String term) {
        return new UserVocabularyResponse(
                userVocabulary.getVocabularyId(),
                term,
                userVocabulary.getStatus(),
                userVocabulary.getProcess(),
                userVocabulary.getLastReviewedAt(),
                userVocabulary.getCreatedAt(),
                userVocabulary.getUpdatedAt()
        );
    }
}
