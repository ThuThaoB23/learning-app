package com.learnapp.repository;

import com.learnapp.entities.VocabularyContributionTopic;
import com.learnapp.entities.VocabularyContributionTopicId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VocabularyContributionTopicRepository
        extends JpaRepository<VocabularyContributionTopic, VocabularyContributionTopicId> {

    List<VocabularyContributionTopic> findByContributionId(UUID contributionId);
}
