package com.learnapp.repository;

import com.learnapp.entities.VocabularyContribution;
import com.learnapp.entities.VocabularyContributionStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VocabularyContributionRepository extends JpaRepository<VocabularyContribution, UUID> {

    Optional<VocabularyContribution> findByIdAndContributorUserId(UUID id, UUID contributorUserId);

    Page<VocabularyContribution> findByContributorUserId(UUID contributorUserId, Pageable pageable);

    Page<VocabularyContribution> findByContributorUserIdAndStatus(
            UUID contributorUserId,
            VocabularyContributionStatus status,
            Pageable pageable
    );

    @Query("""
            select vc
            from VocabularyContribution vc
            where (:status is null or vc.status = :status)
              and (:language is null or vc.language = :language)
              and (
                    :query is null
                    or lower(vc.term) like concat('%', :query, '%')
                    or lower(vc.definition) like concat('%', :query, '%')
              )
            """)
    Page<VocabularyContribution> searchForAdmin(
            @Param("query") String query,
            @Param("language") String language,
            @Param("status") VocabularyContributionStatus status,
            Pageable pageable
    );
}
