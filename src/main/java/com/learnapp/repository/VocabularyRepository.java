package com.learnapp.repository;

import com.learnapp.entities.Vocabulary;
import com.learnapp.entities.VocabularyStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface    VocabularyRepository extends JpaRepository<Vocabulary, UUID> {
    Optional<Vocabulary> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Vocabulary> findByIdAndStatusAndDeletedAtIsNull(UUID id, VocabularyStatus status);

    Optional<Vocabulary> findByTermNormalizedAndLanguageAndDeletedAtIsNull(String termNormalized, String language);

    List<Vocabulary> findByIdInAndStatusAndDeletedAtIsNull(Collection<UUID> ids, VocabularyStatus status);

    boolean existsByTermNormalizedAndLanguage(String termNormalized, String language);

    Page<Vocabulary> findByStatusAndDeletedAtIsNull(VocabularyStatus status, Pageable pageable);

    Page<Vocabulary> findByStatusAndDeletedAtIsNullAndLanguage(
            VocabularyStatus status,
            String language,
            Pageable pageable
    );

    Page<Vocabulary> findByStatusAndDeletedAtIsNullAndTermNormalizedContainingIgnoreCase(
            VocabularyStatus status,
            String termNormalized,
            Pageable pageable
    );

    Page<Vocabulary> findByStatusAndDeletedAtIsNullAndLanguageAndTermNormalizedContainingIgnoreCase(
            VocabularyStatus status,
            String language,
            String termNormalized,
            Pageable pageable
    );

    Page<Vocabulary> findByDeletedAtIsNull(Pageable pageable);

    Page<Vocabulary> findByDeletedAtIsNullAndLanguage(String language, Pageable pageable);

    Page<Vocabulary> findByDeletedAtIsNullAndTermNormalizedContainingIgnoreCase(String termNormalized, Pageable pageable);

    Page<Vocabulary> findByDeletedAtIsNullAndLanguageAndTermNormalizedContainingIgnoreCase(
            String language,
            String termNormalized,
            Pageable pageable
    );

    Page<Vocabulary> findByStatusAndDeletedAtIsNullAndLanguageAndIdNot(
            VocabularyStatus status,
            String language,
            UUID id,
            Pageable pageable
    );

    @Query("""
            select v
            from Vocabulary v
            join TopicVocabulary tv on tv.vocabularyId = v.id
            where tv.topicId = :topicId
              and (:status is null or v.status = :status)
              and v.deletedAt is null
              and (:language is null or v.language = :language)
              and (:termNormalized is null or v.termNormalized like concat('%', :termNormalized, '%'))
            """)
    Page<Vocabulary> searchByTopic(
            @Param("topicId") UUID topicId,
            @Param("status") VocabularyStatus status,
            @Param("language") String language,
            @Param("termNormalized") String termNormalized,
            Pageable pageable
    );

    @Query("""
            select v
            from Vocabulary v
            where v.deletedAt is null
              and (:status is null or v.status = :status)
              and (:language is null or v.language = :language)
              and (:termNormalized is null or v.termNormalized like concat('%', :termNormalized, '%'))
              and not exists (
                    select 1
                    from UserVocabulary uv
                    where uv.userId = :userId
                      and uv.vocabularyId = v.id
              )
            """)
    Page<Vocabulary> searchApprovedNotAddedByUser(
            @Param("userId") UUID userId,
            @Param("status") VocabularyStatus status,
            @Param("language") String language,
            @Param("termNormalized") String termNormalized,
            Pageable pageable
    );

    @Query("""
            select v
            from Vocabulary v
            join TopicVocabulary tv on tv.vocabularyId = v.id
            where tv.topicId = :topicId
              and v.deletedAt is null
              and (:status is null or v.status = :status)
              and (:language is null or v.language = :language)
              and (:termNormalized is null or v.termNormalized like concat('%', :termNormalized, '%'))
              and not exists (
                    select 1
                    from UserVocabulary uv
                    where uv.userId = :userId
                      and uv.vocabularyId = v.id
              )
            """)
    Page<Vocabulary> searchByTopicNotAddedByUser(
            @Param("userId") UUID userId,
            @Param("topicId") UUID topicId,
            @Param("status") VocabularyStatus status,
            @Param("language") String language,
            @Param("termNormalized") String termNormalized,
            Pageable pageable
    );

    @Query("""
            select v
            from Vocabulary v
            where v.deletedAt is null
              and (:status is null or v.status = :status)
              and (:language is null or v.language = :language)
              and (
                    :withoutAudioOnly = false
                    or not exists (
                        select 1
                        from VocabularyAudio va
                        where va.vocabularyId = v.id
                    )
              )
            """)
    Page<Vocabulary> searchForAudioBackfill(
            @Param("language") String language,
            @Param("status") VocabularyStatus status,
            @Param("withoutAudioOnly") boolean withoutAudioOnly,
            Pageable pageable
    );
}
