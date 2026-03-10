package com.learnapp.repository;

import com.learnapp.entities.UserFeedback;
import com.learnapp.entities.UserFeedbackCategory;
import com.learnapp.entities.UserFeedbackStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserFeedbackRepository extends JpaRepository<UserFeedback, UUID> {
    Page<UserFeedback> findByUserId(UUID userId, Pageable pageable);

    Optional<UserFeedback> findByIdAndUserId(UUID id, UUID userId);

    @Query("""
            select f
            from UserFeedback f
            where (:status is null or f.status = :status)
              and (:category is null or f.category = :category)
              and (
                    :query is null
                    or lower(coalesce(f.title, '')) like concat('%', :query, '%')
                    or lower(f.message) like concat('%', :query, '%')
              )
            """)
    Page<UserFeedback> searchForAdmin(
            @Param("query") String query,
            @Param("status") UserFeedbackStatus status,
            @Param("category") UserFeedbackCategory category,
            Pageable pageable
    );
}
