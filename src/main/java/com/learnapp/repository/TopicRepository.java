package com.learnapp.repository;

import com.learnapp.entities.Topic;
import com.learnapp.entities.TopicStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TopicRepository extends JpaRepository<Topic, UUID> {
    Optional<Topic> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Topic> findBySlugAndDeletedAtIsNull(String slug);

    Page<Topic> findByStatusAndDeletedAtIsNull(TopicStatus status, Pageable pageable);

    boolean existsBySlug(String slug);

    boolean existsByNameIgnoreCase(String name);

    @Query("""
            select t
            from Topic t
            where t.deletedAt is null
              and (:name is null or lower(t.name) like concat('%', :name, '%'))
              and (:slug is null or lower(t.slug) like concat('%', :slug, '%'))
              and (:status is null or t.status = :status)
            """)
    Page<Topic> searchTopics(
            @Param("name") String name,
            @Param("slug") String slug,
            @Param("status") TopicStatus status,
            Pageable pageable
    );
}
