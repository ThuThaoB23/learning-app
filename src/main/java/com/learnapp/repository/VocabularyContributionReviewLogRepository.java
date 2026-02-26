package com.learnapp.repository;

import com.learnapp.entities.VocabularyContributionReviewLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VocabularyContributionReviewLogRepository extends JpaRepository<VocabularyContributionReviewLog, UUID> {
    List<VocabularyContributionReviewLog> findByContributionIdOrderByCreatedAtAsc(UUID contributionId);
}
