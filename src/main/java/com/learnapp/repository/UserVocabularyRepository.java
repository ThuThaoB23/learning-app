package com.learnapp.repository;

import com.learnapp.entities.UserVocabStatus;
import com.learnapp.entities.UserVocabulary;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserVocabularyRepository extends JpaRepository<UserVocabulary, UUID> {
    Optional<UserVocabulary> findByIdAndUserId(UUID id, UUID userId);

    Optional<UserVocabulary> findByUserIdAndVocabularyId(UUID userId, UUID vocabularyId);

    boolean existsByUserIdAndVocabularyId(UUID userId, UUID vocabularyId);

    void deleteByUserIdAndVocabularyId(UUID userId, UUID vocabularyId);

    Page<UserVocabulary> findByUserId(UUID userId, Pageable pageable);

    List<UserVocabulary> findByUserId(UUID userId);

    List<UserVocabulary> findByUserIdAndVocabularyIdIn(UUID userId, Collection<UUID> vocabularyIds);

    Page<UserVocabulary> findByUserIdAndStatus(UUID userId, UserVocabStatus status, Pageable pageable);

    long countByUserIdAndStatus(UUID userId, UserVocabStatus status);

    List<UserVocabulary> findByUserIdAndNextDueAtLessThanEqual(UUID userId, LocalDateTime endOfDay);

    List<UserVocabulary> findByUserIdAndProcessLessThanEqual(UUID userId, Integer threshold);

    List<UserVocabulary> findByUserIdAndLastReviewedAtIsNull(UUID userId);
}
