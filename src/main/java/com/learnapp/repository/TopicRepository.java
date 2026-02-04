package com.learnapp.repository;

import com.learnapp.entities.Topic;
import com.learnapp.entities.TopicStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicRepository extends JpaRepository<Topic, UUID> {
    Optional<Topic> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Topic> findBySlugAndDeletedAtIsNull(String slug);

    Page<Topic> findByStatusAndDeletedAtIsNull(TopicStatus status, Pageable pageable);

    boolean existsBySlug(String slug);
}
