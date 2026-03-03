package com.learnapp.repository;

import com.learnapp.entities.VocabularyAudio;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VocabularyAudioRepository extends JpaRepository<VocabularyAudio, UUID> {
    List<VocabularyAudio> findByVocabularyIdOrderByPositionAscCreatedAtAsc(UUID vocabularyId);

    List<VocabularyAudio> findByVocabularyIdInOrderByVocabularyIdAscPositionAscCreatedAtAsc(List<UUID> vocabularyIds);

    java.util.Optional<VocabularyAudio> findByIdAndVocabularyId(UUID id, UUID vocabularyId);

    void deleteByVocabularyId(UUID vocabularyId);
}
