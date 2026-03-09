package com.learnapp.repository;

import com.learnapp.entities.UserFlashcardDeckHistory;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFlashcardDeckHistoryRepository extends JpaRepository<UserFlashcardDeckHistory, UUID> {
    Page<UserFlashcardDeckHistory> findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(
            UUID userId,
            LocalDateTime createdAt,
            Pageable pageable
    );
}
