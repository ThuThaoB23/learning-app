package com.learnapp.repository;

import com.learnapp.entities.VocabularyContributionExample;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VocabularyContributionExampleRepository extends JpaRepository<VocabularyContributionExample, UUID> {
    List<VocabularyContributionExample> findByContributionIdOrderByPositionAscCreatedAtAsc(UUID contributionId);
}
