package com.learnapp.repository;

import com.learnapp.entities.TopicVocabulary;
import com.learnapp.entities.TopicVocabularyId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicVocabularyRepository extends JpaRepository<TopicVocabulary, TopicVocabularyId> {
    boolean existsByTopicIdAndVocabularyId(UUID topicId, UUID vocabularyId);

    List<TopicVocabulary> findByTopicId(UUID topicId);
}
