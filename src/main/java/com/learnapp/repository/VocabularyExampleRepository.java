package com.learnapp.repository;

import com.learnapp.entities.VocabularyExample;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VocabularyExampleRepository extends JpaRepository<VocabularyExample, UUID> {
    List<VocabularyExample> findByVocabularyId(UUID vocabularyId);

    List<VocabularyExample> findByVocabularyIdIn(List<UUID> vocabularyIds);
}
