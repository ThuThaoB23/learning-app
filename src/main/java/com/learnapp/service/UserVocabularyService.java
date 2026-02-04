package com.learnapp.service;

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

    public UserVocabularyService(
            UserVocabularyRepository userVocabularyRepository,
            VocabularyRepository vocabularyRepository,
            UserRepository userRepository
    ) {
        this.userVocabularyRepository = userVocabularyRepository;
        this.vocabularyRepository = vocabularyRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<UserVocabulary> list(UUID userId, UserVocabStatus status, Pageable pageable) {
        ensureUserNotDeleted(userId);
        if (status == null) {
            return userVocabularyRepository.findByUserId(userId, pageable);
        }
        return userVocabularyRepository.findByUserIdAndStatus(userId, status, pageable);
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
                .progress(0)
                .build();

        return userVocabularyRepository.save(userVocabulary);
    }

    public UserVocabulary update(
            UUID userId,
            UUID vocabularyId,
            UserVocabStatus status,
            Integer progress,
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

        if (progress != null) {
            validateProgress(progress);
            userVocabulary.setProgress(progress);
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

    private void validateProgress(int progress) {
        if (progress < 0 || progress > 100) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_PROGRESS", "Progress must be between 0 and 100");
        }
    }
}
